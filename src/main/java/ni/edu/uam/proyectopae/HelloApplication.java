package ni.edu.uam.proyectopae;

import javafx.application.Application;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Navegador;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Navegador.mostrarLogin(stage);
    }

    @Override
    public void stop() {
        Navegador.cerrarModulos();
        HibernateUtil.shutdown();
    }

    public static void main(String[] args) {
        launch();
    }
}
