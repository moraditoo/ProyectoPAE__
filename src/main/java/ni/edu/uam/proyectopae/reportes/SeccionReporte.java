package ni.edu.uam.proyectopae.reportes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Datos preparados dentro de la sesión Hibernate y utilizables después de cerrarla. */
public record SeccionReporte(ApartadoReporte apartado, List<CampoReporte> campos,
                             List<Map<String, String>> filas, String resumen) {
    public SeccionReporte {
        Objects.requireNonNull(apartado, "El apartado es obligatorio.");
        campos = List.copyOf(campos);
        if (campos.isEmpty() || !apartado.campos().containsAll(campos)
                || campos.stream().distinct().count() != campos.size()) {
            throw new IllegalArgumentException("Los campos del apartado no son válidos.");
        }
        List<Map<String, String>> copia = new ArrayList<>();
        for (Map<String, String> fila : filas) {
            Map<String, String> elegidos = new LinkedHashMap<>();
            for (CampoReporte campo : campos) {
                elegidos.put(campo.clave(), Objects.requireNonNullElse(fila.get(campo.clave()), ""));
            }
            copia.add(Collections.unmodifiableMap(elegidos));
        }
        filas = List.copyOf(copia);
        resumen = Objects.requireNonNullElse(resumen, "Registros: " + filas.size());
    }
}
