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
    @FXML private ComboBox<String> cmbHora;
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

        configurarSelectorHoras();
        configurarRestriccionFecha();
        configurarBuscadorClienteReactivo();
        precargarServiciosBase();
        handleRecargarTodo();
    }

    private void configurarSelectorHoras() {
        cmbHora.setItems(FXCollections.observableArrayList(
                "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
                "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM",
                "06:00 PM", "07:00 PM"
        ));
    }

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

    // Busqueda reactiva caracter por caracter
    private void configurarBuscadorClienteReactivo() {
        FilteredList<Cliente> clientesFiltrados = new FilteredList<>(masterClientes, p -> true);

        txtBuscarCliente.textProperty().addListener((observable, valorAnterior, valorNuevo) -> {
            clientesFiltrados.setPredicate(cliente -> {
                if (valorNuevo == null || valorNuevo.trim().isEmpty()) {
                    return true;
                }
                String filtro = valorNuevo.toLowerCase().trim();
                String nombreCompleto = (cliente.getNombres() + " " + cliente.getApellidos()).toLowerCase();
                String tel = cliente.getTelefono() != null ? cliente.getTelefono().toLowerCase() : "";
                String alergias = cliente.getAlergiasPiel() != null ? cliente.getAlergiasPiel().toLowerCase() : "";

                return nombreCompleto.contains(filtro) || tel.contains(filtro) || alergias.contains(filtro);
            });
        });

        lstClientesCoincidentes.setItems(clientesFiltrados);

        // Visualizacion detallada del cliente en cada celda
        lstClientesCoincidentes.setCellFactory(param -> new ListCell<Cliente>() {
            @Override
            protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNombres() + " " + item.getApellidos() + " | Tel: " + item.getTelefono() + " | " + item.getAlergiasPiel());
                }
            }
        });

        lstClientesCoincidentes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, nuevo) -> {
            if (nuevo != null) {
                clienteSeleccionado = nuevo;
                lblClienteSeleccionado.setText("Cliente: " + nuevo.getNombres() + " " + nuevo.getApellidos() +
                        " [Tel: " + nuevo.getTelefono() + " | Ficha: " + nuevo.getAlergiasPiel() + "]");
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
            mostrarMensaje("Error al consultar PostgreSQL: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleAgendarCita() {
        Servicio servicio = cmbServicio.getValue();
        LocalDate fecha = dpFecha.getValue();
        String horaSeleccionada = cmbHora.getValue();

        // 1. Validacion de seleccion de cliente desde el buscador
        if (clienteSeleccionado == null) {
            mostrarMensaje("Validación: Busque y seleccione un cliente de la lista.", true);
            return;
        }

        if (servicio == null || fecha == null || horaSeleccionada == null) {
            mostrarMensaje("Validación: Complete todos los campos requeridos.", true);
            return;
        }

        // 2. Validacion de fecha no anterior a hoy
        if (fecha.isBefore(LocalDate.now())) {
            mostrarMensaje("Validación: No se pueden programar citas en días anteriores.", true);
            return;
        }

        // Mapeo de hora militar
        int hora24 = convertirHora24(horaSeleccionada);
        LocalDateTime fechaHoraCita = fecha.atTime(hora24, 0);

        // 3. Validacion contra horas pasadas durante el mismo dia
        if (fecha.isEqual(LocalDate.now()) && fechaHoraCita.isBefore(LocalDateTime.now())) {
            mostrarMensaje("Validación: La hora seleccionada para el día de hoy ya ha transcurrido.", true);
            return;
        }

        // 4. Validacion de solapamiento horario en PostgreSQL
        for (Cita c : listaCitas) {
            if ("PROGRAMADA".equalsIgnoreCase(c.getEstado()) && c.getFechaHora().isEqual(fechaHoraCita)) {
                mostrarMensaje("Conflicto: Ya existe un turno reservado para las " + horaSeleccionada + " de esa fecha.", true);
                return;
            }
        }

        try {
            Cita nueva = new Cita(clienteSeleccionado, servicio, fechaHoraCita, "PROGRAMADA");
            citaDAO.agendar(nueva);

            dpFecha.setValue(null);
            cmbHora.setValue(null);
            mostrarMensaje("Cita agendada exitosamente para " + clienteSeleccionado.getNombres() + " a las " + horaSeleccionada + ".", false);
            handleRecargarTodo();
        } catch (Exception e) {
            mostrarMensaje("Error ORM al persistir cita: " + e.getMessage(), true);
        }
    }

    private int convertirHora24(String horaFormato) {
        return switch (horaFormato) {
            case "08:00 AM" -> 8;
            case "09:00 AM" -> 9;
            case "10:00 AM" -> 10;
            case "11:00 AM" -> 11;
            case "12:00 PM" -> 12;
            case "01:00 PM" -> 13;
            case "02:00 PM" -> 14;
            case "03:00 PM" -> 15;
            case "04:00 PM" -> 16;
            case "05:00 PM" -> 17;
            case "06:00 PM" -> 18;
            case "07:00 PM" -> 19;
            default -> 8;
        };
    }

    @FXML
    protected void handleCancelarCita() {
        Cita sel = tblCitas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarMensaje("Seleccione una cita activa en la tabla.", true);
            return;
        }

        try {
            citaDAO.cancelar(sel.getIdCita());
            mostrarMensaje("Cita #" + sel.getIdCita() + " marcada como CANCELADA.", false);
            handleRecargarTodo();
        } catch (Exception e) {
            mostrarMensaje("Error al cancelar la cita: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleGenerarResumenHQL() {
        try {
            List<Object[]> resultados = citaDAO.reporteIngresosPorServicio();
            StringBuilder sb = new StringBuilder("HQL 2 (Ingresos por Servicio): ");
            for (Object[] fila : resultados) {
                sb.append(fila[0]).append(" [Citas: ").append(fila[1])
                        .append(" | Ingresos: C$").append(fila[2]).append("]  •  ");
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