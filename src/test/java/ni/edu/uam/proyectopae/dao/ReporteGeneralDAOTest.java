package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.*;
import ni.edu.uam.proyectopae.reportes.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Datos ficticios en H2; nunca carga las credenciales ni la base PostgreSQL del equipo. */
class ReporteGeneralDAOTest {
    private static final LocalDate HOY = LocalDate.now();
    private static SessionFactory fabrica;
    private static Integer servicioDia;
    private static Integer servicioGala;

    @BeforeAll
    static void prepararBaseAislada() {
        fabrica = new Configuration().configure("hibernate.cfg.xml")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:reportes-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .setProperty("hibernate.generate_statistics", "true")
                .buildSessionFactory();
        try (var sesion = fabrica.openSession()) {
            var tx = sesion.beginTransaction();
            Categoria categoria = new Categoria("Rostro", "Bases y correctores");
            Marca marca = new Marca("Marca de prueba");
            Cliente maria = new Cliente("María", "Pérez", "88881001", "maria@example.com", "Femenino", "Piel sensible");
            Cliente ana = new Cliente("Ana", "Rivas", null, null, "Femenino", null);
            Servicio dia = new Servicio("Maquillaje de día", 45, new BigDecimal("140.00"));
            Servicio gala = new Servicio("Maquillaje de gala", 60, new BigDecimal("250.00"));
            sesion.persist(categoria);
            sesion.persist(marca);
            sesion.persist(maria);
            sesion.persist(ana);
            sesion.persist(dia);
            sesion.persist(gala);
            Producto base = new Producto("Base de prueba", new BigDecimal("100.00"), new BigDecimal("160.00"), 5, categoria, marca);
            Producto corrector = new Producto("Corrector de prueba", new BigDecimal("50.00"), new BigDecimal("90.00"), 3, categoria, marca);
            sesion.persist(base);
            sesion.persist(corrector);
            dia.agregarProducto(base);
            dia.agregarProducto(corrector);

            Lote vigente = new Lote("VIGENTE", HOY.plusDays(60), 6, base);
            vigente.setFechaIngreso(HOY.minusDays(3));
            Lote vencido = new Lote("VENCIDO", HOY.minusDays(1), 5, base);
            vencido.setFechaIngreso(HOY.minusDays(8));
            Lote hoy = new Lote("VENCE-HOY", HOY, 3, base);
            Lote bajo = new Lote("BAJO-MINIMO", HOY.plusDays(10), 2, corrector);
            bajo.setFechaIngreso(HOY.minusDays(2));
            for (Lote lote : List.of(vigente, vencido, hoy, bajo)) sesion.persist(lote);

            Cita inicio = new Cita(maria, dia, HOY.atStartOfDay(), "PROGRAMADA");
            Cita fin = new Cita(maria, dia, HOY.atTime(23, 59, 59), "COMPLETADA");
            Cita siguiente = new Cita(maria, dia, HOY.plusDays(1).atStartOfDay(), "CANCELADA");
            Cita otroServicio = new Cita(ana, gala, HOY.atTime(12, 0), "CANCELADA");
            Cita anterior = new Cita(ana, dia, HOY.minusDays(1).atTime(12, 0), "COMPLETADA");
            for (Cita cita : List.of(inicio, fin, siguiente, otroServicio, anterior)) sesion.persist(cita);
            sesion.persist(new ConsumoInsumo(inicio, vigente, 1));
            sesion.persist(new ConsumoInsumo(fin, vigente, 2));
            sesion.persist(new ConsumoInsumo(siguiente, vigente, 1));

            Venta primera = new Venta(maria);
            primera.setFecha(HOY.atStartOfDay());
            primera.agregarDetalle(new DetalleVenta(base, 2));
            primera.agregarDetalle(new DetalleVenta(corrector, 1));
            Venta ultima = venta(ana, base, 1, HOY.atTime(23, 59, 59));
            Venta futura = venta(maria, base, 2, HOY.plusDays(1).atStartOfDay());
            Venta pasada = venta(ana, base, 3, HOY.minusDays(1).atTime(LocalTime.NOON));
            for (Venta venta : List.of(primera, ultima, futura, pasada)) sesion.persist(venta);
            // El precio actual cambia después de las ventas: no debe alterar sus importes históricos.
            base.setPrecioCompra(new BigDecimal("125.00"));
            base.setPrecioVenta(new BigDecimal("200.00"));
            sesion.persist(new Usuario("demo.reportes", "NO_EXPORTAR_CLAVE_123", "ADMINISTRADOR"));
            tx.commit();
            servicioDia = dia.getIdServicio();
            servicioGala = gala.getIdServicio();
        }
        fabrica.getStatistics().clear();
    }

