package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import ni.edu.uam.proyectopae.dao.DashboardDAO;
import ni.edu.uam.proyectopae.dao.DashboardDAO.ClienteRanking;
import ni.edu.uam.proyectopae.dao.DashboardDAO.DatosDashboard;
import ni.edu.uam.proyectopae.dao.DashboardDAO.FinanzaProducto;
import ni.edu.uam.proyectopae.dao.DashboardDAO.LoteAlerta;
import ni.edu.uam.proyectopae.dao.DashboardDAO.MovimientoPeriodo;
import ni.edu.uam.proyectopae.dao.DashboardDAO.ResumenFinanciero;
import ni.edu.uam.proyectopae.dao.DashboardDAO.ServicioRanking;
import ni.edu.uam.proyectopae.dao.DashboardDAO.StockProducto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Icono;
import ni.edu.uam.proyectopae.util.Navegador;
import ni.edu.uam.proyectopae.util.Navegador.Modulo;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.Tablas;
import ni.edu.uam.proyectopae.util.Tema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Módulo "Panel de control": indicadores de inventario, finanzas, clientes y servicios con gráficos. */
public class DashboardController implements Refrescable {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final int MAX_LINEAS_ALERTA = 8;
    private static final int TOP_GRAFICOS = 5;

    @FXML private Label lblActualizado;

    // Indicadores
    @FXML private Label lblKpiIngresos;
    @FXML private Label lblKpiCostos;
    @FXML private Label lblKpiGanancia;
    @FXML private Label lblKpiVencidos;
    @FXML private Label lblKpiPorVencer;
    @FXML private Label lblKpiPorVencerTitulo;
    @FXML private Label lblKpiBajoMinimo;

    // A. Vencimientos
    @FXML private Label lblAyudaVencimientos;
    @FXML private TableView<LoteAlerta> tblVencimientos;
    @FXML private TableColumn<LoteAlerta, String> colVenProducto;
    @FXML private TableColumn<LoteAlerta, String> colVenLote;
    @FXML private TableColumn<LoteAlerta, Integer> colVenCantidad;
    @FXML private TableColumn<LoteAlerta, String> colVenFecha;
    @FXML private TableColumn<LoteAlerta, LoteAlerta> colVenEstado;
    @FXML private BarChart<String, Number> chartVencimientos;

    // B. Existencias mínimas
    @FXML private TableView<StockProducto> tblStock;
    @FXML private TableColumn<StockProducto, String> colStockProducto;
    @FXML private TableColumn<StockProducto, Long> colStockActual;
    @FXML private TableColumn<StockProducto, Integer> colStockMinimo;
    @FXML private TableColumn<StockProducto, StockProducto> colStockEstado;

    // C. Finanzas
    @FXML private Label lblIngresosProductos;
    @FXML private Label lblCostosProductos;
    @FXML private Label lblIngresosServicios;
    @FXML private Label lblCostosServicios;
    @FXML private Label lblIngresosTotales;
    @FXML private Label lblCostosTotales;
    @FXML private Label lblGananciaTotal;
    @FXML private Label lblMargen;
    @FXML private Label lblTituloPeriodo;
    @FXML private TableView<FinanzaProducto> tblFinanzas;
    @FXML private TableColumn<FinanzaProducto, String> colFinProducto;
    @FXML private TableColumn<FinanzaProducto, BigDecimal> colFinCompra;
    @FXML private TableColumn<FinanzaProducto, BigDecimal> colFinVenta;
    @FXML private TableColumn<FinanzaProducto, BigDecimal> colFinGananciaUnit;
    @FXML private TableColumn<FinanzaProducto, Long> colFinUnidades;
    @FXML private TableColumn<FinanzaProducto, BigDecimal> colFinIngresos;
    @FXML private TableColumn<FinanzaProducto, BigDecimal> colFinCostos;
    @FXML private TableColumn<FinanzaProducto, BigDecimal> colFinGanancia;
    @FXML private BarChart<String, Number> chartPeriodo;

    // D. Clientes y servicios
    @FXML private TableView<ClienteRanking> tblClientes;
    @FXML private TableColumn<ClienteRanking, Integer> colCliPosicion;
    @FXML private TableColumn<ClienteRanking, String> colCliNombre;
    @FXML private TableColumn<ClienteRanking, Long> colCliCompras;
    @FXML private TableColumn<ClienteRanking, BigDecimal> colCliIngresos;
    @FXML private BarChart<String, Number> chartClientes;
    @FXML private TableView<ServicioRanking> tblServicios;
    @FXML private TableColumn<ServicioRanking, Integer> colSerPosicion;
    @FXML private TableColumn<ServicioRanking, String> colSerNombre;
    @FXML private TableColumn<ServicioRanking, Long> colSerVeces;
    @FXML private TableColumn<ServicioRanking, BigDecimal> colSerIngresos;
    @FXML private BarChart<String, Number> chartServicios;

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private DatosDashboard datos;

