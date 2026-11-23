package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperPrint;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.Servicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteJasperServiceTest {
    @TempDir Path temporal;
    private final ReporteJasperService jasper = new ReporteJasperService();

    @Test
    void generaPdfConAcentosFiltrosYResumen() throws Exception {
        Cita cita = cita(15, "María", "COMPLETADA");
        JasperPrint reporte = jasper.generar(List.of(cita), Arrays.asList(ColumnaCita.values()),
                cita.getFechaHora().toLocalDate(), cita.getFechaHora().toLocalDate(), "Maquillaje");
        String texto = texto(reporte);
        assertTrue(texto.contains("María Pérez"));
        assertTrue(texto.contains("Completadas: 1"));
        assertTrue(texto.contains("Desde: 23/11/2026"));
        assertTrue(texto.contains("23/11/2026 02:30 PM"));
        Path pdf = temporal.resolve("citas.pdf");
        jasper.exportar(reporte, pdf, ReporteJasperService.Formato.PDF);
        assertEquals("%PDF-", new String(Files.readAllBytes(pdf), 0, 5, StandardCharsets.US_ASCII));
        assertTrue(Files.size(pdf) > 1000);
    }

    @Test
    void exportaSoloColumnasSeleccionadasEnOrdenSinRepetirCabeceras() throws Exception {
        List<Cita> citas = new ArrayList<>();
        for (int i = 1; i <= 50; i++) citas.add(cita(i, "José, \"Pepe\"", "PROGRAMADA"));
        JasperPrint reporte = jasper.generar(citas, List.of(ColumnaCita.CLIENTE, ColumnaCita.ID), null, null, null);
        assertTrue(reporte.getPages().size() > 1);
        assertFalse(texto(reporte).contains("Fecha y hora"));
        assertFalse(texto(reporte).contains("23/11/2026 02:30 PM"));
        Path csv = temporal.resolve("citas.csv");
        jasper.exportar(reporte, csv, ReporteJasperService.Formato.CSV);
        String contenido = Files.readString(csv);
        assertTrue(contenido.startsWith("\uFEFF\"Cliente\",\"N.º de cita\""), contenido);
        List<String> lineas = contenido.substring(1).lines().toList();
        assertEquals(51, lineas.size(), contenido);
        assertEquals("\"José, \"\"Pepe\"\" Pérez\",\"1\"", lineas.get(1));
        assertEquals("\"José, \"\"Pepe\"\" Pérez\",\"50\"", lineas.get(50));
        assertFalse(contenido.contains("Página"));
        assertFalse(contenido.contains("Total:"));
    }

    @Test
    void neutralizaFormulasCsv() throws Exception {
        JasperPrint reporte = jasper.generar(List.of(cita(1, "=1+1", "PROGRAMADA")),
                List.of(ColumnaCita.CLIENTE), null, null, null);
        Path csv = temporal.resolve("formula.csv");
        jasper.exportar(reporte, csv, ReporteJasperService.Formato.CSV);
        assertTrue(Files.readString(csv).contains("'=1+1 Pérez"), Files.readString(csv));
    }

    @Test
    void generaReporteVacioYRechazaConfiguracionInvalida() throws Exception {
        JasperPrint vacio = jasper.generar(List.of(), List.of(ColumnaCita.ID), null, null, null);
        assertTrue(texto(vacio).contains("No se encontraron citas"));
        assertTrue(texto(vacio).contains("Total de citas: 0"));
        assertThrows(IllegalArgumentException.class,
                () -> jasper.generar(List.of(), List.of(), null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> jasper.generar(List.of(), List.of(ColumnaCita.ID, ColumnaCita.ID), null, null, null));
    }

    @Test
    void losTextosLargosCrecenSinInvadirLaSiguienteFila() throws Exception {
        Cita larga = cita(1, "María ".repeat(25), "PROGRAMADA");
        JasperPrint reporte = jasper.generar(List.of(larga, cita(2, "Ana", "CANCELADA")),
                Arrays.asList(ColumnaCita.values()), null, null, null);
        var elementos = reporte.getPages().getFirst().getElements();
        JRPrintText clienteLargo = elementos.stream().filter(JRPrintText.class::isInstance)
                .map(JRPrintText.class::cast).filter(t -> "detalle_cliente".equals(t.getKey())).findFirst().orElseThrow();
        JRPrintText clienteSiguiente = elementos.stream().filter(JRPrintText.class::isInstance)
                .map(JRPrintText.class::cast).filter(t -> "Ana Pérez".equals(t.getFullText())).findFirst().orElseThrow();
        assertTrue(clienteLargo.getHeight() > 32);
        assertTrue(clienteSiguiente.getY() >= clienteLargo.getY() + clienteLargo.getHeight());
    }

    static Cita cita(int id, String nombre, String estado) {
        Cliente cliente = new Cliente();
        cliente.setNombres(nombre);
        cliente.setApellidos("Pérez");
        Servicio servicio = new Servicio();
        servicio.setNombreServicio("Maquillaje");
        Cita cita = new Cita(cliente, servicio, LocalDateTime.of(2026, 11, 23, 14, 30), estado);
        cita.setIdCita(id);
        return cita;
    }

    private static String texto(JasperPrint reporte) {
        return reporte.getPages().stream().flatMap(pagina -> pagina.getElements().stream())
                .filter(JRPrintText.class::isInstance).map(JRPrintText.class::cast)
                .map(JRPrintText::getFullText).reduce("", (a, b) -> a + "\n" + b);
    }
}
