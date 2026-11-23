package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Colores JavaFX para selectores y sus ventanas desplegables. */
final class TemaSelectores {

    private static final String OBSERVADO = "pae-tema-selector";
    private static final String INICIADO = "pae-selector-iniciado";
    private static final String FUENTE = Font.getFamilies().contains("DM Sans") ? "DM Sans" : "Segoe UI";

    private TemaSelectores() {}

    static void aplicar(Control control) {
        if (control.getProperties().putIfAbsent(INICIADO, true) == null) {
            control.skinProperty().addListener((observable, anterior, actual) -> actualizar(control));
            if (control instanceof ComboBoxBase<?> selector) {
                selector.showingProperty().addListener(observable -> pintarOpciones(selector));
            } else if (control instanceof CheckBox casilla) {
                casilla.selectedProperty().addListener(observable -> actualizar(casilla));
                casilla.indeterminateProperty().addListener(observable -> actualizar(casilla));
                casilla.focusedProperty().addListener(observable -> actualizar(casilla));
                casilla.hoverProperty().addListener(observable -> actualizar(casilla));
            }
        }
        actualizar(control);
    }

    private static void actualizar(Control control) {
        observar(control, control);
        if (control instanceof ComboBoxBase<?> selector) pintarOpciones(selector);
    }

    private static void pintarOpciones(ComboBoxBase<?> selector) {
        if (selector.getSkin() instanceof ComboBoxListViewSkin<?> skin) {
            observar(skin.getPopupContent(), selector);
            observar(skin.getDisplayNode(), selector);
        } else if (selector.getSkin() instanceof DatePickerSkin skin) {
            observar(skin.getPopupContent(), selector);
            observar(skin.getDisplayNode(), selector);
        }
    }

