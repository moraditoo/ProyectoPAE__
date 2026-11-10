package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.CitaDAO;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.dao.ServicioDAO;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.Servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AgendaController {

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<Servicio> cmbServicio;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private Label lblEstado;

    @FXML private TableView<Cita> tblCitas;
    @FXML private TableColumn<Cita, Integer> colId;
    @FXML private TableColumn<Cita, String> colCliente;
    @FXML private TableColumn<Cita, String> colServicio;
    @FXML private TableColumn<Cita, LocalDateTime> colFecha;
    @FXML private TableColumn<Cita, String> colEstado;

    private final CitaDAO citaDAO = new CitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();

    private final ObservableList<Cita> listaCitas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCita"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("nombreServicio"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cargarCatalogos();
        refrescarCitas();
    }

    private void cargarCatalogos() {
        try {
            cmbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));
            cmbServicio.setItems(FXCollections.observableArrayList(servicioDAO.listarTodos()));
        } catch (Exception e) {
            lblEstado.setText("Error al cargar catalogos de citas: " + e.getMessage());
        }
    }

    private void refrescarCitas() {
        try {
            listaCitas.setAll(citaDAO.listarCitasConRelaciones());
            tblCitas.setItems(listaCitas);
        } catch (Exception e) {
            lblEstado.setText("Error al consultar citas: " + e.getMessage());
        }
    }

    @FXML
    protected void handleAgendarCita() {
        Cliente cli = cmbCliente.getValue();
        Servicio srv = cmbServicio.getValue();
        LocalDate fec = dpFecha.getValue();
        String horaTxt = txtHora.getText().trim();

        if (cli == null || srv == null || fec == null || horaTxt.isEmpty()) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Validacion: Complete todos los datos requeridos.");
            return;
        }

        try {
            int hora = Integer.parseInt(horaTxt);
            if (hora < 8 || hora > 19) {
                lblEstado.setStyle("-fx-text-fill: red;");
                lblEstado.setText("Validacion: El horario de atencion debe oscilar entre 8 y 19 horas.");
                return;
            }

            LocalDateTime citaFechaHora = fec.atTime(hora, 0);
            Cita cita = new Cita(cli, srv, citaFechaHora, "PROGRAMADA");
            citaDAO.agendar(cita);

            txtHora.clear();
            lblEstado.setStyle("-fx-text-fill: green;");
            lblEstado.setText("Turno agendado y persistido con exito.");
            refrescarCitas();
        } catch (NumberFormatException e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("La hora debe ser un numero entero valido.");
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Error de persistencia en cita: " + e.getMessage());
        }
    }

    @FXML
    protected void handleCancelarCita() {
        Cita sel = tblCitas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Seleccione una cita activa en la tabla.");
            return;
        }

        try {
            citaDAO.cancelar(sel.getIdCita());
            lblEstado.setStyle("-fx-text-fill: green;");
            lblEstado.setText("Cita cancelada mediante Hibernate.");
            refrescarCitas();
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Error al actualizar estado de la cita.");
        }
    }

    @FXML
    protected void handleGenerarResumenHQL() {
        try {
            List<Object[]> resultados = citaDAO.reporteIngresosPorServicio();
            StringBuilder sb = new StringBuilder("Resumen HQL: ");
            for (Object[] fila : resultados) {
                sb.append(fila[0]).append(" [Citas: ").append(fila[1])
                        .append(", Subtotal: C$").append(fila[2]).append("] | ");
            }
            lblEstado.setStyle("-fx-text-fill: #1a237e;");
            lblEstado.setText(sb.toString());
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Error al ejecutar consulta HQL de resumen.");
        }
    }
}