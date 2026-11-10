package ni.edu.uam.proyectopae;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Tab inventario = new Tab("Inventario", FXMLLoader.load(HelloApplication.class.getResource("inventario-view.fxml")));
        Tab lotes = new Tab("Lotes", FXMLLoader.load(HelloApplication.class.getResource("lotes-servicios-view.fxml")));
        TabPane tabs = new TabPane(inventario, lotes);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        lotes.setOnSelectionChanged(event -> {
            if (lotes.isSelected()) {
                try {
                    lotes.setContent(FXMLLoader.load(HelloApplication.class.getResource("lotes-servicios-view.fxml")));
                } catch (IOException e) {
                    throw new IllegalStateException("No se pudo cargar la vista de lotes", e);
                }
            }
        });
        Scene scene = new Scene(tabs, 900, 600);
        stage.setTitle("Proyecto PAE - Inventario");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        ni.edu.uam.proyectopae.util.HibernateUtil.shutdown();
    }
}
