package ni.edu.uam.proyectopae.dao;

/** Se lanza cuando una venta pide más unidades de las disponibles en lotes vigentes. */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String producto, int disponible, int solicitado) {
        super("No hay existencias suficientes de \"" + producto + "\".\n"
                + "Solicitado: " + solicitado + " unidad(es). Disponible en lotes vigentes: " + disponible + ".");
    }
}
