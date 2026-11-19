package ni.edu.uam.proyectopae.util;

/**
 * Implementado por los controladores de cada ventana. VentanaManager lo invoca
 * cuando la ventana se vuelve a abrir desde el menu o cuando otra ventana
 * modifica datos (por ejemplo, se registra un producto y la ventana de Ventas
 * debe ver el nuevo producto sin reiniciar).
 */
public interface Refrescable {
    void refrescar();
}