    @FXML
    public void initialize() {
        configurarTablas();
        configurarEjesEnteros(chartVencimientos, chartClientes, chartServicios);
        lblAyudaVencimientos.setText("Lotes con existencias que ya vencieron o vencen en los próximos "
                + DashboardDAO.DIAS_ALERTA_VENCIMIENTO + " días.");
        lblKpiPorVencerTitulo.setText("Vencen en ≤ " + DashboardDAO.DIAS_ALERTA_VENCIMIENTO + " días");
        lblTituloPeriodo.setText("Ingresos, costos y ganancia por mes (últimos " + DashboardDAO.MESES_PERIODO + " meses)");
        refrescar();
    }

    // ------------------------------------------------------------------ Navegación

    @FXML private void abrirClientes() { Navegador.abrir(Modulo.CLIENTES); }
    @FXML private void abrirProductos() { Navegador.abrir(Modulo.PRODUCTOS); }
    @FXML private void abrirLotes() { Navegador.abrir(Modulo.LOTES); }
    @FXML private void abrirVentas() { Navegador.abrir(Modulo.VENTAS); }
    @FXML private void abrirAgenda() { Navegador.abrir(Modulo.AGENDA); }

    @FXML
    private void handleActualizar() {
        refrescar();
    }

    // ------------------------------------------------------------------ Datos