    private static Venta venta(Cliente cliente, Producto producto, int cantidad, LocalDateTime fecha) {
        Venta venta = new Venta(cliente);
        venta.setFecha(fecha);
        venta.agregarDetalle(new DetalleVenta(producto, cantidad));
        return venta;
    }

    @AfterAll
    static void cerrarBaseAislada() {
        if (fabrica != null) fabrica.close();
    }

    @Test
    void consultaTodosLosApartadosConRelacionesUtilizablesTrasCerrarLaSesion() {
        DatosReporte datos = new ReporteGeneralDAO(fabrica).consultar(SolicitudReporte.todos());
        assertEquals(12, datos.secciones().size());
        assertEquals(2, seccion(datos, ApartadoReporte.PRODUCTOS).filas().size());
        assertEquals(4, seccion(datos, ApartadoReporte.LOTES).filas().size());
        assertEquals(2, seccion(datos, ApartadoReporte.CLIENTES).filas().size());
        assertEquals(2, seccion(datos, ApartadoReporte.SERVICIOS).filas().size());
        assertEquals(5, seccion(datos, ApartadoReporte.CITAS).filas().size());
        assertEquals(4, seccion(datos, ApartadoReporte.VENTAS).filas().size());
        assertEquals(5, seccion(datos, ApartadoReporte.DETALLE_VENTAS).filas().size());
        assertEquals(3, seccion(datos, ApartadoReporte.CONSUMOS).filas().size());
        assertEquals(1, seccion(datos, ApartadoReporte.CATEGORIAS).filas().size());
        assertEquals(1, seccion(datos, ApartadoReporte.MARCAS).filas().size());
        assertTrue(datos.toString().contains("María Pérez"));
        assertTrue(seccion(datos, ApartadoReporte.SERVICIOS).filas().stream()
                .anyMatch(f -> f.get("productos").contains("Base de prueba") && f.get("productos").contains("Corrector de prueba")));
        assertTrue(seccion(datos, ApartadoReporte.CLIENTES).filas().stream()
                .anyMatch(f -> f.get("nombres").equals("Ana") && f.get("correo").isEmpty()));
        assertFalse(datos.toString().contains("NO_EXPORTAR_CLAVE_123"));
        assertEquals(List.of("id", "usuario", "rol"), seccion(datos, ApartadoReporte.USUARIOS).campos()
                .stream().map(CampoReporte::clave).toList());
        assertEquals(0, fabrica.getStatistics().getEntityInsertCount());
        assertEquals(0, fabrica.getStatistics().getEntityUpdateCount());
        assertEquals(0, fabrica.getStatistics().getEntityDeleteCount());
    }

    @Test
    void aplicaFechasInclusivasYServicioSinFiltrarLosCatalogosNiLasVentasPorServicio() {
        SolicitudReporte solicitud = new SolicitudReporte(HOY, HOY, servicioDia, "Maquillaje de día",
                SolicitudReporte.todos().seleccion());
        DatosReporte datos = new ReporteGeneralDAO(fabrica).consultar(solicitud);
        assertEquals(2, seccion(datos, ApartadoReporte.CITAS).filas().size());
        assertEquals(2, seccion(datos, ApartadoReporte.CONSUMOS).filas().size());
        assertEquals(2, seccion(datos, ApartadoReporte.VENTAS).filas().size());
        assertEquals(3, seccion(datos, ApartadoReporte.DETALLE_VENTAS).filas().size());
        assertEquals(1, seccion(datos, ApartadoReporte.SERVICIOS).filas().size());
        assertEquals(2, seccion(datos, ApartadoReporte.CLIENTES).filas().size());
        assertEquals(2, seccion(datos, ApartadoReporte.PRODUCTOS).filas().size());
        assertEquals(1, seccion(datos, ApartadoReporte.LOTES).filas().size());
        assertEquals("VENCE-HOY", seccion(datos, ApartadoReporte.LOTES).filas().getFirst().get("codigo"));
        assertTrue(seccion(datos, ApartadoReporte.CITAS).filas().stream().anyMatch(f -> f.get("fecha").endsWith("00:00")));
        assertTrue(seccion(datos, ApartadoReporte.CITAS).filas().stream().anyMatch(f -> f.get("fecha").endsWith("23:59")));
        assertTrue(seccion(datos, ApartadoReporte.VENTAS).resumen().contains("Total: C$ 570.00"));
        assertTrue(seccion(datos, ApartadoReporte.VENTAS).resumen().contains("Ganancia: C$ 220.00"));
    }

