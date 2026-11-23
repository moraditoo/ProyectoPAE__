package ni.edu.uam.proyectopae.util;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.function.Predicate;

/**
 * Utilitario para aplicar estilos de estado a las filas de las tablas.
 *
 * Utiliza las clases visuales:
 * - fila-peligro
 * - fila-advertencia
 *
 * Los estilos visuales se mantienen centralizados en TemaPAE.
 */
public final class Filas {

    private static final String FILA_PELIGRO = "fila-peligro";
    private static final String FILA_ADVERTENCIA = "fila-advertencia";

    private Filas() {
        // Evita crear instancias de esta clase utilitaria.
    }

    /**
     * Crea una fábrica de filas que aplica el estilo de peligro
     * cuando se cumple la primera condición y advertencia
     * cuando se cumple la segunda.
     *
     * La condición de peligro tiene prioridad.
     *
     * @param esPeligro condición para resaltar una fila en rojo
     * @param esAdvertencia condición para resaltar una fila en amarillo
     * @param <T> tipo de objeto mostrado en la tabla
     * @return fábrica de filas configurada
     */
    public static <T> Callback<TableView<T>, TableRow<T>> porEstado(
            Predicate<T> esPeligro,
            Predicate<T> esAdvertencia
    ) {
        if (esPeligro == null || esAdvertencia == null) {
            throw new IllegalArgumentException(
                    "Las condiciones de las filas no pueden ser null."
            );
        }

        return tabla -> new TableRow<>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().removeAll(
                        FILA_PELIGRO,
                        FILA_ADVERTENCIA
                );

                if (empty || item == null) {
                    return;
                }

                if (esPeligro.test(item)) {
                    getStyleClass().add(FILA_PELIGRO);

                } else if (esAdvertencia.test(item)) {
                    getStyleClass().add(FILA_ADVERTENCIA);
                }
            }
        };
    }

    /**
     * Crea una fábrica de filas que solamente utiliza
     * el estilo de peligro.
     *
     * @param esPeligro condición para resaltar una fila
     * @param <T> tipo de objeto mostrado en la tabla
     * @return fábrica de filas configurada
     */
    public static <T> Callback<TableView<T>, TableRow<T>> peligro(
            Predicate<T> esPeligro
    ) {
        return porEstado(esPeligro, item -> false);
    }

    /**
     * Crea una fábrica de filas que solamente utiliza
     * el estilo de advertencia.
     *
     * @param esAdvertencia condición para resaltar una fila
     * @param <T> tipo de objeto mostrado en la tabla
     * @return fábrica de filas configurada
     */
    public static <T> Callback<TableView<T>, TableRow<T>> advertencia(
            Predicate<T> esAdvertencia
    ) {
        return porEstado(item -> false, esAdvertencia);
    }
}