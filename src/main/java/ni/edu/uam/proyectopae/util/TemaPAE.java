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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Motor de temas y estilos para la interfaz gráfica del Proyecto PAE.
 * Soporta Modo Claro (paleta Figma ciruela) y Modo Oscuro dinámico.
 */
public final class TemaPAE {

    private static boolean modoOscuro = false;

    // --- Paleta Dinámica según Modo Claro / Oscuro ---
    public static Color fondo() {
        return modoOscuro ? Color.web("#17111A") : Color.web("#F6F3F6");
    }

    public static Color blanco() {
        return modoOscuro ? Color.web("#241B26") : Color.WHITE;
    }

    public static Color texto() {
        return modoOscuro ? Color.web("#F6EEF7") : Color.web("#281D2B");
    }

    public static Color secundario() {
        return modoOscuro ? Color.web("#BCAEC2") : Color.web("#615567");
    }

    public static Color borde() {
        return modoOscuro ? Color.web("#3E2D43") : Color.web("#E7DFE8");
    }

    public static Color ciruela() {
        return modoOscuro ? Color.web("#C478D0") : Color.web("#5B2D62");
    }

    public static Color ciruelaOscuro() {
        return modoOscuro ? Color.web("#D992E3") : Color.web("#321937");
    }

    public static Color bannerFondo() {
        return modoOscuro ? Color.web("#140A16") : Color.web("#321937");
    }

    public static Color lila() {
        return modoOscuro ? Color.web("#38233C") : Color.web("#F2E6F3");
    }

    public static Color lilaBorde() {
        return modoOscuro ? Color.web("#523559") : Color.web("#D9BADB");
    }

    public static Color rosa() {
        return modoOscuro ? Color.web("#421A2B") : Color.web("#F8EAF0");
    }

    public static Color rosaTexto() {
        return modoOscuro ? Color.web("#FBA4BD") : Color.web("#813954");
    }

    public static Color claro() {
        return modoOscuro ? Color.web("#ECCEEF") : Color.web("#F3E6F4");
    }

    public static Color verde() {
        return modoOscuro ? Color.web("#173724") : Color.web("#E8F3EB");
    }

    public static Color verdeTexto() {
        return modoOscuro ? Color.web("#75EAA8") : Color.web("#265A40");
    }

    public static Color ambar() {
        return modoOscuro ? Color.web("#3F2B0E") : Color.web("#FFF1DA");
    }

    public static Color ambarTexto() {
        return modoOscuro ? Color.web("#FCD34D") : Color.web("#785016");
    }

    public static Color azul() {
        return modoOscuro ? Color.web("#182B45") : Color.web("#EBF3FC");
    }

    public static Color azulTexto() {
        return modoOscuro ? Color.web("#7BB4F7") : Color.web("#1D4ED8");
    }

    public static Color turquesa() {
        return modoOscuro ? Color.web("#13363B") : Color.web("#E6F7F9");
    }

    public static Color turquesaTexto() {
        return modoOscuro ? Color.web("#5EEAD4") : Color.web("#0E7490");
    }

    public static Color neutro() {
        return modoOscuro ? Color.web("#2C1E30") : Color.web("#F1EDF2");
    }

    public static Color filaAlterna() {
        return modoOscuro ? Color.web("#201723") : Color.web("#FCFAFC");
    }

    private static final String FUENTE = Font.getFamilies().contains("DM Sans") ? "DM Sans" : "Segoe UI";

    private static final String ESTRELLA =
            "M12 2 L14.8 9.2 L22 12 L14.8 14.8 L12 22 L9.2 14.8 L2 12 L9.2 9.2 Z";

    private TemaPAE() {}

    public static boolean isModoOscuro() {
        return modoOscuro;
    }

    public static void setModoOscuro(boolean oscuro) {
        modoOscuro = oscuro;
        VentanaManager.actualizarTemaGlobal();
    }

    public static void alternarModoOscuro() {
        setModoOscuro(!modoOscuro);
    }

    public static boolean usaTema(Parent raiz) {
        return tiene(raiz, "pae-ui");
    }

