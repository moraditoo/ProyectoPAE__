package ni.edu.uam.proyectopae.reportes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Resultado completo de una consulta, con la selección y el momento de su generación. */
public record DatosReporte(SolicitudReporte solicitud, LocalDateTime generado, List<SeccionReporte> secciones) {
    public DatosReporte {
        Objects.requireNonNull(solicitud, "La solicitud es obligatoria.");
        Objects.requireNonNull(generado, "La fecha de generación es obligatoria.");
        secciones = List.copyOf(secciones);
        if (secciones.size() != solicitud.seleccion().size()
                || secciones.stream().map(SeccionReporte::apartado).distinct().count() != secciones.size()
                || secciones.stream().anyMatch(s -> !s.campos().equals(solicitud.seleccion().get(s.apartado())))) {
            throw new IllegalArgumentException("Los apartados y campos deben coincidir con la solicitud.");
        }
    }

    public int totalRegistros() {
        return secciones.stream().mapToInt(seccion -> seccion.filas().size()).sum();
    }
}
