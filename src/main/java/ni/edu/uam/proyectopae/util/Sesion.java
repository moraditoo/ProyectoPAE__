package ni.edu.uam.proyectopae.util;

import ni.edu.uam.proyectopae.models.Usuario;

/** Guarda el usuario autenticado mientras la aplicacion esta abierta. */
public final class Sesion {

    private static Usuario usuarioActual;

    private Sesion() {}

    public static void iniciar(Usuario usuario) { usuarioActual = usuario; }

    public static void cerrar() { usuarioActual = null; }

    public static Usuario getUsuario() { return usuarioActual; }

    public static String descripcion() {
        return usuarioActual == null ? "Sin sesión" : usuarioActual.getUsername() + " (" + usuarioActual.getRol() + ")";
    }
}
