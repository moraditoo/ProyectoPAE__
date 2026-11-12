package ni.edu.uam.proyectopae.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Utilidades de formato (moneda en cordobas, porcentajes y fechas) para tablas y etiquetas. */
public final class Formato {

    private static final DecimalFormat MONEDA = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.US);

    private Formato() {}

    public static String moneda(BigDecimal valor) {
        return "C$ " + MONEDA.format(valor == null ? BigDecimal.ZERO : valor);
    }

    public static String porcentaje(BigDecimal valor) {
        return (valor == null ? "0.0" : valor.setScale(1, RoundingMode.HALF_UP).toPlainString()) + " %";
    }

    public static String fecha(LocalDate f) { return f == null ? "" : f.format(FECHA); }

    public static String fechaHora(LocalDateTime f) { return f == null ? "" : f.format(FECHA_HORA); }

    /** Convierte el resultado de un SUM/COUNT de HQL (Long, Double, BigDecimal...) a BigDecimal. */
    public static BigDecimal aBig(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor instanceof BigDecimal b) return b;
        return new BigDecimal(valor.toString());
    }

    /** Muestra una columna BigDecimal como moneda sin perder el orden numerico. */
    public static <S> void columnaMoneda(TableColumn<S, BigDecimal> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : moneda(item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    public static <S> void columnaPorcentaje(TableColumn<S, BigDecimal> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : porcentaje(item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    public static <S> void columnaFecha(TableColumn<S, LocalDate> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fecha(item));
            }
        });
    }

    public static <S> void columnaFechaHora(TableColumn<S, LocalDateTime> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fechaHora(item));
            }
        });
    }
}
