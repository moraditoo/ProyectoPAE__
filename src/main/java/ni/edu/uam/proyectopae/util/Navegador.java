package ni.edu.uam.proyectopae.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.HelloApplication;
import ni.edu.uam.proyectopae.controllers.MainViewController;
import ni.edu.uam.proyectopae.models.Usuario;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

/**
 * Navegación del sistema mediante ventanas independientes.
 * Cada módulo se abre en su propia ventana y nunca se duplica: si ya está abierto, se trae al frente.
 */
public final class Navegador {

    public static final String TITULO_APP = "Tienda de Maquillaje";

    public enum Modulo {
        CLIENTES("clientes-view.fxml", "Clientes", Icono.Tipo.CLIENTES),
        PRODUCTOS("inventario-view.fxml", "Catálogo de Productos", Icono.Tipo.PRODUCTOS),
        LOTES("lotes-servicios-view.fxml", "Lotes y Caducidad", Icono.Tipo.LOTES),
        VENTAS("ventas-view.fxml", "Ventas", Icono.Tipo.VENTAS),
        AGENDA("agenda-view.fxml", "Agenda de Citas", Icono.Tipo.AGENDA);

        private final String fxml;
        private final String titulo;
        private final Icono.Tipo icono;

        Modulo(String fxml, String titulo, Icono.Tipo icono) {
            this.fxml = fxml;
            this.titulo = titulo;
            this.icono = icono;
        }

        public String getTitulo() { return titulo; }
        public Icono.Tipo getIcono() { return icono; }
    }

    private static Stage ventanaPrincipal;
    private static final Map<Modulo, Stage> ventanasAbiertas = new EnumMap<>(Modulo.class);

    private Navegador() {}

    /** Muestra la ventana de inicio de sesión en el Stage indicado. */
    public static void mostrarLogin(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(loader.load(), 420, 460);
        aplicarEstilos(scene);
        stage.setTitle("Control de Acceso - " + TITULO_APP);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /** Abre el panel principal (Dashboard) maximizado tras un inicio de sesión correcto. */
    public static void abrirPrincipal(Usuario usuario) throws Exception {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
        Parent raiz = loader.load();
        MainViewController controlador = loader.getController();
        controlador.setUsuario(usuario);

        Stage stage = new Stage();
        Scene scene = new Scene(raiz, 1280, 800);
        aplicarEstilos(scene);
        stage.setScene(scene);
        stage.setTitle(TITULO_APP + " - Panel de Control (" + usuario.getRol() + ")");
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setMaximized(true);
        stage.setOnCloseRequest(e -> Platform.exit());
        ventanaPrincipal = stage;

        EventosDatos.suscribir(controlador);
        stage.setOnHidden(e -> EventosDatos.desuscribir(controlador));
        stage.show();

        Platform.runLater(controlador::mostrarAlertasIniciales);
    }

    /** Cierra todas las ventanas de módulos y el panel principal, y vuelve a la pantalla de acceso. */
    public static void cerrarSesion() {
        cerrarModulos();
        if (ventanaPrincipal != null) {
            ventanaPrincipal.setOnCloseRequest(null);
            ventanaPrincipal.close();
            ventanaPrincipal = null;
        }
        try {
            mostrarLogin(new Stage());
        } catch (Exception e) {
            Alertas.error("No se pudo mostrar el inicio de sesión", e.getMessage());
            Platform.exit();
        }
    }

    /** Abre un módulo en su propia ventana, o lo trae al frente si ya estaba abierto. */
    public static void abrir(Modulo modulo) {
        Stage existente = ventanasAbiertas.get(modulo);
        if (existente != null) {
            if (existente.isIconified()) existente.setIconified(false);
            existente.show();
            existente.toFront();
            existente.requestFocus();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(modulo.fxml));
            Parent raiz = loader.load();
            Object controlador = loader.getController();

            Stage stage = new Stage();
            stage.setTitle(TITULO_APP + " - " + modulo.titulo);
            if (ventanaPrincipal != null) stage.initOwner(ventanaPrincipal);

            Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(raiz,
                    Math.min(1320, pantalla.getWidth() * 0.92),
                    Math.min(860, pantalla.getHeight() * 0.92));
            aplicarEstilos(scene);
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);

            Refrescable refrescable = controlador instanceof Refrescable r ? r : null;
            EventosDatos.suscribir(refrescable);
            stage.setOnHidden(e -> {
                ventanasAbiertas.remove(modulo);
                EventosDatos.desuscribir(refrescable);
            });

            ventanasAbiertas.put(modulo, stage);
            stage.show();
            stage.centerOnScreen();
        } catch (Exception e) {
            Alertas.error("No se pudo abrir el módulo " + modulo.titulo, e.getMessage());
        }
    }

    public static void cerrarModulos() {
        for (Stage stage : new ArrayList<>(ventanasAbiertas.values())) {
            stage.close();
        }
        ventanasAbiertas.clear();
    }

    /** Aplica el diseño del sistema (definido en Java por la clase Tema) a una ventana. */
    public static void aplicarEstilos(Scene scene) {
        Tema.aplicar(scene);
    }
}