    @Override
    public void refrescar() {
        try {
            datos = dashboardDAO.cargar();
        } catch (Exception e) {
            lblActualizado.setText("No se pudieron cargar los indicadores.");
            Alertas.errorBD("cargar el panel de control", e);
            return;
        }

        mostrarIndicadores(datos);
        mostrarVencimientos(datos.lotesAlerta());
        tblStock.getItems().setAll(datos.stock());
        mostrarFinanzas(datos.resumen(), datos.finanzasProducto(), datos.periodo());
        mostrarRankings(datos.clientes(), datos.servicios());
        lblActualizado.setText("Datos actualizados el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' hh:mm a")));
    }

    /** Alertas al iniciar sesión: productos vencidos, próximos a vencer y existencias bajo el mínimo. */
    public static void mostrarAlertasInventario(DatosDashboard datos) {
        if (datos == null) return;

        List<LoteAlerta> vencidos = datos.lotesAlerta().stream().filter(LoteAlerta::vencido).toList();
        if (!vencidos.isEmpty()) {
            Alertas.error("Hay " + vencidos.size() + " lote(s) de productos vencidos",
                    "Retírelos de la venta y del uso en servicios:\n" + listar(vencidos,
                            l -> l.producto() + " (lote " + l.codigoLote() + ", " + l.cantidad() + " u.) — venció el " + Tablas.fecha(l.fechaCaducidad())));
        }

        List<LoteAlerta> porVencer = datos.lotesAlerta().stream().filter(l -> !l.vencido()).toList();
        List<StockProducto> bajoMinimo = datos.stock().stream().filter(StockProducto::bajoMinimo).toList();
        if (!porVencer.isEmpty() || !bajoMinimo.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (!porVencer.isEmpty()) {
                sb.append("Productos próximos a vencer (").append(porVencer.size()).append("):\n")
                        .append(listar(porVencer, l -> l.producto() + " (" + l.cantidad() + " u.) — vence en "
                                + l.diasRestantes() + " día(s), " + Tablas.fecha(l.fechaCaducidad())));
            }
            if (!bajoMinimo.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n\n");
                sb.append("Existencias en o por debajo del mínimo (").append(bajoMinimo.size()).append("):\n")
                        .append(listar(bajoMinimo, s -> s.producto() + ": " + s.stockActual() + " u. (mínimo " + s.stockMinimo() + ")"));
            }
            Alertas.advertencia("Productos que requieren atención", sb.toString());
        }
    }

    private void mostrarIndicadores(DatosDashboard d) {
        ResumenFinanciero r = d.resumen();
        lblKpiIngresos.setText(Tablas.dinero(r.ingresosTotales()));
        lblKpiCostos.setText(Tablas.dinero(r.costosTotales()));
        lblKpiGanancia.setText(Tablas.dinero(r.gananciaTotal()));
        lblKpiVencidos.setText(String.valueOf(d.lotesVencidos()));
        lblKpiPorVencer.setText(String.valueOf(d.lotesPorVencer()));
        lblKpiBajoMinimo.setText(String.valueOf(d.productosBajoMinimo()));
    }

    private void mostrarVencimientos(List<LoteAlerta> lotes) {
        tblVencimientos.getItems().setAll(lotes);

        // Unidades agrupadas por producto, separando vencidas de próximas a vencer
        Map<String, int[]> porProducto = new LinkedHashMap<>();
        for (LoteAlerta l : lotes) {
            int[] valores = porProducto.computeIfAbsent(l.producto(), k -> new int[2]);
            valores[l.vencido() ? 0 : 1] += l.cantidad();
        }
        XYChart.Series<String, Number> vencidos = new XYChart.Series<>();
        vencidos.setName("Vencidos");
        XYChart.Series<String, Number> porVencer = new XYChart.Series<>();
        porVencer.setName("Próximos a vencer");
        porProducto.forEach((producto, v) -> {
            vencidos.getData().add(new XYChart.Data<>(producto, v[0]));
            porVencer.getData().add(new XYChart.Data<>(producto, v[1]));
        });
        reemplazarSeries(chartVencimientos, List.of(vencidos, porVencer));
    }

    private void mostrarFinanzas(ResumenFinanciero r, List<FinanzaProducto> productos, List<MovimientoPeriodo> periodo) {
        lblIngresosProductos.setText(Tablas.dinero(r.ingresosProductos()));
        lblCostosProductos.setText("- " + Tablas.dinero(r.costosProductos()));
        lblIngresosServicios.setText(Tablas.dinero(r.ingresosServicios()));
        lblCostosServicios.setText("- " + Tablas.dinero(r.costosServicios()));
        lblIngresosTotales.setText(Tablas.dinero(r.ingresosTotales()));
        lblCostosTotales.setText("- " + Tablas.dinero(r.costosTotales()));
        lblGananciaTotal.setText(Tablas.dinero(r.gananciaTotal()));
        Tema.textoMonto(lblGananciaTotal, r.gananciaTotal().signum() < 0);
        lblMargen.setText(r.ingresosTotales().signum() == 0 ? "—"
                : r.gananciaTotal().multiply(BigDecimal.valueOf(100))
                .divide(r.ingresosTotales(), 1, RoundingMode.HALF_UP) + " %");

        tblFinanzas.getItems().setAll(productos);

        XYChart.Series<String, Number> ingresos = new XYChart.Series<>();
        ingresos.setName("Ingresos");
        XYChart.Series<String, Number> costos = new XYChart.Series<>();
        costos.setName("Costos");
        XYChart.Series<String, Number> ganancia = new XYChart.Series<>();
        ganancia.setName("Ganancia");
        for (MovimientoPeriodo m : periodo) {
            String mes = m.mes().getMonth().getDisplayName(TextStyle.SHORT, ES) + " " + m.mes().getYear();
            ingresos.getData().add(new XYChart.Data<>(mes, m.ingresos()));
            costos.getData().add(new XYChart.Data<>(mes, m.costos()));
            ganancia.getData().add(new XYChart.Data<>(mes, m.ganancia()));
        }
        reemplazarSeries(chartPeriodo, List.of(ingresos, costos, ganancia));
    }

    private void mostrarRankings(List<ClienteRanking> clientes, List<ServicioRanking> servicios) {
        tblClientes.getItems().setAll(clientes);
        tblServicios.getItems().setAll(servicios);

        XYChart.Series<String, Number> serieClientes = new XYChart.Series<>();
        serieClientes.setName("Compras");
        clientes.stream().limit(TOP_GRAFICOS)
                .forEach(c -> serieClientes.getData().add(new XYChart.Data<>(c.cliente(), c.compras())));
        reemplazarSeries(chartClientes, List.of(serieClientes));

        XYChart.Series<String, Number> serieServicios = new XYChart.Series<>();
        serieServicios.setName("Solicitudes");
        servicios.forEach(s -> serieServicios.getData().add(new XYChart.Data<>(s.servicio(), s.solicitudes())));
        reemplazarSeries(chartServicios, List.of(serieServicios));
    }

    // ------------------------------------------------------------------ Configuración

    private void configurarTablas() {
        Tablas.valor(colVenProducto, LoteAlerta::producto);
        Tablas.valor(colVenLote, LoteAlerta::codigoLote);
        Tablas.valor(colVenCantidad, LoteAlerta::cantidad);
        Tablas.valor(colVenFecha, l -> Tablas.fecha(l.fechaCaducidad()));
        Tablas.grafico(colVenEstado, l -> l.vencido()
                ? Tablas.badge("Vencido hace " + (-l.diasRestantes()) + " d", Tema.Estado.PELIGRO, Icono.Tipo.ERROR)
                : Tablas.badge(l.diasRestantes() == 0 ? "Vence hoy" : "Vence en " + l.diasRestantes() + " d",
                Tema.Estado.ALERTA, Icono.Tipo.ADVERTENCIA));
        Tablas.colorearFilas(tblVencimientos, l -> l.vencido() ? Tema.Estado.PELIGRO : Tema.Estado.ALERTA);
        Tablas.centrar(colVenCantidad);

        Tablas.valor(colStockProducto, StockProducto::producto);
        Tablas.valor(colStockActual, StockProducto::stockActual);
        Tablas.valor(colStockMinimo, StockProducto::stockMinimo);
        Tablas.grafico(colStockEstado, s -> s.bajoMinimo()
                ? Tablas.badge(s.stockActual() == 0 ? "Agotado" : "Reponer", Tema.Estado.PELIGRO, Icono.Tipo.ADVERTENCIA)
                : Tablas.badge("Suficiente", Tema.Estado.OK, Icono.Tipo.EXITO));
        Tablas.colorearFilas(tblStock, s -> s.bajoMinimo() ? Tema.Estado.PELIGRO : null);
        Tablas.centrar(colStockActual);
        Tablas.centrar(colStockMinimo);

        Tablas.valor(colFinProducto, FinanzaProducto::producto);
        Tablas.valor(colFinCompra, FinanzaProducto::precioCompra);
        Tablas.valor(colFinVenta, FinanzaProducto::precioVenta);
        Tablas.valor(colFinGananciaUnit, FinanzaProducto::gananciaUnitaria);
        Tablas.valor(colFinUnidades, FinanzaProducto::unidadesVendidas);
        Tablas.valor(colFinIngresos, FinanzaProducto::ingresos);
        Tablas.valor(colFinCostos, FinanzaProducto::costos);
        Tablas.valor(colFinGanancia, FinanzaProducto::ganancia);
        for (TableColumn<FinanzaProducto, BigDecimal> col : List.of(colFinCompra, colFinVenta, colFinGananciaUnit,
                colFinIngresos, colFinCostos, colFinGanancia)) {
            Tablas.formatoDinero(col);
        }
        Tablas.centrar(colFinUnidades);

        colCliPosicion.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                tblClientes.getItems().indexOf(c.getValue()) + 1));
        Tablas.valor(colCliNombre, ClienteRanking::cliente);
        Tablas.valor(colCliCompras, ClienteRanking::compras);
        Tablas.valor(colCliIngresos, ClienteRanking::ingresos);
        Tablas.formatoDinero(colCliIngresos);
        Tablas.centrar(colCliCompras);

        colSerPosicion.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                tblServicios.getItems().indexOf(c.getValue()) + 1));
        Tablas.valor(colSerNombre, ServicioRanking::servicio);
        Tablas.valor(colSerVeces, ServicioRanking::solicitudes);
        Tablas.valor(colSerIngresos, ServicioRanking::ingresos);
        Tablas.formatoDinero(colSerIngresos);
        Tablas.centrar(colSerVeces);
    }

    /** Los ejes de conteo solo muestran números enteros. */
    @SafeVarargs
    private static void configurarEjesEnteros(BarChart<String, Number>... graficos) {
        for (BarChart<String, Number> grafico : graficos) {
            NumberAxis eje = (NumberAxis) grafico.getYAxis();
            eje.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number n) {
                    return n.doubleValue() == Math.rint(n.doubleValue()) ? String.valueOf(n.longValue()) : "";
                }

                @Override
                public Number fromString(String s) {
                    return Long.parseLong(s);
                }
            });
        }
    }

    private static void reemplazarSeries(BarChart<String, Number> grafico, List<XYChart.Series<String, Number>> series) {
        grafico.getData().clear();
        grafico.getData().addAll(series);
    }

    private static <T> String listar(List<T> elementos, Function<T, String> descripcion) {
        StringBuilder sb = new StringBuilder();
        elementos.stream().limit(MAX_LINEAS_ALERTA).forEach(e -> sb.append("\n  •  ").append(descripcion.apply(e)));
        if (elementos.size() > MAX_LINEAS_ALERTA) {
            sb.append("\n  … y ").append(elementos.size() - MAX_LINEAS_ALERTA).append(" más (vea el panel de control).");
        }
        return sb.toString();
    }
}
