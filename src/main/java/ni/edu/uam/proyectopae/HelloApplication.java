package ni.edu.uam.proyectopae;

import javafx.application.Application;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        VentanaManager.setPrincipal(stage);

        VentanaManager.mostrarAcceso(stage);
        stage.getIcons().add(VentanaManager.iconoVentana("fas-spa", "#5B2D62"));
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
