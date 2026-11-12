package ni.edu.uam.proyectopae.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

/**
 * Centraliza los dialogos (Alert) de la aplicacion para que todos
 * tengan el mismo estilo, icono y comportamiento.
 */
public final class Alertas {

    private static final String CSS = "/ni/edu/uam/proyectopae/css/styles.css";

    private Alertas() {}

    public static void info(String titulo, String mensaje) {
        crear(Alert.AlertType.INFORMATION, titulo, mensaje, "fas-check-circle", "#059669").showAndWait();
    }

    public static void error(String titulo, String mensaje) {
        crear(Alert.AlertType.ERROR, titulo, mensaje, "fas-times-circle", "#dc2626").showAndWait();
    }

    public static void advertencia(String titulo, String mensaje) {
        crear(Alert.AlertType.WARNING, titulo, mensaje, "fas-exclamation-triangle", "#d97706").showAndWait();
    }

    /** Validaciones de formulario: mismo estilo que advertencia, titulo fijo. */
    public static void validacion(String mensaje) {
        advertencia("Validación", mensaje);
    }

    /** Muestra un dialogo de confirmacion Si/No. Devuelve true si el usuario acepta. */
    public static boolean confirmar(String titulo, String mensaje) {
        Alert alert = crear(Alert.AlertType.CONFIRMATION, titulo, mensaje, "fas-question-circle", "#2563eb");
        ButtonType si = new ButtonType("Sí", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(si, no);
        Optional<ButtonType> r = alert.showAndWait();
        return r.isPresent() && r.get() == si;
    }

    private static Alert crear(Alert.AlertType tipo, String titulo, String mensaje, String icono, String color) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);

        Label contenido = new Label(mensaje);
        contenido.setWrapText(true);
        contenido.setMaxWidth(460);
        contenido.setStyle("-fx-font-size: 13px; -fx-padding: 6 4 6 4;");
        alert.getDialogPane().setContent(contenido);

        FontIcon fi = new FontIcon(icono);
        fi.setIconSize(36);
        fi.setIconColor(javafx.scene.paint.Color.web(color));
        alert.setGraphic(fi);

        alert.getDialogPane().getStylesheets().add(Alertas.class.getResource(CSS).toExternalForm());

        Window activa = VentanaManager.ventanaActiva();
        if (activa != null) alert.initOwner(activa);
        return alert;
    }
}
