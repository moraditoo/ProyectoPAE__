package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.dao.UsuarioDAO;
import ni.edu.uam.proyectopae.models.Usuario;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Navegador;

import java.util.ArrayList;
import java.util.List;

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
            lblMensaje.setText("Aviso: no hay conexión con PostgreSQL. Verifique el servidor.");
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

        List<String> faltantes = new ArrayList<>();
        if (u.isEmpty()) faltantes.add("Usuario");
        if (p.isEmpty()) faltantes.add("Contraseña");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }

        try {
            Usuario usuario = usuarioDAO.autenticar(u, p);
            if (usuario != null) {
                Stage login = (Stage) txtUsuario.getScene().getWindow();
                Navegador.abrirPrincipal(usuario);
                login.close();
            } else {
                lblMensaje.setText("");
                txtPassword.clear();
                Alertas.error("Acceso denegado", "Usuario o contraseña incorrectos. Intente nuevamente.");
            }
        } catch (Exception ex) {
            Alertas.errorBD("iniciar sesión", ex);
        }
    }
}
