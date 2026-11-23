package ni.edu.uam.proyectopae.util;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;

/** Presentacion de los dialogos mediante propiedades JavaFX. */
final class TemaDialogos {

    private static final String OBSERVADO = "pae-tema-dialogo";
    private static final String FUENTE = Font.getFamilies().contains("DM Sans") ? "DM Sans" : "Segoe UI";

    private TemaDialogos() {}

    static void aplicar(DialogPane panel) {
        if (!panel.getStyleClass().contains("pae-ui")) panel.getStyleClass().add("pae-ui");
        observar(panel);
    }

    private static void observar(Node nodo) {
        if (nodo instanceof Control control && !(control instanceof Label) && !(control instanceof Button)
                && !(control instanceof ButtonBar)) {
            TemaPAE.aplicar(control);
            return;
        }
        if (nodo.getProperties().putIfAbsent(OBSERVADO, true) == null) {
            nodo.getStyleClass().addListener((javafx.beans.InvalidationListener) observable -> pintar(nodo));
            if (nodo instanceof Parent padre) {
                padre.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) cambio -> {
                    while (cambio.next()) for (Node hijo : cambio.getAddedSubList()) observar(hijo);
                });
            }
        }
        pintar(nodo);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) observar(hijo);
        }
    }

    private static void pintar(Node nodo) {
        if (nodo instanceof Button boton) {
            ButtonBar.ButtonData tipo = ButtonBar.getButtonData(boton);
            boolean principal = boton.isDefaultButton() || tipo == ButtonBar.ButtonData.YES || tipo == ButtonBar.ButtonData.OK_DONE;
            String clase = principal ? "btn-primary" : "btn-secondary";
            if (!boton.getStyleClass().contains(clase)) boton.getStyleClass().add(clase);
            TemaPAE.aplicar(boton);
        } else if (nodo instanceof Label etiqueta) {
            boolean encabezado = enEncabezado(etiqueta);
            etiqueta.setFont(Font.font(FUENTE, encabezado ? FontWeight.BOLD : FontWeight.NORMAL, encabezado ? 18 : 13));
            etiqueta.setTextFill(TemaPAE.texto());
        } else if (nodo instanceof FontIcon icono && icono.getProperties().containsKey("pae-alerta")) {
            icono.setIconColor(switch (icono.getIconLiteral()) {
                case "fas-check-circle" -> TemaPAE.verdeTexto();
                case "fas-times-circle" -> TemaPAE.rosaTexto();
                case "fas-exclamation-triangle" -> TemaPAE.ambarTexto();
                default -> TemaPAE.azulTexto();
            });
        } else if (nodo instanceof Region region) {
            if (nodo instanceof DialogPane) {
                fondo(region, TemaPAE.blanco(), 0);
                region.setBorder(new Border(new BorderStroke(TemaPAE.lilaBorde(), BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, BorderWidths.DEFAULT)));
            } else if (nodo.getStyleClass().contains("header-panel")) {
                fondo(region, TemaPAE.lila(), 0);
                region.setBorder(Border.EMPTY);
            } else if (nodo instanceof ButtonBar) {
                fondo(region, TemaPAE.blanco(), 0);
            }
        }
    }

    private static boolean enEncabezado(Node nodo) {
        for (Parent padre = nodo.getParent(); padre != null; padre = padre.getParent()) {
            if (padre.getStyleClass().contains("header-panel")) return true;
        }
        return false;
    }

    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }
}
