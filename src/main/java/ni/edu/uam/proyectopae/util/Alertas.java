package ni.edu.uam.proyectopae.util;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.kordamp.ikonli.javafx.FontIcon;

public final class Alertas {

    private Alertas() {
    }

    public static void info(String titulo, String mensaje) {
        crear(
                Alert.AlertType.INFORMATION,
                titulo,
                mensaje,
                "fas-check-circle",
                Color.web("#267349")
        ).showAndWait();
    }

    public static void error(String titulo, String mensaje) {
        crear(
                Alert.AlertType.ERROR,
                titulo,
                mensaje,
                "fas-times-circle",
                Color.web("#B42335")
        ).showAndWait();
    }

    public static void advertencia(
            String titulo,
            String mensaje
    ) {
        crear(
                Alert.AlertType.WARNING,
                titulo,
                mensaje,
                "fas-exclamation-triangle",
                Color.web("#946000")
        ).showAndWait();
    }

    public static void validacion(String mensaje) {
        advertencia("Validación", mensaje);
    }

    public static boolean confirmar(
            String titulo,
            String mensaje
    ) {
        return preguntar(
                titulo,
                mensaje,
                "fas-question-circle",
                Color.web("#2457A7")
        );
    }

    public static boolean advertenciaConPregunta(
            String titulo,
            String mensaje
    ) {
        return preguntar(
                titulo,
                mensaje,
                "fas-exclamation-triangle",
                Color.web("#946000")
        );
    }

    private static boolean preguntar(
            String titulo,
            String mensaje,
            String icono,
            Color color
    ) {
        Alert alerta = crear(
                Alert.AlertType.CONFIRMATION,
                titulo,
                mensaje,
                icono,
                color
        );

        ButtonType si = new ButtonType(
                "Sí",
                ButtonBar.ButtonData.YES
        );

        ButtonType no = new ButtonType(
                "No",
                ButtonBar.ButtonData.NO
        );

        alerta.getButtonTypes().setAll(si, no);

        return alerta.showAndWait()
                .map(respuesta -> respuesta == si)
                .orElse(false);
    }

    private static Alert crear(
            Alert.AlertType tipo,
            String titulo,
            String mensaje,
            String icono,
            Color color
    ) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);

        Label contenido = new Label(mensaje);
        contenido.setWrapText(true);
        contenido.setMaxWidth(460);
        contenido.setFont(Font.font("Segoe UI", 13));
        contenido.setPadding(new Insets(6, 4, 6, 4));

        FontIcon imagen = new FontIcon(icono);
        imagen.setIconSize(36);
        imagen.setIconColor(color);

        alerta.setGraphic(imagen);
        alerta.getDialogPane().setContent(contenido);

        var ventana = VentanaManager.ventanaActiva();

        if (ventana != null) {
            alerta.initOwner(ventana);
        }

        return alerta;
    }
}