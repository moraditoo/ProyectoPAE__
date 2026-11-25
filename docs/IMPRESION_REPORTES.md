# Impresión de reportes

## Uso

1. Abrir **Reportes**, seleccionar apartados, campos y filtros y pulsar **Generar reporte**.
2. Pulsar **Imprimir**. Se abre una ventana con el documento paginado.
3. Usar **Anterior**, **Siguiente** y la ampliación para revisar las páginas.
4. Elegir **Imprimir** para abrir el diálogo del sistema y seleccionar impresora,
   copias, papel y páginas. Cancelar ese diálogo conserva la vista previa.
5. Elegir **Guardar PDF** para guardar el documento completo en la carpeta deseada.
   Cerrar o cancelar el guardado conserva la vista previa.

El botón del generador se habilita después de generar el reporte y se deshabilita
al cambiar filtros o campos. Es necesario generar de nuevo para imprimir la nueva
selección. La vista previa, el guardado y la impresión usan el mismo documento
generado, sin volver a consultar PostgreSQL.

Las páginas se numeran de forma continua entre todos los apartados. El documento
mantiene sus tablas, títulos y saltos de página. En papel se adapta al área
imprimible elegida, conservando las proporciones. Guardar PDF conserva los tamaños
originales A4 o A3 del reporte y guarda todos los apartados, independientemente de
la página que se esté mirando.

Sin impresora instalada sigue disponible **Guardar PDF**. El mensaje de envío a
la impresora confirma la entrega a su cola; el resultado físico depende de la
impresora. Las operaciones se realizan en segundo plano y los controles se
bloquean mientras se prepara una página, se guarda o se imprime.

## Archivos involucrados

| Archivo | Responsabilidad |
| --- | --- |
| `reportes/ImpresionReporteService.java` | Reúne las páginas de los apartados, genera las imágenes y envía el documento a JavaFX PrinterJob. |
| `reportes/VistaPreviaPdf.java` | Ventana JavaFX, navegación, ampliación, diálogo de impresión y guardado PDF. |
| `controllers/ReporteController.java` | Habilita el botón según el estado del reporte y abre la ventana. |
| `reportes-view.fxml` | Botón Imprimir con icono y ayuda. |
| `module-info.java` | Declara java.desktop para las imágenes que produce JasperReports. |

La interfaz se construye con JavaFX y el tema Java existente, sin archivos CSS ni
dependencias nuevas en Maven. La representación de las páginas procede del mismo
JasperPrint que se exporta al PDF; no requiere un lector PDF externo. Para imprimir
se renderiza cada página a una escala de 3 (216 puntos por pulgada) y se adapta al
papel seleccionado. Se procesa una página a la vez para evitar mantener todas las
imágenes del documento en memoria.

## División de los cambios en commits

Estos son los tres grupos de archivos para registrar la implementación en `andres`.
Ejecutar desde la raíz del proyecto, en ese orden:

```powershell
git add -- src/main/java/module-info.java src/main/java/ni/edu/uam/proyectopae/reportes/ImpresionReporteService.java
git commit -m "feat(reportes): preparar páginas del PDF para vista previa e impresión"

git add -- src/main/java/ni/edu/uam/proyectopae/reportes/VistaPreviaPdf.java src/main/java/ni/edu/uam/proyectopae/controllers/ReporteController.java src/main/resources/ni/edu/uam/proyectopae/reportes-view.fxml
git commit -m "feat(reportes): agregar botón Imprimir con vista previa y guardado PDF"

git add -- README.md docs/IMPRESION_REPORTES.md
git commit -m "docs(reportes): documentar la impresión y el guardado desde la vista previa"
```

## Verificación

Compilación y verificaciones existentes del proyecto:

```powershell
.\scripts\pae.ps1 -Accion verificar
```

Comprobación manual: generar un reporte con varios apartados y varias páginas,
recorrerlo, cambiar ampliación, guardar el PDF, cancelar el diálogo de impresión,
probar los modos claro y oscuro y cambiar filtros para verificar que se requiere
generar de nuevo. La impresión física requiere una impresora disponible.

Resultados de la revisión de esta implementación:

- Compilación correcta y 26 verificaciones existentes aprobadas, sin agregar pruebas nuevas.
- Vista previa comprobada con 12 apartados, 101 filas y 18 páginas de ejemplo.
- Navegación entre páginas y presentación en modo claro y oscuro comprobadas.
- Diálogo nativo de impresión y regreso a la vista previa al cancelar comprobados.
- Guardado desde **Guardar PDF** comprobado: PDF válido con las 18 páginas.
- Cambio de selección comprobado: deshabilita **Imprimir** hasta generar de nuevo.
- No se envió el documento a una impresora física.
