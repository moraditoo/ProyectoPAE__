package ni.edu.uam.proyectopae.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Convierte un ComboBox en un selector con búsqueda: el usuario escribe parte
 * del nombre (sin importar mayúsculas ni tildes) y la lista desplegable muestra
 * solo las coincidencias. Al elegir una opción, el ComboBox conserva el objeto
 * seleccionado igual que antes, por lo que getValue() sigue funcionando.
 *
 * Uso:
 * <pre>
 * ComboBuscable&lt;Cliente&gt; buscador = ComboBuscable.aplicar(cmbCliente,
 *         Cliente::getNombreCompleto,
 *         c -&gt; c.getNombreCompleto() + " " + c.getTelefono(),
 *         "Escribe el nombre o teléfono del cliente...");
 * buscador.setDatos(clienteDAO.listarTodos(), Cliente::getIdCliente);
 * </pre>
 */
public final class ComboBuscable<T> {

    private final ComboBox<T> combo;
    private final Function<T, String> textoVisible;
    private final Function<T, String> textoBusqueda;

    private final ObservableList<T> todos = FXCollections.observableArrayList();
    private final FilteredList<T> filtrados = new FilteredList<>(todos, item -> true);

    /** Evita que los cambios hechos por esta clase se interpreten como escritura del usuario. */
    private boolean ajustando;

    private ComboBuscable(ComboBox<T> combo,
                          Function<T, String> textoVisible,
                          Function<T, String> textoBusqueda,
                          String ayuda) {
        this.combo = combo;
        this.textoVisible = textoVisible;
        this.textoBusqueda = textoBusqueda;

        combo.setEditable(true);
        combo.setItems(filtrados);
        combo.setVisibleRowCount(10);
        if (ayuda != null && !ayuda.isBlank()) {
            combo.setPromptText(ayuda);
        }

        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : texto(textoVisible, item);
            }

