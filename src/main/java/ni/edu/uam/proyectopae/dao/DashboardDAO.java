package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas del panel de control. Todos los indicadores se calculan con los datos reales de PostgreSQL:
 * ingresos = ventas de productos + servicios (citas REALIZADAS);
 * costos = costo de los productos vendidos + insumos consumidos en servicios realizados.
 */
public class DashboardDAO {

    /** Días de anticipación para considerar un lote "próximo a vencer". */
    public static final int DIAS_ALERTA_VENCIMIENTO = 30;
    /** Cantidad de meses que se muestran en el gráfico por período. */
    public static final int MESES_PERIODO = 6;

    static final String HQL_LOTES_ALERTA =
            "SELECT p.nombreProducto, l.codigoLote, l.cantidadDisponible, l.fechaCaducidad " +
                    "FROM Lote l JOIN l.producto p " +
                    "WHERE l.cantidadDisponible > 0 AND l.fechaCaducidad <= :limite " +
                    "ORDER BY l.fechaCaducidad ASC";

    static final String HQL_STOCK =
            "SELECT p.nombreProducto, p.stockMinimo, " +
                    "COALESCE(SUM(CASE WHEN l.fechaCaducidad >= :hoy THEN l.cantidadDisponible ELSE 0 END), 0) " +
                    "FROM Producto p LEFT JOIN p.lotes l " +
                    "GROUP BY p.idProducto, p.nombreProducto, p.stockMinimo " +
                    "ORDER BY p.nombreProducto ASC";

    static final String HQL_FINANZAS_PRODUCTO =
            "SELECT p.nombreProducto, p.precioCompra, p.precioVenta, " +
                    "COALESCE(SUM(d.cantidad), 0), COALESCE(SUM(d.subtotal), 0), " +
                    "COALESCE(SUM(d.costoUnitario * d.cantidad), 0) " +
                    "FROM Producto p LEFT JOIN DetalleVenta d ON d.producto = p " +
                    "GROUP BY p.idProducto, p.nombreProducto, p.precioCompra, p.precioVenta";

    static final String HQL_TOTALES_VENTAS =
            "SELECT COALESCE(SUM(v.total), 0), COALESCE(SUM(v.costoTotal), 0) FROM Venta v";

    static final String HQL_INGRESOS_SERVICIOS =
            "SELECT COALESCE(SUM(s.tarifaBase), 0) FROM Cita c JOIN c.servicio s WHERE c.estado = 'REALIZADA'";

    static final String HQL_COSTOS_SERVICIOS =
            "SELECT COALESCE(SUM(ci.cantidadDescontada * p.precioCompra), 0) " +
                    "FROM ConsumoInsumo ci JOIN ci.cita c JOIN ci.lote l JOIN l.producto p " +
                    "WHERE c.estado = 'REALIZADA'";

    static final String HQL_RANKING_SERVICIOS =
            "SELECT s.nombreServicio, COUNT(c.idCita), " +
                    "COALESCE(SUM(CASE WHEN c.estado = 'REALIZADA' THEN s.tarifaBase ELSE 0 END), 0) " +
                    "FROM Cita c JOIN c.servicio s WHERE c.estado <> 'CANCELADA' " +
                    "GROUP BY s.idServicio, s.nombreServicio " +
                    "ORDER BY COUNT(c.idCita) DESC";

    static final String HQL_CLIENTES_VENTAS =
            "SELECT cl.idCliente, cl.nombres, cl.apellidos, COUNT(v.idVenta), SUM(v.total) " +
                    "FROM Venta v JOIN v.cliente cl " +
                    "GROUP BY cl.idCliente, cl.nombres, cl.apellidos";

    static final String HQL_CLIENTES_SERVICIOS =
            "SELECT cl.idCliente, cl.nombres, cl.apellidos, COUNT(c.idCita), SUM(s.tarifaBase) " +
                    "FROM Cita c JOIN c.cliente cl JOIN c.servicio s WHERE c.estado = 'REALIZADA' " +
                    "GROUP BY cl.idCliente, cl.nombres, cl.apellidos";

    static final String HQL_PERIODO_VENTAS =
            "SELECT v.fecha, v.total, v.costoTotal FROM Venta v WHERE v.fecha >= :desde";

    static final String HQL_PERIODO_SERVICIOS =
            "SELECT c.fechaHora, s.tarifaBase FROM Cita c JOIN c.servicio s " +
                    "WHERE c.estado = 'REALIZADA' AND c.fechaHora >= :desde";

