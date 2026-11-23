package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Paleta de los graficos, sus ejes y las leyendas, sin hojas de estilo. */
final class TemaGraficos {

    private static final String OBSERVADO = "pae-tema-grafico";
    private static final String FUENTE = Font.getFamilies().contains("DM Sans") ? "DM Sans" : "Segoe UI";

    private TemaGraficos() {}

    static void aplicar(Chart grafico) {
        observar(grafico, grafico);
    }

    private static void observar(Node nodo, Chart grafico) {
        if (nodo.getProperties().putIfAbsent(OBSERVADO, true) == null) {
            InvalidationListener cambio = observable -> pintar(nodo, grafico);
            nodo.getStyleClass().addListener(cambio);
            nodo.hoverProperty().addListener(cambio);
            if (nodo instanceof Axis<?> eje) eje.sideProperty().addListener(cambio);
            if (nodo instanceof Parent padre) {
                padre.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) cambioHijos -> {
                    while (cambioHijos.next()) for (Node hijo : cambioHijos.getAddedSubList()) observar(hijo, grafico);
                });
            }
        }
        pintar(nodo, grafico);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) observar(hijo, grafico);
        }
    }

    private static void pintar(Node nodo, Chart grafico) {
        if (nodo instanceof Axis<?> eje) {
            eje.setTickLabelFill(TemaPAE.secundario());
            eje.setTickLabelFont(Font.font(FUENTE, 12));
            Side lado = eje.getSide();
            BorderWidths anchos = lado == Side.LEFT ? new BorderWidths(0, 1, 0, 0)
                    : lado == Side.RIGHT ? new BorderWidths(0, 0, 0, 1)
                    : lado == Side.TOP ? new BorderWidths(0, 0, 1, 0) : new BorderWidths(1, 0, 0, 0);
            eje.setBorder(new Border(new BorderStroke(TemaPAE.borde(), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, anchos)));
        } else if (nodo instanceof Label etiqueta) {
            boolean titulo = tiene(etiqueta, "chart-title");
            etiqueta.setFont(Font.font(FUENTE, titulo ? FontWeight.BOLD : FontWeight.NORMAL, titulo ? 17 : 12));
            etiqueta.setTextFill(titulo ? TemaPAE.texto() : TemaPAE.secundario());
        } else if (nodo instanceof Text texto) {
            if (!texto.fillProperty().isBound()) texto.setFill(TemaPAE.secundario());
        } else if (nodo instanceof Shape figura) {
            if (tiene(figura, "chart-horizontal-grid-lines", "chart-vertical-grid-lines", "axis-tick-mark", "axis-minor-tick-mark")) {
                figura.setStroke(TemaPAE.borde());
            } else if (tiene(figura, "chart-horizontal-zero-line", "chart-vertical-zero-line", "chart-pie-label-line")) {
                figura.setStroke(TemaPAE.secundario());
                figura.setFill(TemaPAE.secundario());
            } else if (tiene(figura, "chart-alternative-row-fill", "chart-alternative-column-fill")) {
                figura.setFill(TemaPAE.filaAlterna());
                figura.setStroke(Color.TRANSPARENT);
            }
        } else if (nodo instanceof Region region) {
            if (tiene(region, "chart-bar", "chart-pie")) {
                Color color = grafico instanceof PieChart circular ? colorLote(region, circular) : colorSerie(indice(region, "series"));
                fondo(region, region.isHover() ? color.brighter() : color, 0);
                region.setBorder(Border.EMPTY);
            } else if (tiene(region, "chart-plot-background")) {
                fondo(region, TemaPAE.blanco(), 0);
            } else if (nodo instanceof Chart || tiene(region, "chart-content", "chart-legend")) {
                fondo(region, Color.TRANSPARENT, 0);
                region.setBorder(Border.EMPTY);
            }
        }
    }

    private static Color colorLote(Node nodo, PieChart grafico) {
        int posicion = indice(nodo, "data");
        if (posicion >= 0 && posicion < grafico.getData().size() && grafico.getStyleClass().contains("pie-lotes")) {
            String nombre = grafico.getData().get(posicion).getName();
            if (nombre.startsWith("Vigentes")) return TemaPAE.verdeTexto();
            if (nombre.startsWith("Por vencer")) return TemaPAE.ambarTexto();
            if (nombre.startsWith("Vencidos")) return TemaPAE.rosaTexto();
        }
        return colorSerie(posicion);
    }

    private static Color colorSerie(int indice) {
        return switch (Math.floorMod(indice, 5)) {
            case 0 -> TemaPAE.ciruela();
            case 1 -> TemaPAE.azulTexto();
            case 2 -> TemaPAE.turquesaTexto();
            case 3 -> TemaPAE.ambarTexto();
            default -> TemaPAE.rosaTexto();
        };
    }

    private static int indice(Node nodo, String prefijo) {
        for (String clase : nodo.getStyleClass()) {
            if (clase.startsWith(prefijo) && clase.length() > prefijo.length()) {
                String numero = clase.substring(prefijo.length());
                if (numero.chars().allMatch(Character::isDigit)) return Integer.parseInt(numero);
            }
        }
        return 0;
    }

    private static boolean tiene(Node nodo, String... clases) {
        for (String clase : clases) if (nodo.getStyleClass().contains(clase)) return true;
        return false;
    }

    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }
}
