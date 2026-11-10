package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.CitaDAO;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.dao.ServicioDAO;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.Servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AgendaController {

    @FXML private TextField txtBuscarCliente;
    @FXML private ListView<Cliente> lstClientesCoincidentes;
    @FXML private Label lblClienteSeleccionado;

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

    private final ObservableList<Cliente> masterClientes = FXCollections.observableArrayList();
    private final ObservableList<Cita> listaCitas = FXCollections.observableArrayList();
    private Cliente clienteSeleccionado = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCita"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("nombreServicio"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        configurarRestriccionFecha();
        configurarBuscadorCliente();
        precargarServiciosBase();
        handleRecargarTodo();
    }

    // Deshabilita en el DatePicker cualquier dia anterior a la fecha actual
    private void configurarRestriccionFecha() {
        dpFecha.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #999999;");
                }
            }
        });
    }

    // Filtra dinamicamente la lista de clientes mientras se escribe en el TextField
    private void configurarBuscadorCliente() {
        FilteredList<Cliente> clientesFiltrados = new FilteredList<>(masterClientes, p -> true);

        txtBuscarCliente.textProperty().addListener((obs, oldVal, newVal) -> {
            clientesFiltrados.setPredicate(cliente -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String filtro = newVal.toLowerCase().trim();
                String nombreCompleto = (cliente.getNombres() + " " + cliente.getApellidos()).toLowerCase();
                String tel = cliente.getTelefono() != null ? cliente.getTelefono() : "";

                return nombreCompleto.contains(filtro) || tel.contains(filtro);
            });
        });

        lstClientesCoincidentes.setItems(clientesFiltrados);

        lstClientesCoincidentes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, nuevo) -> {
            if (nuevo != null) {
                clienteSeleccionado = nuevo;
                lblClienteSeleccionado.setText("Cliente: " + nuevo.getNombres() + " " + nuevo.getApellidos() + " (Tel: " + nuevo.getTelefono() + ")");
                lblClienteSeleccionado.setStyle("-fx-text-fill: #1b5e20; -fx-font-weight: bold;");
            }
        });
    }

    private void precargarServiciosBase() {
        try {
            if (servicioDAO.listarTodos().isEmpty()) {
                servicioDAO.guardar(new Servicio("Maquillaje Social de Día", 45, new BigDecimal("25.00")));
                servicioDAO.guardar(new Servicio("Maquillaje de Gala / Noche", 60, new BigDecimal("40.00")));
                servicioDAO.guardar(new Servicio("Maquillaje Nupcial / Novia", 90, new BigDecimal("75.00")));
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void handleRecargarTodo() {
        try {
            masterClientes.setAll(clienteDAO.listarTodos());
            cmbServicio.setItems(FXCollections.observableArrayList(servicioDAO.listarTodos()));
            listaCitas.setAll(citaDAO.listarCitasConRelaciones());
            tblCitas.setItems(listaCitas);
        } catch (Exception e) {
            mostrarMensaje("Error al sincronizar con PostgreSQL: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleAgendarCita() {
        Servicio srv = cmbServicio.getValue();
        LocalDate fecha = dpFecha.getValue();
        String horaTxt = txtHora.getText().trim();

        // 1. Validacion de cliente seleccionado mediante buscador
        if (clienteSeleccionado == null) {
            mostrarMensaje("Validación: Debe buscar y seleccionar un cliente de la lista.", true);
            return;
        }

        if (srv == null || fecha == null || horaTxt.isEmpty()) {
            mostrarMensaje("Validación: Complete todos los campos del turno.", true);
            return;
        }

        // 2. Validacion de fecha (no se puede agendar en dias pasados)
        if (fecha.isBefore(LocalDate.now())) {
            mostrarMensaje("Regla de negocio: La fecha no puede ser anterior a la actual.", true);
            return;
        }

        try {
            int hora = Integer.parseInt(horaTxt);
            if (hora < 8 || hora > 19) {
                mostrarMensaje("Horario laboral: La hora debe encontrarse entre las 8 y las 19 horas.", true);
                return;
            }

            LocalDateTime fechaHoraInicio = fecha.atTime(hora, 0);

            // 3. Validacion si la cita es para hoy pero a una hora ya transcurrida
            if (fecha.isEqual(LocalDate.now()) && fechaHoraInicio.isBefore(LocalDateTime.now())) {
                mostrarMensaje("Hora inválida: La hora seleccionada para el día de hoy ya ha transcurrido.", true);
                return;
            }

            // 4. Validacion de solapamiento de horario contra citas activas
            for (Cita c : listaCitas) {
                if ("PROGRAMADA".equalsIgnoreCase(c.getEstado()) && c.getFechaHora().isEqual(fechaHoraInicio)) {
                    mostrarMensaje("Solapamiento: Ya existe una cita activa programada para las " + hora + ":00 de ese día.", true);
                    return;
                }
            }

            Cita nueva = new Cita(clienteSeleccionado, srv, fechaHoraInicio, "PROGRAMADA");
            citaDAO.agendar(nueva);

            txtHora.clear();
            dpFecha.setValue(null);
            mostrarMensaje("Cita confirmada y registrada en BD para " + clienteSeleccionado.getNombres() + ".", false);
            handleRecargarTodo();
        } catch (NumberFormatException e) {
            mostrarMensaje("Formato inválido: La hora debe ser un número entero (ej: 9, 14, 18).", true);
        } catch (Exception e) {
            mostrarMensaje("Error ORM al persistir cita: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleCancelarCita() {
        Cita sel = tblCitas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarMensaje("Seleccione una cita de la tabla para cancelar.", true);
            return;
        }

        try {
            citaDAO.cancelar(sel.getIdCita());
            mostrarMensaje("Cita #" + sel.getIdCita() + " cancelada en la base de datos.", false);
            handleRecargarTodo();
        } catch (Exception e) {
            mostrarMensaje("Error al actualizar estado en PostgreSQL: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleGenerarResumenHQL() {
        try {
            List<Object[]> resultados = citaDAO.reporteIngresosPorServicio();
            StringBuilder sb = new StringBuilder("HQL 2 (Ingresos por Servicio): ");
            for (Object[] fila : resultados) {
                sb.append(fila[0]).append(" [Citas: ").append(fila[1])
                        .append(" | Total: C$").append(fila[2]).append("]  •  ");
            }
            mostrarMensaje(sb.toString(), false);
        } catch (Exception e) {
            mostrarMensaje("Error en consulta HQL: " + e.getMessage(), true);
        }
    }

    private void mostrarMensaje(String texto, boolean esError) {
        lblEstado.setStyle(esError ? "-fx-text-fill: #b30000; -fx-font-weight: bold;" : "-fx-text-fill: #1b5e20; -fx-font-weight: bold;");
        lblEstado.setText(texto);
    }
}