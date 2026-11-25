package ni.edu.uam.proyectopae.reportes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import ni.edu.uam.proyectopae.reportes.ReporteJasperService.Formato;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.VentanaManager;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Ventana JavaFX con las páginas del PDF y las acciones de impresión y guardado. */
public final class VistaPreviaPdf {
    private final DocumentoReporte documento;
    private final ImpresionReporteService paginas;
    private final Stage ventana = new Stage();
    private final BooleanProperty ocupado = new SimpleBooleanProperty(false);
    private final IntegerProperty paginaActual = new SimpleIntegerProperty(0);
    private final ImageView imagen = new ImageView();
    private final ScrollPane visor = new ScrollPane();
    private final ComboBox<String> zoom = new ComboBox<>();
    private final Label lblPagina = new Label();
    private final Label lblEstado = new Label();

    private VistaPreviaPdf(Window propietario, DocumentoReporte documento) {
        this.documento = documento;
        paginas = new ImpresionReporteService(documento);
        if (propietario != null) ventana.initOwner(propietario);
        ventana.initModality(Modality.WINDOW_MODAL);
        ventana.setTitle("Vista previa del PDF - Reporte general");

        Button anterior = boton("Anterior", "fas-chevron-left", "btn-ghost");
        Button siguiente = boton("Siguiente", "fas-chevron-right", "btn-ghost");
        anterior.setId("btnPaginaAnterior");
        siguiente.setId("btnPaginaSiguiente");
        anterior.disableProperty().bind(ocupado.or(paginaActual.isEqualTo(0)));
        siguiente.disableProperty().bind(ocupado.or(paginaActual.isEqualTo(paginas.cantidadPaginas() - 1)));
        anterior.setOnAction(evento -> mostrarPagina(paginaActual.get() - 1));
        siguiente.setOnAction(evento -> mostrarPagina(paginaActual.get() + 1));
        lblPagina.setId("lblPaginaPdf");
        zoom.setId("zoomPdf");
        zoom.getItems().addAll("Ajustar al ancho", "75 %", "100 %", "125 %", "150 %", "200 %");
        zoom.setValue("Ajustar al ancho");
        zoom.setAccessibleText("Ampliación del documento");
        zoom.valueProperty().addListener(observable -> ajustarZoom());
        zoom.disableProperty().bind(ocupado);

        Button imprimir = boton("Imprimir", "fas-print", "btn-primary");
        Button guardar = boton("Guardar PDF", "fas-file-pdf", "btn-secondary");
        Button cerrar = boton("Cerrar", "fas-times", "btn-ghost");
        imprimir.setId("btnImprimirPdf");
        guardar.setId("btnGuardarPdf");
        cerrar.setId("btnCerrarPdf");
        imprimir.disableProperty().bind(ocupado);
        guardar.disableProperty().bind(ocupado);
        cerrar.disableProperty().bind(ocupado);
        imprimir.setOnAction(evento -> imprimir());
        guardar.setOnAction(evento -> guardarPdf());
        cerrar.setOnAction(evento -> ventana.close());

        FlowPane navegacion = new FlowPane(10, 8, anterior, lblPagina, siguiente, zoom);
        navegacion.setAlignment(Pos.CENTER_LEFT);
        FlowPane acciones = new FlowPane(10, 8, imprimir, guardar, cerrar);
        acciones.setAlignment(Pos.CENTER_LEFT);
        Label titulo = new Label("Vista previa del PDF");
        titulo.getStyleClass().add("card-title");
        Label ayuda = new Label("Revisa las páginas y elige Imprimir o Guardar PDF.");
        ayuda.getStyleClass().add("hint-label");
        ayuda.setWrapText(true);
        VBox cabecera = new VBox(10, titulo, ayuda, acciones, navegacion);
        cabecera.setPadding(new Insets(18));

        imagen.setId("paginaPdf");
        imagen.setPreserveRatio(true);
        imagen.setSmooth(true);
        StackPane hoja = new StackPane(imagen);
        hoja.setAlignment(Pos.TOP_CENTER);
        hoja.setPadding(new Insets(20));
        visor.setContent(hoja);
        visor.setId("visorPdf");
        visor.setFitToWidth(true);
        visor.setPannable(true);
        visor.viewportBoundsProperty().addListener(observable -> ajustarZoom());

        ProgressIndicator progreso = new ProgressIndicator();
        progreso.setPrefSize(24, 24);
        progreso.setMaxSize(24, 24);
        progreso.visibleProperty().bind(ocupado);
        progreso.managedProperty().bind(ocupado);
        lblEstado.setId("estadoPdf");
        lblEstado.setWrapText(true);
        lblEstado.setMinHeight(Region.USE_PREF_SIZE);
        lblEstado.getStyleClass().add("hint-label");
        HBox pie = new HBox(12, progreso, lblEstado);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.setPadding(new Insets(12, 18, 12, 18));

        BorderPane raiz = new BorderPane(visor, cabecera, null, pie, null);
        raiz.getStyleClass().add("pae-ui");
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        Scene escena = VentanaManager.crearEscena(raiz,
                Math.min(1100, pantalla.getWidth() - 60), Math.min(800, pantalla.getHeight() - 60));
        ventana.setScene(escena);
        ventana.setMinWidth(640);
        ventana.setMinHeight(480);
        ventana.setOnCloseRequest(evento -> {
            if (ocupado.get()) evento.consume();
        });
    }

