package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Tema de las superficies de desplazamiento y las ayudas emergentes. */
final class TemaComplementos {

    private TemaComplementos() {}

    static void aplicar(ScrollPane scroll) {
        observar(scroll, scroll);
    }

    static void prepararTooltip(Control control) {
        if (control.getProperties().putIfAbsent("pae-tooltip-control", true) == null) {
            control.tooltipProperty().addListener((observable, anterior, actual) -> aplicarTooltip(actual));
        }
        aplicarTooltip(control.getTooltip());
    }

    private static void aplicarTooltip(Tooltip ayuda) {
        if (ayuda == null) return;
        if (ayuda.getProperties().putIfAbsent("pae-tooltip", true) == null) {
            ayuda.skinProperty().addListener((observable, anterior, actual) -> pintarTooltip(ayuda));
            ayuda.showingProperty().addListener(observable -> pintarTooltip(ayuda));
        }
        pintarTooltip(ayuda);
    }

    private static void pintarTooltip(Tooltip ayuda) {
        ayuda.setFont(Font.font("Segoe UI", 13));
        if (ayuda.getSkin() != null && ayuda.getSkin().getNode() instanceof Region region) {
            if (region instanceof Labeled etiqueta) etiqueta.setTextFill(TemaPAE.texto());
            fondo(region, TemaPAE.blanco(), 8);
            region.setBorder(new Border(new BorderStroke(TemaPAE.lilaBorde(), BorderStrokeStyle.SOLID,
                    new CornerRadii(8), BorderWidths.DEFAULT)));
            region.setPadding(new Insets(9, 12, 9, 12));
        }
    }

    private static void observar(Node nodo, ScrollPane scroll) {
        if (nodo == scroll.getContent()) return;
        if (nodo instanceof Control && nodo != scroll && !(nodo instanceof ScrollBar)) return;
        if (nodo.getProperties().putIfAbsent("pae-tema-scroll", true) == null) {
            InvalidationListener cambio = observable -> pintar(nodo);
            nodo.hoverProperty().addListener(cambio);
            nodo.pressedProperty().addListener(cambio);
            if (nodo instanceof Parent padre) {
                padre.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) cambioHijos -> {
                    while (cambioHijos.next()) for (Node hijo : cambioHijos.getAddedSubList()) observar(hijo, scroll);
                });
            }
        }
        pintar(nodo);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) observar(hijo, scroll);
        }
    }

    private static void pintar(Node nodo) {
        if (!(nodo instanceof Region region)) return;
        if (nodo instanceof ScrollPane) {
            fondo(region, TemaPAE.fondo(), 0);
            region.setBorder(Border.EMPTY);
            region.setPadding(Insets.EMPTY);
        } else if (nodo instanceof ScrollBar || tiene(nodo, "viewport", "corner", "track", "track-background")) {
            fondo(region, TemaPAE.fondo(), 0);
            region.setBorder(Border.EMPTY);
        } else if (tiene(nodo, "thumb")) {
            fondo(region, nodo.isHover() || nodo.isPressed() ? TemaPAE.ciruela() : TemaPAE.lilaBorde(), 6);
            region.setBorder(Border.EMPTY);
        } else if (tiene(nodo, "increment-button", "decrement-button")) {
            fondo(region, nodo.isHover() ? TemaPAE.lila() : TemaPAE.fondo(), 0);
            region.setBorder(Border.EMPTY);
        } else if (tiene(nodo, "increment-arrow", "decrement-arrow")) {
            fondo(region, TemaPAE.ciruela(), 0);
        }
    }

    private static boolean tiene(Node nodo, String... clases) {
        for (String clase : clases) if (nodo.getStyleClass().contains(clase)) return true;
        return false;
    }

    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }
}
