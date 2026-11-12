package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.CitaDAO;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.dao.ServicioDAO;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.EventosDatos;
import ni.edu.uam.proyectopae.util.Icono;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.Tablas;
import ni.edu.uam.proyectopae.util.Tema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AgendaController implements Refrescable {

    @FXML private TextField txtBuscarCliente;
    @FXML private ListView<Cliente> lstClientesCoincidentes;
    @FXML private Label lblClienteSeleccionado;

    @FXML private ComboBox<Servicio> cmbServicio;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cmbHora;

    @FXML private TableView<Cita> tblCitas;
    @FXML private TableColumn<Cita, Integer> colId;
    @FXML private TableColumn<Cita, String> colCliente;
    @FXML private TableColumn<Cita, String> colServicio;
    @FXML private TableColumn<Cita, LocalDateTime> colFecha;
    @FXML private TableColumn<Cita, Cita> colEstado;

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
        Tablas.formato(colFecha, Tablas::fechaHora);
        Tablas.grafico(colEstado, AgendaController::estadoCita);
        tblCitas.setItems(listaCitas);

        configurarSelectorHoras();
        configurarRestriccionFecha();
        configurarBuscadorClienteReactivo();
        precargarServiciosBase();
        handleRecargarTodo();
    }

    @Override
    public void refrescar() {
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
                    Tema.diaDeshabilitado(this);
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
                String email = cliente.getEmail() != null ? cliente.getEmail().toLowerCase() : "";
                String alergias = cliente.getAlergiasPiel() != null ? cliente.getAlergiasPiel().toLowerCase() : "";

                return nombreCompleto.contains(filtro) || tel.contains(filtro) || email.contains(filtro) || alergias.contains(filtro);
            });
        });

        lstClientesCoincidentes.setItems(clientesFiltrados);
        lstClientesCoincidentes.setPlaceholder(new Label("Sin coincidencias"));

        // Visualizacion detallada del cliente en cada celda
        lstClientesCoincidentes.setCellFactory(param -> new ListCell<Cliente>() {
            @Override
            protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getNombres() + " " + item.getApellidos()
                            + (item.getTelefono() != null ? "  |  Tel: " + item.getTelefono() : "")
                            + "  |  " + item.getAlergiasPiel());
                    setGraphic(new Icono(Icono.Tipo.PERSONA, 14));
                }
            }
        });

        lstClientesCoincidentes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, nuevo) -> {
            if (nuevo != null) {
                clienteSeleccionado = nuevo;
                lblClienteSeleccionado.setText("Cliente: " + nuevo.getNombres() + " " + nuevo.getApellidos()
                        + "  ·  Ficha: " + nuevo.getAlergiasPiel());
                lblClienteSeleccionado.setGraphic(new Icono(Icono.Tipo.EXITO, 15));
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
            if (clienteSeleccionado != null && masterClientes.stream()
                    .noneMatch(c -> c.getIdCliente().equals(clienteSeleccionado.getIdCliente()))) {
                limpiarClienteSeleccionado();
            }
            Servicio servicioActual = cmbServicio.getValue();
            cmbServicio.setItems(FXCollections.observableArrayList(servicioDAO.listarTodos()));
            if (servicioActual != null) {
                cmbServicio.getItems().stream()
                        .filter(s -> s.getIdServicio().equals(servicioActual.getIdServicio()))
                        .findFirst().ifPresent(cmbServicio::setValue);
            }
            listaCitas.setAll(citaDAO.listarCitasConRelaciones());
        } catch (Exception e) {
            Alertas.errorBD("cargar la agenda", e);
        }
    }

    @FXML
    protected void handleAgendarCita() {
        Servicio servicio = cmbServicio.getValue();
        LocalDate fecha = dpFecha.getValue();
        String horaSeleccionada = cmbHora.getValue();

        // 1. Validacion de campos obligatorios (cliente desde el buscador)
        List<String> faltantes = new ArrayList<>();
        if (clienteSeleccionado == null) faltantes.add("Cliente (búsquelo y selecciónelo en la lista)");
        if (servicio == null) faltantes.add("Servicio");
        if (fecha == null) faltantes.add("Fecha");
        if (horaSeleccionada == null) faltantes.add("Hora");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }

        // 2. Validacion de fecha no anterior a hoy
        if (fecha.isBefore(LocalDate.now())) {
            Alertas.datoInvalido("No se pueden programar citas en días anteriores a hoy.");
            return;
        }

        // Mapeo de hora militar
        int hora24 = convertirHora24(horaSeleccionada);
        LocalDateTime fechaHoraCita = fecha.atTime(hora24, 0);

        // 3. Validacion contra horas pasadas durante el mismo dia
        if (fecha.isEqual(LocalDate.now()) && fechaHoraCita.isBefore(LocalDateTime.now())) {
            Alertas.datoInvalido("La hora seleccionada para el día de hoy ya ha transcurrido.");
            return;
        }

        // 4. Validacion de solapamiento horario en PostgreSQL
        for (Cita c : listaCitas) {
            if ("PROGRAMADA".equalsIgnoreCase(c.getEstado()) && c.getFechaHora().isEqual(fechaHoraCita)) {
                Alertas.advertencia("Horario no disponible",
                        "Ya existe un turno reservado para las " + horaSeleccionada + " del " + Tablas.fecha(fecha) + ".");
                return;
            }
        }

        try {
            Cita nueva = new Cita(clienteSeleccionado, servicio, fechaHoraCita, "PROGRAMADA");
            citaDAO.agendar(nueva);

            Alertas.exito("Registro realizado correctamente",
                    "Cita agendada para " + clienteSeleccionado.getNombres() + " " + clienteSeleccionado.getApellidos()
                            + " el " + Tablas.fecha(fecha) + " a las " + horaSeleccionada + ".");
            dpFecha.setValue(null);
            cmbHora.setValue(null);
            handleRecargarTodo();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            Alertas.errorBD("agendar la cita", e);
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
        Cita sel = citaProgramadaSeleccionada("cancelar");
        if (sel == null) return;
        if (!Alertas.confirmar("¿Cancelar la cita #" + sel.getIdCita() + "?",
                sel.getNombreCliente() + " — " + sel.getNombreServicio() + " (" + Tablas.fechaHora(sel.getFechaHora()) + ")")) {
            return;
        }

        try {
            citaDAO.cancelar(sel.getIdCita());
            Alertas.exito("Información actualizada correctamente", "La cita #" + sel.getIdCita() + " fue marcada como CANCELADA.");
            handleRecargarTodo();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            Alertas.errorBD("cancelar la cita", e);
        }
    }

    /** El servicio se prestó y se cobró: pasa a contar como ingreso en el Dashboard financiero. */
    @FXML
    protected void handleMarcarRealizada() {
        Cita sel = citaProgramadaSeleccionada("marcar como realizada");
        if (sel == null) return;
        if (sel.getFechaHora().isAfter(LocalDateTime.now())
                && !Alertas.confirmar("La cita aún no ha llegado a su hora",
                "Está programada para " + Tablas.fechaHora(sel.getFechaHora()) + ". ¿Desea marcarla como realizada de todas formas?")) {
            return;
        }

        try {
            citaDAO.marcarRealizada(sel.getIdCita());
            Alertas.exito("Información actualizada correctamente",
                    "La cita #" + sel.getIdCita() + " fue marcada como REALIZADA y se sumó a los ingresos por servicios.");
            handleRecargarTodo();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            Alertas.errorBD("actualizar la cita", e);
        }
    }

    private Cita citaProgramadaSeleccionada(String accion) {
        Cita sel = tblCitas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Ninguna cita seleccionada", "Seleccione una cita en la tabla para " + accion + ".");
            return null;
        }
        if (!"PROGRAMADA".equalsIgnoreCase(sel.getEstado())) {
            Alertas.advertencia("Acción no permitida",
                    "Solo se pueden " + accion.split(" ")[0] + " citas en estado PROGRAMADA. Esta cita está " + sel.getEstado() + ".");
            return null;
        }
        return sel;
    }

    @FXML
    protected void handleGenerarResumenHQL() {
        try {
            List<Object[]> resultados = citaDAO.reporteIngresosPorServicio();
            if (resultados.isEmpty()) {
                Alertas.info("Resumen por servicio", "No hay citas programadas para resumir.");
                return;
            }
            StringBuilder sb = new StringBuilder("Citas programadas e ingresos proyectados (consulta HQL):\n");
            for (Object[] fila : resultados) {
                sb.append("\n  •  ").append(fila[0]).append(":  ").append(fila[1]).append(" cita(s)  —  ")
                        .append(Tablas.dinero((BigDecimal) fila[2]));
            }
            Alertas.info("Resumen por servicio", sb.toString());
        } catch (Exception e) {
            Alertas.errorBD("generar el resumen", e);
        }
    }

    private void limpiarClienteSeleccionado() {
        clienteSeleccionado = null;
        lblClienteSeleccionado.setText("Ningún cliente seleccionado");
        lblClienteSeleccionado.setGraphic(null);
    }

    private static Node estadoCita(Cita cita) {
        return switch (cita.getEstado() == null ? "" : cita.getEstado().toUpperCase()) {
            case "REALIZADA" -> Tablas.badge("Realizada", Tema.Estado.OK, Icono.Tipo.EXITO);
            case "CANCELADA" -> Tablas.badge("Cancelada", Tema.Estado.NEUTRO, Icono.Tipo.CANCELAR);
            default -> cita.getFechaHora().isBefore(LocalDateTime.now())
                    ? Tablas.badge("Pendiente", Tema.Estado.ALERTA, Icono.Tipo.RELOJ)
                    : Tablas.badge("Programada", Tema.Estado.INFO, Icono.Tipo.AGENDA);
        };
    }
}
