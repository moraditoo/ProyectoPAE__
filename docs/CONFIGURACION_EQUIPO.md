# Configuración para los cuatro integrantes

Cada integrante debe completar estos pasos en su computadora y usar la misma
revisión del repositorio. Esta guía toma Windows e IntelliJ IDEA como referencia.
Cada equipo tendrá su propia base y contraseña; compartir código no comparte datos.

## 1. Herramientas acordadas

- **JDK 21**, preferiblemente Eclipse Temurin, desde [Adoptium](https://adoptium.net/temurin/releases/?version=21).
  Elegir JDK, Windows y la arquitectura del equipo. También sirve el JDK 21 que
  descarga IntelliJ. Maven comprueba la versión y rechaza otras versiones mayores.
- **PostgreSQL 18**, con servidor y pgAdmin, desde la
  [página oficial para Windows](https://www.postgresql.org/download/windows/).
  Si ya funciona una instalación de PostgreSQL, conservarla; no hace falta
  reinstalar el servidor para usar JasperReports.
- **IntelliJ IDEA**, con soporte Java y Maven.
- **Jaspersoft Studio Community 7.0.6**, para quienes editen el diseño. Está en
  [descargas oficiales](https://community.jaspersoft.com/download-jaspersoft/community-edition/),
  sección **Other packages and previous releases → Jaspersoft Studio community edition 7.x → Version 7.0.6**.
  Elegir el paquete de Windows. Esta versión se fija para coincidir con el motor
  del proyecto; no se presenta como la última versión disponible.

La ejecución necesita JDK y PostgreSQL. Maven descarga automáticamente
JasperReports, su exportador PDF, el compilador JDT y las fuentes. Studio es el
editor visual del JRXML y no necesita permanecer abierto al ejecutar la aplicación.
No se utiliza JasperReports Server ni se necesita instalar un complemento de
JasperReports en IntelliJ.

## 2. Obtener el código

Abrir la carpeta del repositorio que contiene `pom.xml`, `mvnw.cmd` y `src`.
Los cuatro deben tener los cambios de JasperReports antes de continuar.

No copiar las carpetas `.idea`, `target` o `.m2` entre compañeros. Maven descarga
las bibliotecas que correspondan al equipo. No agregar JAR manualmente a IntelliJ.

## 3. Preparar PostgreSQL

1. En el instalador, incluir el servidor y pgAdmin. Anotar la contraseña elegida
   para `postgres` y el puerto (normalmente `5432`). Stack Builder es opcional.
2. En pgAdmin, conectarse al servidor local. En **Databases → Create → Database**,
   crear `tienda_maquillaje_db`, con propietario `postgres` o el usuario que use el equipo.
   Si esa base ya existe, conservarla.
3. Alternativamente, ejecutar en el Query Tool conectado a la base `postgres`,
   solo cuando aún no exista la base del proyecto:

   ```sql
   CREATE DATABASE tienda_maquillaje_db;
   ```

Hibernate usa `hbm2ddl.auto=update`: crea o actualiza las tablas al conectarse.
El usuario configurado necesita permiso para crear y modificar tablas. La base
debe existir antes de ejecutar la aplicación. No se necesita restaurar un respaldo
para arrancar con una base vacía.

## 4. Configurar las credenciales de cada computadora

Desde PowerShell, en la raíz del proyecto, ejecutar **una sola vez**:

```powershell
Copy-Item config/pae-local.example.properties config/pae-local.properties
```

Abrir el nuevo archivo en IntelliJ y completar:

```properties
db.url=jdbc:postgresql://localhost:5432/tienda_maquillaje_db
db.user=postgres
db.password=ESCRIBIR_LA_CLAVE_DE_ESTE_EQUIPO
```

Usar valores sin comillas y guardar en UTF-8. En un archivo `.properties`, una
barra inversa literal se escribe `\\`; si la contraseña empieza con un espacio,
escapar ese espacio con `\ `. No compartir este archivo: está excluido de Git.
Si ya existe, editarlo conservando los valores correctos en vez de volver a copiar
el ejemplo encima.

Prioridad de configuración:

1. Variables de entorno `PAE_DB_URL`, `PAE_DB_USER` y `PAE_DB_PASSWORD`.
2. `config/pae-local.properties` del equipo.
3. Valores generales de `src/main/resources/hibernate.cfg.xml`.

No es necesario modificar `hibernate.cfg.xml` por computadora. Las variables
antiguas en una configuración de IntelliJ pueden sobrescribir el archivo personal;
revisarlas si la aplicación intenta usar otro servidor o usuario.

## 5. Validar e iniciar desde PowerShell

En la raíz del repositorio:

```powershell
.\scripts\pae.ps1 -Accion verificar
.\scripts\pae.ps1 -Accion diagnostico
.\scripts\pae.ps1 -Accion ejecutar
```

El script busca un JDK 21 en `JAVA_HOME`, el PATH, la carpeta `.jdks` de IntelliJ
y las ubicaciones habituales de instalación. Cambia el entorno solo durante la
ejecución. No modifica la configuración global de Windows.

Si hay varias instalaciones o el JDK está en otra ubicación:

```powershell
.\scripts\pae.ps1 -Accion verificar -Jdk 'C:\ruta\al\jdk-21'
```

Si PowerShell bloquea el archivo `.ps1`, se puede ejecutar ese comando con una
excepción limitada al proceso, sin cambiar la política global:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\pae.ps1 -Accion verificar
```

Cambiar `verificar` por `diagnostico` o `ejecutar` según corresponda.

Resultados esperados:

- **verificar:** `BUILD SUCCESS`, con todas las pruebas de reportes aprobadas.
  Comprueba compilación, plantilla, PDF, CSV, columnas, varias páginas y textos
  largos. No necesita servidor de base de datos ni interfaz gráfica.
- **diagnostico:** muestra `[OK] JasperReports` y `[OK] PostgreSQL e Hibernate`,
  seguidos de `BUILD SUCCESS`. Usa el mismo sistema de módulos que la aplicación,
  por lo que también detecta bibliotecas o fuentes ausentes al ejecutar.
  El diagnóstico inicializa Hibernate con el esquema habitual `update` y consulta
  las citas existentes; no inserta las citas ficticias del PDF de ejemplo.
- **ejecutar:** abre el acceso de la aplicación. En una base sin usuarios,
  la cuenta inicial predeterminada es `admin` / `admin123`. Si la base ya tenía
  cuentas, usar una de esas cuentas.

El diagnóstico escribe `reporte-general-ejemplo.pdf` y `reporte-general-ejemplo.csv` dentro de
`target/diagnostico-reportes`. `verificar` limpia `target`, por lo que debe
ejecutarse antes del diagnóstico si se quieren conservar los ejemplos para revisar.

Con `JAVA_HOME` configurado, los comandos equivalentes son:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd compile -Pdiagnostico javafx:run
.\mvnw.cmd compile javafx:run
```

En macOS o Linux, usar `./mvnw` con JDK 21 y los mismos objetivos Maven, y mantener
el archivo personal en `config/`. La interfaz necesita una sesión gráfica.

## 6. Configurar IntelliJ IDEA

1. Abrir `pom.xml` como proyecto Maven y esperar a que termine la sincronización.
2. En **File → Project Structure → Project**, seleccionar **SDK 21** y nivel de
   lenguaje **21**. Usar **Download JDK → 21 → Eclipse Temurin** si hace falta.
3. En **Settings → Build, Execution, Deployment → Build Tools → Maven**, seleccionar
   **Use Maven wrapper** como Maven home. En **Runner**, seleccionar JDK 21 como JRE.
   En **Importing**, usar también JDK 21 para la importación de Maven.
4. En la ventana Maven, ejecutar **Reload All Maven Projects**.
5. Crear una configuración **Maven** con directorio de trabajo igual a la raíz
   del repositorio y línea de comandos `compile javafx:run`.
6. Crear otra configuración Maven para `compile -Pdiagnostico javafx:run` si se
   quiere ejecutar el diagnóstico desde el IDE. No dejar activado el perfil
   `diagnostico` en la configuración normal de la aplicación.

El archivo `config/pae-local.properties` funciona también desde IntelliJ cuando
el directorio de trabajo es la raíz del repositorio. El wrapper y Maven resuelven
JavaFX y JasperReports; no se necesitan rutas locales a un SDK de JavaFX ni a los
JAR de Studio en los argumentos de la VM.

## 7. Instalar y usar Jaspersoft Studio

1. Instalar Studio Community **7.0.6** desde la descarga oficial citada arriba.
   Mantener la misma versión en quienes editen la plantilla. Si el sitio solicita
   una cuenta para descargar, completar ese acceso en el navegador.
2. Abrir Studio y elegir un workspace personal, por ejemplo
   `C:\Users\TU_USUARIO\JaspersoftWorkspace`. Mantenerlo fuera de este repositorio.
3. Abrir el archivo real `src/main/resources/reportes/general.jrxml` con
   **File → Open File**. Si se importa dentro de un proyecto de Studio, asegurarse
   de guardar los cambios en el JRXML del repositorio.
4. Editar fuentes, colores, tamaños, títulos y bordes en la vista **Design**.
   Esta plantilla usa sintaxis de JasperReports 7. No convertirla a sintaxis 6.x.
5. Para una vista previa del diseño en Studio, elegir el adaptador vacío
   **One Empty Record / Empty Data Adapter**. Los datos reales los entrega la
   aplicación; no es necesario configurar una conexión PostgreSQL en Studio.
   En esa vista previa se pueden proporcionar los parámetros `TITULO`, `ALCANCE`,
   `FILTROS`, `GENERADO` y `RESUMEN`. Las columnas se crean desde Java según el
   checklist, por lo que Studio solo muestra la estructura del diseño. Para ver
   las tablas completas, ejecutar el diagnóstico del proyecto y abrir su PDF.
6. Guardar el JRXML, ejecutar `verificar` y después `diagnostico` desde el proyecto.
   Revisar el PDF antes de compartir el cambio.

La aplicación compila la plantilla al generar el reporte. Solo se versiona el
JRXML; no se deben sustituir las dependencias Maven por las bibliotecas internas
de Studio ni copiar archivos `.jasper` compilados por otra versión.
Jaspersoft documenta la [compatibilidad entre Studio y el motor](https://community.jaspersoft.com/knowledgebase/faq/understanding-version-compatibility-across-jaspersoft-studio-jasperreports-library-and-jasperreports-server-r5085/).

### Contrato de la plantilla

`general.jrxml` conserva los parámetros String `TITULO`, `ALCANCE`, `FILTROS`,
`GENERADO` y `RESUMEN`, los estilos `Cabecera` y `Celda`, las bandas de cabecera y
detalle, y las claves `ancho_*` y `pagina`. `ReporteGeneralJasperService` agrega
los campos String y sus columnas a esas bandas. Las claves de las columnas
generadas son `cabecera_<clave>` y `detalle_<clave>`. Conservar las propiedades
`export.csv` para excluir títulos, totales, pies y cabeceras repetidas del CSV.

El catálogo de opciones está en `ApartadoReporte`; la selección inmutable, en
`SolicitudReporte`, y las consultas de solo lectura, en `ReporteGeneralDAO`.
El PDF combina un documento por apartado y usa A3 horizontal al superar ocho
campos. El CSV usa siempre Apartado, Registro, Campo y Valor para admitir varios
apartados con campos distintos en un único archivo.

`citas.jrxml` y `ReporteJasperService` conservan el motor anterior y sus pruebas;
la pantalla de reportes utiliza el motor general.

Las fuentes **DejaVu Sans** se distribuyen por Maven y se incrustan en el PDF,
para conservar los acentos y el aspecto en los cuatro equipos.

## 8. Comprobar el flujo de usuario

1. Iniciar sesión y abrir **Clientes** para registrar un cliente si la base está vacía.
2. Abrir **Agenda de citas**, que precarga los servicios básicos cuando no hay
   servicios registrados. Seleccionar cliente, servicio, fecha y hora, registrar
   una cita y después abrir **Reportes**.
3. En Reportes, pulsar **Ninguno** junto a **Seleccionar todo** y marcar Clientes
   y Agenda de citas. Desplegar cada apartado con su flecha y escoger campos.
4. Elegir fechas que incluyan esa cita, pulsar **Generar reporte** y comprobar
   ambas pestañas, los campos y los datos. La tabla permite desplazarse para ver
   todas las columnas; el orden de la consulta se conserva en la exportación.
5. Pulsar **Exportar PDF / CSV**, guardar un PDF y comprobar sus dos secciones.
6. Elegir CSV en el mismo diálogo y comprobar sus columnas Apartado, Registro,
   Campo y Valor. Un registro produce una fila por campo elegido.
7. Desmarcar un campo: la vista previa se limpia y Exportar queda desactivado.
   Generar de nuevo y comprobar que ese campo ya no aparece en ambos formatos.
8. Dejar solo Agenda de citas y probar un intervalo sin citas. La tabla muestra
   el aviso de ausencia de registros y permite exportar ese resultado.
9. Pulsar **Seleccionar todo** y generar para comprobar los 12 apartados. Los
   catálogos muestran datos actuales; las fechas afectan movimientos e ingreso
   de lotes. Servicio afecta servicios, citas, consumos y citas del resumen.
10. **Limpiar filtros** borra fechas y servicio, conservando apartados y campos.
    No se puede generar sin apartados ni con un apartado incluido sin campos.

Cambiar filtros o casillas limpia la vista previa para evitar exportar resultados
de una consulta anterior. La generación y exportación se ejecutan en segundo
plano; mientras trabajan se desactivan los controles relacionados.

## 9. Errores frecuentes

- **No se encontró JDK 21 / JAVA_HOME inválido:** instalar JDK 21 y usar `-Jdk`.
  Seleccionar también 21 en el SDK y el Maven Runner de IntelliJ.
- **Connection refused:** iniciar el servicio PostgreSQL y comprobar puerto y
  servidor en `db.url`.
- **database ... does not exist:** crear `tienda_maquillaje_db` en ese servidor.
- **password authentication failed:** corregir `db.user` y `db.password` del
  archivo personal, y revisar si hay variables `PAE_DB_*` sobrescribiéndolo.
- **Module net.sf.jasperreports... not found / clase JRPdfExporter ausente:**
  recargar Maven, comprobar Internet y ejecutar `verificar` desde el wrapper.
  Conservar la misma versión para las cuatro dependencias de JasperReports.
- **Error al leer el JRXML:** comprobar que Studio usa la versión acordada,
  conservar las claves y campos de la plantilla y volver a ejecutar las pruebas.
- **No se puede guardar el PDF o CSV:** cerrar el archivo en el visor o Excel,
  elegir una carpeta con permiso de escritura y volver a exportar.
- **No aparecen datos:** comprobar las fechas y el servicio; cada compañero ve
  los datos de su propia base. El diagnóstico no registra citas ficticias.
- **El JAR no inicia con doble clic:** el empaquetado actual no es un instalador
  autónomo. Usar `pae.ps1 -Accion ejecutar` o Maven con `javafx:run`.

## 10. Criterio para dar cada computadora por configurada

Cada integrante debe confirmar que: `verificar` termina correctamente,
`diagnostico` muestra ambos `[OK]`, la aplicación permite iniciar sesión y el flujo
de usuario produce un PDF y un CSV con varios apartados de su base. La verificación de una
computadora no sustituye la de las otras tres.

## Archivos de la integración

- `pom.xml` y `module-info.java`: dependencias y módulos JasperReports.
- `src/main/resources/jasperreports.properties`: compilador JDT incluido por Maven.
- `src/main/resources/reportes/general.jrxml`: estructura y estilos del reporte activo.
- `reportes/ApartadoReporte.java` y `SolicitudReporte.java`: campos y selección.
- `dao/ReporteGeneralDAO.java`: consultas de solo lectura mediante Hibernate.
- `reportes/ReporteGeneralJasperService.java`: generación de todos los apartados.
- `reportes/ExportadorJasper.java`: guardado PDF y CSV con protección de fórmulas.
- `controllers/ReporteController.java`: checklist, filtros, pestañas y guardado desde JavaFX.
- `reportes/DiagnosticoReportes.java`: verificación del motor y PostgreSQL local.
- `scripts/pae.ps1`: selección de JDK y comandos comunes del equipo.
- `config/pae-local.example.properties`: ejemplo sin credenciales personales.
- `src/test/java/.../reportes/`: pruebas de selección y exportación sin PostgreSQL.
- `src/test/java/.../dao/ReporteGeneralDAOTest.java`: consultas con H2 aislado.
- `src/test/java/.../controllers/ReporteControllerTest.java`: carga del FXML y flujo
  de selección, vista previa, invalidación, errores y exportación. Necesita una
  sesión gráfica JavaFX, pero no abre ventanas ni toca la base del equipo.
