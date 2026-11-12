package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Sesion;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Menu principal con tarjetas. Reemplaza al antiguo TabPane (fichas):
 * cada tarjeta abre su modulo en una ventana independiente.
 */
public class MenuController {

    @FXML private Label lblUsuario;
    @FXML private Label lblFecha;

    @FXML
    public void initialize() {
        lblUsuario.setText(Sesion.descripcion());
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-NI")));
        lblFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
    }

    @FXML
    private void abrirClientes() {
        VentanaManager.abrir("clientes-view.fxml", "Clientes", "fas-users", "#7c3aed");
    }

    @FXML
    private void abrirProductos() {
        VentanaManager.abrir("inventario-view.fxml", "Productos", "fas-box-open", "#2563eb");
    }

    @FXML
    private void abrirLotes() {
        VentanaManager.abrir("lotes-servicios-view.fxml", "Lotes y Caducidad", "fas-boxes", "#db2777");
    }

    @FXML
    private void abrirAgenda() {
        VentanaManager.abrir("agenda-view.fxml", "Agenda de Citas", "fas-calendar-alt", "#4f46e5");
    }

    @FXML
    private void cerrarSesion() {
        if (!Alertas.confirmar("Cerrar sesión", "¿Desea cerrar la sesión? Se cerrarán todas las ventanas abiertas.")) {
            return;
        }
        try {
            VentanaManager.cerrarTodas();
            Sesion.cerrar();

            FXMLLoader loader = VentanaManager.loader("login-view.fxml");
            Parent root = loader.load();
            Stage stage = VentanaManager.getPrincipal();
            stage.setMaximized(false);
            stage.setScene(VentanaManager.crearEscena(root, 440, 560));
            stage.setTitle("Control de Acceso - Tienda de Maquillaje");
            stage.centerOnScreen();
        } catch (Exception ex) {
            Alertas.error("Error", "No se pudo volver al login: " + VentanaManager.causaRaiz(ex));
        }
    }
}
