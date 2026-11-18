package ni.edu.uam.proyectopae.util;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
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

    /** Aplica la presentación correspondiente a la vista cargada. */
    public static Scene crearEscena(Parent root, double ancho, double alto) {
        Scene scene = new Scene(root, ancho, alto);
        if (TemaPAE.usaTema(root)) {
            TemaPAE.aplicar(root);
            return scene;
        }
        URL stylesheet = VentanaManager.class.getResource(CSS);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    public static void mostrarAcceso(Stage stage) throws java.io.IOException {
        Parent acceso = loader("login-view.fxml").load();
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        stage.setMaximized(false);
        stage.setScene(crearEscena(acceso, Math.min(1280, pantalla.getWidth() - 60),
                Math.min(820, pantalla.getHeight() - 70)));
        stage.setMinWidth(780);
        stage.setMinHeight(560);
        stage.setTitle("Control de Acceso - Tienda de Maquillaje");
        stage.sizeToScene();
        stage.centerOnScreen();
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
            double alto = Math.min(820, pantalla.getHeight() - 60);

            Stage stage = new Stage();
            stage.initOwner(principal);
            stage.setTitle(titulo + " - Tienda de Maquillaje");
            if (root instanceof BorderPane vista && TemaPAE.usaTema(vista)) {
                var encabezado = vista.getTop();
                vista.setTop(null);
                VBox superior = new VBox(TemaPAE.barra(titulo, () -> {
                    stage.setIconified(true);
                    if (principal != null) {
                        principal.setIconified(false);
                        principal.toFront();
                        principal.requestFocus();
                    }
                }));
                if (encabezado != null) superior.getChildren().add(encabezado);
                vista.setTop(superior);
            }
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

    /** Re-aplica el tema visual (modo claro u oscuro) a todas las ventanas abiertas. */
    public static void actualizarTemaGlobal() {
        if (principal != null && principal.getScene() != null) {
            Parent root = principal.getScene().getRoot();
            if (TemaPAE.usaTema(root)) {
                TemaPAE.aplicar(root);
            }
        }
        for (Stage stage : new ArrayList<>(ventanas.values())) {
            if (stage != null && stage.getScene() != null) {
                Parent root = stage.getScene().getRoot();
                if (TemaPAE.usaTema(root)) {
                    TemaPAE.aplicar(root);
                }
            }
        }
    }

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
