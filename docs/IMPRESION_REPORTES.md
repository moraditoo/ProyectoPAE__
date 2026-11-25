# Impresión con JasperViewer

## Uso

1. Abrir **Reportes**, seleccionar apartados, campos y filtros y pulsar **Generar reporte**.
2. Pulsar **Imprimir** para abrir **JasperViewer**, el visor original de JasperReports.
3. Usar las flechas, el número de página y el zoom de su barra de herramientas.
4. Pulsar el icono de impresora para abrir el diálogo de impresión.
5. Pulsar el icono de guardar, elegir **PDF** en el selector de formato e indicar
   el nombre y la ubicación del archivo.

La barra de herramientas, los diálogos y la apariencia pertenecen a JasperReports.
El visor utiliza Swing, incluido en Java, y conserva la presentación original de
la herramienta. El resto de la aplicación continúa usando JavaFX.

El botón del generador se habilita después de generar el reporte. Cambiar filtros
o campos lo deshabilita hasta generar de nuevo. JasperViewer muestra los datos
del documento que se abrió; no vuelve a consultar PostgreSQL.

Todos los apartados se reúnen en un JasperPrint con partes, conservando el orden
de las páginas y el formato A4 o A3 de cada apartado. El guardado del visor incluye
el documento completo. La exportación PDF y CSV del generador sigue disponible.

JasperViewer se abre con `isExitOnClose=false` y cierre `DISPOSE_ON_CLOSE`: cerrar
su ventana no cierra el programa principal. Se reemplaza únicamente el manejador
de cierre para evitar un error de limpieza de pestañas en JasperReports 7.0.6.
La barra de herramientas y los diálogos conservan su implementación original.
No se genera una interfaz de previsualización adicional.

## Archivos involucrados

| Archivo | Responsabilidad |
| --- | --- |
| `reportes/ImpresionReporteService.java` | Reúne los apartados y abre el JasperViewer original en el hilo de Swing. |
| `controllers/ReporteController.java` | Abre el visor desde una tarea y muestra el resultado o los errores. |
| `reportes-view.fxml` | Botón Imprimir y su ayuda. |
| `module-info.java` | La declaración existente de java.desktop permite utilizar Swing. |

Se eliminó `VistaPreviaPdf.java` y la impresión mediante imágenes y JavaFX
PrinterJob. JasperViewer está incluido en la dependencia JasperReports existente;
no hace falta agregar dependencias a Maven ni archivos CSS.

## Verificación manual

Generar un reporte con varios apartados y páginas A4 y A3. Abrir JasperViewer,
recorrer las páginas, guardar como PDF y abrir y cancelar la impresión. Cerrar el
visor y comprobar que la aplicación sigue abierta. La impresión física requiere
una impresora disponible.

Comprobado con datos de ejemplo: el visor original abre 12 apartados y 12 páginas;
su selector de guardado ofrece PDF y el diálogo de impresión reconoce las 12
páginas. La exportación del mismo JasperPrint genera un PDF válido de 12 páginas,
con formato A4 en la primera y A3 en la segunda. No se imprimió en papel.
El cierre corregido se comprobó sin excepciones y conservó abierto el generador.

Compilación sin ejecutar pruebas automatizadas:

```powershell
.\mvnw.cmd -B -Dmaven.test.skip=true package
```

Nombre sugerido para el commit:

```text
refactor(reportes): usar el visor original de JasperReports para imprimir
```
