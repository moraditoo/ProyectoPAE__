package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;

/** Colores de las tablas mediante propiedades JavaFX, incluidas sus celdas virtuales. */
final class TemaTablas {

    private static final String OBSERVADO = "pae-tema-tabla";
    private static final String FUENTE = Font.getFamilies().contains("DM Sans") ? "DM Sans" : "Segoe UI";

    private TemaTablas() {}

    static void aplicar(TableView<?> tabla) {
        tabla.setPadding(new Insets(2));
        tabla.setFixedCellSize(44);
        observar(tabla, tabla);
    }

    private static void observar(Node nodo, TableView<?> tabla) {
        if (nodo.getProperties().putIfAbsent(OBSERVADO, true) == null) {
            InvalidationListener cambio = observable -> actualizar(nodo, tabla);
            nodo.hoverProperty().addListener(cambio);
            nodo.pressedProperty().addListener(cambio);
            nodo.focusedProperty().addListener(cambio);
            nodo.getStyleClass().addListener(cambio);

            if (nodo instanceof Cell<?> celda) {
                celda.itemProperty().addListener(cambio);
                celda.emptyProperty().addListener(cambio);
                celda.selectedProperty().addListener(cambio);
                celda.graphicProperty().addListener(cambio);
            }
            if (nodo instanceof TableRow<?> fila) fila.indexProperty().addListener(cambio);
            if (nodo instanceof Parent padre) {
                padre.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) cambioHijos -> {
                    while (cambioHijos.next()) {
                        for (Node hijo : cambioHijos.getAddedSubList()) observar(hijo, tabla);
                    }
                });
            }
        }

        pintar(nodo, tabla);
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) observar(hijo, tabla);
        }
    }

    private static void actualizar(Node nodo, TableView<?> tabla) {
        pintar(nodo, tabla);
        if (nodo instanceof TableRow<?> fila) {
            for (Node hijo : fila.getChildrenUnmodifiable()) pintar(hijo, tabla);
        }
    }

    private static void pintar(Node nodo, TableView<?> tabla) {
        if (nodo instanceof TableView<?> vista) {
            fondo(vista, TemaPAE.blanco(), 9);
            borde(vista, vista.isFocused() ? TemaPAE.ciruela() : TemaPAE.lilaBorde(), new BorderWidths(1), 9);
        } else if (nodo instanceof TableRow<?> fila) {
            pintarFila(fila);
        } else if (nodo instanceof TableCell<?, ?> celda) {
            pintarCelda(celda, tabla);
        } else if (nodo instanceof TableHeaderRow || nodo instanceof TableColumnHeader) {
            Region encabezado = (Region) nodo;
            fondo(encabezado, nodo.isHover() ? TemaPAE.neutro() : TemaPAE.lila(), 0);
            borde(encabezado, TemaPAE.lilaBorde(), new BorderWidths(0, 0, 1, 0), 0);
        } else if (nodo instanceof Label etiqueta) {
            boolean encabezado = enEncabezado(etiqueta);
            etiqueta.setFont(Font.font(FUENTE, encabezado ? FontWeight.BOLD : FontWeight.NORMAL, 13));
            etiqueta.setTextFill(encabezado ? TemaPAE.ciruela() : TemaPAE.secundario());
        } else if (nodo instanceof Region region) {
            if (tiene(nodo, "filler", "show-hide-columns-button")) {
                fondo(region, TemaPAE.lila(), 0);
                borde(region, TemaPAE.lilaBorde(), new BorderWidths(0, 0, 1, 0), 0);
            } else if (nodo instanceof ScrollBar || tiene(nodo, "track", "track-background", "corner")) {
                fondo(region, TemaPAE.fondo(), 0);
                region.setBorder(Border.EMPTY);
            } else if (tiene(nodo, "thumb")) {
                fondo(region, nodo.isHover() || nodo.isPressed() ? TemaPAE.ciruela() : TemaPAE.lilaBorde(), 6);
                region.setBorder(Border.EMPTY);
            } else if (tiene(nodo, "increment-button", "decrement-button")) {
                fondo(region, nodo.isHover() ? TemaPAE.lila() : TemaPAE.fondo(), 0);
                region.setBorder(Border.EMPTY);
            } else if (tiene(nodo, "arrow", "increment-arrow", "decrement-arrow", "show-hide-column-image")) {
                fondo(region, TemaPAE.ciruela(), 0);
            } else if (tiene(nodo, "virtual-flow", "clipped-container", "sheet", "placeholder")) {
                fondo(region, TemaPAE.blanco(), 0);
            } else if (tiene(nodo, "column-resize-line")) {
                fondo(region, TemaPAE.ciruela(), 0);
            }
        } else if (nodo instanceof Shape figura && tiene(nodo, "sort-order-dot")) {
            figura.setFill(TemaPAE.ciruela());
        }
    }

    private static void pintarFila(TableRow<?> fila) {
        Color color = TemaPAE.blanco();
        if (!fila.isEmpty()) {
            if (fila.isSelected()) color = TemaPAE.lila();
            else if (tiene(fila, "fila-peligro")) color = TemaPAE.rosa();
            else if (tiene(fila, "fila-advertencia")) color = TemaPAE.ambar();
            else if (fila.isHover()) color = TemaPAE.neutro();
            else if (fila.getIndex() % 2 != 0) color = TemaPAE.filaAlterna();
        }
        fondo(fila, color, 0);
        borde(fila, fila.isSelected() ? TemaPAE.ciruela() : Color.TRANSPARENT,
                new BorderWidths(0, 0, 0, 3), 0);
    }

    private static void pintarCelda(TableCell<?, ?> celda, TableView<?> tabla) {
        boolean seleccionada = tabla.getSelectionModel() != null
                && tabla.getSelectionModel().isCellSelectionEnabled() && celda.isSelected();
        fondo(celda, seleccionada ? TemaPAE.lila() : Color.TRANSPARENT, 0);
        borde(celda, TemaPAE.borde(), new BorderWidths(0, 0, 1, 0), 0);
        celda.setFont(Font.font(FUENTE, 13));
        Color tinta = TemaPAE.texto();
        if (tiene(celda, "estado-completada")) tinta = TemaPAE.verdeTexto();
        else if (tiene(celda, "estado-cancelada")) tinta = TemaPAE.rosaTexto();
        else if (tiene(celda, "estado-pendiente")) tinta = TemaPAE.azulTexto();
        celda.setTextFill(tinta);
        if (celda.getGraphic() instanceof FontIcon icono) icono.setIconColor(tinta);
    }

    private static boolean enEncabezado(Node nodo) {
        for (Parent padre = nodo.getParent(); padre != null; padre = padre.getParent()) {
            if (padre instanceof TableColumnHeader) return true;
        }
        return false;
    }

    private static boolean tiene(Node nodo, String... nombres) {
        for (String nombre : nombres) if (nodo.getStyleClass().contains(nombre)) return true;
        return false;
    }

    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }

    private static void borde(Region region, Color color, BorderWidths ancho, double radio) {
        region.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(radio), ancho)));
    }
}
