package ni.edu.uam.proyectopae.util;

import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

import java.util.Set;

/** Ayudas para los graficos de los dashboards. */
public final class Graficos {

    private Graficos() {}

    /**
     * Asigna un color a cada porcion del pastel y a su simbolo en la leyenda
     * (en el mismo orden en que se agregaron los datos).
     */
    public static void colorearPastel(PieChart chart, String... colores) {
        for (int i = 0; i < chart.getData().size(); i++) {
            Node n = chart.getData().get(i).getNode();
            if (n != null) n.setStyle("-fx-pie-color: " + colores[i % colores.length] + ";");
        }
        int i = 0;
        for (Node item : chart.lookupAll(".chart-legend-item")) {
            if (item instanceof Label l && l.getGraphic() != null) {
                l.getGraphic().setStyle("-fx-background-color: " + colores[i % colores.length] + ";");
            }
            i++;
        }
    }

    /** Etiqueta corta y sin repetir para el eje de categorias de un grafico de barras. */
    public static String etiqueta(String texto, Set<String> usadas) {
        String base = texto.length() > 18 ? texto.substring(0, 17) + "…" : texto;
        String e = base;
        int n = 2;
        while (!usadas.add(e)) {
            e = base + " (" + n++ + ")";
        }
        return e;
    }
}
