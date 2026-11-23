package ni.edu.uam.proyectopae.reportes;

/** Columna disponible en un apartado; el peso permite distribuir su ancho en el PDF. */
public record CampoReporte(String clave, String titulo, int peso) {
    public CampoReporte {
        if (clave == null || clave.isBlank() || titulo == null || titulo.isBlank() || peso <= 0) {
            throw new IllegalArgumentException("El campo debe tener clave, título y un peso positivo.");
        }
    }
}
