package ni.edu.uam.proyectopae.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Sesion;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Menu principal con tarjetas. Reemplaza al antiguo TabPane (fichas):
 * cada tarjeta abre su modulo en una ventana independiente.
 */
public class MenuController {

    @FXML private Label lblUsuario;
    @FXML private Label lblFecha;
    @FXML private Label lblBienvenida;
    @FXML private Label lblIniciales;
    @FXML private Label lblBadgeInventario;
    @FXML private Label lblDetalleInventario;
    @FXML private HBox alertaInventario;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() {
        lblUsuario.setText(Sesion.descripcion());
        String usuario = Sesion.getUsuario() == null ? "" : Sesion.getUsuario().getUsername().strip();
        lblBienvenida.setText(usuario.isEmpty() ? "Bienvenido" : "Hola, " + usuario);
        lblIniciales.setText(usuario.substring(0, Math.min(2, usuario.length())).toUpperCase(Locale.ROOT));
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-NI")));
        lblFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));

        alertaInventario.managedProperty().bind(alertaInventario.visibleProperty());
        VentanaManager.alCambiarDatos(this::actualizarAlertas);
        Platform.runLater(this::actualizarAlertas);
    }

    /** [porVencer, vencidos, bajoMinimo] */
    private int[] contarAlertas() {
        int porVencer = loteDAO.listarPorVencer(Lote.DIAS_ALERTA_VENCIMIENTO).size();
        int vencidos = loteDAO.listarVencidosConStock().size();
        int bajoMinimo = (int) productoDAO.listarConStock().stream().filter(Producto::isBajoStockMinimo).count();
        return new int[]{porVencer, vencidos, bajoMinimo};
    }

    private void actualizarAlertas() {
        try {
            int[] a = contarAlertas();
            int total = a[0] + a[1] + a[2];
            lblBadgeInventario.setText(total + (total == 1 ? " alerta de inventario" : " alertas de inventario"));
            lblDetalleInventario.setText(a[0] + " por vencer · " + a[1] + " vencidos con stock · "
                    + a[2] + " bajo el stock mínimo");
            alertaInventario.setVisible(total > 0);
        } catch (Exception ex) {
            lblBadgeInventario.setText("Alertas de inventario");
            lblDetalleInventario.setText("No se pudieron consultar las alertas. Abre el Dashboard para volver a intentarlo.");
            alertaInventario.setVisible(true);
        }
    }

    @FXML
    private void abrirDashboardInventario() {
        VentanaManager.abrir("dashboard-inventario-view.fxml", "Dashboard de Inventario", "fas-clipboard-list", "#d97706");
    }

    @FXML
    private void abrirVentas() {
        VentanaManager.abrir("ventas-view.fxml", "Ventas", "fas-cash-register", "#0891b2");
    }

    @FXML
    private void abrirClientes() {
        VentanaManager.abrir("clientes-view.fxml", "Clientes", "fas-users", "#7c3aed");
    }

    @FXML
    private void abrirProductos() {
        VentanaManager.abrir("inventario-view.fxml", "Productos", "fas-box-open", "#2563eb");
    }

    @FXML
    private void abrirLotes() {
        VentanaManager.abrir("lotes-servicios-view.fxml", "Lotes y Caducidad", "fas-boxes", "#db2777");
    }

    @FXML
    private void abrirAgenda() {
        VentanaManager.abrir("agenda-view.fxml", "Agenda de Citas", "fas-calendar-alt", "#4f46e5");
    }

    @FXML
    private void abrirReportes() {
        VentanaManager.abrir(
                "reportes-view.fxml", "Reportes de citas", "fas-file-alt", "#7c3aed");
    }

    @FXML
    private void cerrarSesion() {
        if (!Alertas.confirmar("Cerrar sesión", "¿Desea cerrar la sesión? Se cerrarán todas las ventanas abiertas.")) {
            return;
        }
        try {
            VentanaManager.cerrarTodas();
            Sesion.cerrar();

            VentanaManager.mostrarAcceso(VentanaManager.getPrincipal());
        } catch (Exception ex) {
            Alertas.error("Error", "No se pudo volver al login: " + VentanaManager.causaRaiz(ex));
        }
    }
}
