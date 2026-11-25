package ni.edu.uam.proyectopae.api;

/**
 * Resultado de una solicitud exitosa: los datos ya convertidos desde JSON
 * y la información HTTP que se muestra en la interfaz (método, URL y estado).
 */
public record RespuestaApi<T>(T datos, String metodo, String url, int estado) {

    /** Ejemplo: "GET http://localhost:8080/api/productos?buscar=labial → 200". */
    public String resumen() {
        return metodo + " " + url + " → " + estado;
    }
}