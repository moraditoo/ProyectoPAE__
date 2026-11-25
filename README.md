# Proyecto PAE

Aplicación JavaFX con JDK 21, Hibernate y PostgreSQL. Los reportes generales se
generan con **JasperReports 7.0.6** y se exportan únicamente en PDF, con apartados y
campos seleccionables mediante casillas.

La [guía de instalación del equipo](docs/CONFIGURACION_EQUIPO.md) contiene los pasos
para preparar las cuatro computadoras, configurar IntelliJ y editar las plantillas
con Jaspersoft Studio 7.0.6.

## Preparación inicial

1. Instalar JDK 21 y PostgreSQL (la guía usa PostgreSQL 18).
2. Crear una base vacía llamada `tienda_maquillaje_db` en PostgreSQL.
3. Copiar `config/pae-local.example.properties` como `config/pae-local.properties`
   y escribir las credenciales locales. El archivo personal está excluido de Git.

Desde la raíz del proyecto, en PowerShell:

```powershell
# Ejecutar solo la primera vez; después completar las credenciales del archivo.
Copy-Item config/pae-local.example.properties config/pae-local.properties

# Compilar, probar JasperReports y empaquetar (no requiere PostgreSQL encendido).
.\scripts\pae.ps1 -Accion verificar

# Comprobar JasperReports, las fuentes y la conexión real con PostgreSQL.
.\scripts\pae.ps1 -Accion diagnostico

# Abrir la aplicación.
.\scripts\pae.ps1 -Accion ejecutar
```

El script encuentra JDK 21, incluido el instalado por IntelliJ en `.jdks`.
Se puede elegir uno explícitamente con `-Jdk 'C:\ruta\al\jdk-21'`.
Maven Wrapper descarga las dependencias; no hace falta instalar Maven, JavaFX ni
los JAR de JasperReports por separado. La primera ejecución necesita Internet.

Con `JAVA_HOME` configurado también se puede usar directamente el wrapper:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd compile -Pdiagnostico javafx:run
.\mvnw.cmd compile javafx:run
```

## Reportes

En **Menú → Reportes**:

1. Marcar los apartados que se quieren incluir. Inicialmente están todos seleccionados.
2. Desplegar cada apartado con su flecha y marcar sus campos. Cada apartado incluido
   necesita al menos un campo. **Seleccionar todo** restaura todos los apartados y campos;
   **Ninguno**, junto a ese botón, desmarca los apartados conservando sus campos.
3. Elegir fechas y servicio si corresponde; dejarlos vacíos incluye todo el período.
4. Pulsar **Generar reporte** y revisar cada apartado en su pestaña.
5. Pulsar **Imprimir** para abrir **JasperViewer**, el visor original de JasperReports.
   Su barra de herramientas permite recorrer las páginas y ajustar la ampliación.
6. Desde JasperViewer, usar el icono de impresora para imprimir o el icono de
   guardar para elegir el formato **PDF** y la ubicación del documento.
   **Exportar PDF** también permite guardar el documento desde el generador.

El visor reúne los apartados del reporte generado y conserva sus tamaños de página
A4 o A3, sin consultar de nuevo la base. Utiliza la interfaz Swing propia de
JasperReports, con sus controles y apariencia originales. Cerrar JasperViewer
conserva abierta la aplicación. Consulta la [guía de impresión](docs/IMPRESION_REPORTES.md).

Están disponibles resumen general, productos e inventario, lotes y caducidad,
clientes, servicios, agenda de citas, ventas, detalle de ventas, consumos de
insumos, categorías, marcas y usuarios con sus roles. No se incluyen contraseñas.

Las fechas filtran ventas y sus detalles, citas, consumos según su cita e ingreso
de lotes. Los catálogos y el stock corresponden al estado actual. El filtro de
servicio afecta servicios, citas, consumos y los indicadores de citas del resumen;
las ventas de productos son independientes del servicio.

El PDF incluye una sección por apartado, con tablas en A4 o A3 horizontal según
la cantidad de campos. Los identificadores de la base son campos seleccionables.
El PDF conserva la selección y los datos de la vista previa. JasperViewer ofrece
únicamente PDF en su opción de guardado.

Cambiar filtros o casillas limpia la vista previa y requiere generar de nuevo.
Los apartados sin resultados se pueden exportar con el aviso **Sin registros con
los filtros seleccionados**. **Limpiar filtros** conserva las casillas elegidas.

La plantilla activa está en `src/main/resources/reportes/general.jrxml`. El motor
construye sus columnas según la selección y la rellena con datos consultados por
Hibernate. `citas.jrxml` se conserva para el motor anterior y sus pruebas. El
diagnóstico guarda ejemplos ficticios de los 12 apartados en
`target/diagnostico-reportes/`; esos datos nunca se insertan en PostgreSQL.

## Base de datos y acceso

Se usa PostgreSQL, no H2. Hibernate crea o actualiza las tablas en la base existente.
Las variables `PAE_DB_URL`, `PAE_DB_USER` y `PAE_DB_PASSWORD` tienen prioridad sobre
el archivo personal. El directorio de trabajo debe ser la raíz del proyecto.

En una base sin usuarios, el inicio de sesión crea `admin` / `admin123`.
`PAE_ADMIN_USER` y `PAE_ADMIN_PASSWORD` permiten elegir esos valores antes del
primer inicio. Una base que ya tenga usuarios conserva sus cuentas.

Las pruebas del motor no requieren PostgreSQL; las de consultas usan una base H2
aislada y las de pantalla cargan el FXML en una sesión gráfica sin abrir ventanas.
El diagnóstico comprueba JasperReports y la consulta de los 12 apartados en la base local.
