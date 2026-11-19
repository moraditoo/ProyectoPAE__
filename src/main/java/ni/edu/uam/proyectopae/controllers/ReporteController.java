package ni.edu.uam.proyectopae.controllers;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import ni.edu.uam.proyectopae.dao.ReporteDAO;
import ni.edu.uam.proyectopae.dao.ServicioDAO;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.ExportadorUtil;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ReporteController {

    @FXML private VBox configuracion;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Servicio> cmbServicio;

    @FXML private CheckBox chkId;
    @FXML private CheckBox chkFecha;
    @FXML private CheckBox chkCliente;
    @FXML private CheckBox chkServicio;
    @FXML private CheckBox chkEstado;

    @FXML private TableView<Cita> tabla;
    @FXML private TableColumn<Cita, Integer> colId;
    @FXML private TableColumn<Cita, LocalDateTime> colFecha;
    @FXML private TableColumn<Cita, String> colCliente;
    @FXML private TableColumn<Cita, String> colServicio;
    @FXML private TableColumn<Cita, String> colEstado;

    @FXML private Label lblEstado;
    @FXML private Button btnExportar;

    @FXML private Label lblTotalCitas;
    @FXML private Label lblCitasProgramadas;
    @FXML private Label lblCitasCompletadas;
    @FXML private Label lblCitasCanceladas;

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();
    private final Servicio todosLosServicios = new Servicio();
    private final BooleanProperty ocupado = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCita"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("nombreServicio"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        Formato.columnaFechaHora(colFecha);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colId.visibleProperty().bind(chkId.selectedProperty());
        colFecha.visibleProperty().bind(chkFecha.selectedProperty());
        colCliente.visibleProperty().bind(chkCliente.selectedProperty());
        colServicio.visibleProperty().bind(chkServicio.selectedProperty());
        colEstado.visibleProperty().bind(chkEstado.selectedProperty());

        configuracion.disableProperty().bind(ocupado);
        btnExportar.disableProperty().bind(
                ocupado.or(Bindings.isEmpty(tabla.getItems())));

        todosLosServicios.setNombreServicio("Todos los servicios");
        cmbServicio.setConverter(new StringConverter<>() {
            @Override
            public String toString(Servicio servicio) {
                return servicio == null ? "" : servicio.getNombreServicio();
            }

            @Override
            public Servicio fromString(String texto) {
                return null;
            }
        });
        cmbServicio.getItems().add(todosLosServicios);
        cmbServicio.setValue(todosLosServicios);

        InvalidationListener cambio = observable -> invalidarVistaPrevia();
        dpDesde.valueProperty().addListener(cambio);
        dpHasta.valueProperty().addListener(cambio);
        cmbServicio.valueProperty().addListener(cambio);
        chkId.selectedProperty().addListener(cambio);
        chkFecha.selectedProperty().addListener(cambio);
        chkCliente.selectedProperty().addListener(cambio);
        chkServicio.selectedProperty().addListener(cambio);
        chkEstado.selectedProperty().addListener(cambio);

        ejecutar(servicioDAO::listarTodos,
                servicios -> cmbServicio.getItems().addAll(servicios),
                "No se pudieron cargar los servicios");
    }

    @FXML
    private void generarReporte() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            Alertas.validacion("La fecha inicial debe ser anterior o igual a la fecha final.");
            return;
        }
        if (tabla.getVisibleLeafColumns().isEmpty()) {
            Alertas.validacion("Selecciona al menos una columna para el reporte.");
            return;
        }

        Servicio servicio = cmbServicio.getValue();
        Integer idServicio = servicio == null ? null : servicio.getIdServicio();

        tabla.getItems().clear();
        lblEstado.setText("Generando reporte…");

        ejecutar(() -> reporteDAO.buscarCitas(desde, hasta, idServicio), citas -> {
            tabla.getItems().setAll(citas);
            tabla.sort();

            long prog = citas.stream().filter(c -> "PROGRAMADA".equalsIgnoreCase(c.getEstado())).count();
            long comp = citas.stream().filter(c -> "COMPLETADA".equalsIgnoreCase(c.getEstado())).count();
            long canc = citas.stream().filter(c -> "CANCELADA".equalsIgnoreCase(c.getEstado())).count();

            if (lblTotalCitas != null) lblTotalCitas.setText(citas.size() + " citas");
            if (lblCitasProgramadas != null) lblCitasProgramadas.setText(prog + " programadas");
            if (lblCitasCompletadas != null) lblCitasCompletadas.setText(comp + " completadas");
            if (lblCitasCanceladas != null) lblCitasCanceladas.setText(canc + " canceladas");

            lblEstado.setText(citas.isEmpty()
                    ? "No se encontraron citas con esos filtros."
                    : "Citas en la vista previa: " + citas.size());
        }, "No se pudo generar el reporte");
    }

    @FXML
    private void limpiarFiltros() {
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        cmbServicio.setValue(todosLosServicios);
        invalidarVistaPrevia();
    }

    private void invalidarVistaPrevia() {
        tabla.getItems().clear();
        if (lblTotalCitas != null) lblTotalCitas.setText("0 citas");
        if (lblCitasProgramadas != null) lblCitasProgramadas.setText("0 programadas");
        if (lblCitasCompletadas != null) lblCitasCompletadas.setText("0 completadas");
        if (lblCitasCanceladas != null) lblCitasCanceladas.setText("0 canceladas");
        lblEstado.setText("Pulsa Generar reporte para actualizar la vista previa.");
    }

    @FXML
    private void exportarCsv() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte de citas");
        selector.setInitialFileName("reporte-citas.csv");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));

        File archivo = selector.showSaveDialog(tabla.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        Path destino = archivo.toPath();
        if (!archivo.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            destino = destino.resolveSibling(archivo.getName() + ".csv");
            if (Files.exists(destino) && !Alertas.confirmar(
                    "Reemplazar archivo", "¿Deseas reemplazar " + destino.getFileName() + "?")) {
                return;
            }
        }

        List<TableColumn<Cita, ?>> columnas = List.copyOf(tabla.getVisibleLeafColumns());
        List<String> encabezados = columnas.stream().map(TableColumn::getText).toList();
        List<List<Object>> filas = new ArrayList<>();

        for (Cita cita : tabla.getItems()) {
            List<Object> fila = new ArrayList<>(columnas.size());
            for (TableColumn<Cita, ?> columna : columnas) {
                Object valor = columna.getCellData(cita);
                fila.add(valor instanceof LocalDateTime fecha ? Formato.fechaHora(fecha) : valor);
            }
            filas.add(fila);
        }

        Path destinoCsv = destino;
        lblEstado.setText("Exportando reporte…");

        ejecutar(() -> {
                    ExportadorUtil.exportarCsv(destinoCsv, encabezados, filas);
                    return destinoCsv.getFileName().toString();
                }, nombre -> lblEstado.setText("Archivo guardado: " + nombre),
                "No se pudo exportar el CSV");
    }

    private <T> void ejecutar(Callable<T> operacion, Consumer<T> completar, String mensajeError) {
        ocupado.set(true);

        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(evento -> {
            ocupado.set(false);
            completar.accept(tarea.getValue());
        });

        tarea.setOnFailed(evento -> {
            ocupado.set(false);
            lblEstado.setText(mensajeError);

            Scene escena = tabla.getScene();
            if (escena != null && escena.getWindow() != null && escena.getWindow().isShowing()) {
                Alertas.error(mensajeError, VentanaManager.causaRaiz(tarea.getException()));
            }
        });

        Thread hilo = new Thread(tarea, "proyectopae-reportes");
        hilo.setDaemon(true);
        hilo.start();
    }
}