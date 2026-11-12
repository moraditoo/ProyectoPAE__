package ni.edu.uam.proyectopae.util;

/** Controladores cuyas vistas deben recargar sus datos cuando otra ventana modifica la base de datos. */
public interface Refrescable {
    void refrescar();
}
