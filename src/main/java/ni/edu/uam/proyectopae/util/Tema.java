package ni.edu.uam.proyectopae.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Arrays;
import java.util.List;

/**
 * Diseño visual del sistema escrito 100 % en Java (sin hojas de estilo).
 * Los FXML marcan cada elemento con {@code <properties estilo="card"/>} y esta clase
 * recorre la vista aplicando fondos, bordes, fuentes y colores con la API de JavaFX.
 */
public final class Tema {

    // ------------------------------------------------------------ Paleta
    public static final Color PRIMARIO = Color.web("#8e3b6b");
    public static final Color PRIMARIO_OSCURO = Color.web("#6d2b52");
    public static final Color FONDO = Color.web("#f4f1f6");
    public static final Color BORDE = Color.web("#e2dbe8");
    public static final Color TEXTO = Color.web("#2f2838");
    public static final Color TEXTO_SUAVE = Color.web("#6f6679");
    public static final Color OK = Color.web("#2e7d32");
    public static final Color ALERTA = Color.web("#ef6c00");
    public static final Color PELIGRO = Color.web("#c62828");
    public static final Color INFO = Color.web("#1565c0");
    public static final Color ICONO = Color.web("#6f6679");
    private static final Color SIDEBAR = Color.web("#2d2438");
    private static final Color SIDEBAR_HOVER = Color.web("#3d3249");
    private static final Color SELECCION = Color.web("#8e3b6b");
    private static final Color SELECCION_INACTIVA = Color.web("#e8d5e2");

    private static final String FUENTE = "Segoe UI";
    private static final String PROPIEDAD = "estilo";
    private static final String APLICADO = "tema.aplicado";

    /** Estados de una etiqueta o fila: definen colores de fondo, texto e icono. */
    public enum Estado {
        OK("#e5f3e6", "#2e7d32", "#fbfefb"),
        ALERTA("#ffe9d1", "#a64b00", "#fff6e9"),
        PELIGRO("#fbd9d9", "#c62828", "#fdecec"),
        INFO("#e3edf9", "#1565c0", "#f3f8fd"),
        NEUTRO("#eeeaf1", "#6f6679", "#ffffff");

        final Color fondo;
        final Color texto;
        final Color fila;

        Estado(String fondo, String texto, String fila) {
            this.fondo = Color.web(fondo);
            this.texto = Color.web(texto);
            this.fila = Color.web(fila);
        }

        public Color getTexto() { return texto; }
    }

    private Tema() {}

    // ------------------------------------------------------------ Punto de entrada

    public static void aplicar(Scene scene) {
        scene.setFill(FONDO);
        if (scene.getRoot() instanceof Region raiz && raiz.getBackground() == null) {
            raiz.setBackground(fondo(FONDO, 0));
        }
        aplicar(scene.getRoot());
    }

    public static void aplicar(Node raiz) {
        recorrer(raiz, null);
    }

    private static void recorrer(Node nodo, Color colorIconos) {
        if (nodo == null) return;
        Color contexto = colorIconos;
        if (!Boolean.TRUE.equals(nodo.getProperties().get(APLICADO))) {
            nodo.getProperties().put(APLICADO, true);
            estilizarControlBase(nodo);
            contexto = estilizar(nodo, estilos(nodo), colorIconos);
        } else {
            contexto = colorContexto(estilos(nodo), colorIconos);
        }

        if (nodo instanceof Icono icono && contexto != null) icono.setColorContexto(contexto);
        if (nodo instanceof Labeled l) recorrer(l.getGraphic(), contexto);
        if (nodo instanceof TableView<?> t) recorrer(t.getPlaceholder(), contexto);
        if (nodo instanceof ListView<?> lv) recorrer(lv.getPlaceholder(), contexto);
        if (nodo instanceof ScrollPane sp) {
            sp.setBackground(fondo(FONDO, 0));
            recorrer(sp.getContent(), contexto);
        }
        if (nodo instanceof MenuBar barra) {
            barra.setBackground(fondo(Color.WHITE, 0));
            barra.setBorder(bordeInferior());
            barra.getMenus().forEach(Tema::colorearMenu);
        }
        if (nodo instanceof Parent p && !(nodo instanceof Control)) {
            for (Node hijo : p.getChildrenUnmodifiable()) recorrer(hijo, contexto);
        }
    }

