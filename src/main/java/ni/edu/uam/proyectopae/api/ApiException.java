package ni.edu.uam.proyectopae.api;

/**
 * Error al consumir la API. codigoHttp guarda el estado de la respuesta
 * (400, 404, 409, 500...) o 0 cuando no hubo respuesta (API apagada, sin red).
 */
public class ApiException extends Exception {

    private final int codigoHttp;

    public ApiException(int codigoHttp, String mensaje) {
        super(mensaje);
        this.codigoHttp = codigoHttp;
    }

    public ApiException(int codigoHttp, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoHttp = codigoHttp;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }

    public boolean isSinConexion() {
        return codigoHttp == 0;
    }
}