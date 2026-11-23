package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.*;
import ni.edu.uam.proyectopae.reportes.*;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** Consulta los apartados seleccionados sin modificar registros ni devolver entidades lazy. */
public final class ReporteGeneralDAO {
    private final Supplier<SessionFactory> fabrica;

    public ReporteGeneralDAO() { this.fabrica = HibernateUtil::getSessionFactory; }

    /** Permite verificar las consultas con una base de pruebas independiente. */
    public ReporteGeneralDAO(SessionFactory fabrica) { this.fabrica = () -> fabrica; }

    public DatosReporte consultar(SolicitudReporte solicitud) {
        LocalDateTime generado = LocalDateTime.now();
        try (Session sesion = fabrica.get().openSession()) {
            sesion.setDefaultReadOnly(true);
            sesion.setHibernateFlushMode(FlushMode.MANUAL);
            Transaction tx = sesion.beginTransaction();
            try {
                Consulta consulta = new Consulta(sesion, solicitud, generado.toLocalDate());
                List<SeccionReporte> secciones = new ArrayList<>();
                solicitud.seleccion().forEach((apartado, campos) -> {
                    List<Map<String, String>> filas = consulta.filas(apartado);
                    secciones.add(new SeccionReporte(apartado, campos, filas, consulta.resumen(apartado, filas)));
                });
                tx.commit();
                return new DatosReporte(solicitud, generado, secciones);
            } catch (RuntimeException error) {
                try { if (tx.isActive()) tx.rollback(); }
                catch (RuntimeException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        }
    }

    private static final class Consulta {
        private final Session sesion;
        private final SolicitudReporte solicitud;
        private final LocalDate hoy;
        private final Map<String, List<?>> cache = new HashMap<>();
        private Map<Integer, Long> stock;

        Consulta(Session sesion, SolicitudReporte solicitud, LocalDate hoy) {
            this.sesion = sesion;
            this.solicitud = solicitud;
            this.hoy = hoy;
        }

        List<Map<String, String>> filas(ApartadoReporte apartado) {
            return switch (apartado) {
                case RESUMEN -> resumenGeneral();
                case PRODUCTOS -> productos().stream().map(p -> fila(apartado,
                        p.getIdProducto(), p.getNombreProducto(), p.getCategoria().getIdCategoria(), p.getNombreCategoria(),
                        p.getMarca().getIdMarca(), p.getNombreMarca(), p.getPrecioCompra(), p.getPrecioVenta(),
                        p.getGananciaUnitaria(), p.getMargen().toPlainString() + " %", stock(p), p.getStockMinimo(),
                        stock(p) < p.getStockMinimo() ? "BAJO EL MÍNIMO" : "SUFICIENTE")).toList();
                case LOTES -> periodo("SELECT l FROM Lote l JOIN FETCH l.producto p WHERE 1=1",
                        Lote.class, "l.fechaIngreso", true, null, "l.fechaIngreso DESC, l.idLote DESC").stream()
                        .map(l -> fila(apartado, l.getIdLote(), l.getProducto().getIdProducto(), l.getNombreProducto(),
                                l.getCodigoLote(), l.getFechaIngreso(), l.getFechaCaducidad(), l.getCantidadDisponible(),
                                dias(l), estado(l))).toList();
                case CLIENTES -> listar("FROM Cliente ORDER BY nombres, apellidos, idCliente", Cliente.class).stream()
                        .map(c -> fila(apartado, c.getIdCliente(), c.getNombres(), c.getApellidos(), c.getSexo(),
                                c.getTelefono(), c.getCorreo(), c.getAlergiasPiel())).toList();
                case SERVICIOS -> servicios().stream()
                        .filter(s -> solicitud.idServicio() == null || solicitud.idServicio().equals(s.getIdServicio()))
                        .map(s -> fila(apartado, s.getIdServicio(), s.getNombreServicio(), s.getDuracionMinutos(),
                                s.getTarifaBase(), String.join("; ", s.getProductos().stream()
                                        .sorted(Comparator.comparing(Producto::getNombreProducto))
                                        .map(p -> p.getIdProducto() + " - " + p.getNombreProducto()).toList()))).toList();
                case CITAS -> citas().stream().map(c -> fila(apartado, c.getIdCita(), c.getCliente().getIdCliente(),
                        c.getNombreCliente(), c.getServicio().getIdServicio(), c.getNombreServicio(), c.getFechaHora(),
                        c.getEstado(), c.getServicio().getTarifaBase())).toList();
                case VENTAS -> ventas().stream().map(v -> fila(apartado, v.getIdVenta(), v.getCliente().getIdCliente(),
                        v.getNombreCliente(), v.getFecha(), v.getTotal(), v.getCostoTotal(), v.getGanancia())).toList();
                case DETALLE_VENTAS -> periodo("""
                        SELECT d FROM DetalleVenta d JOIN FETCH d.venta v
                        JOIN FETCH v.cliente JOIN FETCH d.producto WHERE 1=1
                        """, DetalleVenta.class, "v.fecha", false, null, "v.fecha DESC, v.idVenta DESC, d.idDetalle").stream()
                        .map(d -> fila(apartado, d.getIdDetalle(), d.getVenta().getIdVenta(), d.getVenta().getFecha(),
                                d.getVenta().getNombreCliente(), d.getProducto().getIdProducto(), d.getNombreProducto(),
                                d.getCantidad(), d.getPrecioUnitario(), d.getCostoUnitario(), d.getSubtotal(), d.getGanancia())).toList();
                case CONSUMOS -> periodo("""
                        SELECT co FROM ConsumoInsumo co JOIN FETCH co.cita ci
                        JOIN FETCH ci.cliente JOIN FETCH ci.servicio JOIN FETCH co.lote l
                        JOIN FETCH l.producto WHERE 1=1
                        """, ConsumoInsumo.class, "ci.fechaHora", false, "ci.servicio.idServicio",
                        "ci.fechaHora DESC, co.idConsumo DESC").stream()
                        .map(c -> fila(apartado, c.getIdConsumo(), c.getCita().getIdCita(), c.getCita().getFechaHora(),
                                c.getCita().getNombreCliente(), c.getCita().getNombreServicio(), c.getLote().getIdLote(),
                                c.getLote().getCodigoLote(), c.getLote().getProducto().getIdProducto(),
                                c.getLote().getNombreProducto(), c.getCantidadDescontada())).toList();
                case CATEGORIAS -> listar("FROM Categoria ORDER BY nombre, idCategoria", Categoria.class).stream()
                        .map(c -> fila(apartado, c.getIdCategoria(), c.getNombre(), c.getDescripcion())).toList();
                case MARCAS -> listar("FROM Marca ORDER BY nombreMarca, idMarca", Marca.class).stream()
                        .map(m -> fila(apartado, m.getIdMarca(), m.getNombreMarca())).toList();
                // Proyección explícita: la contraseña ni siquiera se carga en memoria para el reporte.
                case USUARIOS -> listar("SELECT u.idUsuario, u.username, u.rol FROM Usuario u ORDER BY u.username", Object[].class)
                        .stream().map(u -> fila(apartado, u)).toList();
            };
        }

        private List<Producto> productos() {
            return listar("SELECT p FROM Producto p JOIN FETCH p.categoria JOIN FETCH p.marca ORDER BY p.nombreProducto, p.idProducto", Producto.class);
        }

        private List<Lote> lotes() {
            return listar("SELECT l FROM Lote l JOIN FETCH l.producto ORDER BY l.idLote", Lote.class);
        }

        private List<Servicio> servicios() {
            return listar("SELECT DISTINCT s FROM Servicio s LEFT JOIN FETCH s.productos ORDER BY s.nombreServicio, s.idServicio", Servicio.class);
        }

        private List<Cita> citas() {
            return periodo("SELECT c FROM Cita c JOIN FETCH c.cliente JOIN FETCH c.servicio WHERE 1=1",
                    Cita.class, "c.fechaHora", false, "c.servicio.idServicio", "c.fechaHora DESC, c.idCita DESC");
        }

        private List<Venta> ventas() {
            return periodo("SELECT v FROM Venta v JOIN FETCH v.cliente WHERE 1=1",
                    Venta.class, "v.fecha", false, null, "v.fecha DESC, v.idVenta DESC");
        }

        private long stock(Producto producto) {
            if (stock == null) {
                stock = new HashMap<>();
                sesion.createQuery("""
                        SELECT l.producto.idProducto, SUM(l.cantidadDisponible) FROM Lote l
                        WHERE l.fechaCaducidad >= :hoy GROUP BY l.producto.idProducto
                        """, Object[].class).setParameter("hoy", hoy).list()
                        .forEach(fila -> stock.put((Integer) fila[0], ((Number) fila[1]).longValue()));
            }
            return stock.getOrDefault(producto.getIdProducto(), 0L);
        }

        private long dias(Lote lote) {
            return java.time.temporal.ChronoUnit.DAYS.between(hoy, lote.getFechaCaducidad());
        }

        private String estado(Lote lote) {
            return dias(lote) < 0 ? "VENCIDO" : dias(lote) <= Lote.DIAS_ALERTA_VENCIMIENTO ? "POR VENCER" : "VIGENTE";
        }

        private List<Map<String, String>> resumenGeneral() {
            List<Map<String, String>> filas = new ArrayList<>();
            indicador(filas, "Productos en catálogo (actual)", productos().size());
            indicador(filas, "Clientes registrados (actual)", listar("SELECT COUNT(c) FROM Cliente c", Long.class).getFirst());
            indicador(filas, "Unidades de stock vigente (actual)", productos().stream().mapToLong(this::stock).sum());
            indicador(filas, "Productos bajo el mínimo (actual)", productos().stream().filter(p -> stock(p) < p.getStockMinimo()).count());
            indicador(filas, "Lotes con stock por vencer en 30 días (actual)", lotes().stream()
                    .filter(l -> l.getCantidadDisponible() > 0 && dias(l) >= 0 && dias(l) <= Lote.DIAS_ALERTA_VENCIMIENTO).count());
            indicador(filas, "Lotes vencidos con stock (actual)", lotes().stream().filter(l -> l.getCantidadDisponible() > 0 && dias(l) < 0).count());
            indicador(filas, "Ventas del período", ventas().size());
            indicador(filas, "Total vendido en el período", sumarVentas(Venta::getTotal));
            indicador(filas, "Costo de ventas del período", sumarVentas(Venta::getCostoTotal));
            indicador(filas, "Ganancia de ventas del período", sumarVentas(Venta::getGanancia));
            indicador(filas, "Citas del período y servicio", citas().size());
            for (String estado : List.of("PROGRAMADA", "COMPLETADA", "CANCELADA")) {
                indicador(filas, "Citas " + estado.toLowerCase(Locale.ROOT) + "s del período y servicio",
                        citas().stream().filter(c -> estado.equalsIgnoreCase(c.getEstado())).count());
            }
            return filas;
        }

        private void indicador(List<Map<String, String>> filas, String titulo, Object valor) {
            filas.add(fila(ApartadoReporte.RESUMEN, titulo, valor));
        }

        private BigDecimal sumarVentas(java.util.function.Function<Venta, BigDecimal> valor) {
            return ventas().stream().map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        String resumen(ApartadoReporte apartado, List<Map<String, String>> filas) {
            if (apartado == ApartadoReporte.VENTAS) {
                return "Ventas: " + filas.size() + " | Total: " + texto(sumarVentas(Venta::getTotal))
                        + " | Costo: " + texto(sumarVentas(Venta::getCostoTotal))
                        + " | Ganancia: " + texto(sumarVentas(Venta::getGanancia));
            }
            if (apartado == ApartadoReporte.CITAS) {
                return "Citas: " + filas.size() + " | Programadas: " + contarCitas("PROGRAMADA")
                        + " | Completadas: " + contarCitas("COMPLETADA") + " | Canceladas: " + contarCitas("CANCELADA");
            }
            return "Registros: " + filas.size();
        }

        private long contarCitas(String estado) {
            return citas().stream().filter(c -> estado.equalsIgnoreCase(c.getEstado())).count();
        }

        @SuppressWarnings("unchecked")
        private <T> List<T> listar(String hql, Class<T> tipo) {
            return (List<T>) cache.computeIfAbsent(hql, clave -> sesion.createQuery(hql, tipo).setReadOnly(true).list());
        }

        @SuppressWarnings("unchecked")
        private <T> List<T> periodo(String base, Class<T> tipo, String fecha, boolean soloFecha, String servicio, String orden) {
            StringBuilder hql = new StringBuilder(base);
            if (solicitud.desde() != null) hql.append(" AND ").append(fecha).append(" >= :desde");
            boolean tieneHasta = solicitud.hasta() != null && (soloFecha || !LocalDate.MAX.equals(solicitud.hasta()));
            if (tieneHasta) hql.append(" AND ").append(fecha).append(soloFecha ? " <= :hasta" : " < :hasta");
            if (servicio != null && solicitud.idServicio() != null) hql.append(" AND ").append(servicio).append(" = :servicio");
            hql.append(" ORDER BY ").append(orden);
            return (List<T>) cache.computeIfAbsent(hql.toString(), clave -> {
                Query<T> consulta = sesion.createQuery(clave, tipo).setReadOnly(true);
                if (solicitud.desde() != null) consulta.setParameter("desde", soloFecha ? solicitud.desde() : solicitud.desde().atStartOfDay());
                if (tieneHasta) consulta.setParameter("hasta", soloFecha ? solicitud.hasta() : solicitud.hasta().plusDays(1).atStartOfDay());
                if (servicio != null && solicitud.idServicio() != null) consulta.setParameter("servicio", solicitud.idServicio());
                return consulta.list();
            });
        }
    }

    private static Map<String, String> fila(ApartadoReporte apartado, Object... valores) {
        if (valores.length != apartado.campos().size()) {
            throw new IllegalArgumentException("La fila no coincide con los campos de " + apartado.titulo() + ".");
        }
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i < valores.length; i++) fila.put(apartado.campos().get(i).clave(), texto(valores[i]));
        return fila;
    }

    private static String texto(Object valor) {
        if (valor == null) return "";
        if (valor instanceof LocalDateTime fecha) return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (valor instanceof LocalDate fecha) return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (valor instanceof BigDecimal dinero) return String.format(Locale.US, "C$ %,.2f", dinero);
        return valor.toString();
    }
}
