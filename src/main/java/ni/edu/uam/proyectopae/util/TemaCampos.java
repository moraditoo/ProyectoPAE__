package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/** Colores de texto y seleccion mediante las propiedades de las skins de JavaFX. */
final class TemaCampos {

    private static final String OBSERVADO = "pae-tema-area";

    private TemaCampos() {}

    static void aplicar(TextInputControl entrada) {
        if (entrada instanceof TextField campo) {
            campo.setPrefHeight(42);
            if (!(campo.getSkin() instanceof CampoSkin)) campo.setSkin(new CampoSkin(campo));
            ((CampoSkin) campo.getSkin()).actualizar();
        } else if (entrada instanceof TextArea area) {
            if (!(area.getSkin() instanceof AreaSkin)) area.setSkin(new AreaSkin(area));
            ((AreaSkin) area.getSkin()).actualizar();
        }
    }

    private static final class CampoSkin extends TextFieldSkin {
        CampoSkin(TextField campo) {
            super(campo);
        }

        void actualizar() {
            setTextFill(TemaPAE.texto());
            setPromptTextFill(TemaPAE.secundario());
            setHighlightFill(TemaPAE.ciruela());
            setHighlightTextFill(TemaPAE.isModoOscuro() ? TemaPAE.bannerFondo() : Color.WHITE);
        }
    }

    private static final class AreaSkin extends TextAreaSkin {
        AreaSkin(TextArea area) {
            super(area);
        }

        void actualizar() {
            setTextFill(TemaPAE.texto());
            setPromptTextFill(TemaPAE.secundario());
            setHighlightFill(TemaPAE.ciruela());
            setHighlightTextFill(TemaPAE.isModoOscuro() ? TemaPAE.bannerFondo() : Color.WHITE);
            for (Node hijo : getChildren()) observar(hijo);
        }
    }

    private static void observar(Node nodo) {
        if (nodo.getProperties().putIfAbsent(OBSERVADO, true) == null) {
            InvalidationListener cambio = observable -> pintar(nodo);
            nodo.hoverProperty().addListener(cambio);
            nodo.pressedProperty().addListener(cambio);
            nodo.getStyleClass().addListener(cambio);
            if (nodo instanceof Parent padre) {
                padre.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) cambioHijos -> {
                    while (cambioHijos.next()) {
                        for (Node hijo : cambioHijos.getAddedSubList()) observar(hijo);
                    }
                });
            }
        }
        pintar(nodo);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) observar(hijo);
        }
    }

    private static void pintar(Node nodo) {
        if (!(nodo instanceof Region region)) return;
        if (nodo instanceof ScrollPane) {
            fondo(region, Color.TRANSPARENT, 9);
            region.setBorder(Border.EMPTY);
            region.setPadding(Insets.EMPTY);
        } else if (tiene(nodo, "viewport", "content", "corner")) {
            fondo(region, TemaPAE.blanco(), 9);
        } else if (nodo instanceof ScrollBar || tiene(nodo, "track", "track-background")) {
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
