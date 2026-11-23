package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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
        return modoOscuro ? Color.web("#17141C") : Color.web("#F8F6F7");
    }

    public static Color blanco() {
        return modoOscuro ? Color.web("#25212B") : Color.WHITE;
    }

    public static Color texto() {
        return modoOscuro ? Color.web("#F6EEF7") : Color.web("#281D2B");
    }

    public static Color secundario() {
        return modoOscuro ? Color.web("#C3B9C8") : Color.web("#615567");
    }

    public static Color borde() {
        return modoOscuro ? Color.web("#49404E") : Color.web("#DFD9E2");
    }

    public static Color ciruela() {
        return modoOscuro ? Color.web("#E9A9CA") : Color.web("#87395F");
    }

    public static Color ciruelaOscuro() {
        return modoOscuro ? Color.web("#F4C6DC") : Color.web("#321937");
    }

    public static Color bannerFondo() {
        return modoOscuro ? Color.web("#140A16") : Color.web("#321937");
    }

    public static Color lila() {
        return modoOscuro ? Color.web("#482C40") : Color.web("#F7E8EE");
    }

    public static Color lilaBorde() {
        return modoOscuro ? Color.web("#966783") : Color.web("#DCB5C8");
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
        if (modoOscuro == oscuro) return;
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
        divisor.getStyleClass().add("separador");

        Label nombre = new Label("ProyectoPAE");
        nombre.getStyleClass().add("marca-texto");

        Label seccion = new Label(modulo);
        seccion.getStyleClass().add("barra-seccion");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Button btnTema = crearBotonTema();

        Button menu = new Button("Menú principal");
        menu.getStyleClass().add("btn-secondary");
        menu.setGraphic(new FontIcon("fas-arrow-left"));
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
        btn.setOnAction(e -> alternarModoOscuro());
        return btn;
    }

    public static void actualizarBotonTema(Button btn) {
        String destino = modoOscuro ? "modo claro" : "modo oscuro";
        FontIcon icono = new FontIcon(modoOscuro ? "fas-sun" : "fas-moon");
        icono.setIconSize(14);
        btn.setGraphic(icono);
        btn.setText(modoOscuro ? "Modo claro" : "Modo oscuro");
        btn.setTooltip(new Tooltip("Cambiar a " + destino));
        btn.setAccessibleText("Cambiar a " + destino);
    }

    public static void aplicar(Parent raiz) {
        pintar(raiz);
    }

    private static void pintar(Node nodo) {
        if (nodo == null) {
            return;
        }

        if (nodo instanceof Region region) {
            pintarRegion(region);
        }

        if (nodo instanceof Label etiqueta) {
            pintarEtiqueta(etiqueta);
            pintar(etiqueta.getGraphic());

        } else if (nodo instanceof Button boton) {
            pintarBoton(boton);

        } else if (nodo instanceof CheckBox casilla) {
            letra(casilla, 14, FontWeight.NORMAL, texto());
            casilla.setCursor(Cursor.HAND);

        } else if (nodo instanceof TextInputControl entrada) {
            entrada.setFont(fuente(FontWeight.NORMAL, 13));
            TemaCampos.aplicar(entrada);
            enfocable(entrada);

        } else if (nodo instanceof ComboBoxBase<?> selector) {
            selector.setPrefHeight(42);

            fondo(selector, Color.WHITE, 9);
            bordeControl(selector, lilaBorde(), 9);

            alInteractuar(selector, () ->
                    bordeControl(
                            selector,
                            selector.isFocused() || selector.isShowing()
                                    ? ciruela()
                                    : lilaBorde(),
                            9
                    )
            );

            if (selector instanceof DatePicker fecha) {
                fecha.getEditor().setFont(
                        fuente(FontWeight.NORMAL, 13)
                );
            }

            if (selector instanceof ComboBox<?> combo
                    && combo.isEditable()) {
                combo.getEditor().setFont(
                        fuente(FontWeight.NORMAL, 13)
                );
            }

        } else if (nodo instanceof TableView<?> tabla) {
            pintarTabla(tabla);

        } else if (nodo instanceof FontIcon icono) {
            Node padre = icono.getParent();
            Color tinta = ciruela();

            if (tiene(padre, "chip-verde")) {
                tinta = verdeTexto();

            } else if (tiene(
                    padre,
                    "chip-ambar",
                    "alerta-menu"
            )) {
                tinta = ambarTexto();

            } else if (tiene(padre, "chip-turquesa")) {
                tinta = turquesaTexto();

            } else if (tiene(padre, "chip-rosa")) {
                tinta = rosaTexto();
            }

            icono.setIconColor(tinta);
        }

        if (nodo instanceof ScrollPane scroll) {
            fondo(scroll, fondo(), 0);
            pintar(scroll.getContent());

        } else if (nodo instanceof Pane panel) {
            for (Node hijo : panel.getChildren()) {
                pintar(hijo);
            }
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
            fondo(r, blanco(), 14);
            borde(r, borde(), 14);
            r.setPadding(new Insets(12, 16, 12, 16));
        }
        if (tiene(r, "panel-acceso", "bienvenida")) {
            LinearGradient gradiente = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, bannerFondo()), new Stop(1, Color.web(modoOscuro ? "#543247" : "#82455F")));
            r.setBackground(new Background(new BackgroundFill(gradiente,
                    new CornerRadii(tiene(r, "bienvenida") ? 16 : 0), Insets.EMPTY)));
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
        if (tiene(boton, "btn-tema")) actualizarBotonTema(boton);
        boton.setCursor(Cursor.HAND);
        if (tiene(boton, "tarjeta-menu")) {
            boton.setAlignment(Pos.CENTER_LEFT);
            boton.setMinWidth(0);
            boton.setPadding(new Insets(12, 15, 12, 15));
            if (boton.getGraphic() instanceof Region contenido) {
                if (!contenido.prefWidthProperty().isBound()) {
                    contenido.prefWidthProperty().bind(Bindings.max(100, boton.widthProperty().subtract(32)));
                }
                pintar(contenido);
            }
        } else {
            boton.setMinHeight(tiene(boton, "btn-tema") ? 34 : 40);
            boton.setPadding(new Insets(0, 13, 0, 13));
        }
        alInteractuar(boton, () -> actualizarBoton(boton));
    }

    private static void actualizarBoton(Button b) {
        boolean principal = tiene(b, "btn-primary");
        boolean peligro = tiene(b, "btn-danger");
        boolean tarjeta = tiene(b, "tarjeta-menu");
        boolean tema = tiene(b, "btn-tema");
        boolean activo = b.isHover() || b.isArmed();
        int radio = tarjeta ? 14 : tema || tiene(b, "quick-action-btn") ? 20 : 9;
        Color tinta = principal ? (modoOscuro ? bannerFondo() : Color.WHITE) : peligro ? rosaTexto() : ciruela();
        Color superficie = blanco();
        if (principal) superficie = activo ? ciruelaOscuro() : ciruela();
        else if (activo || tema) superficie = peligro ? rosa() : lila();
        else if (tiene(b, "btn-ghost")) superficie = Color.TRANSPARENT;
        fondo(b, superficie, radio);
        b.setTextFill(tinta);

        b.setFont(
                fuente(
                        FontWeight.BOLD,
                        tema ? 12 : 13
                )
        );
        Color contorno = b.isFocused() || activo ? ciruela() : borde();
        if (!b.isFocused() && (principal || tiene(b, "btn-ghost"))) contorno = Color.TRANSPARENT;
        bordeControl(b, contorno, radio);
        if (b.getGraphic() instanceof FontIcon icono) icono.setIconColor(tinta);
        b.setTranslateY(b.isArmed() ? 1 : tarjeta && b.isHover() ? -2 : 0);
    }

    private static void alInteractuar(Control control, Runnable actualizar) {
        if (control.getProperties().putIfAbsent("pae-interaccion", true) == null) {
            InvalidationListener cambio = o -> actualizar.run();
            control.focusedProperty().addListener(cambio);
            control.hoverProperty().addListener(cambio);
            if (control instanceof Button boton) boton.armedProperty().addListener(cambio);
            if (control instanceof ComboBoxBase<?> combo) combo.showingProperty().addListener(cambio);
        }
        actualizar.run();
    }

    private static void enfocable(Control control) {
        alInteractuar(control, () -> {
            boolean abierto = control instanceof ComboBoxBase<?> combo && combo.isShowing();
            fondo(control, blanco(), 9);
            bordeControl(control, control.isFocused() || abierto ? ciruela() : control.isHover() ? secundario() : lilaBorde(), 9);
        });
    }

    private static <T> void pintarTabla(TableView<T> tabla) {
        TemaTablas.aplicar(tabla);
    }

    private static void decorar(Pane panel, boolean acceso) {
        if (!panel.getChildren().isEmpty()) return;
        panel.setMouseTransparent(true);
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(panel.widthProperty());
        recorte.heightProperty().bind(panel.heightProperty());
        recorte.setArcWidth(acceso ? 0 : 32);
        recorte.setArcHeight(acceso ? 0 : 32);
        panel.setClip(recorte);

        // Polvera abierta. La ilustración queda anclada al borde, lejos del formulario.
        Group paleta = new Group();
        Color metal = Color.web("#E5BCC6");
        Circle espejo = new Circle(45, 43, 39, Color.web("#B78D9E"));
        espejo.setStroke(metal);
        espejo.setStrokeWidth(3);
        Line reflejo = new Line(26, 52, 51, 24);
        Line brillo = new Line(32, 57, 47, 40);
        for (Line trazo : new Line[]{reflejo, brillo}) {
            trazo.setStroke(Color.web("#F1DCE2"));
            trazo.setStrokeWidth(2);
        }
        Circle estuche = new Circle(94, 82, 46, Color.web("#483140"));
        estuche.setStroke(metal);
        estuche.setStrokeWidth(3);
        paleta.getChildren().addAll(espejo, reflejo, brillo, estuche);
        String[] tonos = {"#CF91A3", "#F1CAB9", "#AC7286", "#98654F"};
        for (int i = 0; i < tonos.length; i++) {
            Arc sombra = new Arc(94, 82, 38, 38, 90 * i + 2, 86);
            sombra.setType(ArcType.ROUND);
            sombra.setFill(Color.web(tonos[i]));
            paleta.getChildren().add(sombra);
        }
        paleta.setManaged(false);
        double escala = acceso ? 1.28 : 0.83;
        paleta.setScaleX(escala);
        paleta.setScaleY(escala);
        paleta.setRotate(-14);
        paleta.layoutXProperty().bind(panel.widthProperty().subtract(acceso ? 185 : 175));
        paleta.layoutYProperty().bind(acceso ? panel.heightProperty().subtract(210)
                : panel.heightProperty().subtract(128).divide(2));
        panel.getChildren().add(paleta);
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

    private static void letra(
            Labeled etiqueta,
            double tamano,
            FontWeight grosor,
            Color tinta
    ) {
        etiqueta.setFont(fuente(grosor, tamano));
        etiqueta.setTextFill(tinta);
    }
    private static void fondo(Region region, Color color, double radio) {
        region.setBackground(new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY)));
    }

    private static void borde(Region region, Color color, double radio) {
        region.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(radio), BorderWidths.DEFAULT)));
    }

    private static void bordeControl(Control control, Color contorno, int radio) {
        BorderStroke base = new BorderStroke(contorno, BorderStrokeStyle.SOLID, new CornerRadii(radio), BorderWidths.DEFAULT);
        if (control.isFocused()) {
            BorderStroke foco = new BorderStroke(ciruela(), BorderStrokeStyle.SOLID, new CornerRadii(radio + 3),
                    new BorderWidths(2), new Insets(-3));
            control.setBorder(new Border(base, foco));
        } else {
            control.setBorder(new Border(base));
        }
    }

    private static boolean tiene(Node nodo, String... clases) {
        if (nodo == null || nodo.getStyleClass() == null) return false;
        for (String clase : clases) if (nodo.getStyleClass().contains(clase)) return true;
        return false;
    }
}
