package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import ni.edu.uam.proyectopae.dao.CitaDAO;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.dao.ServicioDAO;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.TemaPAE;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @FXML private TableColumn<Cita, String> colEstado;

    private final CitaDAO citaDAO = new CitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();

    private final ObservableList<Cliente> masterClientes =
            FXCollections.observableArrayList();

    private final ObservableList<Cita> listaCitas =
            FXCollections.observableArrayList();

    private Cliente clienteSeleccionado = null;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCita"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("nombreServicio"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        Formato.columnaFechaHora(colFecha);

        configurarColumnaEstado();
        configurarSelectorHoras();
        configurarRestriccionFecha();
        configurarBuscadorClienteReactivo();
        precargarServiciosBase();
        handleRecargarTodo();
    }

    /**
     * Presenta los estados de las citas con iconos y propiedades JavaFX.
     */
    private void configurarColumnaEstado() {
        colEstado.setCellFactory(col -> new TableCell<>() {

            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                getStyleClass().removeAll("estado-completada", "estado-cancelada", "estado-pendiente");

                if (empty || estado == null) {
                    setText(null);
                    setGraphic(null);
                    setTextFill(TemaPAE.texto());
                    setFont(null);
                    return;
                }

                String icono;
                Color color;

                switch (estado) {
                    case "COMPLETADA" -> {
                        icono = "fas-check-circle";
                        color = TemaPAE.verdeTexto();
                        getStyleClass().add("estado-completada");
                    }
                    case "CANCELADA" -> {
                        icono = "fas-times-circle";
                        color = TemaPAE.rosaTexto();
                        getStyleClass().add("estado-cancelada");
                    }
                    default -> {
                        icono = "fas-clock";
                        color = TemaPAE.azulTexto();
                        getStyleClass().add("estado-pendiente");
                    }
                }

                FontIcon iconoEstado = new FontIcon(icono);
                iconoEstado.setIconSize(14);
                iconoEstado.setIconColor(color);

                setGraphic(iconoEstado);
                setText(estado);
                setTextFill(color);
            }
        });
    }

    @Override
    public void refrescar() {
        handleRecargarTodo();
    }

    private void configurarSelectorHoras() {
        cmbHora.setItems(FXCollections.observableArrayList(
                "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
                "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM",
                "04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM"
        ));
    }

    private void configurarRestriccionFecha() {
        dpFecha.setDayCellFactory(picker -> new DateCell() {

            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                boolean fechaNoDisponible = empty
                        || date == null
                        || date.isBefore(LocalDate.now());

                setDisable(fechaNoDisponible);
                setOpacity(fechaNoDisponible ? 0.45 : 1.0);
            }
        });
    }

    private void configurarBuscadorClienteReactivo() {
        FilteredList<Cliente> clientesFiltrados =
                new FilteredList<>(masterClientes, p -> true);

        txtBuscarCliente.textProperty().addListener(
                (observable, valorAnterior, valorNuevo) -> {

                    clientesFiltrados.setPredicate(cliente -> {
                        if (valorNuevo == null || valorNuevo.trim().isEmpty()) {
                            return true;
                        }

                        String filtro = valorNuevo.toLowerCase().trim();

                        String nombreCompleto =
                                (cliente.getNombres() + " " + cliente.getApellidos())
                                        .toLowerCase();

                        String telefono = cliente.getTelefono() != null
                                ? cliente.getTelefono().toLowerCase()
                                : "";

                        String alergias = cliente.getAlergiasPiel() != null
                                ? cliente.getAlergiasPiel().toLowerCase()
                                : "";

                        return nombreCompleto.contains(filtro)
                                || telefono.contains(filtro)
                                || alergias.contains(filtro);
                    });
                }
        );

        lstClientesCoincidentes.setItems(clientesFiltrados);

        lstClientesCoincidentes.setCellFactory(param ->
                new ListCell<>() {

                    @Override
                    protected void updateItem(Cliente item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getNombres() + " "
                                    + item.getApellidos()
                                    + " | Tel: " + item.getTelefono()
                                    + " | " + item.getAlergiasPiel());
                        }
                    }
                }
        );

        lstClientesCoincidentes.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, nuevo) -> {

                    if (nuevo != null) {
                        clienteSeleccionado = nuevo;

                        lblClienteSeleccionado.setText(
                                "Cliente: " + nuevo.getNombreCompleto()
                                        + " | Tel: "
                                        + (nuevo.getTelefono() == null
                                        ? "-" : nuevo.getTelefono())
                                        + " | Alergias: "
                                        + nuevo.getAlergiasPiel()
                        );

                        if (!lblClienteSeleccionado.getStyleClass()
                                .contains("editando")) {
                            lblClienteSeleccionado.getStyleClass()
                                    .add("editando");
                        }
                    }
                });
    }

    private void precargarServiciosBase() {
        try {
            if (servicioDAO.listarTodos().isEmpty()) {
                servicioDAO.guardar(new Servicio(
                        "Maquillaje Social de Día",
                        45,
                        new BigDecimal("25.00")
                ));

                servicioDAO.guardar(new Servicio(
                        "Maquillaje de Gala / Noche",
                        60,
                        new BigDecimal("40.00")
                ));

                servicioDAO.guardar(new Servicio(
                        "Maquillaje Nupcial / Novia",
                        90,
                        new BigDecimal("75.00")
                ));
            }
        } catch (Exception ignored) {
        }
    }

    @FXML
    public void handleRecargarTodo() {
        try {
            masterClientes.setAll(clienteDAO.listarTodos());

            Servicio servicioActual = cmbServicio.getValue();

            cmbServicio.setItems(
                    FXCollections.observableArrayList(servicioDAO.listarTodos())
            );

            if (servicioActual != null) {
                cmbServicio.getItems().stream()
                        .filter(sv -> sv.getIdServicio()
                                .equals(servicioActual.getIdServicio()))
                        .findFirst()
                        .ifPresent(cmbServicio::setValue);
            }

            listaCitas.setAll(citaDAO.listarCitasConRelaciones());
            tblCitas.setItems(listaCitas);

        } catch (Exception e) {
            Alertas.error(
                    "Error de conexión",
                    "No se pudo consultar PostgreSQL:\n"
                            + VentanaManager.causaRaiz(e)
            );
        }
    }

    @FXML
    protected void handleAgendarCita() {
        Servicio servicio = cmbServicio.getValue();
        LocalDate fecha = dpFecha.getValue();
        String horaSeleccionada = cmbHora.getValue();

        if (clienteSeleccionado == null) {
            Alertas.validacion(
                    "Busque y seleccione un cliente de la lista."
            );
            return;
        }

        if (servicio == null || fecha == null || horaSeleccionada == null) {
            Alertas.validacion(
                    "Seleccione el servicio, la fecha y la hora de la cita."
            );
            return;
        }

        if (fecha.isBefore(LocalDate.now())) {
            Alertas.validacion(
                    "No se pueden programar citas en días anteriores."
            );
            return;
        }

        int hora24 = convertirHora24(horaSeleccionada);
        LocalDateTime fechaHoraCita = fecha.atTime(hora24, 0);

        if (fecha.isEqual(LocalDate.now())
                && fechaHoraCita.isBefore(LocalDateTime.now())) {
            Alertas.validacion(
                    "La hora seleccionada para el día de hoy ya transcurrió."
            );
            return;
        }

        for (Cita cita : listaCitas) {
            if ("PROGRAMADA".equalsIgnoreCase(cita.getEstado())
                    && cita.getFechaHora().isEqual(fechaHoraCita)) {

                Alertas.advertencia(
                        "Horario ocupado",
                        "Ya existe una cita programada a las "
                                + horaSeleccionada + " de esa fecha."
                );
                return;
            }
        }

        try {
            Cita nueva = new Cita(
                    clienteSeleccionado,
                    servicio,
                    fechaHoraCita,
                    "PROGRAMADA"
            );

            citaDAO.agendar(nueva);

            dpFecha.setValue(null);
            cmbHora.setValue(null);

            handleRecargarTodo();
            VentanaManager.notificarCambios(this);

            Alertas.info(
                    "Cita agendada",
                    "Cita agendada para "
                            + clienteSeleccionado.getNombreCompleto()
                            + " el " + Formato.fecha(fecha)
                            + " a las " + horaSeleccionada + "."
            );

        } catch (Exception e) {
            Alertas.error(
                    "Error al agendar",
                    VentanaManager.causaRaiz(e)
            );
        }
    }

    @FXML
    private void handleConfigurarProductos() {
        Servicio seleccionado = cmbServicio.getValue();

        if (seleccionado == null) {
            Alertas.validacion("Selecciona un servicio.");
            return;
        }

        try {
            Servicio servicio = servicioDAO.buscarConProductos(
                    seleccionado.getIdServicio()
            );

            if (servicio == null) {
                Alertas.validacion("El servicio ya no existe.");
                return;
            }

            ListView<Producto> lista = new ListView<>();

            lista.getSelectionModel().setSelectionMode(
                    SelectionMode.MULTIPLE
            );

            lista.getItems().setAll(
                    new ProductoDAO().listarConRelaciones()
            );

            for (int i = 0; i < lista.getItems().size(); i++) {
                Integer id = lista.getItems().get(i).getIdProducto();

                boolean asociado = servicio.getProductos().stream()
                        .anyMatch(producto ->
                                producto.getIdProducto().equals(id)
                        );

                if (asociado) {
                    lista.getSelectionModel().select(i);
                }
            }

            lista.setPrefSize(450, 300);

            ButtonType guardar = new ButtonType(
                    "Guardar",
                    ButtonBar.ButtonData.OK_DONE
            );

            Dialog<ButtonType> dialogo = new Dialog<>();

            dialogo.setTitle("Productos del servicio");

            dialogo.setHeaderText(
                    servicio.getNombreServicio()
                            + "\nUsa Ctrl para seleccionar varios productos."
            );

            var ventana = VentanaManager.ventanaActiva();

            if (ventana != null) {
                dialogo.initOwner(ventana);
            }

            dialogo.getDialogPane().setContent(lista);

            dialogo.getDialogPane().getButtonTypes().addAll(
                    guardar,
                    ButtonType.CANCEL
            );

            var respuesta = dialogo.showAndWait();

            if (respuesta.isPresent() && respuesta.get() == guardar) {

                List<Integer> ids = lista.getSelectionModel()
                        .getSelectedItems()
                        .stream()
                        .map(Producto::getIdProducto)
                        .toList();

                servicioDAO.actualizarProductos(
                        servicio.getIdServicio(),
                        ids
                );

                Alertas.info(
                        "Productos actualizados",
                        "Se guardó la configuración del servicio."
                );

                VentanaManager.notificarCambios(this);
            }

        } catch (Exception error) {
            Alertas.error(
                    "No se pudieron configurar los productos",
                    VentanaManager.causaRaiz(error)
            );
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
        Cita seleccionada = citaProgramadaSeleccionada("cancelar");

        if (seleccionada == null) {
            return;
        }

        if (!Alertas.confirmar(
                "Cancelar cita",
                "¿Cancelar la cita #" + seleccionada.getIdCita()
                        + " de " + seleccionada.getNombreCliente() + "?"
        )) {
            return;
        }

        try {
            citaDAO.cancelar(seleccionada.getIdCita());

            handleRecargarTodo();
            VentanaManager.notificarCambios(this);

            Alertas.info(
                    "Cita cancelada",
                    "La cita #" + seleccionada.getIdCita()
                            + " fue marcada como CANCELADA."
            );

        } catch (Exception e) {
            Alertas.error(
                    "Error al cancelar la cita",
                    VentanaManager.causaRaiz(e)
            );
        }
    }

    @FXML
    protected void handleCompletarCita() {
        Cita seleccionada = citaProgramadaSeleccionada("completar");

        if (seleccionada == null) {
            return;
        }

        if (seleccionada.getFechaHora().isAfter(LocalDateTime.now())
                && !Alertas.confirmar(
                "Completar cita",
                "La cita aún no ha llegado a su hora. "
                        + "¿Marcarla como completada de todos modos?"
        )) {
            return;
        }

        try {
            citaDAO.completar(seleccionada.getIdCita());

            handleRecargarTodo();
            VentanaManager.notificarCambios(this);

            Alertas.info(
                    "Cita completada",
                    "Servicio \"" + seleccionada.getNombreServicio()
                            + "\" registrado como realizado."
            );

        } catch (Exception e) {
            Alertas.error(
                    "Error al completar la cita",
                    VentanaManager.causaRaiz(e)
            );
        }
    }

    private Cita citaProgramadaSeleccionada(String accion) {
        Cita seleccionada =
                tblCitas.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            Alertas.advertencia(
                    "Seleccione una cita",
                    "Seleccione en la tabla la cita que desea "
                            + accion + "."
            );
            return null;
        }

        if (!"PROGRAMADA".equals(seleccionada.getEstado())) {
            Alertas.advertencia(
                    "Acción no permitida",
                    "Solo se puede " + accion
                            + " una cita PROGRAMADA (estado actual: "
                            + seleccionada.getEstado() + ")."
            );
            return null;
        }

        return seleccionada;
    }

    @FXML
    protected void handleGenerarResumenHQL() {
        try {
            List<Object[]> resultados =
                    citaDAO.reporteIngresosPorServicio();

            if (resultados.isEmpty()) {
                Alertas.info(
                        "Ingresos proyectados",
                        "No hay citas programadas pendientes."
                );
                return;
            }

            StringBuilder sb = new StringBuilder();
            BigDecimal total = BigDecimal.ZERO;

            for (Object[] fila : resultados) {
                BigDecimal monto = Formato.aBig(fila[2]);
                total = total.add(monto);

                sb.append("• ")
                        .append(fila[0])
                        .append(": ")
                        .append(fila[1])
                        .append(" citas → ")
                        .append(Formato.moneda(monto))
                        .append("\n");
            }

            sb.append("\nTotal proyectado: ")
                    .append(Formato.moneda(total));

            Alertas.info(
                    "Ingresos proyectados por servicio (HQL 2)",
                    sb.toString()
            );

        } catch (Exception e) {
            Alertas.error(
                    "Error en consulta HQL",
                    VentanaManager.causaRaiz(e)
            );
        }
    }
}
