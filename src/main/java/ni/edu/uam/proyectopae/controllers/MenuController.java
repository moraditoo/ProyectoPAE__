package ni.edu.uam.proyectopae.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
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
    @FXML private Label lblBadgeInventario;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() {
        lblUsuario.setText(Sesion.descripcion());
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-NI")));
        lblFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));

        lblBadgeInventario.managedProperty().bind(lblBadgeInventario.visibleProperty());
        VentanaManager.alCambiarDatos(this::actualizarBadge);

        // Al iniciar sesion se avisa de productos por vencer y stock bajo
        Platform.runLater(this::mostrarAlertasInventario);
    }

    /** [porVencer, vencidos, bajoMinimo] */
    private int[] contarAlertas() {
        int porVencer = loteDAO.listarPorVencer(Lote.DIAS_ALERTA_VENCIMIENTO).size();
        int vencidos = loteDAO.listarVencidosConStock().size();
        int bajoMinimo = (int) productoDAO.listarConStock().stream().filter(Producto::isBajoStockMinimo).count();
        return new int[]{porVencer, vencidos, bajoMinimo};
    }

    private void actualizarBadge() {
        try {
            int[] a = contarAlertas();
            int total = a[0] + a[1] + a[2];
            lblBadgeInventario.setText(total + (total == 1 ? " alerta" : " alertas"));
            lblBadgeInventario.setVisible(total > 0);
        } catch (Exception e) {
            lblBadgeInventario.setVisible(false);
        }
    }

    private void mostrarAlertasInventario() {
        try {
            int[] a = contarAlertas();
            actualizarBadge();
            if (a[0] + a[1] + a[2] == 0) return;

            StringBuilder sb = new StringBuilder();
            if (a[1] > 0) sb.append("• ").append(a[1]).append(" lote(s) VENCIDOS todavía tienen unidades.\n");
            if (a[0] > 0) sb.append("• ").append(a[0]).append(" lote(s) vencen en los próximos ")
                    .append(Lote.DIAS_ALERTA_VENCIMIENTO).append(" días.\n");
            if (a[2] > 0) sb.append("• ").append(a[2]).append(" producto(s) están por debajo del stock mínimo.\n");
            sb.append("\n¿Desea abrir el Dashboard de Inventario?");

            if (Alertas.advertenciaConPregunta("Alertas de inventario", sb.toString())) {
                abrirDashboardInventario();
            }
        } catch (Exception e) {
            Alertas.error("Error de conexión", "No se pudieron revisar las alertas de inventario:\n" + VentanaManager.causaRaiz(e));
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
    private void cerrarSesion() {
        if (!Alertas.confirmar("Cerrar sesión", "¿Desea cerrar la sesión? Se cerrarán todas las ventanas abiertas.")) {
            return;
        }
        try {
            VentanaManager.cerrarTodas();
            Sesion.cerrar();

            FXMLLoader loader = VentanaManager.loader("login-view.fxml");
            Parent root = loader.load();
            Stage stage = VentanaManager.getPrincipal();
            stage.setMaximized(false);
            stage.setScene(VentanaManager.crearEscena(root, 440, 560));
            stage.setTitle("Control de Acceso - Tienda de Maquillaje");
            stage.centerOnScreen();
        } catch (Exception ex) {
            Alertas.error("Error", "No se pudo volver al login: " + VentanaManager.causaRaiz(ex));
        }
    }
}
