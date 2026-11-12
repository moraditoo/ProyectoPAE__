package ni.edu.uam.proyectopae.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Aviso simple de "los datos cambiaron": cuando una ventana guarda en la base de datos,
 * las demás ventanas abiertas (incluido el Dashboard) recargan su información sin reiniciar el sistema.
 */
public final class EventosDatos {

    private static final List<Refrescable> suscriptores = new CopyOnWriteArrayList<>();

    private EventosDatos() {}

    public static void suscribir(Refrescable vista) {
        if (vista != null && !suscriptores.contains(vista)) {
            suscriptores.add(vista);
        }
    }

    public static void desuscribir(Refrescable vista) {
        suscriptores.remove(vista);
    }

    /** Notifica a todas las vistas suscritas excepto a la que originó el cambio (que ya se recargó). */
    public static void publicar(Refrescable origen) {
        for (Refrescable vista : suscriptores) {
            if (vista == origen) continue;
            try {
                vista.refrescar();
            } catch (Exception e) {
                System.err.println("No se pudo refrescar la vista " + vista.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
