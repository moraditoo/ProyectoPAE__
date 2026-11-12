package ni.edu.uam.proyectopae.util;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** Utilidades para tablas: formatos de moneda/fecha, alineación, etiquetas de estado y color de filas. */
public final class Tablas {

    private static final DecimalFormat FORMATO_DINERO =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private Tablas() {}

    public static String dinero(BigDecimal valor) {
        BigDecimal v = valor != null ? valor.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return "C$ " + FORMATO_DINERO.format(v);
    }

    public static String fecha(LocalDate fecha) {
        return fecha != null ? FORMATO_FECHA.format(fecha) : "";
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha != null ? FORMATO_FECHA_HORA.format(fecha) : "";
    }

    /** Etiqueta de estado con icono y color. */
    public static Label badge(String texto, Tema.Estado estado, Icono.Tipo icono) {
        return Tema.badge(texto, estado, icono);
    }

    /** Columna cuyo valor se obtiene con una función (útil para records y valores calculados). */
    public static <S, T> void valor(TableColumn<S, T> columna, Function<S, T> extractor) {
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(extractor.apply(c.getValue())));
    }

    /** Muestra el valor de la columna con el formato indicado. */
    public static <S, T> void formato(TableColumn<S, T> columna, Function<T, String> formateador) {
        formato(columna, formateador, Pos.CENTER_LEFT);
    }

    public static <S, T> void formato(TableColumn<S, T> columna, Function<T, String> formateador, Pos alineacion) {
        columna.setCellFactory(c -> new TableCell<>() {
            {
                setAlignment(alineacion);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formateador.apply(item));
            }
        });
    }

    public static <S> void formatoDinero(TableColumn<S, BigDecimal> columna) {
        formato(columna, Tablas::dinero, Pos.CENTER_RIGHT);
    }

    /** Centra el contenido de una columna (cantidades, existencias). */
    public static <S, T> void centrar(TableColumn<S, T> columna) {
        formato(columna, String::valueOf, Pos.CENTER);
    }

    /** Muestra en la celda un nodo gráfico (por ejemplo una etiqueta de estado) construido a partir de la fila. */
    public static <S> void grafico(TableColumn<S, S> columna, Function<S, Node> constructor) {
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : constructor.apply(item));
            }
        });
    }

    /**
     * Colorea las filas según el estado que devuelve el clasificador (PELIGRO, ALERTA o null para normal).
     * La fila seleccionada se resalta con el color principal del sistema.
     */
    public static <S> void colorearFilas(TableView<S> tabla, Function<S, Tema.Estado> clasificador) {
        tabla.setRowFactory(t -> new TableRow<>() {
            {
                InvalidationListener repintar = obs -> pintar();
                selectedProperty().addListener(repintar);
                tabla.focusedProperty().addListener(repintar);
            }

            @Override
            protected void updateItem(S item, boolean empty) {
                super.updateItem(item, empty);
                pintar();
            }

            private void pintar() {
                S item = getItem();
                Tema.Estado estado = isEmpty() || item == null ? null : clasificador.apply(item);
                setBackground(Tema.fondoFila(estado, isSelected() && !isEmpty(), tabla.isFocused()));
            }
        });
    }
}
