package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReporteGeneralJasperServiceTest {
    @TempDir Path temporal;
    private final ReporteGeneralJasperService jasper = new ReporteGeneralJasperService();

    @Test
    void generaUnSoloPdfConLosDoceApartados() throws Exception {
        DatosReporte datos = ejemploCompleto();
        DocumentoReporte documento = jasper.generar(datos);
        assertEquals(12, documento.apartadosPdf().size());
        assertEquals(List.of(ApartadoReporte.values()).stream().map(ApartadoReporte::titulo).toList(),
                documento.apartadosPdf().stream().map(JasperPrint::getName).toList());
        assertTrue(documento.apartadosPdf().stream().allMatch(p -> !p.getPages().isEmpty()));
        assertEquals(1191, documento.apartadosPdf().get(1).getPageWidth());
        assertEquals(842, documento.apartadosPdf().get(3).getPageWidth());
        assertTrue(texto(documento.apartadosPdf().get(3)).contains("Camila"));
        assertTrue(texto(documento.apartadosPdf().get(3)).contains("Lucía"));
        assertTrue(texto(documento.apartadosPdf().get(6)).contains("C$ 2,030.00"));

        // Ejemplos reproducibles para revisar visualmente; no se conecta a ninguna base de datos.
        Path carpeta = Path.of("target", "pruebas-reportes");
        Files.createDirectories(carpeta);
        Path pdf = carpeta.resolve("reporte-general-ejemplo.pdf");
        jasper.exportarPdf(documento, pdf);
        assertEquals("%PDF-", new String(Files.readAllBytes(pdf), 0, 5, StandardCharsets.US_ASCII));
    }

    @Test
    void respetaLosApartadosElegidosYElOrdenDeCamposSinIncluirDatosOcultos() throws Exception {
        var nombre = campo(ApartadoReporte.CLIENTES, "nombres");
        var correo = campo(ApartadoReporte.CLIENTES, "correo");
        var seleccion = Map.of(ApartadoReporte.CLIENTES, List.of(correo, nombre));
        SolicitudReporte solicitud = new SolicitudReporte(null, null, null, null, seleccion);
        SeccionReporte clientes = new SeccionReporte(ApartadoReporte.CLIENTES, List.of(correo, nombre),
                List.of(fila("nombres", "María", "correo", "maria@example.com", "alergias", "DATO_NO_SELECCIONADO")), null);
        DocumentoReporte documento = jasper.generar(new DatosReporte(solicitud, LocalDateTime.now(), List.of(clientes)));
        assertEquals(1, documento.apartadosPdf().size());
        JasperPrint pdf = documento.apartadosPdf().getFirst();
        List<JRPrintText> cabeceras = textos(pdf).stream().filter(t -> t.getKey() != null && t.getKey().startsWith("cabecera_")).toList();
        assertEquals(List.of("Correo electrónico", "Nombres"), cabeceras.stream().map(JRPrintText::getFullText).toList());
        assertTrue(cabeceras.get(0).getX() < cabeceras.get(1).getX());
        assertFalse(texto(pdf).contains("DATO_NO_SELECCIONADO"));
    }

    @Test
    void conservaLosApartadosVaciosYExplicaLaAusenciaDeRegistros() throws Exception {
        var seleccion = new EnumMap<ApartadoReporte, List<CampoReporte>>(ApartadoReporte.class);
        seleccion.put(ApartadoReporte.CITAS, ApartadoReporte.CITAS.campos());
        seleccion.put(ApartadoReporte.VENTAS, ApartadoReporte.VENTAS.campos());
        var solicitud = new SolicitudReporte(null, null, null, null, seleccion);
        var secciones = seleccion.entrySet().stream()
                .map(e -> new SeccionReporte(e.getKey(), e.getValue(), List.of(), null)).toList();
        DocumentoReporte documento = jasper.generar(new DatosReporte(solicitud, LocalDateTime.now(), secciones));
        assertEquals(2, documento.paginasPdf());
        for (JasperPrint apartado : documento.apartadosPdf()) {
            assertTrue(texto(apartado).contains("Sin registros con los filtros seleccionados."));
            assertTrue(texto(apartado).contains("Registros: 0"));
        }
    }

    @Test
    void paginaTextosLargosConCabecerasRepetidasYSinSuperponerFilas() throws Exception {
        var nombres = campo(ApartadoReporte.CLIENTES, "nombres");
        var alergias = campo(ApartadoReporte.CLIENTES, "alergias");
        var campos = List.of(nombres, alergias);
        var solicitud = new SolicitudReporte(null, null, null, null, Map.of(ApartadoReporte.CLIENTES, campos));
        List<Map<String, String>> filas = new ArrayList<>();
        for (int i = 1; i <= 25; i++) filas.add(fila("nombres", "Cliente " + i,
                "alergias", "Texto ficticio de sensibilidad dérmica para comprobar el ajuste de las celdas. ".repeat(6)));
        var documento = jasper.generar(new DatosReporte(solicitud, LocalDateTime.now(),
                List.of(new SeccionReporte(ApartadoReporte.CLIENTES, campos, filas, null))));
        JasperPrint reporte = documento.apartadosPdf().getFirst();
        assertTrue(reporte.getPages().size() > 1);
        assertTrue(texto(reporte).contains("Cliente 25"));
        for (var pagina : reporte.getPages()) {
            List<JRPrintText> celdas = pagina.getElements().stream().filter(JRPrintText.class::isInstance)
                    .map(JRPrintText.class::cast).filter(t -> "detalle_alergias".equals(t.getKey())).toList();
            if (!celdas.isEmpty()) {
                assertTrue(pagina.getElements().stream().anyMatch(e -> "cabecera_alergias".equals(e.getKey())));
                for (int i = 1; i < celdas.size(); i++) {
                    assertTrue(celdas.get(i).getY() >= celdas.get(i - 1).getY() + celdas.get(i - 1).getHeight());
                }
            }
        }
        Path carpeta = Path.of("target", "pruebas-reportes");
        Files.createDirectories(carpeta);
        jasper.exportarPdf(documento, carpeta.resolve("reporte-textos-largos.pdf"));
    }

    @Test
    void unErrorAlGuardarConservaElDestinoAnteriorYLimpiaElTemporal() throws Exception {
        Path destino = temporal.resolve("ocupado.pdf");
        Files.createDirectory(destino);
        Path anterior = destino.resolve("original.txt");
        Files.writeString(anterior, "ARCHIVO ANTERIOR");
        // La exportación se genera, pero el destino es un directorio ocupado y no se puede reemplazar.
        var campo = campo(ApartadoReporte.MARCAS, "nombre");
        var solicitud = new SolicitudReporte(null, null, null, null, Map.of(ApartadoReporte.MARCAS, List.of(campo)));
        var seccion = new SeccionReporte(ApartadoReporte.MARCAS, List.of(campo), List.of(Map.of("nombre", "Marca de prueba")), null);
        var documento = jasper.generar(new DatosReporte(solicitud, LocalDateTime.now(), List.of(seccion)));
        assertThrows(IOException.class, () -> jasper.exportarPdf(documento, destino));
        assertEquals("ARCHIVO ANTERIOR", Files.readString(anterior));
        try (var archivos = Files.list(temporal)) {
            assertEquals(List.of(destino), archivos.toList());
        }
    }

    @Test
    void rechazaDocumentosPdfSinApartadosOSinPaginas() {
        assertThrows(IllegalArgumentException.class, () -> ExportadorJasper.pdf(List.of(), temporal.resolve("sin-apartados.pdf")));
        assertThrows(IllegalArgumentException.class, () -> ExportadorJasper.pdf(List.of(new JasperPrint()), temporal.resolve("sin-paginas.pdf")));
    }

    private static CampoReporte campo(ApartadoReporte apartado, String clave) {
        return apartado.campos().stream().filter(c -> c.clave().equals(clave)).findFirst().orElseThrow();
    }

    private static List<JRPrintText> textos(JasperPrint reporte) {
        return reporte.getPages().stream().flatMap(p -> p.getElements().stream())
                .filter(JRPrintText.class::isInstance).map(JRPrintText.class::cast).toList();
    }

    private static String texto(JasperPrint reporte) {
        return String.join("\n", textos(reporte).stream().map(JRPrintText::getFullText).toList());
    }

    private static Map<String, String> fila(String... pares) {
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) fila.put(pares[i], pares[i + 1]);
        return fila;
    }

    private static DatosReporte ejemploCompleto() {
        Map<ApartadoReporte, List<Map<String, String>>> registros = new EnumMap<>(ApartadoReporte.class);
        registros.put(ApartadoReporte.RESUMEN, List.of(fila("indicador", "Productos en catálogo", "valor", "2"),
                fila("indicador", "Total vendido", "valor", "C$ 2,030.00")));
        registros.put(ApartadoReporte.PRODUCTOS, List.of(
                fila("id", "1", "nombre", "Base Fit Me tono 128", "idCategoria", "1", "categoria", "Rostro", "idMarca", "1", "marca", "Maybelline",
                        "compra", "C$ 450.00", "venta", "C$ 690.00", "ganancia", "C$ 240.00", "margen", "34.78 %", "stock", "18", "minimo", "5", "estado", "SUFICIENTE"),
                fila("id", "2", "nombre", "Máscara Sky High negra", "idCategoria", "2", "categoria", "Ojos", "idMarca", "1", "marca", "Maybelline",
                        "compra", "C$ 430.00", "venta", "C$ 650.00", "ganancia", "C$ 220.00", "margen", "33.85 %", "stock", "19", "minimo", "5", "estado", "SUFICIENTE")));
        registros.put(ApartadoReporte.LOTES, List.of(
                fila("id", "1", "idProducto", "1", "producto", "Base Fit Me tono 128", "codigo", "FM-2611-A", "ingreso", "23/11/2026", "caducidad", "15/12/2026", "cantidad", "18", "dias", "22", "estado", "POR VENCER"),
                fila("id", "2", "idProducto", "2", "producto", "Máscara Sky High negra", "codigo", "MAS-2611-A", "ingreso", "23/11/2026", "caducidad", "31/05/2027", "cantidad", "19", "dias", "189", "estado", "VIGENTE")));
        registros.put(ApartadoReporte.CLIENTES, List.of(
                fila("id", "1", "nombres", "Camila", "apellidos", "Torres", "sexo", "Femenino", "telefono", "88881001", "correo", "camila.torres@example.com", "alergias", "Sin alergias declaradas"),
                fila("id", "2", "nombres", "Lucía", "apellidos", "Rivas", "sexo", "Femenino", "telefono", "88881003", "correo", "lucia.rivas@example.com", "alergias", "Registro ficticio de piel sensible")));
        registros.put(ApartadoReporte.SERVICIOS, List.of(fila("id", "1", "nombre", "Maquillaje Social de Día", "duracion", "45", "tarifa", "C$ 900.00",
                "productos", "1 - Base Fit Me tono 128; 2 - Máscara Sky High negra")));
        registros.put(ApartadoReporte.CITAS, List.of(fila("id", "1", "idCliente", "1", "cliente", "Camila Torres", "idServicio", "1", "servicio", "Maquillaje Social de Día",
                "fecha", "24/11/2026 09:00", "estado", "PROGRAMADA", "tarifa", "C$ 900.00")));
        registros.put(ApartadoReporte.VENTAS, List.of(fila("id", "1", "idCliente", "1", "cliente", "Camila Torres", "fecha", "23/11/2026 17:00",
                "total", "C$ 2,030.00", "costo", "C$ 1,330.00", "ganancia", "C$ 700.00")));
        registros.put(ApartadoReporte.DETALLE_VENTAS, List.of(
                fila("id", "1", "idVenta", "1", "fecha", "23/11/2026 17:00", "cliente", "Camila Torres", "idProducto", "1", "producto", "Base Fit Me tono 128",
                        "cantidad", "2", "precio", "C$ 690.00", "costo", "C$ 450.00", "subtotal", "C$ 1,380.00", "ganancia", "C$ 480.00"),
                fila("id", "2", "idVenta", "1", "fecha", "23/11/2026 17:00", "cliente", "Camila Torres", "idProducto", "2", "producto", "Máscara Sky High negra",
                        "cantidad", "1", "precio", "C$ 650.00", "costo", "C$ 430.00", "subtotal", "C$ 650.00", "ganancia", "C$ 220.00")));
        registros.put(ApartadoReporte.CONSUMOS, List.of());
        registros.put(ApartadoReporte.CATEGORIAS, List.of(fila("id", "1", "nombre", "Rostro", "descripcion", "Bases, correctores y polvos"),
                fila("id", "2", "nombre", "Ojos", "descripcion", "Máscaras, sombras y delineadores")));
        registros.put(ApartadoReporte.MARCAS, List.of(fila("id", "1", "nombre", "Maybelline")));
        registros.put(ApartadoReporte.USUARIOS, List.of(fila("id", "1", "usuario", "demo.admin", "rol", "ADMINISTRADOR")));
        List<SeccionReporte> secciones = new ArrayList<>();
        for (ApartadoReporte apartado : ApartadoReporte.values()) {
            List<Map<String, String>> filas = registros.get(apartado);
            secciones.add(new SeccionReporte(apartado, apartado.campos(), filas,
                    apartado == ApartadoReporte.VENTAS ? "Ventas: 1 | Total: C$ 2,030.00 | Ganancia: C$ 700.00" : null));
        }
        return new DatosReporte(SolicitudReporte.todos(), LocalDateTime.of(2026, 11, 23, 17, 30), secciones);
    }

}
