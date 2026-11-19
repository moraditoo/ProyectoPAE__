package ni.edu.uam.proyectopae.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class ExportadorUtil {

    private ExportadorUtil() {
    }

    public static void exportarCsv(
            Path destino, List<String> encabezados, List<? extends List<?>> filas
    ) throws IOException {
        if (encabezados.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos una columna.");
        }

        Path archivo = destino.toAbsolutePath().normalize();
        Path temporal = Files.createTempFile(archivo.getParent(), "reporte-", ".tmp");

        try {
            try (BufferedWriter escritor = Files.newBufferedWriter(
                    temporal, StandardCharsets.UTF_8)) {
                escritor.write('\uFEFF');
                escribirFila(escritor, encabezados);

                for (List<?> fila : filas) {
                    if (fila.size() != encabezados.size()) {
                        throw new IllegalArgumentException(
                                "La fila tiene un número de columnas distinto al encabezado.");
                    }
                    escribirFila(escritor, fila);
                }
            }

            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException error) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException errorLimpieza) {
                error.addSuppressed(errorLimpieza);
            }
            throw error;
        }
    }

    private static void escribirFila(BufferedWriter escritor, List<?> valores)
            throws IOException {
        StringJoiner linea = new StringJoiner(",");

        for (Object valor : valores) {
            String texto = Objects.toString(valor, "");
            if (valor instanceof CharSequence && pareceFormula(texto)) {
                texto = "'" + texto;
            }
            linea.add("\"" + texto.replace("\"", "\"\"") + "\"");
        }

        escritor.write(linea.toString());
        escritor.write("\r\n");
    }

    private static boolean pareceFormula(String texto) {
        if (texto.startsWith("\t") || texto.startsWith("\r") || texto.startsWith("\n")) {
            return true;
        }

        String inicio = texto.stripLeading();
        return !inicio.isEmpty() && "=+-@＝＋－＠".indexOf(inicio.charAt(0)) >= 0;
    }
}