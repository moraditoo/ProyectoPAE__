# Proyecto PAE

Aplicación JavaFX con JDK 21, Hibernate y PostgreSQL. Los reportes de citas se
generan con **JasperReports 7.0.6** y se exportan en PDF y CSV.

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

En **Menú → Reportes**, seleccionar fechas, servicio y columnas. Pulsar
**Generar reporte**, y después **Exportar PDF / CSV**. El diálogo permite escoger
el formato. Los cambios de filtros o casillas requieren volver a generar.
La exportación respeta el orden actual de filas y columnas de la tabla.

La plantilla editable está en `src/main/resources/reportes/citas.jrxml`.
JasperReports la compila y rellena dentro de la aplicación, utilizando los datos
filtrados por Hibernate. El diagnóstico guarda ejemplos con datos ficticios en
`target/diagnostico-reportes/`; esos datos no se insertan en PostgreSQL.

## Base de datos y acceso

Se usa PostgreSQL, no H2. Hibernate crea o actualiza las tablas en la base existente.
Las variables `PAE_DB_URL`, `PAE_DB_USER` y `PAE_DB_PASSWORD` tienen prioridad sobre
el archivo personal. El directorio de trabajo debe ser la raíz del proyecto.

En una base sin usuarios, el inicio de sesión crea `admin` / `admin123`.
`PAE_ADMIN_USER` y `PAE_ADMIN_PASSWORD` permiten elegir esos valores antes del
primer inicio. Una base que ya tenga usuarios conserva sus cuentas.

Las pruebas automáticas cubren el motor de reportes sin conectarse a bases de datos
ni abrir ventanas. El diagnóstico comprueba la integración con la base local.