    public static void mostrar(Window propietario, DocumentoReporte documento) {
        VistaPreviaPdf vista = new VistaPreviaPdf(propietario, documento);
        vista.ventana.show();
        vista.mostrarPagina(0);
    }

    private void mostrarPagina(int indice) {
        if (ocupado.get() || indice < 0 || indice >= paginas.cantidadPaginas()) return;
        paginaActual.set(indice);
        lblPagina.setText("Página " + (indice + 1) + " de " + paginas.cantidadPaginas()
                + " · " + paginas.tituloPagina(indice));
        imagen.setImage(null);
        lblEstado.setText("Preparando página.");
        ejecutar(() -> paginas.renderizarPagina(indice, 2f), pagina -> {
            imagen.setImage(pagina);
            ajustarZoom();
            visor.setVvalue(0);
            visor.setHvalue(0);
            lblEstado.setText("Documento listo para imprimir o guardar en PDF.");
        }, "No se pudo mostrar la página");
    }

    private void ajustarZoom() {
        if (imagen.getImage() == null) return;
        double ancho = "Ajustar al ancho".equals(zoom.getValue())
                ? Math.max(100, visor.getViewportBounds().getWidth() - 48)
                : imagen.getImage().getWidth() / 2 * Integer.parseInt(zoom.getValue().replace(" %", "")) / 100.0;
        imagen.setFitWidth(ancho);
    }

    private void imprimir() {
        if (ocupado.get()) return;
        try {
            PrinterJob trabajo = PrinterJob.createPrinterJob();
            if (trabajo == null) {
                lblEstado.setText("No hay impresoras disponibles. Puedes guardar el documento con Guardar PDF.");
                Alertas.validacion("No hay impresoras disponibles. Puedes guardar el documento en PDF.");
                return;
            }
            trabajo.getJobSettings().setJobName("Reporte general - Proyecto PAE");
            trabajo.getJobSettings().setPageLayout(trabajo.getPrinter().createPageLayout(
                    trabajo.getJobSettings().getPageLayout().getPaper(),
                    PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT));
            if (!trabajo.showPrintDialog(ventana)) {
                trabajo.cancelJob();
                lblEstado.setText("Impresión cancelada. Puedes seguir revisando o guardar el PDF.");
                return;
            }
            lblEstado.setText("Enviando documento a la impresora.");
            ejecutar(() -> {
                paginas.imprimir(trabajo);
                return trabajo.getPrinter().getName();
            }, nombre -> lblEstado.setText("Documento enviado a la impresora: " + nombre),
                    "No se pudo imprimir el reporte");
        } catch (RuntimeException | LinkageError error) {
            mostrarError("No se pudo abrir la impresión", error);
        }
    }

    private void guardarPdf() {
        if (ocupado.get()) return;
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte en PDF");
        selector.setInitialFileName("reporte-general.pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File archivo = selector.showSaveDialog(ventana);
        if (archivo == null) return;
        Path destino = archivo.toPath();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            destino = destino.resolveSibling(archivo.getName() + ".pdf");
            if (Files.exists(destino) && !Alertas.confirmar("Reemplazar archivo",
                    "¿Deseas reemplazar " + destino.getFileName() + "?")) return;
        }
        Path pdf = destino;
        lblEstado.setText("Guardando documento PDF.");
        ejecutar(() -> {
            new ReporteGeneralJasperService().exportar(documento, pdf, Formato.PDF);
            return pdf.getFileName().toString();
        }, nombre -> lblEstado.setText("PDF guardado: " + nombre), "No se pudo guardar el PDF");
    }

    private <T> void ejecutar(Callable<T> operacion, Consumer<T> completar, String mensajeError) {
        ocupado.set(true);
        Task<T> tarea = new Task<>() {
            @Override protected T call() throws Exception { return operacion.call(); }
        };
        tarea.setOnSucceeded(evento -> {
            ocupado.set(false);
            completar.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            ocupado.set(false);
            mostrarError(mensajeError, tarea.getException());
        });
        Thread hilo = new Thread(tarea, "proyectopae-vista-pdf");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarError(String mensaje, Throwable error) {
        String detalle = VentanaManager.causaRaiz(error);
        lblEstado.setText(mensaje + ": " + detalle);
        Alertas.error(mensaje, detalle);
    }

    private static Button boton(String texto, String icono, String estilo) {
        Button boton = new Button(texto, new FontIcon(icono));
        boton.getStyleClass().add(estilo);
        return boton;
    }
}
