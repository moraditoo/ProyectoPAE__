package ni.edu.uam.proyectopae.util;

import java.util.Set;

public final class Graficos {

    private Graficos() {
    }

    public static String etiqueta(
            String texto,
            Set<String> usadas
    ) {
        String base = texto.length() > 18
                ? texto.substring(0, 17) + "."
                : texto;

        String etiqueta = base;
        int numero = 2;

        while (!usadas.add(etiqueta)) {
            etiqueta = base + " (" + numero++ + ")";
        }

        return etiqueta;
    }
}