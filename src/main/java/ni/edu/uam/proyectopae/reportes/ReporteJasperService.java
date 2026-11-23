package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import ni.edu.uam.proyectopae.models.Cita;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Genera y exporta el mismo reporte con JasperReports, sin conexiones SQL en el JRXML. */
public final class ReporteJasperService {
    private static final String PLANTILLA = "/reportes/citas.jrxml";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public enum Formato { PDF, CSV }

    public JasperPrint generar(List<Cita> citas, List<ColumnaCita> columnas,
                               LocalDate desde, LocalDate hasta, String servicio)
            throws JRException, IOException {
        if (columnas.isEmpty() || new HashSet<>(columnas).size() != columnas.size()) {
            throw new IllegalArgumentException("Selecciona al menos una columna, sin duplicados.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la final.");
        }

        JasperDesign diseno;
        try (InputStream entrada = ReporteJasperService.class.getResourceAsStream(PLANTILLA)) {
            if (entrada == null) {
                throw new IOException("No se encontró la plantilla " + PLANTILLA + ". Recompila el proyecto.");
            }
            diseno = JRXmlLoader.load(entrada);
        }
        ajustarColumnas((JRDesignBand) diseno.getColumnHeader(), columnas, "cabecera_", diseno.getColumnWidth());
        ajustarColumnas((JRDesignBand) diseno.getDetailSection().getBands()[0], columnas, "detalle_", diseno.getColumnWidth());

        List<Map<String, ?>> filas = new ArrayList<>(citas.size());
        for (Cita cita : citas) {
            Map<String, Object> fila = new HashMap<>();
            for (ColumnaCita columna : columnas) {
                fila.put(columna.campo(), columna.valor(cita));
            }
            filas.add(fila);
        }

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("FILTROS", "Desde: " + (desde == null ? "Sin límite" : FECHA.format(desde))
                + "   |   Hasta: " + (hasta == null ? "Sin límite" : FECHA.format(hasta))
                + "   |   Servicio: " + (servicio == null ? "Todos los servicios" : servicio));
        parametros.put("GENERADO", "Generado el " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("RESUMEN", "Total de citas: " + citas.size() + "    |    Programadas: " + contar(citas, "PROGRAMADA")
                + "    |    Completadas: " + contar(citas, "COMPLETADA") + "    |    Canceladas: " + contar(citas, "CANCELADA"));
        parametros.put("REPORT_LOCALE", Locale.forLanguageTag("es-NI"));

        return JasperFillManager.fillReport(JasperCompileManager.compileReport(diseno), parametros,
                new JRMapCollectionDataSource(filas));
    }

    private static long contar(List<Cita> citas, String estado) {
        return citas.stream().filter(cita -> estado.equalsIgnoreCase(cita.getEstado())).count();
    }

    private static void ajustarColumnas(JRDesignBand banda, List<ColumnaCita> columnas,
                                        String prefijo, int ancho) {
        Map<String, JRDesignElement> elementos = new HashMap<>();
        for (var elemento : banda.getElements()) {
            if (elemento.getKey() != null && elemento.getKey().startsWith(prefijo)) {
                elementos.put(elemento.getKey(), (JRDesignElement) elemento);
                banda.removeElement((JRDesignElement) elemento);
            }
        }
        int pesoTotal = columnas.stream().mapToInt(ColumnaCita::peso).sum();
        int pesoAcumulado = 0;
        int x = 0;
        for (ColumnaCita columna : columnas) {
            JRDesignElement elemento = elementos.get(prefijo + columna.campo());
            if (elemento == null) {
                throw new IllegalStateException("Falta la columna " + columna.campo() + " en la plantilla.");
            }
            pesoAcumulado += columna.peso();
            int fin = ancho * pesoAcumulado / pesoTotal;
            elemento.setX(x);
            elemento.setWidth(fin - x);
            banda.addElement(elemento);
            x = fin;
        }
    }

    /** Escribe primero un temporal para conservar el archivo anterior si la exportación falla. */
    public void exportar(JasperPrint reporte, Path destino, Formato formato) throws IOException, JRException {
        Path archivo = destino.toAbsolutePath().normalize();
        Path temporal = Files.createTempFile(archivo.getParent(), "reporte-", ".tmp");
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                if (formato == Formato.PDF) {
                    JasperExportManager.exportReportToPdfStream(reporte, salida);
                } else {
                    JRCsvExporter exportador = new CsvSeguro();
                    exportador.setExporterInput(new SimpleExporterInput(reporte));
                    exportador.setExporterOutput(new SimpleWriterExporterOutput(salida, "UTF-8"));
                    SimpleCsvExporterConfiguration configuracion = new SimpleCsvExporterConfiguration();
                    configuracion.setWriteBOM(true);
                    configuracion.setForceFieldEnclosure(true);
                    configuracion.setRecordDelimiter("\r\n");
                    exportador.setConfiguration(configuracion);
                    exportador.exportReport();
                }
            }
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | JRException | RuntimeException error) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException limpieza) {
                error.addSuppressed(limpieza);
            }
            throw error;
        }
    }

    /** Conserva la protección de fórmulas, incluso tras espacios o caracteres de ancho completo. */
    private static final class CsvSeguro extends JRCsvExporter {
        @Override
        protected String prepareText(String texto) {
            if (texto != null) {
                String inicio = texto.stripLeading();
                if (texto.startsWith("\t") || texto.startsWith("\r") || texto.startsWith("\n")
                        || (!inicio.isEmpty() && "=+-@＝＋－＠".indexOf(inicio.charAt(0)) >= 0)) {
                    texto = "'" + texto;
                }
            }
            return super.prepareText(texto);
        }
    }
}