    private static void observar(Node nodo, Control origen) {
        if (nodo == null) return;
        if (nodo.getProperties().putIfAbsent(OBSERVADO, true) == null) {
            InvalidationListener cambio = observable -> pintar(nodo, origen);
            nodo.hoverProperty().addListener(cambio);
            nodo.pressedProperty().addListener(cambio);
            nodo.focusedProperty().addListener(cambio);
            nodo.disabledProperty().addListener(cambio);
            nodo.getStyleClass().addListener(cambio);
            if (nodo instanceof Cell<?> celda) {
                celda.itemProperty().addListener(cambio);
                celda.emptyProperty().addListener(cambio);
                celda.selectedProperty().addListener(cambio);
                celda.textProperty().addListener(cambio);
            }
            if (nodo instanceof Parent padre) {
                padre.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) cambioHijos -> {
                    while (cambioHijos.next()) {
                        for (Node hijo : cambioHijos.getAddedSubList()) observar(hijo, origen);
                    }
                });
            }
        }
        pintar(nodo, origen);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) observar(hijo, origen);
        }
    }

    private static void pintar(Node nodo, Control origen) {
        if (nodo instanceof TextField editor) {
            editor.setFont(Font.font(FUENTE, 13));
            TemaCampos.aplicar(editor);
            fondo(editor, Color.TRANSPARENT, 9);
            editor.setBorder(Border.EMPTY);
        } else if (nodo instanceof ListView<?> lista) {
            fondo(lista, TemaPAE.blanco(), 9);
            borde(lista, lista.isFocused() ? TemaPAE.ciruela() : TemaPAE.lilaBorde(), 9);
            lista.setPadding(new Insets(2));
        } else if (nodo instanceof ListCell<?> celda) {
            pintarCelda(celda, origen);
        } else if (nodo instanceof DateCell dia) {
            pintarDia(dia);
        } else if (nodo instanceof Label etiqueta) {
            boolean encabezado = tiene(etiqueta, "spinner-label");
            etiqueta.setFont(Font.font(FUENTE, encabezado ? FontWeight.BOLD : FontWeight.NORMAL, 13));
            etiqueta.setTextFill(encabezado ? TemaPAE.ciruela() : TemaPAE.secundario());
        } else if (nodo instanceof Region region) {
            pintarRegion(region, origen);
        }
    }

    private static void pintarCelda(ListCell<?> celda, Control origen) {
        boolean muestraValor = origen instanceof ComboBox<?> && dentroDe(celda, origen);
        Color color = Color.TRANSPARENT;
        if (!muestraValor) {
            color = celda.isEmpty() ? TemaPAE.blanco()
                    : celda.isSelected() ? TemaPAE.lila()
                    : celda.isHover() ? TemaPAE.neutro() : TemaPAE.blanco();
        }
        fondo(celda, color, 0);
        borde(celda, !muestraValor && celda.isFocused() ? TemaPAE.ciruela() : Color.TRANSPARENT, 0);
        celda.setFont(Font.font(FUENTE, 13));
        celda.setTextFill(muestraValor && celda.getItem() == null ? TemaPAE.secundario() : TemaPAE.texto());
        celda.setPadding(new Insets(7, 10, 7, 10));
    }

    private static void pintarDia(DateCell dia) {
        boolean encabezado = tiene(dia, "day-name-cell", "week-number-cell");
        boolean seleccionado = tiene(dia, "selected");
        Color color = encabezado ? TemaPAE.lila() : seleccionado ? TemaPAE.ciruela()
                : dia.isHover() || dia.isFocused() ? TemaPAE.neutro() : TemaPAE.blanco();
        Color tinta = encabezado ? TemaPAE.ciruela()
                : seleccionado ? TemaPAE.isModoOscuro() ? TemaPAE.bannerFondo() : Color.WHITE
                : tiene(dia, "previous-month", "next-month") || dia.isDisabled() ? TemaPAE.secundario() : TemaPAE.texto();
        fondo(dia, color, 4);
        borde(dia, tiene(dia, "today") || dia.isFocused() ? TemaPAE.ciruela() : Color.TRANSPARENT, 4);
        dia.setTextFill(tinta);
        dia.setFont(Font.font(FUENTE, encabezado ? FontWeight.BOLD : FontWeight.NORMAL, 13));
    }

    private static void pintarRegion(Region region, Control origen) {
        if (origen instanceof CheckBox casilla && tiene(region, "box", "mark")) {
            boolean marcada = casilla.isSelected() || casilla.isIndeterminate();
            if (tiene(region, "box")) {
                fondo(region, marcada ? TemaPAE.ciruela() : TemaPAE.blanco(), 4);
                borde(region, marcada || casilla.isFocused() || casilla.isHover() ? TemaPAE.ciruela() : TemaPAE.lilaBorde(), 4);
            } else {
                fondo(region, marcada ? TemaPAE.isModoOscuro() ? TemaPAE.bannerFondo() : Color.WHITE : Color.TRANSPARENT, 0);
            }
        } else if (tiene(region, "date-picker-popup")) {
            fondo(region, TemaPAE.blanco(), 9);
            borde(region, TemaPAE.lilaBorde(), 9);
        } else if (tiene(region, "month-year-pane", "spinner")) {
            fondo(region, TemaPAE.lila(), 0);
        } else if (tiene(region, "left-button", "right-button", "arrow-button")) {
            fondo(region, region.isHover() || region.isPressed() ? TemaPAE.neutro() : TemaPAE.lila(), 5);
            region.setBorder(Border.EMPTY);
        } else if (tiene(region, "arrow", "left-arrow", "right-arrow", "increment-arrow", "decrement-arrow")) {
            fondo(region, TemaPAE.ciruela(), 0);
        } else if (tiene(region, "calendar-grid", "virtual-flow", "clipped-container", "sheet", "placeholder", "corner")) {
            fondo(region, TemaPAE.blanco(), 0);
        } else if (region instanceof ScrollBar || tiene(region, "track", "track-background")) {
            fondo(region, TemaPAE.fondo(), 0);
            region.setBorder(Border.EMPTY);
        } else if (tiene(region, "thumb")) {
            fondo(region, region.isHover() || region.isPressed() ? TemaPAE.ciruela() : TemaPAE.lilaBorde(), 6);
            region.setBorder(Border.EMPTY);
        } else if (tiene(region, "increment-button", "decrement-button")) {
            fondo(region, region.isHover() ? TemaPAE.lila() : TemaPAE.fondo(), 0);
            region.setBorder(Border.EMPTY);
        }
    }

    private static boolean dentroDe(Node nodo, Node contenedor) {
        for (Parent padre = nodo.getParent(); padre != null; padre = padre.getParent()) {
            if (padre instanceof ListView<?>) return false;
            if (padre == contenedor) return true;
        }
        return false;
    }

    private static boolean tiene(Node nodo, String... clases) {
        for (String clase : clases) if (nodo.getStyleClass().contains(clase)) return true;
        return false;
    }

    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }

    private static void borde(Region region, Color color, double radio) {
        region.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(radio), BorderWidths.DEFAULT)));
    }
}