    /**
     * Construye la barra superior con navegación, marca y botón de modo oscuro.
     */
    public static HBox barra(String modulo, Runnable volver) {
        HBox barra = new HBox(16);
        barra.getStyleClass().add("barra-aplicacion");

        StackPane marca = new StackPane();
        marca.getStyleClass().add("marca");

        Region divisor = new Region();
        divisor.setMinSize(1, 23);
        divisor.setMaxSize(1, 23);
        fondo(divisor, borde(), 0);

        Label nombre = new Label("ProyectoPAE");
        nombre.getStyleClass().add("marca-texto");

        Label seccion = new Label(modulo);
        seccion.getStyleClass().add("barra-seccion");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Button btnTema = crearBotonTema();

        Button menu = new Button("Menú principal");
        menu.getStyleClass().add("btn-secondary");
        menu.setGraphic(icono("M14 5 L7 12 L14 19 M7 12 H21", ciruela(), 16));
        menu.setOnAction(evento -> volver.run());

        barra.getChildren().setAll(new HBox(11, marca, nombre), divisor, seccion, espacio, btnTema, menu);
        ((HBox) barra.getChildren().getFirst()).setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    /**
     * Crea un botón visualmente reactivo para alternar entre modo claro y oscuro.
     */
    public static Button crearBotonTema() {
        Button btn = new Button();
        btn.getStyleClass().add("btn-tema");
        actualizarBotonTema(btn);
        btn.setOnAction(e -> alternarModoOscuro());
        return btn;
    }

    public static void actualizarBotonTema(Button btn) {
        FontIcon icono = new FontIcon(modoOscuro ? "fas-sun" : "fas-moon");
        icono.setIconSize(14);
        icono.setIconColor(modoOscuro ? Color.web("#FCD34D") : ciruela());
        btn.setGraphic(icono);
        btn.setText(modoOscuro ? "Modo claro" : "Modo oscuro");
        btn.setTooltip(new Tooltip(modoOscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro"));
        letra(btn, 12, FontWeight.BOLD, modoOscuro ? Color.web("#FCD34D") : ciruela());
        fondo(btn, modoOscuro ? Color.web("#38233C") : Color.web("#F2E6F3"), 18);
        borde(btn, modoOscuro ? Color.web("#5E3E66") : Color.web("#D9BADB"), 18);
        btn.setPadding(new Insets(5, 12, 5, 12));
        btn.setCursor(Cursor.HAND);
    }

    public static void aplicar(Parent raiz) {
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
            if (!tiene(icono.getParent(), "btn-tema")) {
                icono.setIconColor(ciruela());
            }
        }

        // Las pieles de los controles y sus nodos internos se crean al mostrar la escena.
        if (nodo instanceof ScrollPane scroll && scroll.getContent() != null) {
            fondo(scroll, fondo(), 0);
            scroll.skinProperty().addListener((o, antes, ahora) -> Platform.runLater(() -> {
                if (scroll.lookup(".viewport") instanceof Region viewport) fondo(viewport, fondo(), 0);
            }));
            pintar(scroll.getContent());
        } else if (nodo instanceof Parent padre && !(nodo instanceof Control)) {
            for (Node hijo : padre.getChildrenUnmodifiable()) pintar(hijo);
        }
    }

    private static void pintarRegion(Region r) {
        if (tiene(r, "pae-ui", "content-area")) fondo(r, fondo(), 0);
        if (tiene(r, "content-area")) r.setPadding(new Insets(18, 36, 28, 36));
        if (tiene(r, "barra-aplicacion")) {
            fondo(r, blanco(), 0);
            r.setBorder(new Border(new BorderStroke(
                    null, null, borde(), null,
                    BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE,
                    CornerRadii.EMPTY, new BorderWidths(1), Insets.EMPTY
            )));
            r.setPadding(new Insets(0, 30, 0, 30));
            r.setMinHeight(65);
            if (r instanceof HBox caja) caja.setAlignment(Pos.CENTER_LEFT);
        }
        if (tiene(r, "encabezado-modulo")) {
            r.setPadding(new Insets(26, 36, 0, 36));
        }
        if (tiene(r, "card", "login-card", "kpi-card")) {
            fondo(r, blanco(), 16);
            borde(r, borde(), 16);
            r.setPadding(new Insets(tiene(r, "login-card") ? 32 : 20));
            r.setEffect(new DropShadow(10, 0, 3, modoOscuro ? Color.rgb(0, 0, 0, 0.40) : Color.rgb(0, 0, 0, 0.05)));
        }
        if (tiene(r, "mini-kpi")) {
            fondo(r, modoOscuro ? Color.web("#221626") : Color.web("#FFFFFF"), 14);
            borde(r, borde(), 14);
            r.setPadding(new Insets(12, 16, 12, 16));
            r.setEffect(new DropShadow(8, 0, 2, modoOscuro ? Color.rgb(0, 0, 0, 0.35) : Color.rgb(0, 0, 0, 0.04)));
        }
        if (tiene(r, "panel-acceso", "bienvenida")) {
            fondo(r, bannerFondo(), tiene(r, "bienvenida") ? 16 : 0);
            if (tiene(r, "bienvenida")) {
                borde(r, modoOscuro ? Color.web("#3E2445") : Color.TRANSPARENT, 16);
                r.setEffect(new DropShadow(14, 0, 4, modoOscuro ? Color.rgb(0, 0, 0, 0.50) : Color.rgb(50, 25, 55, 0.18)));
            }
        }
        if (tiene(r, "decoracion-acceso", "decoracion-menu") && r instanceof Pane panel) {
            decorar(panel, tiene(r, "decoracion-acceso"));
        }
        if (tiene(r, "marca", "marca-clara", "marca-acceso") && r instanceof StackPane marca) {
            boolean clara = !tiene(r, "marca");
            fondo(r, clara ? lila() : ciruela(), 11);
            marca.getChildren().setAll(icono(ESTRELLA, clara ? ciruela() : (modoOscuro ? Color.web("#140A16") : Color.WHITE), 21));
        }
        if (tiene(r, "chip-lila")) fondo(r, lila(), 9);
        if (tiene(r, "avatar")) fondo(r, lila(), 17);
        if (tiene(r, "chip-rosa", "fila-peligro")) fondo(r, rosa(), 9);
        if (tiene(r, "chip-verde")) fondo(r, verde(), 9);
        if (tiene(r, "chip-ambar", "fila-advertencia")) fondo(r, ambar(), 9);
        if (tiene(r, "chip-azul")) fondo(r, azul(), 9);
        if (tiene(r, "chip-turquesa")) fondo(r, turquesa(), 9);

        if (tiene(r, "alerta-menu")) {
            fondo(r, ambar(), 12);
            borde(r, modoOscuro ? Color.web("#6B4715") : Color.web("#F6D89B"), 12);
            r.setPadding(new Insets(12, 16, 12, 16));
        }
        if (tiene(r, "resumen-total", "estado-reporte")) {
            fondo(r, lila(), 10);
            borde(r, borde(), 10);
            r.setPadding(new Insets(12, 14, 12, 14));
        }
        if (tiene(r, "separador")) fondo(r, borde(), 0);
    }

    private static void pintarEtiqueta(Label l) {
        if (tiene(l, "marca-texto")) letra(l, 19, FontWeight.BOLD, texto());
        else if (tiene(l, "marca-invertida")) letra(l, 19, FontWeight.BOLD, Color.WHITE);
        else if (tiene(l, "titulo-marca-acceso")) letra(l, 54, FontWeight.BOLD, Color.WHITE);
        else if (tiene(l, "bienvenida-titulo")) letra(l, 36, FontWeight.BOLD, Color.WHITE);
        else if (tiene(l, "bienvenida-meta")) letra(l, 13, FontWeight.SEMI_BOLD, claro());
        else if (tiene(l, "bienvenida-subtitulo")) letra(l, 15, FontWeight.NORMAL, claro());
        else if (tiene(l, "header-title", "login-title")) letra(l, 32, FontWeight.BOLD, texto());
        else if (tiene(l, "header-subtitle")) letra(l, 15, FontWeight.NORMAL, secundario());
        else if (tiene(l, "card-title")) letra(l, 19, FontWeight.BOLD, texto());
        else if (tiene(l, "menu-card-title")) letra(l, 16, FontWeight.BOLD, texto());
        else if (tiene(l, "kpi-valor")) letra(l, 32, FontWeight.BOLD, texto());
        else if (tiene(l, "kpi-valor-mini")) letra(l, 20, FontWeight.BOLD, texto());
        else if (tiene(l, "kpi-titulo", "barra-seccion")) letra(l, 13, FontWeight.SEMI_BOLD, secundario());
        else if (tiene(l, "kpi-titulo-mini")) letra(l, 12, FontWeight.SEMI_BOLD, secundario());
        else if (tiene(l, "total-venta")) letra(l, 25, FontWeight.BOLD, ciruela());
        else if (tiene(l, "form-label", "usuario-menu")) letra(l, 13, FontWeight.BOLD, texto());
        else if (tiene(l, "modo-label")) letra(l, 13, FontWeight.SEMI_BOLD, ciruela());
        else if (tiene(l, "login-error")) letra(l, 13, FontWeight.SEMI_BOLD, rosaTexto());
        else if (tiene(l, "alerta-titulo")) letra(l, 14, FontWeight.BOLD, ambarTexto());
        else if (tiene(l, "alerta-texto")) letra(l, 13, FontWeight.NORMAL, ambarTexto());
        else if (tiene(l, "badge-verde")) {
            letra(l, 12, FontWeight.BOLD, verdeTexto());
            l.setPadding(new Insets(5, 10, 5, 10));
            fondo(l, verde(), 20);
        } else if (tiene(l, "badge-lila")) {
            letra(l, 12, FontWeight.BOLD, ciruela());
            l.setPadding(new Insets(5, 10, 5, 10));
            fondo(l, lila(), 20);
        } else if (tiene(l, "badge-ambar")) {
            letra(l, 12, FontWeight.BOLD, ambarTexto());
            l.setPadding(new Insets(5, 10, 5, 10));
            fondo(l, ambar(), 20);
        } else if (tiene(l, "badge-rosa")) {
            letra(l, 12, FontWeight.BOLD, rosaTexto());
            l.setPadding(new Insets(5, 10, 5, 10));
            fondo(l, rosa(), 20);
        } else if (tiene(l, "badge-azul")) {
            letra(l, 12, FontWeight.BOLD, azulTexto());
            l.setPadding(new Insets(5, 10, 5, 10));
            fondo(l, azul(), 20);
        } else if (tiene(l, "menu-card-subtitle", "hint-label")) letra(l, 13, FontWeight.NORMAL, secundario());
        else letra(l, 13, FontWeight.NORMAL, texto());
    }

    private static void pintarBoton(Button boton) {
        if (tiene(boton, "btn-tema")) {
            actualizarBotonTema(boton);
            return;
        }

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

        if (tiene(boton, "quick-action-btn")) {
            boton.setCursor(Cursor.HAND);
            boton.setPadding(new Insets(8, 14, 8, 14));
            letra(boton, 13, FontWeight.SEMI_BOLD, texto());
            Runnable pintarQuick = () -> {
                boolean hover = boton.isHover();
                fondo(boton, hover ? lila() : blanco(), 20);
                borde(boton, hover ? ciruela() : borde(), 20);
                boton.setEffect(new DropShadow(hover ? 8 : 4, 0, 2, modoOscuro ? Color.rgb(0, 0, 0, 0.3) : Color.rgb(0, 0, 0, 0.05)));
            };
            pintarQuick.run();
            boton.hoverProperty().addListener((o, a, b) -> pintarQuick.run());
            if (boton.getGraphic() instanceof FontIcon fi) fi.setIconColor(ciruela());
            return;
        }

        boolean principal = tiene(boton, "btn-primary");
        boolean peligro = tiene(boton, "btn-danger");
        boolean discreto = tiene(boton, "btn-ghost");
        Color tinta = principal ? (modoOscuro ? Color.web("#140A16") : Color.WHITE) : peligro ? rosaTexto() : ciruela();
        letra(boton, 13, FontWeight.BOLD, tinta);
        if (boton.getGraphic() instanceof FontIcon icono) icono.setIconColor(tinta);
        boton.setMinHeight(40);
        boton.setPadding(new Insets(0, 14, 0, 14));
        boton.setCursor(Cursor.HAND);

        Runnable actualizar = () -> {
            Color color = principal
                    ? (boton.isHover() ? ciruelaOscuro() : ciruela())
                    : boton.isHover() ? (peligro ? rosa() : lila()) : discreto ? Color.TRANSPARENT : blanco();
            fondo(boton, color, 9);
            Color contorno = boton.isFocused() ? (principal ? lilaBorde() : ciruela())
                    : principal || discreto ? Color.TRANSPARENT : borde();
            borde(boton, contorno, 9);
            boton.setEffect(new DropShadow(boton.isHover() ? 8 : 0, 0, 2,
                    modoOscuro ? Color.rgb(0, 0, 0, 0.4) : Color.rgb(91, 45, 98, 0.15)));
        };
        actualizar.run();
        boton.hoverProperty().addListener((o, antes, ahora) -> actualizar.run());
        boton.focusedProperty().addListener((o, antes, ahora) -> actualizar.run());
    }

    private static void actualizarTarjeta(Button b) {
        fondo(b, b.isHover() ? lila() : blanco(), 16);
        borde(b, b.isFocused() || b.isHover() ? ciruela() : borde(), 16);
        b.setEffect(new DropShadow(b.isHover() ? 12 : 5, 0, b.isHover() ? 4 : 2,
                modoOscuro ? Color.rgb(0, 0, 0, 0.45) : (b.isHover() ? Color.rgb(91, 45, 98, 0.18) : Color.rgb(0, 0, 0, 0.04))));
    }

    private static void enfocable(Region control) {
        fondo(control, blanco(), 9);
        borde(control, borde(), 9);
        control.focusedProperty().addListener((o, antes, ahora) -> borde(control, ahora ? ciruela() : borde(), 9));
    }

    private static void pintarCasilla(CheckBox control) {
        letra(control, 14, FontWeight.NORMAL, texto());
        Runnable pintar = () -> {
            if (control.lookup(".box") instanceof Region caja) {
                fondo(caja, control.isSelected() ? ciruela() : blanco(), 4);
                borde(caja, control.isFocused() || control.isSelected() ? ciruela() : secundario(), 4);
            }
            if (control.lookup(".mark") instanceof Region marca) {
                fondo(marca, control.isSelected() ? (modoOscuro ? Color.web("#140A16") : Color.WHITE) : Color.TRANSPARENT, 0);
            }
        };
        control.skinProperty().addListener((o, antes, ahora) -> Platform.runLater(pintar));
        control.selectedProperty().addListener((o, antes, ahora) -> pintar.run());
        control.focusedProperty().addListener((o, antes, ahora) -> pintar.run());
    }

    private static <T> void pintarTabla(TableView<T> tabla) {
        fondo(tabla, blanco(), 10);
        borde(tabla, borde(), 10);
        tabla.setFixedCellSize(47);
        if (tabla.getPlaceholder() != null) pintar(tabla.getPlaceholder());

        var factory = tabla.getRowFactory();
        tabla.setRowFactory(t -> {
            TableRow<T> fila = factory == null ? new TableRow<>() : factory.call(t);
            Runnable colorFila = () -> {
                Color color = fila.isEmpty() ? blanco()
                        : fila.isSelected() ? ciruela()
                        : tiene(fila, "fila-peligro") ? rosa()
                        : tiene(fila, "fila-advertencia") ? ambar()
                        : fila.getIndex() % 2 == 0 ? blanco() : filaAlterna();
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
                    if (nodo instanceof Region region) fondo(region, neutro(), 0);
                    if (nodo instanceof Region region && selector.equals(".column-header")) {
                        region.setPrefHeight(39);
                        if (region.lookup(".label") instanceof Label titulo) {
                            letra(titulo, 12, FontWeight.BOLD, secundario());
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
        panel.getChildren().clear();

        if (acceso) {
            agregarElipse(panel, 0.80, 0.11, 271, null, modoOscuro ? 0.20 : 0.35);
            agregarElipse(panel, 0.80, 0.11, 230, null, modoOscuro ? 0.15 : 0.23);
            agregarElipse(panel, 0.93, 0.80, 211, ciruela(), modoOscuro ? 0.40 : 0.62);
            agregarElipse(panel, 0.89, 0.79, 174, null, modoOscuro ? 0.30 : 0.52);
            agregarElipse(panel, 0.89, 0.79, 154, null, modoOscuro ? 0.18 : 0.30);
            agregarEstrella(panel, 0.77, 0.76, 104, Color.web("#F1A5CC"));
            agregarEstrella(panel, 0.89, 0.67, 30, claro());
            agregarEstrella(panel, 0.62, 0.86, 22, claro());
        } else {
            agregarElipse(panel, 0.93, 0.36, 228, null, modoOscuro ? 0.22 : 0.35);
            agregarElipse(panel, 0.93, 0.40, 194, null, modoOscuro ? 0.16 : 0.26);
            agregarEstrella(panel, 0.925, 0.52, 88, Color.web("#F1A5CC"));
        }
    }

    private static void agregarElipse(Pane panel, double x, double y, double radio, Color color, double opacidad) {
        Ellipse elipse = new Ellipse(radio, radio);
        elipse.setFill(color == null ? Color.TRANSPARENT : color);
        if (color == null) elipse.setStroke(lilaBorde());
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
