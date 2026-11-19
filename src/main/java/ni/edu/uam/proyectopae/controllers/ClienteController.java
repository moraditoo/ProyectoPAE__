package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.VentanaManager;

public class ClienteController implements Refrescable {

    private static final String REGEX_CORREO = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private ComboBox<String> cmbSexo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextArea txtAlergias;
    @FXML private TextField txtBuscar;
    @FXML private Label lblModo;
    @FXML private Label lblTotal;
    @FXML private Button btnGuardar;

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colApellidos;
    @FXML private TableColumn<Cliente, String> colSexo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colAlergias;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
    private final FilteredList<Cliente> clientesFiltrados = new FilteredList<>(listaClientes, c -> true);

    /** Cliente seleccionado en la tabla; null = se registrara uno nuevo. */
    private Cliente clienteEnEdicion;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCliente"));
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colSexo.setCellValueFactory(new PropertyValueFactory<>("sexo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colAlergias.setCellValueFactory(new PropertyValueFactory<>("alergiasPiel"));

        cmbSexo.setItems(FXCollections.observableArrayList("Femenino", "Masculino", "Otro"));

        tblClientes.setItems(clientesFiltrados);
        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) cargarEnFormulario(sel);
        });

        txtBuscar.textProperty().addListener((obs, a, texto) -> aplicarFiltro(texto));

        refrescarTabla();
        actualizarModo();
    }

    @Override
    public void refrescar() {
        refrescarTabla();
    }

    public void refrescarTabla() {
        try {
            listaClientes.setAll(clienteDAO.listarTodos());
            aplicarFiltro(txtBuscar.getText());
        } catch (Exception e) {
            Alertas.error("Error de conexión", "No se pudo consultar la base de datos:\n" + VentanaManager.causaRaiz(e));
        }
    }

    private void aplicarFiltro(String texto) {
        String f = texto == null ? "" : texto.toLowerCase().trim();
        clientesFiltrados.setPredicate(c -> f.isEmpty()
                || c.getNombreCompleto().toLowerCase().contains(f)
                || (c.getTelefono() != null && c.getTelefono().contains(f))
                || (c.getCorreo() != null && c.getCorreo().toLowerCase().contains(f)));
        lblTotal.setText(clientesFiltrados.size() + " de " + listaClientes.size() + " clientes");
    }

    private void cargarEnFormulario(Cliente c) {
        clienteEnEdicion = c;
        txtNombres.setText(c.getNombres());
        txtApellidos.setText(c.getApellidos());
        cmbSexo.setValue(c.getSexo());
        txtTelefono.setText(c.getTelefono());
        txtCorreo.setText(c.getCorreo());
        txtAlergias.setText(c.getAlergiasPiel());
        actualizarModo();
    }

    private void actualizarModo() {
        lblModo.getStyleClass().remove("editando");
        if (clienteEnEdicion == null) {
            lblModo.setText("Nuevo cliente");
            btnGuardar.setText("Guardar");
        } else {
            lblModo.setText("Editando cliente #" + clienteEnEdicion.getIdCliente());
            lblModo.getStyleClass().add("editando");
            btnGuardar.setText("Guardar cambios");
        }
    }

    /** Un solo boton: registra si es nuevo o actualiza si hay un cliente seleccionado. */
    @FXML
    protected void handleGuardarCliente() {
        String nombres = texto(txtNombres);
        String apellidos = texto(txtApellidos);
        String sexo = cmbSexo.getValue();
        String telefono = texto(txtTelefono);
        String correo = texto(txtCorreo).toLowerCase();
        String alergias = txtAlergias.getText() == null ? "" : txtAlergias.getText().trim();

        if (nombres.length() < 2 || apellidos.length() < 2) {
            Alertas.validacion("Nombres y Apellidos son obligatorios (mínimo 2 caracteres).");
            return;
        }
        if (sexo == null) {
            Alertas.validacion("Seleccione el sexo del cliente.");
            return;
        }
        if (!telefono.isEmpty() && !telefono.matches("^[0-9+ -]{7,15}$")) {
            Alertas.validacion("El teléfono solo puede contener números, espacios, + o - (7 a 15 caracteres).");
            return;
        }
        if (!correo.isEmpty() && !correo.matches(REGEX_CORREO)) {
            Alertas.validacion("El correo electrónico no tiene un formato válido.\nEjemplo: cliente@correo.com");
            return;
        }

        try {
            Integer idActual = clienteEnEdicion != null ? clienteEnEdicion.getIdCliente() : null;
            if (!correo.isEmpty() && clienteDAO.existeCorreo(correo, idActual)) {
                Alertas.validacion("El correo " + correo + " ya está registrado para otro cliente.");
                return;
            }

            String alergiasFinal = alergias.isEmpty() ? "Sin alergias declaradas" : alergias;
            String correoFinal = correo.isEmpty() ? null : correo;

            if (clienteEnEdicion != null) {
                clienteEnEdicion.setNombres(nombres);
                clienteEnEdicion.setApellidos(apellidos);
                clienteEnEdicion.setSexo(sexo);
                clienteEnEdicion.setTelefono(telefono);
                clienteEnEdicion.setCorreo(correoFinal);
                clienteEnEdicion.setAlergiasPiel(alergiasFinal);
                clienteDAO.actualizar(clienteEnEdicion);
                Alertas.info("Cliente actualizado", "Los datos de " + nombres + " " + apellidos + " se actualizaron correctamente.");
            } else {
                clienteDAO.guardar(new Cliente(nombres, apellidos, telefono, correoFinal, sexo, alergiasFinal));
                Alertas.info("Cliente registrado", nombres + " " + apellidos + " fue registrado correctamente.");
            }

            handleLimpiarCampos();
            refrescarTabla();
            VentanaManager.notificarCambios(this);
        } catch (Exception ex) {
            Alertas.error("Error al guardar", VentanaManager.causaRaiz(ex));
        }
    }

    @FXML
    protected void handleEliminarCliente() {
        Cliente sel = tblClientes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Eliminar cliente", "Seleccione un cliente en la tabla para eliminar.");
            return;
        }
        if (!Alertas.confirmar("Eliminar cliente", "¿Está seguro de eliminar a " + sel.getNombreCompleto() + "?\nEsta acción no se puede deshacer.")) {
            return;
        }

        try {
            clienteDAO.eliminar(sel.getIdCliente());
            handleLimpiarCampos();
            refrescarTabla();
            VentanaManager.notificarCambios(this);
            Alertas.info("Cliente eliminado", "El cliente fue eliminado correctamente.");
        } catch (Exception ex) {
            Alertas.error("No se puede eliminar", "El cliente tiene citas o ventas asociadas.");
        }
    }

    @FXML
    protected void handleLimpiarCampos() {
        clienteEnEdicion = null;
        txtNombres.clear();
        txtApellidos.clear();
        cmbSexo.setValue(null);
        txtTelefono.clear();
        txtCorreo.clear();
        txtAlergias.clear();
        tblClientes.getSelectionModel().clearSelection();
        actualizarModo();
        txtNombres.requestFocus();
    }

    private static String texto(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }
}
