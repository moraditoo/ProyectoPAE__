package ni.edu.uam.proyectopae.util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Navegacion por VENTANAS (en lugar de pestanas).
 * <ul>
 *   <li>El menu principal vive en el Stage principal (maximizado).</li>
 *   <li>Cada modulo se abre en su propia ventana; si ya estaba abierta se trae al frente.</li>
 *   <li>Cuando un modulo guarda datos llama a {@link #notificarCambios(Object)} y las demas
 *       ventanas abiertas se refrescan.</li>
 * </ul>
 */
public final class VentanaManager {

    private static final String BASE = "/ni/edu/uam/proyectopae/";
    private static final String CSS = BASE + "css/styles.css";

    private static Stage principal;
    private static final Map<String, Stage> ventanas = new LinkedHashMap<>();
    private static final Map<Stage, Object> controladores = new LinkedHashMap<>();
    private static final List<Runnable> oyentesCambios = new ArrayList<>();

    private VentanaManager() {}

    public static void setPrincipal(Stage stage) { principal = stage; }

    public static Stage getPrincipal() { return principal; }

    /** Crea una escena aplicando la hoja de estilos global. */
    public static Scene crearEscena(Parent root, double ancho, double alto) {
        Scene scene = new Scene(root, ancho, alto);
        scene.getStylesheets().add(VentanaManager.class.getResource(CSS).toExternalForm());
        return scene;
    }

    public static FXMLLoader loader(String fxml) {
        return new FXMLLoader(VentanaManager.class.getResource(BASE + fxml));
    }

    /**
     * Abre un modulo en una ventana independiente. Si ya esta abierta, la trae al frente y la refresca.
     */
    public static void abrir(String fxml, String titulo, String icono, String colorHex) {
        Stage existente = ventanas.get(fxml);
        if (existente != null) {
            if (existente.isIconified()) existente.setIconified(false);
            existente.toFront();
            existente.requestFocus();
            refrescar(controladores.get(existente));
            return;
        }

        try {
            FXMLLoader loader = loader(fxml);
            Parent root = loader.load();

            Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
            double ancho = Math.min(1280, pantalla.getWidth() - 80);
            double alto = Math.min(800, pantalla.getHeight() - 60);

            Stage stage = new Stage();
            stage.initOwner(principal);
            stage.setTitle(titulo + " - Tienda de Maquillaje");
            stage.setScene(crearEscena(root, ancho, alto));
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.getIcons().add(iconoVentana(icono, colorHex));
            stage.setOnHidden(e -> {
                ventanas.remove(fxml);
                controladores.remove(stage);
            });

            ventanas.put(fxml, stage);
            controladores.put(stage, loader.getController());
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alertas.error("No se pudo abrir la ventana",
                    "Ocurrió un error al cargar '" + titulo + "':\n" + causaRaiz(ex));
        }
    }

    /** Refresca todas las ventanas abiertas excepto la que origino el cambio. */
    public static void notificarCambios(Object origen) {
        for (Object ctrl : new ArrayList<>(controladores.values())) {
            if (ctrl != origen) refrescar(ctrl);
        }
        oyentesCambios.forEach(Runnable::run);
    }

    /** Permite que el menu principal actualice sus indicadores cuando cambian los datos. */
    public static void alCambiarDatos(Runnable oyente) { oyentesCambios.add(oyente); }

    public static void cerrarTodas() {
        new ArrayList<>(ventanas.values()).forEach(Stage::close);
        ventanas.clear();
        controladores.clear();
        oyentesCambios.clear();
    }

    /** Ventana con foco (para centrar los dialogos sobre ella). */
    public static Window ventanaActiva() {
        for (Window w : Window.getWindows()) {
            if (w.isFocused() && w.isShowing()) return w;
        }
        return principal != null && principal.isShowing() ? principal : null;
    }

    /** Genera el icono de la barra de tareas a partir de un icono de Font Awesome. */
    public static Image iconoVentana(String icono, String colorHex) {
        FontIcon fi = new FontIcon(icono);
        fi.setIconSize(36);
        fi.setIconColor(Color.WHITE);
        StackPane fondo = new StackPane(fi);
        fondo.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 12;");
        fondo.setPrefSize(56, 56);
        new Scene(fondo);
        fondo.applyCss();
        fondo.layout();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return fondo.snapshot(sp, null);
    }

    public static String causaRaiz(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c.getMessage() != null ? c.getMessage() : c.getClass().getSimpleName();
    }

    private static void refrescar(Object controlador) {
        if (controlador instanceof Refrescable r) {
            try {
                r.refrescar();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