    private static List<String> estilos(Node nodo) {
        Object valor = nodo.getProperties().get(PROPIEDAD);
        return valor == null ? List.of() : Arrays.asList(valor.toString().trim().split("[\\s,]+"));
    }

    /** Ajustes comunes a todos los campos de formulario. */
    private static void estilizarControlBase(Node nodo) {
        if (nodo instanceof TextField campo) {
            campo.setPrefHeight(34);
        } else if (nodo instanceof ComboBoxBase<?> combo) {
            combo.setPrefHeight(34);
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setBackground(new Background(
                    new BackgroundFill(Color.web("#cfc5d8"), new CornerRadii(6), Insets.EMPTY),
                    new BackgroundFill(Color.WHITE, new CornerRadii(5), new Insets(1))));
        }
    }

    /** Aplica el estilo indicado por la propiedad "estilo" y devuelve el color de iconos para sus hijos. */
    private static Color estilizar(Node nodo, List<String> estilos, Color colorIconos) {
        Color contexto = colorContexto(estilos, colorIconos);
        for (String estilo : estilos) {
            switch (estilo) {
                case "card" -> tarjeta((Region) nodo, 16);
                case "kpi" -> {
                    VBox kpi = (VBox) nodo;
                    tarjeta(kpi, 0);
                    kpi.setPadding(new Insets(14, 16, 14, 16));
                    kpi.setSpacing(4);
                    kpi.setMinWidth(150);
                }
                case "kpi-info" -> acentoIzquierdo((Region) nodo, PRIMARIO);
                case "kpi-ok" -> acentoIzquierdo((Region) nodo, OK);
                case "kpi-alerta" -> acentoIzquierdo((Region) nodo, ALERTA);
                case "kpi-peligro" -> acentoIzquierdo((Region) nodo, PELIGRO);
                case "kpi-etiqueta" -> texto((Labeled) nodo, 12, false, TEXTO_SUAVE, 8);
                case "kpi-valor" -> texto((Labeled) nodo, 22, true, TEXTO, 0);
                case "titulo-seccion" -> texto((Labeled) nodo, 15, true, TEXTO, 8);
                case "titulo-grupo" -> {
                    texto((Labeled) nodo, 18, true, PRIMARIO_OSCURO, 10);
                    ((Labeled) nodo).setPadding(new Insets(6, 0, 0, 0));
                }
                case "titulo-modulo" -> texto((Labeled) nodo, 20, true, TEXTO, 0);
                case "titulo-login" -> texto((Labeled) nodo, 20, true, TEXTO, 0);
                case "subtitulo-modulo" -> texto((Labeled) nodo, 12.5, false, TEXTO_SUAVE, 0);
                case "texto-ayuda" -> texto((Labeled) nodo, 12, false, TEXTO_SUAVE, 0);
                case "texto-error" -> ((Labeled) nodo).setTextFill(PELIGRO);
                case "etiqueta-campo" -> texto((Labeled) nodo, 12.5, true, TEXTO, 6);
                case "total-venta" -> texto((Labeled) nodo, 20, true, PRIMARIO_OSCURO, 0);
                case "indicador-modo" -> indicadorModo((Labeled) nodo, false);
                case "encabezado-modulo" -> {
                    HBox caja = (HBox) nodo;
                    caja.setBackground(fondo(Color.WHITE, 0));
                    caja.setBorder(bordeInferior());
                    caja.setPadding(new Insets(14, 22, 14, 22));
                    caja.setSpacing(14);
                }
                case "barra-superior" -> {
                    HBox caja = (HBox) nodo;
                    caja.setBackground(fondo(Color.WHITE, 0));
                    caja.setBorder(bordeInferior());
                    caja.setPadding(new Insets(12, 24, 12, 24));
                    caja.setSpacing(12);
                }
                case "contenido-modulo" -> {
                    HBox caja = (HBox) nodo;
                    caja.setPadding(new Insets(18));
                    caja.setSpacing(16);
                }
                case "dashboard-contenido" -> {
                    VBox caja = (VBox) nodo;
                    caja.setPadding(new Insets(20, 24, 28, 24));
                    caja.setSpacing(18);
                    caja.setBackground(fondo(FONDO, 0));
                }
                case "sidebar" -> {
                    VBox caja = (VBox) nodo;
                    caja.setBackground(fondo(SIDEBAR, 0));
                    caja.setPadding(new Insets(18, 12, 18, 12));
                    caja.setSpacing(4);
                    caja.setPrefWidth(230);
                }
                case "sidebar-titulo" -> texto((Labeled) nodo, 16, true, Color.WHITE, 10);
                case "sidebar-seccion" -> {
                    texto((Labeled) nodo, 11, true, Color.web("#9d8fae"), 0);
                    ((Labeled) nodo).setPadding(new Insets(14, 8, 4, 8));
                }
                case "sidebar-usuario" -> {
                    texto((Labeled) nodo, 12, false, Color.web("#cbbfd8"), 8);
                    ((Labeled) nodo).setPadding(new Insets(4, 8, 4, 8));
                }
                case "nav-button" -> botonNavegacion((Button) nodo, estilos.contains("activo"));
                case "btn-primario" -> boton((Button) nodo, PRIMARIO, PRIMARIO_OSCURO, Color.WHITE, null);
                case "btn-peligro" -> boton((Button) nodo, PELIGRO, Color.web("#9f1f1f"), Color.WHITE, null);
                case "btn-exito" -> boton((Button) nodo, OK, Color.web("#1f5e23"), Color.WHITE, null);
                case "btn-secundario" -> boton((Button) nodo, Color.WHITE, Color.web("#f3eef7"), TEXTO, BORDE);
                case "fondo-login" -> ((Region) nodo).setBackground(new Background(new BackgroundFill(
                        new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                                new Stop(0, PRIMARIO_OSCURO), new Stop(1, SIDEBAR)),
                        CornerRadii.EMPTY, Insets.EMPTY)));
                case "icono-encabezado", "icono-marca" -> ((Icono) nodo).setColor(PRIMARIO);
                default -> { /* marcador sin estilo propio (p. ej. "activo") */ }
            }
        }
        return contexto;
    }

    /** Color que heredan los iconos dentro de un elemento (botones, títulos, barra lateral...). */
    private static Color colorContexto(List<String> estilos, Color heredado) {
        for (String estilo : estilos) {
            switch (estilo) {
                case "btn-primario", "btn-peligro", "btn-exito" -> { return Color.WHITE; }
                case "btn-secundario", "titulo-seccion", "etiqueta-campo" -> { return PRIMARIO; }
                case "titulo-grupo", "indicador-modo" -> { return PRIMARIO_OSCURO; }
                case "nav-button" -> { return estilos.contains("activo") ? Color.WHITE : Color.web("#b9a9ca"); }
                case "sidebar-titulo" -> { return Color.web("#e7a6cc"); }
                case "sidebar-usuario" -> { return Color.web("#cbbfd8"); }
                case "kpi-info" -> { return PRIMARIO; }
                case "kpi-ok" -> { return OK; }
                case "kpi-alerta" -> { return ALERTA; }
                case "kpi-peligro" -> { return PELIGRO; }
                default -> { }
            }
        }
        return heredado;
    }

    private static void colorearMenu(MenuItem item) {
        if (item.getGraphic() instanceof Icono icono) icono.setColorContexto(PRIMARIO);
        if (item instanceof Menu menu) menu.getItems().forEach(Tema::colorearMenu);
    }

    // ------------------------------------------------------------ Componentes reutilizables

    /** Etiqueta pequeña de estado con icono y color (usada en tablas). */
    public static Label badge(String texto, Estado estado, Icono.Tipo tipoIcono) {
        Icono icono = new Icono(tipoIcono, 15);
        icono.setColor(estado.texto);
        Label etiqueta = new Label(texto, icono);
        etiqueta.setFont(Font.font(FUENTE, FontWeight.BOLD, 11.5));
        etiqueta.setTextFill(estado.texto);
        etiqueta.setBackground(fondo(estado.fondo, 10));
        etiqueta.setPadding(new Insets(2, 9, 2, 7));
        etiqueta.setGraphicTextGap(5);
        return etiqueta;
    }

    /** Indicador "Nuevo registro" / "Editando #id" de los formularios con botón Guardar. */
    public static void indicadorModo(Labeled etiqueta, boolean editando) {
        Color texto = editando ? Color.web("#a64b00") : PRIMARIO_OSCURO;
        etiqueta.setBackground(fondo(editando ? Color.web("#fff3e0") : Color.web("#ede7f2"), 14));
        etiqueta.setPadding(new Insets(5, 12, 5, 12));
        etiqueta.setFont(Font.font(FUENTE, FontWeight.BOLD, 12));
        etiqueta.setTextFill(texto);
        etiqueta.setGraphicTextGap(6);
        if (etiqueta.getGraphic() instanceof Icono icono) icono.setColor(texto);
    }

    /** Color de fondo de una fila de tabla según su estado (null = fila normal). */
    public static Background fondoFila(Estado estado, boolean seleccionada, boolean tablaEnfocada) {
        if (seleccionada) return fondo(tablaEnfocada ? SELECCION : SELECCION_INACTIVA, 0);
        return fondo(estado != null ? estado.fila : Color.WHITE, 0);
    }

    /** Celda de fecha deshabilitada (días anteriores a hoy en los DatePicker). */
    public static void diaDeshabilitado(DateCell celda) {
        celda.setBackground(fondo(Color.web("#eeeeee"), 0));
        celda.setTextFill(Color.web("#999999"));
    }

    public static void textoMonto(Labeled etiqueta, boolean negativo) {
        etiqueta.setTextFill(negativo ? PELIGRO : PRIMARIO_OSCURO);
    }

    // ------------------------------------------------------------ Ayudantes

    private static void tarjeta(Region region, double relleno) {
        region.setBackground(fondo(Color.WHITE, 10));
        region.setBorder(new Border(new BorderStroke(BORDE, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT)));
        if (relleno > 0) region.setPadding(new Insets(relleno));
        region.setEffect(new DropShadow(10, 0, 2, Color.rgb(60, 30, 80, 0.08)));
    }

    private static void acentoIzquierdo(Region region, Color color) {
        region.setBorder(new Border(new BorderStroke(BORDE, BORDE, BORDE, color,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(1, 1, 1, 4), Insets.EMPTY)));
    }

    private static void texto(Labeled etiqueta, double tamano, boolean negrita, Color color, double espacioIcono) {
        etiqueta.setFont(Font.font(FUENTE, negrita ? FontWeight.BOLD : FontWeight.NORMAL, tamano));
        etiqueta.setTextFill(color);
        if (espacioIcono > 0) etiqueta.setGraphicTextGap(espacioIcono);
    }

    private static void boton(Button boton, Color fondo, Color fondoHover, Color texto, Color borde) {
        boton.setFont(Font.font(FUENTE, FontWeight.BOLD, 13));
        boton.setTextFill(texto);
        boton.setCursor(Cursor.HAND);
        boton.setGraphicTextGap(8);
        boton.setPadding(new Insets(8, 16, 8, 14));
        boton.setMinWidth(Region.USE_PREF_SIZE);
        if (borde != null) {
            boton.setBorder(new Border(new BorderStroke(borde, BorderStrokeStyle.SOLID, new CornerRadii(6), BorderWidths.DEFAULT)));
        }
        Runnable pintar = () -> boton.setBackground(fondo(boton.isHover() ? fondoHover : fondo, 6));
        pintar.run();
        boton.hoverProperty().addListener((obs, antes, ahora) -> pintar.run());
    }

    private static void botonNavegacion(Button boton, boolean activo) {
        boton.setFont(Font.font(FUENTE, activo ? FontWeight.BOLD : FontWeight.NORMAL, 13.5));
        boton.setTextFill(activo ? Color.WHITE : Color.web("#e4dcec"));
        boton.setAlignment(Pos.CENTER_LEFT);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setPadding(new Insets(10, 12, 10, 12));
        boton.setGraphicTextGap(12);
        boton.setCursor(Cursor.HAND);
        Color normal = activo ? PRIMARIO : Color.TRANSPARENT;
        Color hover = activo ? PRIMARIO : SIDEBAR_HOVER;
        Runnable pintar = () -> boton.setBackground(fondo(boton.isHover() ? hover : normal, 6));
        pintar.run();
        boton.hoverProperty().addListener((obs, antes, ahora) -> pintar.run());
    }

    private static Border bordeInferior() {
        return new Border(new BorderStroke(Color.TRANSPARENT, Color.TRANSPARENT, BORDE, Color.TRANSPARENT,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 0, 1, 0), Insets.EMPTY));
    }

    public static Background fondo(Paint color, double radio) {
        return new Background(new BackgroundFill(color, new CornerRadii(radio), Insets.EMPTY));
    }
}
