package ni.edu.uam.proyectopae;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {
    @BeforeAll
    static void initialize() throws Exception {
        System.setProperty("proyectopae.jdbc.url", "jdbc:h2:mem:pruebas;DB_CLOSE_DELAY=-1");
        FutureTask<Void> ready = new FutureTask<>(() -> null);
        Platform.startup(ready);
        ready.get(20, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
    }

    @AfterAll
    static void shutdown() {
        HibernateUtil.shutdown();
        Platform.exit();
    }

    @Test
    void persisteProductoYLote() {
        Producto producto = new Producto("Producto de prueba");
        ProductoDAO productos = new ProductoDAO();
        productos.guardar(producto);
        assertTrue(productos.listarConRelaciones().stream()
                .anyMatch(p -> p.getIdProducto().equals(producto.getIdProducto())));

        Lote lote = new Lote("PRUEBA-001", LocalDate.now().plusMonths(1), 10, producto);
        LoteDAO lotes = new LoteDAO();
        lotes.guardar(lote);
        Lote recuperado = lotes.listarConProducto().stream()
                .filter(l -> l.getIdLote().equals(lote.getIdLote())).findFirst().orElseThrow();
        assertEquals("Producto de prueba", recuperado.getNombreProducto());
        recuperado.setCantidadDisponible(5);
        lotes.actualizar(recuperado);
        assertEquals(5, lotes.listarConProducto().stream()
                .filter(l -> l.getIdLote().equals(lote.getIdLote()))
                .findFirst().orElseThrow().getCantidadDisponible());
        lotes.eliminar(lote.getIdLote());
        assertTrue(lotes.listarConProducto().stream()
                .noneMatch(l -> l.getIdLote().equals(lote.getIdLote())));
    }

    @Test
    void cargaVistasYRegistraProductoDesdeInventario() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            for (String view : new String[]{"hello-view.fxml", "login-view.fxml",
                    "agenda-view.fxml", "lotes-servicios-view.fxml"}) {
                assertNotNull(FXMLLoader.load(HelloApplication.class.getResource(view)));
            }
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("inventario-view.fxml"));
            TextField nombre = (TextField) root.lookup("#txtNombre");
            Button guardar = (Button) root.lookupAll(".button").stream()
                    .filter(n -> ((Button) n).getText().equals("Guardar producto"))
                    .findFirst().orElseThrow();
            nombre.setText("   ");
            guardar.fire();
            assertFalse(((Label) root.lookup("#lblEstado")).getText().isEmpty());
            nombre.setText("Producto desde interfaz");
            guardar.fire();
            assertTrue(new ProductoDAO().listarConRelaciones().stream()
                    .anyMatch(p -> p.getNombreProducto().equals("Producto desde interfaz")));
            Stage stage = new Stage();
            new HelloApplication().start(stage);
            assertTrue(stage.isShowing());
            stage.close();
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }
}
