package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Exportadores comunes para reportes de citas y reportes con varios apartados. */
final class ExportadorJasper {
    private ExportadorJasper() { }

    static void pdf(List<JasperPrint> apartados, Path destino) throws IOException, JRException {
        List<JasperPrint> copia = List.copyOf(apartados);
        if (copia.isEmpty()) throw new IllegalArgumentException("Selecciona al menos un apartado.");
        if (copia.stream().anyMatch(apartado -> apartado.getPages().isEmpty())) {
            throw new IllegalArgumentException("Todos los apartados deben tener al menos una página.");
        }
        escribir(destino, salida -> {
            JRPdfExporter exportador = new JRPdfExporter();
            exportador.setExporterInput(SimpleExporterInput.getInstance(copia));
            exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(salida));
            SimplePdfExporterConfiguration configuracion = new SimplePdfExporterConfiguration();
            configuracion.setCreatingBatchModeBookmarks(true);
            configuracion.setMetadataTitle(copia.size() == 1 ? copia.getFirst().getName() : "Reporte general - Proyecto PAE");
            exportador.setConfiguration(configuracion);
            exportador.exportReport();
        });
    }

    static void csv(JasperPrint reporte, Path destino) throws IOException, JRException {
        escribir(destino, salida -> {
            JRCsvExporter exportador = new CsvSeguro();
            exportador.setExporterInput(new SimpleExporterInput(reporte));
            exportador.setExporterOutput(new SimpleWriterExporterOutput(salida, "UTF-8"));
            SimpleCsvExporterConfiguration configuracion = new SimpleCsvExporterConfiguration();
            configuracion.setWriteBOM(true);
            configuracion.setForceFieldEnclosure(true);
            configuracion.setRecordDelimiter("\r\n");
            exportador.setConfiguration(configuracion);
            exportador.exportReport();
        });
    }

    /** El archivo anterior se conserva si falla la generación del temporal. */
    private static void escribir(Path destino, Escritura exportacion) throws IOException, JRException {
        Path archivo = destino.toAbsolutePath().normalize();
        Path temporal = Files.createTempFile(archivo.getParent(), "reporte-", ".tmp");
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                exportacion.escribir(salida);
            }
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | JRException | RuntimeException error) {
            try { Files.deleteIfExists(temporal); }
            catch (IOException limpieza) { error.addSuppressed(limpieza); }
            throw error;
        }
    }

    @FunctionalInterface
    private interface Escritura {
        void escribir(OutputStream salida) throws IOException, JRException;
    }

    /** Evita fórmulas al abrir el CSV, también tras espacios o con símbolos de ancho completo. */
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