    static final String HQL_PERIODO_COSTOS_SERVICIOS =
            "SELECT c.fechaHora, ci.cantidadDescontada * p.precioCompra " +
                    "FROM ConsumoInsumo ci JOIN ci.cita c JOIN ci.lote l JOIN l.producto p " +
                    "WHERE c.estado = 'REALIZADA' AND c.fechaHora >= :desde";

    // ---------------------------------------------------------------- DTOs

    public record LoteAlerta(String producto, String codigoLote, int cantidad, LocalDate fechaCaducidad, long diasRestantes) {
        public boolean vencido() { return diasRestantes < 0; }
    }

    public record StockProducto(String producto, long stockActual, int stockMinimo) {
        public boolean bajoMinimo() { return stockActual <= stockMinimo; }
    }

    public record FinanzaProducto(String producto, BigDecimal precioCompra, BigDecimal precioVenta,
                                  long unidadesVendidas, BigDecimal ingresos, BigDecimal costos) {
        public BigDecimal gananciaUnitaria() { return precioVenta.subtract(precioCompra); }
        public BigDecimal ganancia() { return ingresos.subtract(costos); }
    }

    public record ResumenFinanciero(BigDecimal ingresosProductos, BigDecimal costosProductos,
                                    BigDecimal ingresosServicios, BigDecimal costosServicios) {
        public BigDecimal ingresosTotales() { return ingresosProductos.add(ingresosServicios); }
        public BigDecimal costosTotales() { return costosProductos.add(costosServicios); }
        public BigDecimal gananciaTotal() { return ingresosTotales().subtract(costosTotales()); }
    }

    public record ClienteRanking(String cliente, long compras, BigDecimal ingresos) {}

    public record ServicioRanking(String servicio, long solicitudes, BigDecimal ingresos) {}

    public record MovimientoPeriodo(YearMonth mes, BigDecimal ingresos, BigDecimal costos) {
        public BigDecimal ganancia() { return ingresos.subtract(costos); }
    }

    public record DatosDashboard(List<LoteAlerta> lotesAlerta, List<StockProducto> stock,
                                 List<FinanzaProducto> finanzasProducto, ResumenFinanciero resumen,
                                 List<ClienteRanking> clientes, List<ServicioRanking> servicios,
                                 List<MovimientoPeriodo> periodo) {
        public long lotesVencidos() { return lotesAlerta.stream().filter(LoteAlerta::vencido).count(); }
        public long lotesPorVencer() { return lotesAlerta.stream().filter(l -> !l.vencido()).count(); }
        public long productosBajoMinimo() { return stock.stream().filter(StockProducto::bajoMinimo).count(); }
    }

    // ---------------------------------------------------------------- Carga

