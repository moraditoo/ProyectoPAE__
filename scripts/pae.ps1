param(
    [ValidateSet('verificar', 'diagnostico', 'ejecutar')]
    [string]$Accion = 'ejecutar',
    [string]$Jdk
)

$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot

function Test-Jdk21([string]$Ruta) {
    if (-not $Ruta -or -not (Test-Path -LiteralPath (Join-Path $Ruta 'bin/javac.exe'))) { return $false }
    $release = Join-Path $Ruta 'release'
    return (Test-Path -LiteralPath $release) -and
        [bool](Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="21(?:\.|"|\+)' -Quiet)
}

$candidatos = @()
if ($Jdk) {
    if (-not (Test-Jdk21 $Jdk)) { throw 'El parametro -Jdk debe apuntar a la carpeta de un JDK 21.' }
    $candidatos += $Jdk
} else {
    $candidatos += $env:JAVA_HOME
    $javacLocal = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacLocal) { $candidatos += Split-Path -Parent (Split-Path -Parent $javacLocal.Source) }
    $carpetas = @(
        (Join-Path $env:USERPROFILE '.jdks'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Microsoft')
    )
    foreach ($carpeta in $carpetas) {
        if (Test-Path -LiteralPath $carpeta) {
            $candidatos += Get-ChildItem -LiteralPath $carpeta -Directory | Select-Object -ExpandProperty FullName
        }
    }
}
$jdkElegido = $candidatos | Where-Object { Test-Jdk21 $_ } | Select-Object -First 1
if (-not $jdkElegido) {
    throw 'No se encontro JDK 21. Instalalo desde https://adoptium.net/temurin/releases/?version=21 o usa -Jdk con su ruta.'
}

# Solo modifica el entorno de esta ejecucion; no requiere permisos de administrador.
$javaAnterior = $env:JAVA_HOME
$pathAnterior = $env:PATH
$codigo = 1
Push-Location $raizProyecto
try {
    $env:JAVA_HOME = $jdkElegido
    $env:PATH = (Join-Path $jdkElegido 'bin') + [IO.Path]::PathSeparator + $env:PATH
    Write-Host "JDK 21: $jdkElegido"
    switch ($Accion) {
        'verificar' { & .\mvnw.cmd -B clean verify }
        'diagnostico' { & .\mvnw.cmd -B compile -Pdiagnostico javafx:run }
        'ejecutar' { & .\mvnw.cmd -B compile javafx:run }
    }
    $codigo = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $javaAnterior
    $env:PATH = $pathAnterior
    Pop-Location
}
if ($codigo -ne 0) { throw "Maven termino con codigo $codigo. Revisa el error mostrado arriba." }