            @Override
            public T fromString(String texto) {
                return buscarCoincidencia(texto);
            }
        });

        TextField editor = combo.getEditor();

        // Cada vez que el usuario escribe, se filtra la lista.
        editor.textProperty().addListener((obs, anterior, texto) -> {
            if (ajustando) {
                return;
            }
            T seleccionado = combo.getValue();
            if (seleccionado != null && Objects.equals(texto(textoVisible, seleccionado), texto)) {
                return; // es el texto del elemento recién elegido, no una búsqueda
            }
            Platform.runLater(this::filtrarSegunEditor);
        });

        // Al entrar al campo se selecciona el texto para reemplazarlo fácilmente.
        editor.focusedProperty().addListener((obs, antes, enfocado) -> {
            if (enfocado) {
                Platform.runLater(editor::selectAll);
            }
        });

        // Al abrir con la flecha o con clic se ve la lista completa.
        combo.addEventHandler(ComboBoxBase.ON_SHOWING, evento -> {
            if (!ajustando && combo.getValue() != null) {
                filtrados.setPredicate(null);
            }
        });

        // Tras elegir una opción, la próxima vez se vuelve a mostrar la lista completa.
        combo.addEventHandler(ComboBoxBase.ON_HIDDEN, evento -> {
            if (!ajustando && combo.getValue() != null) {
                Platform.runLater(() -> filtrados.setPredicate(null));
            }
        });
    }

    /**
     * Activa la búsqueda en un ComboBox existente (por ejemplo, uno definido en FXML).
     *
     * @param combo         ComboBox del FXML
     * @param textoVisible  texto que aparece en el campo al elegir una opción
     * @param textoBusqueda texto en el que se busca (puede unir nombre, teléfono, marca...)
     * @param ayuda         texto guía del campo vacío
     */
    public static <T> ComboBuscable<T> aplicar(ComboBox<T> combo,
                                               Function<T, String> textoVisible,
                                               Function<T, String> textoBusqueda,
                                               String ayuda) {
        return new ComboBuscable<>(combo, textoVisible, textoBusqueda, ayuda);
    }

    /**
     * Reemplaza las opciones del ComboBox. Si había un elemento elegido y sigue
     * existiendo (misma clave), se vuelve a seleccionar.
     */
    public void setDatos(Collection<? extends T> datos, Function<T, ?> clave) {
        T anterior = combo.getValue();
        Object claveAnterior = anterior == null ? null : clave.apply(anterior);

        ajustando = true;
        try {
            combo.setValue(null);
            filtrados.setPredicate(null);
            todos.setAll(datos);
            if (claveAnterior != null) {
                todos.stream()
                        .filter(item -> claveAnterior.equals(clave.apply(item)))
                        .findFirst()
                        .ifPresent(combo::setValue);
            }
            if (combo.getValue() == null) {
                combo.getEditor().clear();
            }
        } finally {
            ajustando = false;
        }
    }

    /** Selecciona el primer elemento que cumpla la condición; si no hay, deja el campo vacío. */
    public void seleccionar(Predicate<T> condicion) {
        ajustando = true;
        try {
            filtrados.setPredicate(null);
            T encontrado = todos.stream().filter(condicion).findFirst().orElse(null);
            combo.setValue(encontrado);
            if (encontrado == null) {
                combo.getEditor().clear();
            }
        } finally {
            ajustando = false;
        }
    }

    /** Quita la selección, borra el texto escrito y vuelve a mostrar todas las opciones. */
    public void limpiar() {
        ajustando = true;
        try {
            combo.setValue(null);
            combo.getEditor().clear();
            filtrados.setPredicate(null);
            combo.hide();
        } finally {
            ajustando = false;
        }
    }

    public T getValor() {
        return combo.getValue();
    }

    /** Todos los elementos cargados, sin aplicar el filtro de búsqueda. */
    public List<T> getTodos() {
        return List.copyOf(todos);
    }

    // ---------------------------------------------------------------- internos

    private void filtrarSegunEditor() {
        TextField editor = combo.getEditor();
        String escrito = editor.getText() == null ? "" : editor.getText();

        ajustando = true;
        try {
            // El usuario cambió el texto de un elemento ya elegido: se quita la selección
            // pero se conserva lo que escribió.
            T seleccionado = combo.getValue();
            if (seleccionado != null && !Objects.equals(texto(textoVisible, seleccionado), escrito)) {
                int cursor = editor.getCaretPosition();
                combo.setValue(null);
                editor.setText(escrito);
                editor.positionCaret(Math.min(cursor, escrito.length()));
            }

            String filtro = normalizar(escrito);
            filtrados.setPredicate(filtro.isEmpty()
                    ? null
                    : item -> normalizar(texto(textoBusqueda, item)).contains(filtro));

            // Se cierra y abre para que la ventana desplegable ajuste su alto.
            if (editor.isFocused()) {
                combo.hide();
                if (!filtrados.isEmpty()) {
                    combo.show();
                }
            }
        } finally {
            ajustando = false;
        }
    }

    /** Convierte el texto escrito en un elemento al presionar Enter o salir del campo. */
    private T buscarCoincidencia(String texto) {
        String buscado = normalizar(texto);
        if (buscado.isEmpty()) {
            return null;
        }

        T actual = combo.getValue();
        if (actual != null && normalizar(texto(textoVisible, actual)).equals(buscado)) {
            return actual;
        }

        for (T item : todos) {
            if (normalizar(texto(textoVisible, item)).equals(buscado)) {
                return item;
            }
        }

        // Si lo escrito coincide con un único elemento, se elige ese.
        List<T> coincidencias = todos.stream()
                .filter(item -> normalizar(texto(textoBusqueda, item)).contains(buscado))
                .limit(2)
                .toList();
        return coincidencias.size() == 1 ? coincidencias.get(0) : null;
    }

    private static <T> String texto(Function<T, String> funcion, T item) {
        String valor = funcion.apply(item);
        return valor == null ? "" : valor;
    }

    /** Minúsculas y sin tildes: "Fátima" y "fatima" coinciden. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}