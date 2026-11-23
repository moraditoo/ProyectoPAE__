package ni.edu.uam.proyectopae.reportes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Selección inmutable: evita que cambios posteriores de la pantalla alteren una consulta en curso. */
public record SolicitudReporte(LocalDate desde, LocalDate hasta, Integer idServicio,
                               String servicio, Map<ApartadoReporte, List<CampoReporte>> seleccion) {
    public SolicitudReporte {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la final.");
        }
        if (idServicio != null && idServicio <= 0) {
            throw new IllegalArgumentException("El servicio seleccionado no es válido.");
        }
        if (seleccion == null || seleccion.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un apartado para el reporte.");
        }
        Map<ApartadoReporte, List<CampoReporte>> copia = new EnumMap<>(ApartadoReporte.class);
        seleccion.forEach((apartado, campos) -> {
            if (apartado == null || campos == null || campos.isEmpty()) {
                throw new IllegalArgumentException("Selecciona al menos un campo por apartado.");
            }
            if (new HashSet<>(campos).size() != campos.size() || !apartado.campos().containsAll(campos)) {
                throw new IllegalArgumentException("Hay campos inválidos o repetidos en " + apartado.titulo() + ".");
            }
            copia.put(apartado, List.copyOf(campos));
        });
        seleccion = Collections.unmodifiableMap(copia);
        servicio = idServicio == null ? "Todos los servicios" :
                (servicio == null || servicio.isBlank() ? "Servicio #" + idServicio : servicio);
    }

    public static SolicitudReporte todos() {
        Map<ApartadoReporte, List<CampoReporte>> seleccion = new EnumMap<>(ApartadoReporte.class);
        for (ApartadoReporte apartado : ApartadoReporte.values()) seleccion.put(apartado, apartado.campos());
        return new SolicitudReporte(null, null, null, null, seleccion);
    }

    public String descripcionFiltros() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "Desde: " + (desde == null ? "Sin límite" : formato.format(desde))
                + " | Hasta: " + (hasta == null ? "Sin límite" : formato.format(hasta))
                + " | Servicio: " + servicio;
    }
}