    public DatosDashboard cargar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate hoy = LocalDate.now();
            return new DatosDashboard(
                    lotesAlerta(session, hoy),
                    stock(session, hoy),
                    finanzasProducto(session),
                    resumen(session),
                    rankingClientes(session),
                    rankingServicios(session),
                    periodo(session, hoy));
        }
    }

    private List<LoteAlerta> lotesAlerta(Session session, LocalDate hoy) {
        List<LoteAlerta> lista = new ArrayList<>();
        for (Object[] f : session.createQuery(HQL_LOTES_ALERTA, Object[].class)
                .setParameter("limite", hoy.plusDays(DIAS_ALERTA_VENCIMIENTO)).list()) {
            LocalDate fecha = (LocalDate) f[3];
            lista.add(new LoteAlerta((String) f[0], (String) f[1], ((Number) f[2]).intValue(), fecha,
                    ChronoUnit.DAYS.between(hoy, fecha)));
        }
        return lista;
    }

    private List<StockProducto> stock(Session session, LocalDate hoy) {
        List<StockProducto> lista = new ArrayList<>();
        for (Object[] f : session.createQuery(HQL_STOCK, Object[].class).setParameter("hoy", hoy).list()) {
            lista.add(new StockProducto((String) f[0], ((Number) f[2]).longValue(), ((Number) f[1]).intValue()));
        }
        // Primero los que necesitan reposición
        lista.sort(Comparator.comparing((StockProducto s) -> !s.bajoMinimo())
                .thenComparingLong(s -> s.stockActual() - s.stockMinimo()));
        return lista;
    }

    private List<FinanzaProducto> finanzasProducto(Session session) {
        List<FinanzaProducto> lista = new ArrayList<>();
        for (Object[] f : session.createQuery(HQL_FINANZAS_PRODUCTO, Object[].class).list()) {
            lista.add(new FinanzaProducto((String) f[0], dinero(f[1]), dinero(f[2]),
                    ((Number) f[3]).longValue(), dinero(f[4]), dinero(f[5])));
        }
        lista.sort(Comparator.comparing(FinanzaProducto::ingresos).reversed()
                .thenComparing(FinanzaProducto::producto));
        return lista;
    }

    private ResumenFinanciero resumen(Session session) {
        Object[] ventas = session.createQuery(HQL_TOTALES_VENTAS, Object[].class).uniqueResult();
        Object servicios = session.createQuery(HQL_INGRESOS_SERVICIOS, Object.class).uniqueResult();
        Object costosServicios = session.createQuery(HQL_COSTOS_SERVICIOS, Object.class).uniqueResult();
        return new ResumenFinanciero(dinero(ventas[0]), dinero(ventas[1]), dinero(servicios), dinero(costosServicios));
    }

    private List<ServicioRanking> rankingServicios(Session session) {
        List<ServicioRanking> lista = new ArrayList<>();
        for (Object[] f : session.createQuery(HQL_RANKING_SERVICIOS, Object[].class).list()) {
            lista.add(new ServicioRanking((String) f[0], ((Number) f[1]).longValue(), dinero(f[2])));
        }
        return lista;
    }

    /** Combina las compras de productos y los servicios realizados de cada cliente. */
    private List<ClienteRanking> rankingClientes(Session session) {
        Map<Integer, Object[]> acumulado = new LinkedHashMap<>();
        for (String hql : List.of(HQL_CLIENTES_VENTAS, HQL_CLIENTES_SERVICIOS)) {
            for (Object[] f : session.createQuery(hql, Object[].class).list()) {
                Object[] actual = acumulado.computeIfAbsent((Integer) f[0],
                        id -> new Object[]{f[1] + " " + f[2], 0L, BigDecimal.ZERO});
                actual[1] = (Long) actual[1] + ((Number) f[3]).longValue();
                actual[2] = ((BigDecimal) actual[2]).add(dinero(f[4]));
            }
        }
        List<ClienteRanking> lista = new ArrayList<>();
        for (Object[] a : acumulado.values()) {
            lista.add(new ClienteRanking((String) a[0], (Long) a[1], (BigDecimal) a[2]));
        }
        lista.sort(Comparator.comparingLong(ClienteRanking::compras).reversed()
                .thenComparing(ClienteRanking::ingresos, Comparator.reverseOrder()));
        return lista;
    }

    /** Ingresos y costos agrupados por mes (últimos MESES_PERIODO meses, incluyendo el actual). */
    private List<MovimientoPeriodo> periodo(Session session, LocalDate hoy) {
        YearMonth primerMes = YearMonth.from(hoy).minusMonths(MESES_PERIODO - 1L);
        LocalDateTime desde = primerMes.atDay(1).atStartOfDay();

        Map<YearMonth, BigDecimal[]> meses = new LinkedHashMap<>();
        for (int i = 0; i < MESES_PERIODO; i++) {
            meses.put(primerMes.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Object[] f : session.createQuery(HQL_PERIODO_VENTAS, Object[].class).setParameter("desde", desde).list()) {
            acumular(meses, (LocalDateTime) f[0], dinero(f[1]), dinero(f[2]));
        }
        for (Object[] f : session.createQuery(HQL_PERIODO_SERVICIOS, Object[].class).setParameter("desde", desde).list()) {
            acumular(meses, (LocalDateTime) f[0], dinero(f[1]), BigDecimal.ZERO);
        }
        for (Object[] f : session.createQuery(HQL_PERIODO_COSTOS_SERVICIOS, Object[].class).setParameter("desde", desde).list()) {
            acumular(meses, (LocalDateTime) f[0], BigDecimal.ZERO, dinero(f[1]));
        }

        List<MovimientoPeriodo> lista = new ArrayList<>();
        meses.forEach((mes, v) -> lista.add(new MovimientoPeriodo(mes, v[0], v[1])));
        return lista;
    }

    private static void acumular(Map<YearMonth, BigDecimal[]> meses, LocalDateTime fecha, BigDecimal ingreso, BigDecimal costo) {
        BigDecimal[] valores = meses.get(YearMonth.from(fecha));
        if (valores != null) {
            valores[0] = valores[0].add(ingreso);
            valores[1] = valores[1].add(costo);
        }
    }

    private static BigDecimal dinero(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor instanceof BigDecimal bd) return bd;
        return new BigDecimal(valor.toString());
    }
}
