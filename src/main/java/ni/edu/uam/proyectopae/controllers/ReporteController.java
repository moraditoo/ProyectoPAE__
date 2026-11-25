package ni.edu.uam.proyectopae.controllers;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import ni.edu.uam.proyectopae.dao.ReporteGeneralDAO;
import ni.edu.uam.proyectopae.dao.ServicioDAO;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.reportes.*;
import ni.edu.uam.proyectopae.reportes.ReporteJasperService.Formato;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.TemaPAE;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ReporteController {
    @FXML private VBox configuracion;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Servicio> cmbServicio;
    @FXML private VBox listaApartados;
    @FXML private Label lblSeleccion;
    @FXML private TabPane vistaPrevia;
    @FXML private VBox panelSinResultados;
    @FXML private Label lblResumen;
    @FXML private Label lblEstado;
    @FXML private Button btnExportar;
    @FXML private Button btnImprimir;
    @FXML private ProgressIndicator progreso;

    private final Map<ApartadoReporte, CheckBox> apartados = new EnumMap<>(ApartadoReporte.class);
    private final Map<ApartadoReporte, Map<CampoReporte, CheckBox>> campos = new EnumMap<>(ApartadoReporte.class);
    private final BooleanProperty ocupado = new SimpleBooleanProperty(false);
    private final BooleanProperty listo = new SimpleBooleanProperty(false);
    private final Servicio todosLosServicios = new Servicio();
    private final Function<SolicitudReporte, DatosReporte> consultar;
    private final Callable<List<Servicio>> listarServicios;
    private final ReporteGeneralJasperService jasper;
    private DocumentoReporte reporteGenerado;

    public ReporteController() {
        this(new ReporteGeneralDAO()::consultar, new ServicioDAO()::listarTodos,
                new ReporteGeneralJasperService());
    }

    /** Permite comprobar la pantalla sin conectarla a la base de datos del equipo. */
    ReporteController(Function<SolicitudReporte, DatosReporte> consultar,
                      Callable<List<Servicio>> listarServicios, ReporteGeneralJasperService jasper) {
        this.consultar = consultar;
        this.listarServicios = listarServicios;
        this.jasper = jasper;
    }

    @FXML
    private void initialize() {
        construirChecklist();
        configuracion.disableProperty().bind(ocupado);
        vistaPrevia.disableProperty().bind(ocupado);
        btnExportar.disableProperty().bind(ocupado.or(listo.not()));
        btnImprimir.disableProperty().bind(ocupado.or(listo.not()));
        progreso.visibleProperty().bind(ocupado);
        progreso.managedProperty().bind(ocupado);
        vistaPrevia.visibleProperty().bind(listo);
        vistaPrevia.managedProperty().bind(listo);
        panelSinResultados.visibleProperty().bind(listo.not());
        panelSinResultados.managedProperty().bind(listo.not());

        todosLosServicios.setNombreServicio("Todos los servicios");
        cmbServicio.setConverter(new StringConverter<>() {
            @Override public String toString(Servicio servicio) {
                return servicio == null ? "" : servicio.getNombreServicio();
            }
            @Override public Servicio fromString(String texto) { return null; }
        });
        cmbServicio.getItems().add(todosLosServicios);
        cmbServicio.setValue(todosLosServicios);
        InvalidationListener cambio = observable -> invalidarVistaPrevia();
        dpDesde.valueProperty().addListener(cambio);
        dpHasta.valueProperty().addListener(cambio);
        cmbServicio.valueProperty().addListener(cambio);

        lblEstado.setText("Cargando servicios…");
        ejecutar(listarServicios, servicios -> {
            cmbServicio.getItems().addAll(servicios);
            invalidarVistaPrevia();
        }, "No se pudieron cargar los servicios");
    }

    private void construirChecklist() {
        for (ApartadoReporte apartado : ApartadoReporte.values()) {
            CheckBox incluir = new CheckBox(apartado.titulo());
            incluir.setId("apartado_" + apartado.name());
            incluir.setSelected(true);
            incluir.setWrapText(true);
            apartados.put(apartado, incluir);

            VBox contenido = new VBox(8);
            contenido.setPadding(new Insets(10));
            contenido.disableProperty().bind(incluir.selectedProperty().not());
            Label alcance = new Label(apartado.alcance());
            alcance.setWrapText(true);
            alcance.setMinHeight(Region.USE_PREF_SIZE);
            alcance.getStyleClass().add("hint-label");
            Button todos = new Button("Todos los campos");
            Button ninguno = new Button("Ninguno");
            todos.getStyleClass().add("btn-ghost");
            ninguno.getStyleClass().add("btn-ghost");
            todos.setOnAction(evento -> seleccionarCampos(apartado, true));
            ninguno.setOnAction(evento -> seleccionarCampos(apartado, false));
            contenido.getChildren().addAll(alcance, new HBox(8, todos, ninguno));

            Map<CampoReporte, CheckBox> casillas = new LinkedHashMap<>();
            campos.put(apartado, casillas);
            for (CampoReporte campo : apartado.campos()) {
                CheckBox casilla = new CheckBox(campo.titulo());
                casilla.setId("campo_" + apartado.name() + "_" + campo.clave());
                casilla.setWrapText(true);
                casilla.setSelected(true);
                casilla.selectedProperty().addListener(observable -> cambioSeleccion());
                casillas.put(campo, casilla);
                contenido.getChildren().add(casilla);
            }
            TitledPane panel = new TitledPane("", contenido);
            panel.setId("panel_" + apartado.name());
            panel.setGraphic(incluir);
            panel.setAnimated(false);
            panel.setExpanded(false);
            panel.setMaxWidth(Double.MAX_VALUE);
            listaApartados.getChildren().add(panel);
            incluir.selectedProperty().addListener(observable -> cambioSeleccion());
        }
        actualizarSeleccion();
    }

    @FXML private void seleccionarTodos() {
        apartados.values().forEach(casilla -> casilla.setSelected(true));
        campos.keySet().forEach(apartado -> seleccionarCampos(apartado, true));
    }

    @FXML private void desmarcarTodos() {
        apartados.values().forEach(casilla -> casilla.setSelected(false));
    }

    private void seleccionarCampos(ApartadoReporte apartado, boolean seleccionados) {
        campos.get(apartado).values().forEach(casilla -> casilla.setSelected(seleccionados));
    }

    private void cambioSeleccion() {
        actualizarSeleccion();
        invalidarVistaPrevia();
    }

    private void actualizarSeleccion() {
        long cantidad = apartados.values().stream().filter(CheckBox::isSelected).count();
        long cantidadCampos = campos.entrySet().stream()
                .filter(e -> apartados.get(e.getKey()).isSelected())
                .flatMap(e -> e.getValue().values().stream()).filter(CheckBox::isSelected).count();
        lblSeleccion.setText(cantidad + " apartados · " + cantidadCampos + " campos seleccionados");
    }

    SolicitudReporte crearSolicitud() {
        Map<ApartadoReporte, List<CampoReporte>> seleccion = new EnumMap<>(ApartadoReporte.class);
        apartados.forEach((apartado, casilla) -> {
            if (casilla.isSelected()) {
                List<CampoReporte> incluidos = campos.get(apartado).entrySet().stream()
                        .filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).toList();
                if (incluidos.isEmpty()) {
                    throw new IllegalArgumentException("Selecciona al menos un campo en " + apartado.titulo() + ".");
                }
                seleccion.put(apartado, incluidos);
            }
        });
        Servicio servicio = cmbServicio.getValue();
        return new SolicitudReporte(dpDesde.getValue(), dpHasta.getValue(),
                servicio == null ? null : servicio.getIdServicio(),
                servicio == null ? null : servicio.getNombreServicio(), seleccion);
    }

    @FXML
    private void generarReporte() {
        SolicitudReporte solicitud;
        try {
            solicitud = crearSolicitud();
        } catch (IllegalArgumentException error) {
            Alertas.validacion(error.getMessage());
            return;
        }
        invalidarVistaPrevia();
        lblEstado.setText("Consultando datos y generando el reporte…");
        ejecutar(() -> jasper.generar(consultar.apply(solicitud)), this::mostrarReporte,
                "No se pudo generar el reporte");
    }

    private void mostrarReporte(DocumentoReporte documento) {
        reporteGenerado = documento;
        for (SeccionReporte seccion : documento.datos().secciones()) {
            TableView<Map<String, String>> tabla = new TableView<>();
            tabla.getStyleClass().add("filas-multilinea");
            tabla.setId("tabla_" + seccion.apartado().name());
            tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            tabla.setPlaceholder(new Label("Sin registros con los filtros seleccionados."));
            for (CampoReporte campo : seccion.campos()) {
                TableColumn<Map<String, String>, String> columna = new TableColumn<>(campo.titulo());
                columna.setId(campo.clave());
                columna.setCellValueFactory(celda -> new SimpleStringProperty(celda.getValue().get(campo.clave())));
                columna.setMinWidth(90);
                columna.setPrefWidth(Math.max(110, campo.peso()));
                // El PDF y el CSV mantienen el mismo orden de la consulta y de los campos.
                columna.setSortable(false);
                columna.setReorderable(false);
                columna.setCellFactory(c -> new TableCell<>() {
                    { setWrapText(true); }
                    @Override protected void updateItem(String texto, boolean vacia) {
                        super.updateItem(texto, vacia);
                        setText(vacia ? null : texto);
                    }
                });
                tabla.getColumns().add(columna);
            }
            tabla.getItems().setAll(seccion.filas());
            Label alcance = new Label(seccion.apartado().alcance());
            alcance.setWrapText(true);
            alcance.setMinHeight(Region.USE_PREF_SIZE);
            alcance.getStyleClass().add("hint-label");
            Label resumen = new Label(seccion.resumen());
            resumen.setWrapText(true);
            resumen.setMinHeight(Region.USE_PREF_SIZE);
            resumen.getStyleClass().add("form-label");
            VBox contenido = new VBox(10, alcance, tabla, resumen);
            contenido.setPadding(new Insets(12, 0, 0, 0));
            VBox.setVgrow(tabla, Priority.ALWAYS);
            Tab tab = new Tab(seccion.apartado().titulo() + " (" + seccion.filas().size() + ")", contenido);
            tab.setId(seccion.apartado().name());
            tab.setClosable(false);
            vistaPrevia.getTabs().add(tab);
        }
        listo.set(true);
        lblResumen.setText(documento.datos().secciones().size() + " apartados · "
                + documento.datos().totalRegistros() + " filas · " + documento.paginasPdf() + " páginas PDF\n"
                + documento.datos().solicitud().descripcionFiltros() + "\nGenerado el "
                + documento.datos().generado().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblEstado.setText("Reporte listo. Pulsa Imprimir para abrir JasperViewer y usar sus opciones de impresión y guardado.");
        TemaPAE.aplicar(vistaPrevia);
    }

    @FXML private void limpiarFiltros() {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        cmbServicio.setValue(todosLosServicios);
        invalidarVistaPrevia();
    }

    private void invalidarVistaPrevia() {
        reporteGenerado = null;
        listo.set(false);
        vistaPrevia.getTabs().clear();
        lblResumen.setText("Generá el reporte para consultar los apartados seleccionados.");
        lblEstado.setText("Pulsa Generar reporte para actualizar la vista previa.");
    }

    @FXML
    private void imprimirReporte() {
        if (reporteGenerado == null || ocupado.get()) return;
        DocumentoReporte documento = reporteGenerado;
        lblEstado.setText("Abriendo el visor de JasperReports.");
        ejecutar(() -> {
            new ImpresionReporteService().mostrar(documento);
            return documento.paginasPdf();
        }, paginas -> lblEstado.setText("Reporte abierto en JasperViewer: " + paginas + " páginas."),
                "No se pudo abrir el visor de JasperReports");
    }

    @FXML
    private void exportarReporte() {
        if (reporteGenerado == null) return;
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte general");
        selector.setInitialFileName("reporte-general.pdf");
        FileChooser.ExtensionFilter pdf = new FileChooser.ExtensionFilter("Documento PDF", "*.pdf");
        FileChooser.ExtensionFilter csv = new FileChooser.ExtensionFilter("Archivo CSV", "*.csv");
        selector.getExtensionFilters().addAll(pdf, csv);
        selector.setSelectedExtensionFilter(pdf);
        File archivo = selector.showSaveDialog(vistaPrevia.getScene().getWindow());
        if (archivo == null) return;

        Formato formato = selector.getSelectedExtensionFilter() == csv ? Formato.CSV : Formato.PDF;
        String extension = formato == Formato.PDF ? ".pdf" : ".csv";
        Path destino = archivo.toPath();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(extension)) {
            String nombre = archivo.getName().replaceFirst("(?i)\\.(pdf|csv)$", "");
            destino = destino.resolveSibling(nombre + extension);
            if (Files.exists(destino) && !Alertas.confirmar(
                    "Reemplazar archivo", "¿Deseas reemplazar " + destino.getFileName() + "?")) return;
        }
        guardarReporte(destino, formato);
    }

    void guardarReporte(Path destino, Formato formato) {
        if (reporteGenerado == null || ocupado.get()) return;
        DocumentoReporte documento = reporteGenerado;
        lblEstado.setText("Exportando reporte…");
        ejecutar(() -> {
            jasper.exportar(documento, destino, formato);
            return destino.getFileName().toString();
        }, nombre -> lblEstado.setText("Archivo guardado: " + nombre), "No se pudo exportar el reporte");
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
            String detalle = VentanaManager.causaRaiz(tarea.getException());
            lblEstado.setText(mensajeError + ": " + detalle);
            Scene escena = vistaPrevia.getScene();
            if (escena != null && escena.getWindow() != null && escena.getWindow().isShowing()) {
                Alertas.error(mensajeError, detalle);
            }
        });
        Thread hilo = new Thread(tarea, "proyectopae-reportes");
        hilo.setDaemon(true);
        hilo.start();
    }
}
