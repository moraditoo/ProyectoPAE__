package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.EventosDatos;
import ni.edu.uam.proyectopae.util.Icono;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.Tema;

import java.util.ArrayList;
import java.util.List;

public class ClienteController implements Refrescable {

    private static final String SIN_ALERGIAS = "Sin alergias declaradas";
    private static final String REGEX_NOMBRE = "^[\\p{L} .'-]{2,60}$";
    private static final String REGEX_TELEFONO = "^[0-9+ -]{7,15}$";
    private static final String REGEX_EMAIL = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$";

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cmbSexo;
    @FXML private TextArea txtAlergias;
    @FXML private Label lblModo;
    @FXML private TextField txtBuscar;
    @FXML private Label lblTotal;

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colApellidos;
    @FXML private TableColumn<Cliente, String> colSexo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEmail;
    @FXML private TableColumn<Cliente, String> colAlergias;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();

    /** Cliente seleccionado que se está editando; null cuando el formulario registra un cliente nuevo. */
    private Cliente clienteEnEdicion;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCliente"));
        colNombres.setCellValueFactory(new PropertyValueFactory<>("nombres"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colSexo.setCellValueFactory(new PropertyValueFactory<>("sexo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colAlergias.setCellValueFactory(new PropertyValueFactory<>("alergiasPiel"));

        cmbSexo.setItems(FXCollections.observableArrayList("Femenino", "Masculino", "Otro"));

        FilteredList<Cliente> filtrados = new FilteredList<>(listaClientes, c -> true);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> {
            String filtro = texto == null ? "" : texto.trim().toLowerCase();
            filtrados.setPredicate(c -> filtro.isEmpty()
                    || (c.getNombres() + " " + c.getApellidos()).toLowerCase().contains(filtro)
                    || (c.getTelefono() != null && c.getTelefono().toLowerCase().contains(filtro))
                    || (c.getEmail() != null && c.getEmail().toLowerCase().contains(filtro)));
            actualizarTotal(filtrados.size());
        });
        SortedList<Cliente> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblClientes.comparatorProperty());
        tblClientes.setItems(ordenados);

        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                cargarEnFormulario(nuevo);
            }
        });

        actualizarModo();
        refrescarTabla();
    }

    @Override
    public void refrescar() {
        refrescarTabla();
    }

    @FXML
    public void refrescarTabla() {
        try {
            listaClientes.setAll(clienteDAO.listarTodos());
            actualizarTotal(tblClientes.getItems().size());
        } catch (Exception e) {
            Alertas.errorBD("cargar los clientes", e);
        }
    }

    private void cargarEnFormulario(Cliente cliente) {
        clienteEnEdicion = cliente;
        txtNombres.setText(cliente.getNombres());
        txtApellidos.setText(cliente.getApellidos());
        txtTelefono.setText(cliente.getTelefono());
        txtEmail.setText(cliente.getEmail());
        cmbSexo.setValue(cliente.getSexo());
        txtAlergias.setText(cliente.getAlergiasPiel());
        actualizarModo();
    }

    /** Botón único "Guardar": inserta si no hay cliente seleccionado y actualiza si lo hay. */
    @FXML
    protected void handleGuardarCliente() {
        String nombres = texto(txtNombres);
        String apellidos = texto(txtApellidos);
        String telefono = texto(txtTelefono);
        String email = texto(txtEmail).toLowerCase();
        String sexo = cmbSexo.getValue();
        String alergias = txtAlergias.getText() != null ? txtAlergias.getText().trim() : "";

        List<String> faltantes = new ArrayList<>();
        if (nombres.isEmpty()) faltantes.add("Nombres");
        if (apellidos.isEmpty()) faltantes.add("Apellidos");
        if (sexo == null) faltantes.add("Sexo");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }

        if (!nombres.matches(REGEX_NOMBRE) || !apellidos.matches(REGEX_NOMBRE)) {
            Alertas.datoInvalido("Los nombres y apellidos deben tener entre 2 y 60 caracteres y contener solo letras.");
            return;
        }
        if (!telefono.isEmpty() && !telefono.matches(REGEX_TELEFONO)) {
            Alertas.datoInvalido("El teléfono debe tener entre 7 y 15 dígitos (se permiten +, espacios y guiones).");
            return;
        }
        if (!email.isEmpty() && (email.length() > 100 || !email.matches(REGEX_EMAIL))) {
            Alertas.datoInvalido("El correo electrónico no tiene un formato válido. Ejemplo: nombre@correo.com");
            return;
        }
        if (!email.isEmpty() && correoEnUso(email)) {
            Alertas.datoInvalido("El correo \"" + email + "\" ya está registrado para otro cliente.");
            return;
        }

        String alergiasFinal = alergias.isEmpty() ? SIN_ALERGIAS : alergias;
        String emailFinal = email.isEmpty() ? null : email;
        String telefonoFinal = telefono.isEmpty() ? null : telefono;

        try {
            if (clienteEnEdicion != null) {
                clienteEnEdicion.setNombres(nombres);
                clienteEnEdicion.setApellidos(apellidos);
                clienteEnEdicion.setTelefono(telefonoFinal);
                clienteEnEdicion.setEmail(emailFinal);
                clienteEnEdicion.setSexo(sexo);
                clienteEnEdicion.setAlergiasPiel(alergiasFinal);
                clienteDAO.actualizar(clienteEnEdicion);
                Alertas.exito("Información actualizada correctamente",
                        "Los datos de " + nombres + " " + apellidos + " se actualizaron en la base de datos.");
            } else {
                clienteDAO.guardar(new Cliente(nombres, apellidos, telefonoFinal, emailFinal, sexo, alergiasFinal));
                Alertas.exito("Registro realizado correctamente",
                        "El cliente " + nombres + " " + apellidos + " fue registrado en la base de datos.");
            }
            handleLimpiarCampos();
            refrescarTabla();
            EventosDatos.publicar(this);
        } catch (Exception ex) {
            Alertas.errorBD("guardar el cliente", ex);
            refrescarTabla();
        }
    }

    @FXML
    protected void handleEliminarCliente() {
        Cliente seleccionado = clienteEnEdicion != null ? clienteEnEdicion : tblClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.advertencia("Ningún cliente seleccionado", "Seleccione un cliente en la tabla para eliminarlo.");
            return;
        }
        if (!Alertas.confirmar("¿Eliminar al cliente " + seleccionado + "?",
                "También se eliminarán sus citas registradas. Esta acción no se puede deshacer.")) {
            return;
        }

        try {
            clienteDAO.eliminar(seleccionado.getIdCliente());
            Alertas.exito("Cliente eliminado", "El cliente " + seleccionado + " fue eliminado.");
            handleLimpiarCampos();
            refrescarTabla();
            EventosDatos.publicar(this);
        } catch (Exception ex) {
            if (Alertas.esErrorConexion(ex)) {
                Alertas.errorBD("eliminar el cliente", ex);
            } else {
                Alertas.error("No se puede eliminar el cliente",
                        "El cliente tiene ventas o consumos asociados y debe conservarse para el historial.");
            }
        }
    }

    @FXML
    protected void handleLimpiarCampos() {
        txtNombres.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtAlergias.clear();
        cmbSexo.setValue(null);
        tblClientes.getSelectionModel().clearSelection();
        clienteEnEdicion = null;
        actualizarModo();
        txtNombres.requestFocus();
    }

    private boolean correoEnUso(String email) {
        return listaClientes.stream().anyMatch(c -> c.getEmail() != null
                && c.getEmail().equalsIgnoreCase(email)
                && (clienteEnEdicion == null || !c.getIdCliente().equals(clienteEnEdicion.getIdCliente())));
    }

    private void actualizarModo() {
        if (clienteEnEdicion != null) {
            lblModo.setText("Editando #" + clienteEnEdicion.getIdCliente());
            lblModo.setGraphic(new Icono(Icono.Tipo.GUARDAR, 15));
        } else {
            lblModo.setText("Nuevo registro");
            lblModo.setGraphic(new Icono(Icono.Tipo.NUEVO, 15));
        }
        Tema.indicadorModo(lblModo, clienteEnEdicion != null);
    }

    private void actualizarTotal(int visibles) {
        lblTotal.setText(visibles + " de " + listaClientes.size() + " cliente(s)");
    }

    private static String texto(TextField campo) {
        return campo.getText() != null ? campo.getText().trim() : "";
    }
}
