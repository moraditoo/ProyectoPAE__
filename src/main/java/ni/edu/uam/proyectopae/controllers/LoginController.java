package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.dao.UsuarioDAO;
import ni.edu.uam.proyectopae.models.Usuario;

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
                // Cargar vista principal con los modulos del sistema
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ni/edu/uam/proyectopae/main-view.fxml"));
                Scene scene = new Scene(loader.load(), 850, 600);

                Stage stage = (Stage) txtUsuario.getScene().getWindow();
                stage.setTitle("Tienda de Maquillaje - Panel Principal (" + usuario.getRol() + ")");
                stage.setScene(scene);
                stage.centerOnScreen();
            } else {
                lblMensaje.setStyle("-fx-text-fill: red;");
                lblMensaje.setText("Credenciales invalidas. Intente nuevamente.");
            }
        } catch (Exception ex) {
            lblMensaje.setStyle("-fx-text-fill: red;");
            lblMensaje.setText("Error al acceder a los modulos: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}