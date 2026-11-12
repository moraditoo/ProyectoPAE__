package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.dao.UsuarioDAO;
import ni.edu.uam.proyectopae.models.Usuario;
import ni.edu.uam.proyectopae.util.Sesion;
import ni.edu.uam.proyectopae.util.VentanaManager;

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
        } catch (Throwable e) {
            lblMensaje.setText("Aviso: Verifique la conexión a PostgreSQL.");
        }
    }

    @FXML
    protected void handleEnterLogin(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }

    @FXML
    protected void handleLogin() {
        String u = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
        String p = txtPassword.getText() != null ? txtPassword.getText().trim() : "";

        if (u.isEmpty() || p.isEmpty()) {
            lblMensaje.setText("Complete usuario y contraseña.");
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(u, p);
            if (usuario != null) {
                Sesion.iniciar(usuario);

                // Pagina principal: menu de tarjetas en ventana MAXIMIZADA
                FXMLLoader loader = VentanaManager.loader("menu-view.fxml");
                Parent root = loader.load();

                Stage stage = (Stage) txtUsuario.getScene().getWindow();
                stage.setTitle("Tienda de Maquillaje - Menú Principal (" + usuario.getRol() + ")");
                stage.setScene(VentanaManager.crearEscena(root, 1280, 760));
                stage.setMaximized(true);
            } else {
                lblMensaje.setText("Credenciales inválidas. Intente nuevamente.");
                txtPassword.clear();
                txtPassword.requestFocus();
            }
        } catch (Throwable ex) {
            lblMensaje.setText("Error al consultar la base de datos: " + VentanaManager.causaRaiz(ex));
        }
    }
}