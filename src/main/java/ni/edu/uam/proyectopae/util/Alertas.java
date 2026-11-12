package ni.edu.uam.proyectopae.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Ventanas de alerta uniformes para todo el sistema: cada tipo de situación usa el tipo de
 * diálogo adecuado (información, advertencia, error o confirmación) y un icono que la identifica.
 */
public final class Alertas {

    private static final String TITULO_APP = "Tienda de Maquillaje";

    private Alertas() {}

    public static void exito(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, encabezado, mensaje, Icono.Tipo.EXITO, Tema.OK);
    }

    public static void info(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, encabezado, mensaje, Icono.Tipo.INFO, Tema.INFO);
    }

    public static void advertencia(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.WARNING, encabezado, mensaje, Icono.Tipo.ADVERTENCIA, Tema.ALERTA);
    }

    public static void error(String encabezado, String mensaje) {
        mostrar(Alert.AlertType.ERROR, encabezado, mensaje, Icono.Tipo.ERROR, Tema.PELIGRO);
    }

    /** Validación de campos obligatorios vacíos. */
    public static void camposObligatorios(List<String> campos) {
        StringBuilder sb = new StringBuilder("Complete los siguientes campos obligatorios:\n");
        for (String campo : campos) {
            sb.append("\n  •  ").append(campo);
        }
        advertencia("Campos obligatorios sin completar", sb.toString());
    }

    /** Datos introducidos con un formato o valor incorrecto. */
    public static void datoInvalido(String mensaje) {
        advertencia("Datos incorrectos", mensaje);
    }

    /** Error al consultar o guardar en PostgreSQL. Distingue fallos de conexión de otros errores. */
    public static void errorBD(String accion, Throwable e) {
        String encabezado;
        String mensaje;
        if (esErrorConexion(e)) {
            encabezado = "Error de conexión con la base de datos";
            mensaje = "No fue posible " + accion + " porque no hay conexión con PostgreSQL.\n"
                    + "Verifique que el servidor de base de datos esté activo e intente nuevamente.";
        } else if (buscarCausa(e, ConstraintViolationException.class) != null) {
            encabezado = "No se pudo " + accion;
            mensaje = "La base de datos rechazó la operación porque viola una restricción "
                    + "(registro duplicado o con información relacionada).";
        } else {
            encabezado = "Error al " + accion;
            mensaje = "Ocurrió un error en la base de datos y la operación no se completó.";
        }

        Alert alerta = crear(Alert.AlertType.ERROR, encabezado, mensaje, Icono.Tipo.ERROR, Tema.PELIGRO);
        TextArea detalle = new TextArea(causaRaiz(e));
        detalle.setEditable(false);
        detalle.setWrapText(true);
        detalle.setPrefRowCount(4);
        alerta.getDialogPane().setExpandableContent(detalle);
        alerta.showAndWait();
    }

    /** Pregunta de confirmación (por ejemplo, antes de eliminar). Devuelve true si el usuario acepta. */
    public static boolean confirmar(String encabezado, String mensaje) {
        Alert alerta = crear(Alert.AlertType.CONFIRMATION, encabezado, mensaje, Icono.Tipo.PREGUNTA, Tema.INFO);
        ButtonType si = new ButtonType("Sí, continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alerta.getButtonTypes().setAll(si, no);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == si;
    }

    private static void mostrar(Alert.AlertType tipo, String encabezado, String mensaje, Icono.Tipo icono, Color color) {
        crear(tipo, encabezado, mensaje, icono, color).showAndWait();
    }

    private static Alert crear(Alert.AlertType tipo, String encabezado, String mensaje, Icono.Tipo icono, Color color) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(TITULO_APP);
        alerta.setHeaderText(encabezado);

        Label contenido = new Label(mensaje);
        contenido.setWrapText(true);
        contenido.setMaxWidth(460);
        DialogPane panel = alerta.getDialogPane();
        panel.setContent(contenido);
        panel.setMinWidth(480);
        panel.setGraphic(Icono.conColor(icono, color));
        ((Icono) panel.getGraphic()).setTamano(40);
        Tema.aplicar(panel);

        ventanaActiva().ifPresent(alerta::initOwner);
        return alerta;
    }

    private static Optional<Window> ventanaActiva() {
        return Window.getWindows().stream().filter(Window::isFocused).findFirst()
                .or(() -> Window.getWindows().stream().filter(Window::isShowing).findFirst());
    }

    public static boolean esErrorConexion(Throwable e) {
        if (buscarCausa(e, HibernateUtil.ErrorConexionException.class) != null
                || buscarCausa(e, JDBCConnectionException.class) != null) {
            return true;
        }
        SQLException sql = buscarCausa(e, SQLException.class);
        return sql != null && sql.getSQLState() != null && sql.getSQLState().startsWith("08");
    }

    private static <T extends Throwable> T buscarCausa(Throwable e, Class<T> tipo) {
        Throwable actual = e;
        while (actual != null) {
            if (tipo.isInstance(actual)) return tipo.cast(actual);
            if (actual.getCause() == actual) break;
            actual = actual.getCause();
        }
        return null;
    }

    private static String causaRaiz(Throwable e) {
        Throwable actual = e;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        return actual.getClass().getSimpleName() + ": " + actual.getMessage();
    }
}
