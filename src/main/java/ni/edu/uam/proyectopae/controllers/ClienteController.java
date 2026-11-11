package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.models.Cliente;

public class ClienteController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtAlergias;
    @FXML private Label lblEstado;

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colApellidos;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colAlergias;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCliente"));
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colAlergias.setCellValueFactory(new PropertyValueFactory<>("alergiasPiel"));

        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, nuevo) -> {
            if (nuevo != null) {
                txtNombres.setText(nuevo.getNombres());
                txtApellidos.setText(nuevo.getApellidos());
                txtTelefono.setText(nuevo.getTelefono());
                txtAlergias.setText(nuevo.getAlergiasPiel());
            }
        });

        refrescarTabla();
    }

    public void refrescarTabla() {
        try {
            listaClientes.setAll(clienteDAO.listarTodos());
            tblClientes.setItems(listaClientes);
        } catch (Exception e) {
            mostrarMensaje("Error al conectar con la base de datos: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleGuardarCliente() {
        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String alergias = txtAlergias.getText().trim();

        if (nombres.isEmpty() || apellidos.isEmpty()) {
            mostrarMensaje("Validación: Nombres y Apellidos son obligatorios.", true);
            return;
        }

        if (!telefono.isEmpty() && !telefono.matches("^[0-9+ -]{7,15}$")) {
            mostrarMensaje("Validación: El formato del teléfono contiene caracteres inválidos.", true);
            return;
        }

        try {
            Cliente seleccionado = tblClientes.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getNombres().equals(nombres) && seleccionado.getApellidos().equals(apellidos)) {
                seleccionado.setTelefono(telefono);
                seleccionado.setAlergiasPiel(alergias.isEmpty() ? "Sin alergias declaradas" : alergias);
                clienteDAO.actualizar(seleccionado);
                mostrarMensaje("Cliente actualizado con éxito.", false);
            } else {
                Cliente nuevo = new Cliente(nombres, apellidos, telefono, alergias.isEmpty() ? "Sin alergias declaradas" : alergias);
                clienteDAO.guardar(nuevo);
                mostrarMensaje("Cliente registrado con éxito en PostgreSQL.", false);
            }
            handleLimpiarCampos();
            refrescarTabla();
        } catch (Exception ex) {
            mostrarMensaje("Error de persistencia: " + ex.getMessage(), true);
        }
    }

    @FXML
    protected void handleEliminarCliente() {
        Cliente seleccionado = tblClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Seleccione un cliente en la tabla para eliminar.", true);
            return;
        }

        try {
            clienteDAO.eliminar(seleccionado.getIdCliente());
            mostrarMensaje("Cliente eliminado con éxito.", false);
            handleLimpiarCampos();
            refrescarTabla();
        } catch (Exception ex) {
            mostrarMensaje("No se puede eliminar: El cliente tiene citas o consumos asociados.", true);
        }
    }

    @FXML
    protected void handleLimpiarCampos() {
        txtNombres.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtAlergias.clear();
        tblClientes.getSelectionModel().clearSelection();
    }

    private void mostrarMensaje(String texto, boolean esError) {
        lblEstado.setStyle(esError ? "-fx-text-fill: #b30000; -fx-font-weight: bold;" : "-fx-text-fill: #1b5e20; -fx-font-weight: bold;");
        lblEstado.setText(texto);
    }
}