    @Test
    void conservaPreciosHistoricosYExcluyeElStockVencidoIncluyendoLoQueVenceHoy() {
        DatosReporte datos = new ReporteGeneralDAO(fabrica).consultar(SolicitudReporte.todos());
        var base = seccion(datos, ApartadoReporte.PRODUCTOS).filas().stream()
                .filter(f -> f.get("nombre").equals("Base de prueba")).findFirst().orElseThrow();
        assertEquals("9", base.get("stock"));
        assertEquals("C$ 200.00", base.get("venta"));
        assertEquals("C$ 125.00", base.get("compra"));
        assertTrue(seccion(datos, ApartadoReporte.DETALLE_VENTAS).filas().stream()
                .filter(f -> f.get("producto").equals("Base de prueba"))
                .allMatch(f -> f.get("precio").equals("C$ 160.00") && f.get("costo").equals("C$ 100.00")));
        var resumen = seccion(datos, ApartadoReporte.RESUMEN).filas();
        assertTrue(resumen.stream().anyMatch(f -> f.get("indicador").equals("Unidades de stock vigente (actual)") && f.get("valor").equals("11")));
        assertTrue(resumen.stream().anyMatch(f -> f.get("indicador").equals("Productos bajo el mínimo (actual)") && f.get("valor").equals("1")));
        assertTrue(resumen.stream().anyMatch(f -> f.get("indicador").equals("Lotes vencidos con stock (actual)") && f.get("valor").equals("1")));
    }

    @Test
    void consultaUnicamenteElApartadoSeleccionadoYDevuelveSoloSusCamposElegidos() {
        CampoReporte nombre = ApartadoReporte.CLIENTES.campos().get(1);
        CampoReporte correo = ApartadoReporte.CLIENTES.campos().get(5);
        SolicitudReporte solicitud = new SolicitudReporte(null, null, null, null,
                Map.of(ApartadoReporte.CLIENTES, List.of(correo, nombre)));
        long consultasAntes = fabrica.getStatistics().getQueryExecutionCount();
        DatosReporte datos = new ReporteGeneralDAO(fabrica).consultar(solicitud);
        assertEquals(1, fabrica.getStatistics().getQueryExecutionCount() - consultasAntes);
        assertEquals(1, datos.secciones().size());
        assertEquals(List.of(correo, nombre), datos.secciones().getFirst().campos());
        assertTrue(datos.secciones().getFirst().filas().stream()
                .allMatch(f -> List.copyOf(f.keySet()).equals(List.of("correo", "nombres"))));
        assertFalse(datos.toString().contains("Piel sensible"));
        assertThrows(UnsupportedOperationException.class, () -> datos.secciones().getFirst().filas().getFirst().put("nombres", "Otro"));
    }

    @Test
    void admiteLimitesAbiertosYConservaApartadosSeleccionadosAunqueNoTenganResultados() {
        Map<ApartadoReporte, List<CampoReporte>> seleccion = new EnumMap<>(ApartadoReporte.class);
        seleccion.put(ApartadoReporte.CITAS, ApartadoReporte.CITAS.campos());
        ReporteGeneralDAO dao = new ReporteGeneralDAO(fabrica);
        assertEquals(4, dao.consultar(new SolicitudReporte(HOY, null, null, null, seleccion)).totalRegistros());
        assertEquals(4, dao.consultar(new SolicitudReporte(null, HOY, null, null, seleccion)).totalRegistros());
        assertEquals(1, dao.consultar(new SolicitudReporte(null, null, servicioGala, null, seleccion)).totalRegistros());
        seleccion.put(ApartadoReporte.VENTAS, ApartadoReporte.VENTAS.campos());
        seleccion.put(ApartadoReporte.CONSUMOS, ApartadoReporte.CONSUMOS.campos());
        DatosReporte vacio = dao.consultar(new SolicitudReporte(HOY.plusYears(1), null, null, null, seleccion));
        assertEquals(3, vacio.secciones().size());
        assertEquals(0, vacio.totalRegistros());
        assertTrue(vacio.secciones().stream().allMatch(s -> s.filas().isEmpty()));
    }

    private static SeccionReporte seccion(DatosReporte datos, ApartadoReporte apartado) {
        return datos.secciones().stream().filter(s -> s.apartado() == apartado).findFirst().orElseThrow();
    }
}
