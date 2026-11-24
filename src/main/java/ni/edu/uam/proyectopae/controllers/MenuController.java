package ni.edu.uam.proyectopae.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import ni.edu.uam.proyectopae.dao.*;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.models.Venta;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Sesion;
import ni.edu.uam.proyectopae.util.TemaPAE;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Menú principal con tarjetas y estadísticas en vivo.
 * Cada módulo se abre en su propia ventana independiente.
 */
public class MenuController {

    @FXML private Label lblUsuario;
    @FXML private Label lblFecha;
    @FXML private Label lblBienvenida;
    @FXML private Label lblIniciales;
    @FXML private Label lblBadgeInventario;
    @FXML private Label lblDetalleInventario;
    @FXML private HBox alertaInventario;

    @FXML private Label lblKpiVentasHoy;
    @FXML private Label lblKpiProductos;
    @FXML private Label lblKpiClientes;
    @FXML private Label lblKpiCitas;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final CitaDAO citaDAO = new CitaDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    @FXML
    public void initialize() {
        lblUsuario.setText(Sesion.descripcion());
        String usuario = Sesion.getUsuario() == null ? "" : Sesion.getUsuario().getUsername().strip();
        lblBienvenida.setText(usuario.isEmpty() ? "Bienvenido" : "Hola, " + usuario);
        lblIniciales.setText(usuario.substring(0, Math.min(2, usuario.length())).toUpperCase(Locale.ROOT));

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-NI")));
        lblFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));


        if (alertaInventario != null) {
            alertaInventario.managedProperty().bind(alertaInventario.visibleProperty());
        }

        VentanaManager.alCambiarDatos(() -> {
            actualizarAlertas();
            actualizarMetricas();
        });

        Platform.runLater(() -> {
            actualizarAlertas();
            actualizarMetricas();
        });
    }

    @FXML
    private void handleAlternarTema() {
        TemaPAE.alternarModoOscuro();
    }

    @FXML
    public void refrescarMetricas() {
        actualizarAlertas();
        actualizarMetricas();
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
            if (lblBadgeInventario != null) {
                lblBadgeInventario.setText(total + (total == 1 ? " alerta de inventario" : " alertas de inventario"));
            }
            if (lblDetalleInventario != null) {
                lblDetalleInventario.setText(a[0] + " por vencer · " + a[1] + " vencidos con stock · "
                        + a[2] + " bajo el stock mínimo");
            }
            if (alertaInventario != null) {
                alertaInventario.setVisible(total > 0);
            }
        } catch (Exception ex) {
            if (lblBadgeInventario != null) {
                lblBadgeInventario.setText("Alertas de inventario");
            }
            if (lblDetalleInventario != null) {
                lblDetalleInventario.setText("No se pudieron consultar las alertas. Abre el Dashboard para volver a intentarlo.");
            }
            if (alertaInventario != null) {
                alertaInventario.setVisible(true);
            }
        }
    }

    private void actualizarMetricas() {
        Thread hilo = new Thread(() -> {
            try {
                BigDecimal totalVentas = BigDecimal.ZERO;
                try {
                    List<Venta> ventas = ventaDAO.listarConCliente();
                    LocalDate hoy = LocalDate.now();
                    for (Venta v : ventas) {
                        if (v.getFecha() != null && v.getFecha().toLocalDate().equals(hoy)) {
                            totalVentas = totalVentas.add(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);
                        }
                    }
                } catch (Exception ignored) {}

                int countProd = 0;
                try { countProd = productoDAO.listarConStock().size(); } catch (Exception ignored) {}

                int countCli = 0;
                try { countCli = clienteDAO.listarTodos().size(); } catch (Exception ignored) {}

                int countCitas = 0;
                try { countCitas = citaDAO.listarCitasConRelaciones().size(); } catch (Exception ignored) {}

                final String strVentas = Formato.moneda(totalVentas);
                final String strProd = countProd + (countProd == 1 ? " producto" : " productos");
                final String strCli = countCli + (countCli == 1 ? " cliente" : " clientes");
                final String strCitas = countCitas + (countCitas == 1 ? " cita" : " citas");

                Platform.runLater(() -> {
                    if (lblKpiVentasHoy != null) lblKpiVentasHoy.setText(strVentas);
                    if (lblKpiProductos != null) lblKpiProductos.setText(strProd);
                    if (lblKpiClientes != null) lblKpiClientes.setText(strCli);
                    if (lblKpiCitas != null) lblKpiCitas.setText(strCitas);
                });
            } catch (Exception ignored) {}
        }, "proyectopae-metricas-menu");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void abrirDashboardInventario() {
        VentanaManager.abrir("dashboard-inventario-view.fxml", "Dashboard de Inventario", "fas-clipboard-list", "#d97706");
    }

    @FXML
    public void abrirVentas() {
        VentanaManager.abrir("ventas-view.fxml", "Ventas", "fas-cash-register", "#0891b2");
    }

    @FXML
    public void abrirClientes() {
        VentanaManager.abrir("clientes-view.fxml", "Clientes", "fas-users", "#7c3aed");
    }

    @FXML
    public void abrirProductos() {
        VentanaManager.abrir("inventario-view.fxml", "Productos", "fas-box-open", "#2563eb");
    }

    @FXML
    public void abrirLotes() {
        VentanaManager.abrir("lotes-servicios-view.fxml", "Lotes y Caducidad", "fas-boxes", "#db2777");
    }

    @FXML
    public void abrirAgenda() {
        VentanaManager.abrir("agenda-view.fxml", "Agenda de Citas", "fas-calendar-alt", "#4f46e5");
    }

    @FXML
    public void abrirReportes() {
        VentanaManager.abrir("reportes-view.fxml", "Reportes generales", "fas-file-alt", "#7c3aed");
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
