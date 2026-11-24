package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.List;
import java.util.Objects;

/** Versiones PDF y CSV generadas a partir de la misma consulta y selección de campos. */
public record DocumentoReporte(DatosReporte datos, List<JasperPrint> apartadosPdf, JasperPrint csv) {
    public DocumentoReporte {
        Objects.requireNonNull(datos, "Los datos son obligatorios.");
        apartadosPdf = List.copyOf(apartadosPdf);
        Objects.requireNonNull(csv, "El reporte CSV es obligatorio.");
        if (apartadosPdf.isEmpty() || apartadosPdf.size() != datos.secciones().size()) {
            throw new IllegalArgumentException("Debe generarse un reporte PDF por apartado seleccionado.");
        }
    }

    public int paginasPdf() {
        return apartadosPdf.stream().mapToInt(apartado -> apartado.getPages().size()).sum();
    }
}
