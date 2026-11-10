package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectofinalpae.dao.UsuarioDAO;
import ni.edu.uam.proyectofinalpae.models.Usuario;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        try {
            if (usuarioDAO.listarTodos().isEmpty()) {
                usuarioDAO.guardar(new Usuario("admin", "admin123", "ADMINISTRADOR"));
            }
        } catch (Exception e) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Aviso: Verifique la conexion a PostgreSQL.");
        }
    }

    @FXML
    protected void handleLogin() {
        String u = txtUsuario.getText().trim();
        String p = txtPassword.getText().trim();

        if (u.isEmpty() || p.isEmpty()) {
            lblMensaje.setStyle("-fx-text-fill: #b30000;");
            lblMensaje.setText("Validacion: Complete todos los campos.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(u, p);
            if (usuario != null) {
                lblMensaje.setStyle("-fx-text-fill: green;");
                lblMensaje.setText("Autenticacion correcta. Rol: " + usuario.getRol());
            } else {
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Credenciales invalidas. Intente nuevamente.");
            }
        } catch (Exception ex) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error al consultar base de datos: " + ex.getMessage());
        }
    }
}