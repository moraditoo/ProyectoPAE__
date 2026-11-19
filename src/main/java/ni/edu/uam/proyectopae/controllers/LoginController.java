package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import ni.edu.uam.proyectopae.dao.UsuarioDAO;
import ni.edu.uam.proyectopae.models.Usuario;
import ni.edu.uam.proyectopae.util.Sesion;
import ni.edu.uam.proyectopae.util.TemaPAE;
import ni.edu.uam.proyectopae.util.VentanaManager;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnVerClave;
    @FXML private FontIcon iconoClave;
    @FXML private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        txtPasswordVisible.textProperty()
                .bindBidirectional(txtPassword.textProperty());

        lblMensaje.visibleProperty()
                .bind(lblMensaje.textProperty().isNotEmpty());

        lblMensaje.managedProperty()
                .bind(lblMensaje.visibleProperty());

        try {
            if (usuarioDAO.listarTodos().isEmpty()) {
                String usuarioInicial =
                        System.getenv("PAE_ADMIN_USER");

                String claveInicial =
                        System.getenv("PAE_ADMIN_PASSWORD");

                if (usuarioInicial == null
                        || usuarioInicial.isBlank()
                        || claveInicial == null
                        || claveInicial.isBlank()) {

                    throw new IllegalStateException(
                            "La primera ejecución requiere "
                                    + "PAE_ADMIN_USER y PAE_ADMIN_PASSWORD."
                    );
                }

                usuarioDAO.guardar(
                        new Usuario(
                                usuarioInicial,
                                claveInicial,
                                "ADMINISTRADOR"
                        )
                );
            }
        } catch (Throwable e) {
            lblMensaje.setText(
                    VentanaManager.causaRaiz(e)
            );
        }
    }

    @FXML
    protected void handleAlternarTema() {
        TemaPAE.alternarModoOscuro();
    }

    @FXML
    private void alternarVisibilidadClave() {
        boolean mostrar = !txtPasswordVisible.isVisible();
        TextField anterior = mostrar ? txtPassword : txtPasswordVisible;
        TextField siguiente = mostrar ? txtPasswordVisible : txtPassword;
        int ancla = anterior.getAnchor();
        int cursor = anterior.getCaretPosition();
        anterior.setVisible(false);
        anterior.setManaged(false);
        siguiente.setManaged(true);
        siguiente.setVisible(true);
        siguiente.requestFocus();
        siguiente.selectRange(ancla, cursor);
        String accion = mostrar ? "Ocultar contraseña" : "Mostrar contraseña";
        btnVerClave.setAccessibleText(accion);
        btnVerClave.getTooltip().setText(accion);
        iconoClave.setIconLiteral(mostrar ? "far-eye-slash" : "far-eye");
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
                stage.setScene(VentanaManager.crearEscena(root, 1280, 820));
                stage.setMaximized(true);
            } else {
                lblMensaje.setText("Credenciales inválidas. Intente nuevamente.");
                txtPassword.clear();
                if (txtPasswordVisible.isVisible()) alternarVisibilidadClave();
                txtPassword.requestFocus();
            }
        } catch (Throwable ex) {
            lblMensaje.setText("Error al consultar la base de datos: " + VentanaManager.causaRaiz(ex));
        }
    }
}
