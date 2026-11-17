package ni.edu.uam.proyectopae.util;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;

/** Estilos de la propuesta aplicados mediante propiedades de JavaFX. */
public final class TemaPAE {
    private static final Color FONDO = Color.web("#F6F3F6");
    private static final Color BLANCO = Color.WHITE;
    private static final Color TEXTO = Color.web("#281D2B");
    private static final Color SECUNDARIO = Color.web("#615567");
    private static final Color BORDE = Color.web("#E7DFE8");
    private static final Color CIRUELA = Color.web("#5B2D62");
    private static final Color OSCURO = Color.web("#321937");
    private static final Color LILA = Color.web("#F2E6F3");
    private static final Color ROSA = Color.web("#F8EAF0");
    private static final Color ROSA_TEXTO = Color.web("#813954");
    private static final Color CLARO = Color.web("#F3E6F4");
    private static final Color VERDE = Color.web("#E8F3EB");
    private static final Color VERDE_TEXTO = Color.web("#265A40");
    private static final Color AMBAR = Color.web("#FFF1DA");
    private static final Color AMBAR_TEXTO = Color.web("#785016");
    private static final Color NEUTRO = Color.web("#F1EDF2");
    private static final Color FILA_ALTERNA = Color.web("#FCFAFC");
    private static final String FUENTE = Font.getFamilies().contains("DM Sans") ? "DM Sans" : "Segoe UI";

    private static final String ESTRELLA =
            "M12 2 L14.8 9.2 L22 12 L14.8 14.8 L12 22 L9.2 14.8 L2 12 L9.2 9.2 Z";

    private TemaPAE() {}

    static boolean usaTema(Parent raiz) {
        return tiene(raiz, "pae-ui");
    }

