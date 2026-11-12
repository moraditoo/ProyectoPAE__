package ni.edu.uam.proyectopae;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        VentanaManager.setPrincipal(stage);

        FXMLLoader fxmlLoader = VentanaManager.loader("login-view.fxml");
        stage.setScene(VentanaManager.crearEscena(fxmlLoader.load(), 440, 560));
        stage.setTitle("Control de Acceso - Tienda de Maquillaje");
        stage.getIcons().add(VentanaManager.iconoVentana("fas-spa", "#7c3aed"));
        stage.show();
    }

    @Override
    public void stop() {
        try {
            HibernateUtil.shutdown();
        } catch (Throwable ignored) {
            // Si la BD nunca se conecto no hay nada que cerrar
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