    static HBox barra(String modulo, Runnable volver) {
        HBox barra = new HBox(20);
        barra.getStyleClass().add("barra-aplicacion");
        StackPane marca = new StackPane();
        marca.getStyleClass().add("marca");
        Region divisor = new Region();
        divisor.setMinSize(1, 23);
        divisor.setMaxSize(1, 23);
        fondo(divisor, BORDE, 0);
        Label nombre = new Label("ProyectoPAE");
        nombre.getStyleClass().add("marca-texto");
        Label seccion = new Label(modulo);
        seccion.getStyleClass().add("barra-seccion");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Button menu = new Button("Menú principal");
        menu.getStyleClass().add("btn-secondary");
        menu.setGraphic(icono("M14 5 L7 12 L14 19 M7 12 H21", CIRUELA, 16));
        menu.setOnAction(evento -> volver.run());
        barra.getChildren().setAll(new HBox(11, marca, nombre), divisor, seccion, espacio, menu);
        ((HBox) barra.getChildren().getFirst()).setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    static void aplicar(Parent raiz) {
        pintar(raiz);
    }

    private static void pintar(Node nodo) {
        if (nodo instanceof Region region) {
            pintarRegion(region);
        }
        if (nodo instanceof Label etiqueta) {
            pintarEtiqueta(etiqueta);
            if (etiqueta.getGraphic() != null) pintar(etiqueta.getGraphic());
        } else if (nodo instanceof Button boton) {
            pintarBoton(boton);
        } else if (nodo instanceof CheckBox casilla) {
            pintarCasilla(casilla);
        } else if (nodo instanceof TextInputControl entrada) {
            entrada.setFont(fuente(FontWeight.NORMAL, 13));
            if (entrada instanceof TextField campo) campo.setPrefHeight(42);
            enfocable(entrada);
        } else if (nodo instanceof ComboBoxBase<?> selector) {
            selector.setPrefHeight(42);
            enfocable(selector);
        } else if (nodo instanceof TableView<?> tabla) {
            pintarTabla(tabla);
        } else if (nodo instanceof FontIcon icono) {
            icono.setIconColor(CIRUELA);
        }
        // Las pieles de los controles y sus nodos internos se crean al mostrar la escena.
        if (nodo instanceof ScrollPane scroll && scroll.getContent() != null) {
            fondo(scroll, FONDO, 0);
            scroll.skinProperty().addListener((o, antes, ahora) -> Platform.runLater(() -> {
                if (scroll.lookup(".viewport") instanceof Region viewport) fondo(viewport, FONDO, 0);
            }));
            pintar(scroll.getContent());
        } else if (nodo instanceof Parent padre && !(nodo instanceof Control)) {
            for (Node hijo : padre.getChildrenUnmodifiable()) pintar(hijo);
        }
    }

    private static void pintarRegion(Region r) {
        if (tiene(r, "pae-ui", "content-area")) fondo(r, FONDO, 0);
        if (tiene(r, "content-area")) r.setPadding(new Insets(18, 36, 28, 36));
        if (tiene(r, "barra-aplicacion")) {
            fondo(r, BLANCO, 0);
            r.setPadding(new Insets(0, 30, 0, 30));
            r.setMinHeight(65);
            if (r instanceof HBox caja) caja.setAlignment(Pos.CENTER_LEFT);
        }
        if (tiene(r, "encabezado-modulo")) {
            r.setPadding(new Insets(26, 36, 0, 36));
        }
        if (tiene(r, "card", "login-card", "kpi-card")) {
            fondo(r, BLANCO, 16);
            borde(r, BORDE, 16);
            r.setPadding(new Insets(tiene(r, "login-card") ? 32 : 20));
        }
        if (tiene(r, "panel-acceso", "bienvenida")) fondo(r, OSCURO, tiene(r, "bienvenida") ? 16 : 0);
        if (tiene(r, "decoracion-acceso", "decoracion-menu") && r instanceof Pane panel) {
            decorar(panel, tiene(r, "decoracion-acceso"));
        }
        if (tiene(r, "marca", "marca-clara", "marca-acceso") && r instanceof StackPane marca) {
            boolean clara = !tiene(r, "marca");
            fondo(r, clara ? LILA : CIRUELA, 11);
            marca.getChildren().setAll(icono(ESTRELLA, clara ? CIRUELA : BLANCO, 21));
        }
        if (tiene(r, "chip-lila")) fondo(r, LILA, 9);
        if (tiene(r, "avatar")) fondo(r, LILA, 17);
        if (tiene(r, "chip-rosa", "fila-peligro")) fondo(r, ROSA, 9);
        if (tiene(r, "chip-verde")) fondo(r, VERDE, 9);
        if (tiene(r, "chip-ambar", "fila-advertencia")) fondo(r, AMBAR, 9);
        if (tiene(r, "alerta-menu")) {
            fondo(r, AMBAR, 12);
            r.setPadding(new Insets(12, 16, 12, 16));
        }
        if (tiene(r, "resumen-total", "estado-reporte")) {
            fondo(r, LILA, 10);
            r.setPadding(new Insets(12, 14, 12, 14));
        }
        if (tiene(r, "separador")) fondo(r, BORDE, 0);
    }

    private static void pintarEtiqueta(Label l) {
        if (tiene(l, "marca-texto")) letra(l, 19, FontWeight.BOLD, TEXTO);
        else if (tiene(l, "marca-invertida")) letra(l, 19, FontWeight.BOLD, BLANCO);
        else if (tiene(l, "titulo-marca-acceso")) letra(l, 58, FontWeight.BOLD, BLANCO);
        else if (tiene(l, "bienvenida-titulo")) letra(l, 40, FontWeight.BOLD, BLANCO);
        else if (tiene(l, "bienvenida-meta")) letra(l, 13, FontWeight.SEMI_BOLD, CLARO);
        else if (tiene(l, "bienvenida-subtitulo")) letra(l, 16, FontWeight.NORMAL, CLARO);
        else if (tiene(l, "header-title", "login-title")) letra(l, 36, FontWeight.BOLD, TEXTO);
        else if (tiene(l, "header-subtitle")) letra(l, 15, FontWeight.NORMAL, SECUNDARIO);
        else if (tiene(l, "card-title")) letra(l, 20, FontWeight.BOLD, TEXTO);
        else if (tiene(l, "menu-card-title")) letra(l, 16, FontWeight.BOLD, TEXTO);
        else if (tiene(l, "kpi-valor")) letra(l, 33, FontWeight.BOLD, TEXTO);
        else if (tiene(l, "kpi-titulo", "barra-seccion")) letra(l, 14, FontWeight.SEMI_BOLD, SECUNDARIO);
        else if (tiene(l, "total-venta")) letra(l, 25, FontWeight.BOLD, CIRUELA);
        else if (tiene(l, "form-label", "usuario-menu")) letra(l, 13, FontWeight.BOLD, TEXTO);
        else if (tiene(l, "modo-label")) letra(l, 13, FontWeight.SEMI_BOLD, CIRUELA);
        else if (tiene(l, "login-error")) letra(l, 13, FontWeight.SEMI_BOLD, ROSA_TEXTO);
        else if (tiene(l, "alerta-titulo")) letra(l, 14, FontWeight.BOLD, AMBAR_TEXTO);
        else if (tiene(l, "alerta-texto")) letra(l, 13, FontWeight.NORMAL, AMBAR_TEXTO);
        else if (tiene(l, "badge-verde")) {
            letra(l, 13, FontWeight.BOLD, VERDE_TEXTO);
            l.setPadding(new Insets(7, 11, 7, 11));
            fondo(l, VERDE, 20);
        } else if (tiene(l, "menu-card-subtitle", "hint-label")) letra(l, 13, FontWeight.NORMAL, SECUNDARIO);
        else letra(l, 13, FontWeight.NORMAL, TEXTO);
    }

    private static void pintarBoton(Button boton) {
        if (tiene(boton, "tarjeta-menu")) {
            boton.setAlignment(Pos.CENTER_LEFT);
            boton.setPadding(new Insets(12, 15, 12, 15));
            boton.setCursor(Cursor.HAND);
            if (boton.getGraphic() instanceof Region contenido) {
                boton.setMinWidth(0);
                contenido.prefWidthProperty().bind(Bindings.max(115, boton.widthProperty().subtract(32)));
            }
            actualizarTarjeta(boton);
            boton.hoverProperty().addListener((o, antes, ahora) -> actualizarTarjeta(boton));
            boton.focusedProperty().addListener((o, antes, ahora) -> actualizarTarjeta(boton));
            pintar(boton.getGraphic());
            return;
        }
        boolean principal = tiene(boton, "btn-primary");
        boolean peligro = tiene(boton, "btn-danger");
        boolean discreto = tiene(boton, "btn-ghost");
        Color tinta = principal ? BLANCO : peligro ? ROSA_TEXTO : CIRUELA;
        letra(boton, 13, FontWeight.BOLD, tinta);
        if (boton.getGraphic() instanceof FontIcon icono) icono.setIconColor(tinta);
        boton.setMinHeight(40);
        boton.setPadding(new Insets(0, 14, 0, 14));
        boton.setCursor(Cursor.HAND);
        Runnable actualizar = () -> {
            Color color = principal ? (boton.isHover() ? OSCURO : CIRUELA)
                    : boton.isHover() ? (peligro ? ROSA : LILA) : discreto ? Color.TRANSPARENT : BLANCO;
            fondo(boton, color, 9);
            Color contorno = boton.isFocused() ? (principal ? Color.web("#D9BADB") : CIRUELA)
                    : principal || discreto ? Color.TRANSPARENT : BORDE;
            borde(boton, contorno, 9);
        };
        actualizar.run();
        boton.hoverProperty().addListener((o, antes, ahora) -> actualizar.run());
        boton.focusedProperty().addListener((o, antes, ahora) -> actualizar.run());
    }

    private static void actualizarTarjeta(Button b) {
        fondo(b, b.isHover() ? LILA : BLANCO, 16);
        borde(b, b.isFocused() || b.isHover() ? CIRUELA : BORDE, 16);
    }

    private static void enfocable(Region control) {
        fondo(control, BLANCO, 9);
        borde(control, BORDE, 9);
        control.focusedProperty().addListener((o, antes, ahora) -> borde(control, ahora ? CIRUELA : BORDE, 9));
    }

    private static void pintarCasilla(CheckBox control) {
        letra(control, 14, FontWeight.NORMAL, TEXTO);
        Runnable pintar = () -> {
            if (control.lookup(".box") instanceof Region caja) {
                fondo(caja, control.isSelected() ? CIRUELA : BLANCO, 4);
                borde(caja, control.isFocused() || control.isSelected() ? CIRUELA : SECUNDARIO, 4);
            }
            if (control.lookup(".mark") instanceof Region marca) {
                fondo(marca, control.isSelected() ? BLANCO : Color.TRANSPARENT, 0);
            }
        };
        control.skinProperty().addListener((o, antes, ahora) -> Platform.runLater(pintar));
        control.selectedProperty().addListener((o, antes, ahora) -> pintar.run());
        control.focusedProperty().addListener((o, antes, ahora) -> pintar.run());
    }

    private static <T> void pintarTabla(TableView<T> tabla) {
        fondo(tabla, BLANCO, 10);
        borde(tabla, BORDE, 10);
        tabla.setFixedCellSize(47);
        if (tabla.getPlaceholder() != null) pintar(tabla.getPlaceholder());

        var factory = tabla.getRowFactory();
        tabla.setRowFactory(t -> {
            TableRow<T> fila = factory == null ? new TableRow<>() : factory.call(t);
            Runnable colorFila = () -> {
                Color color = fila.isEmpty() ? BLANCO
                        : fila.isSelected() ? CIRUELA
                        : tiene(fila, "fila-peligro") ? ROSA
                        : tiene(fila, "fila-advertencia") ? AMBAR
                        : fila.getIndex() % 2 == 0 ? BLANCO : FILA_ALTERNA;
                fondo(fila, color, 0);
            };
            fila.indexProperty().addListener((o, a, b) -> colorFila.run());
            fila.emptyProperty().addListener((o, a, b) -> colorFila.run());
            fila.selectedProperty().addListener((o, a, b) -> colorFila.run());
            fila.getStyleClass().addListener((ListChangeListener<String>) cambio -> colorFila.run());
            colorFila.run();
            return fila;
        });
        Runnable encabezado = () -> {
            for (String selector : new String[]{".column-header-background", ".column-header", ".filler"}) {
                for (Node nodo : tabla.lookupAll(selector)) {
                    if (nodo instanceof Region region) fondo(region, NEUTRO, 0);
                    if (nodo instanceof Region region && selector.equals(".column-header")) {
                        region.setPrefHeight(39);
                        if (region.lookup(".label") instanceof Label titulo) {
                            letra(titulo, 12, FontWeight.BOLD, SECUNDARIO);
                            titulo.setAlignment(Pos.CENTER_LEFT);
                            titulo.setPadding(new Insets(0, 0, 0, 8));
                        }
                    }
                }
            }
        };
        tabla.skinProperty().addListener((o, antes, ahora) -> Platform.runLater(encabezado));
        tabla.getVisibleLeafColumns().addListener((ListChangeListener<TableColumn<T, ?>>) c ->
                Platform.runLater(encabezado));
    }

    private static void decorar(Pane panel, boolean acceso) {
        panel.setMouseTransparent(true);
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(panel.widthProperty());
        recorte.heightProperty().bind(panel.heightProperty());
        recorte.setArcWidth(acceso ? 0 : 32);
        recorte.setArcHeight(acceso ? 0 : 32);
        panel.setClip(recorte);
        if (acceso) {
            agregarElipse(panel, 0.80, 0.11, 271, null, 0.35);
            agregarElipse(panel, 0.80, 0.11, 230, null, 0.23);
            agregarElipse(panel, 0.93, 0.80, 211, CIRUELA, 0.62);
            agregarElipse(panel, 0.89, 0.79, 174, null, 0.52);
            agregarElipse(panel, 0.89, 0.79, 154, null, 0.30);
            agregarEstrella(panel, 0.77, 0.76, 104, Color.web("#F1A5CC"));
            agregarEstrella(panel, 0.89, 0.67, 30, CLARO);
            agregarEstrella(panel, 0.62, 0.86, 22, CLARO);
        } else {
            agregarElipse(panel, 0.93, 0.36, 228, null, 0.35);
            agregarElipse(panel, 0.93, 0.40, 194, null, 0.26);
            agregarEstrella(panel, 0.925, 0.52, 88, Color.web("#F1A5CC"));
        }
    }

    private static void agregarElipse(Pane panel, double x, double y, double radio, Color color, double opacidad) {
        Ellipse elipse = new Ellipse(radio, radio);
        elipse.setFill(color == null ? Color.TRANSPARENT : color);
        if (color == null) elipse.setStroke(Color.web("#D9BADB"));
        elipse.setOpacity(opacidad);
        elipse.centerXProperty().bind(panel.widthProperty().multiply(x));
        elipse.centerYProperty().bind(panel.heightProperty().multiply(y));
        panel.getChildren().add(elipse);
    }

    private static void agregarEstrella(Pane panel, double x, double y, double tamano, Color color) {
        Node estrella = icono(ESTRELLA, color, tamano);
        estrella.setManaged(false);
        estrella.layoutXProperty().bind(panel.widthProperty().multiply(x));
        estrella.layoutYProperty().bind(panel.heightProperty().multiply(y));
        panel.getChildren().add(estrella);
    }

    private static SVGPath icono(String camino, Color tinta, double tamano) {
        SVGPath figura = new SVGPath();
        figura.setContent(camino);
        figura.setFill(Color.TRANSPARENT);
        figura.setStroke(tinta);
        figura.setStrokeWidth(1.65);
        figura.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        figura.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        figura.setScaleX(tamano / 22);
        figura.setScaleY(tamano / 22);
        if (tamano > 40) figura.setStrokeWidth(0.45);
        return figura;
    }

    private static Font fuente(FontWeight grosor, double tamano) {
        return Font.font(FUENTE, grosor, tamano);
    }

    private static void letra(Labeled etiqueta, double tamano, FontWeight grosor, Color tinta) {
        etiqueta.setFont(fuente(grosor, tamano));
        etiqueta.setTextFill(tinta);
    }

    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }

    private static void borde(Region region, Color color, double radio) {
        region.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(radio), BorderWidths.DEFAULT)));
    }

    private static boolean tiene(Node nodo, String... clases) {
        for (String clase : clases) if (nodo.getStyleClass().contains(clase)) return true;
        return false;
    }
}
