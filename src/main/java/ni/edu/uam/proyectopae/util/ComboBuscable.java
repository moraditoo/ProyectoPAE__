package ni.edu.uam.proyectopae.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Selector con búsqueda: en la pantalla, el ComboBox del FXML se reemplaza por un
 * campo de texto. Mientras el usuario escribe, debajo aparece una lista solo con
 * las coincidencias (sin importar mayúsculas ni tildes). Al elegir una opción,
 * el objeto se guarda en el ComboBox original, por lo que el controlador sigue
 * usando cmbCliente.getValue(), valueProperty() y setCellFactory() igual que antes.
 *
 * Controles del teclado: ↓ / ↑ recorren la lista, Enter elige, Esc la cierra.
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

    private static final double ALTO_FILA = 34;
    private static final int FILAS_VISIBLES = 8;

    private final ComboBox<T> combo;
    private final Function<T, String> textoVisible;
    private final Function<T, String> textoBusqueda;

    private final TextField campo = new TextField();
    private final ListView<T> lista = new ListView<>();
    private final StackPane contenedor = new StackPane(lista);
    private final Popup ventanaLista = new Popup();

    private final ObservableList<T> todos = FXCollections.observableArrayList();
    private final FilteredList<T> filtrados = new FilteredList<>(todos, item -> true);

    /** Evita que los cambios hechos por esta clase se traten como escritura del usuario. */
    private boolean ajustando;

    private ComboBuscable(ComboBox<T> combo,
                          Function<T, String> textoVisible,
                          Function<T, String> textoBusqueda,
                          String ayuda) {
        this.combo = combo;
        this.textoVisible = textoVisible;
        this.textoBusqueda = textoBusqueda;

        combo.setItems(todos);
        prepararCampo(ayuda);
        prepararLista();
        reemplazarComboEnPantalla();
        conectarEventos();
    }

    /**
     * Activa la búsqueda sobre un ComboBox definido en el FXML.
     * Debe llamarse en initialize() del controlador.
     *
     * @param combo         ComboBox del FXML (debe estar dentro de un VBox, HBox u otro Pane)
     * @param textoVisible  texto que queda en el campo al elegir una opción
     * @param textoBusqueda texto donde se busca (puede unir nombre, teléfono, marca...)
     * @param ayuda         texto guía del campo vacío
     */
    public static <T> ComboBuscable<T> aplicar(ComboBox<T> combo,
                                               Function<T, String> textoVisible,
                                               Function<T, String> textoBusqueda,
                                               String ayuda) {
        return new ComboBuscable<>(combo, textoVisible, textoBusqueda, ayuda);
    }

    // ------------------------------------------------------------ API pública

    /**
     * Reemplaza las opciones. Si había un elemento elegido y sigue existiendo
     * (misma clave), se mantiene seleccionado.
     */
    public void setDatos(Collection<? extends T> datos, Function<T, ?> clave) {
        T anterior = combo.getValue();
        Object claveAnterior = anterior == null ? null : clave.apply(anterior);

        todos.setAll(datos);

        if (anterior != null) {
            T encontrado = todos.stream()
                    .filter(item -> Objects.equals(claveAnterior, clave.apply(item)))
                    .findFirst()
                    .orElse(null);
            fijarValor(encontrado);
        } else if (ventanaLista.isShowing()) {
            filtrar(campo.getText());
            mostrarLista();
        }
    }

    /** Selecciona el primer elemento que cumpla la condición; si no hay, deja el campo vacío. */
    public void seleccionar(Predicate<T> condicion) {
        fijarValor(todos.stream().filter(condicion).findFirst().orElse(null));
    }

    /** Quita la selección y borra el texto. */
    public void limpiar() {
        fijarValor(null);
        filtrados.setPredicate(null);
        ventanaLista.hide();
    }

    public T getValor() {
        return combo.getValue();
    }

    /** Campo de texto que se muestra en lugar del ComboBox. */
    public TextField getCampo() {
        return campo;
    }

    public List<T> getTodos() {
        return List.copyOf(todos);
    }

    // ------------------------------------------------------------ preparación

    private void prepararCampo(String ayuda) {
        campo.setPromptText(ayuda != null && !ayuda.isBlank() ? ayuda : combo.getPromptText());
        campo.setMaxWidth(combo.getMaxWidth());
        campo.setPrefWidth(combo.getPrefWidth());
        campo.setMinWidth(combo.getMinWidth());
        campo.setAccessibleText(combo.getAccessibleText());
    }

    private void prepararLista() {
        lista.setItems(filtrados);
        lista.setFocusTraversable(false);
        lista.setFixedCellSize(ALTO_FILA);
        lista.setPlaceholder(new Label("Sin coincidencias"));
        // Usa el mismo formato de filas que el controlador definió para el ComboBox
        lista.cellFactoryProperty().bind(combo.cellFactoryProperty());
        lista.prefWidthProperty().bind(campo.widthProperty());

        contenedor.setEffect(new DropShadow(12, 0, 4, Color.rgb(0, 0, 0, 0.18)));
        ventanaLista.getContent().add(contenedor);
        ventanaLista.setAutoHide(true);
        ventanaLista.setHideOnEscape(true);
        ventanaLista.setAutoFix(true);
    }

    /** Pone el campo de texto en el mismo lugar que ocupaba el ComboBox. */
    private void reemplazarComboEnPantalla() {
        Parent padre = combo.getParent();
        if (!(padre instanceof Pane panel)) {
            throw new IllegalStateException(
                    "ComboBuscable: el ComboBox debe estar dentro de un VBox, HBox u otro Pane.");
        }
        VBox.setVgrow(campo, VBox.getVgrow(combo));
        VBox.setMargin(campo, VBox.getMargin(combo));
        HBox.setHgrow(campo, HBox.getHgrow(combo));
        HBox.setMargin(campo, HBox.getMargin(combo));

        int posicion = panel.getChildren().indexOf(combo);
        panel.getChildren().set(posicion, campo);
    }

    private void conectarEventos() {
        // Cada letra escrita filtra la lista
        campo.textProperty().addListener((obs, anterior, texto) -> {
            if (ajustando) {
                return;
            }
            T seleccionado = combo.getValue();
            if (seleccionado != null && !Objects.equals(texto(textoVisible, seleccionado), texto)) {
                // El usuario cambió el texto del elemento elegido: se quita la selección
                ajustando = true;
                try {
                    combo.setValue(null);
                } finally {
                    ajustando = false;
                }
            }
            if (campo.isFocused()) {
                filtrar(texto);
                mostrarLista();
            }
        });

        // Si el controlador cambia el valor por código, el campo muestra ese valor
        combo.valueProperty().addListener((obs, anterior, valor) -> {
            if (!ajustando) {
                ajustando = true;
                try {
                    campo.setText(valor == null ? "" : texto(textoVisible, valor));
                } finally {
                    ajustando = false;
                }
            }
        });

        // Teclado: flechas recorren la lista, Enter elige, Esc cierra
        campo.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            switch (evento.getCode()) {
                case DOWN -> {
                    if (!ventanaLista.isShowing()) {
                        filtrar(combo.getValue() != null ? "" : campo.getText());
                        mostrarLista();
                    } else {
                        mover(1);
                    }
                    evento.consume();
                }
                case UP -> {
                    if (ventanaLista.isShowing()) {
                        mover(-1);
                    }
                    evento.consume();
                }
                case ENTER -> {
                    T marcado = ventanaLista.isShowing() ? lista.getSelectionModel().getSelectedItem() : null;
                    if (marcado != null) {
                        elegir(marcado);
                        evento.consume();
                    } else if (combo.getValue() == null) {
                        T unico = coincidenciaUnica(campo.getText());
                        if (unico != null) {
                            elegir(unico);
                        }
                        evento.consume();
                    }
                }
                case ESCAPE -> ventanaLista.hide();
                default -> {
                    // las demás teclas escriben normalmente
                }
            }
        });

        // Clic en el campo: abre la lista (completa si ya hay un elemento elegido)
        campo.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && !ventanaLista.isShowing()) {
                filtrar(combo.getValue() != null ? "" : campo.getText());
                mostrarLista();
            }
        });

        // Clic en una opción de la lista: la elige
        lista.setOnMouseClicked(evento -> {
            Node nodo = evento.getPickResult().getIntersectedNode();
            while (nodo != null && !(nodo instanceof ListCell<?>)) {
                nodo = nodo.getParent();
            }
            T item = lista.getSelectionModel().getSelectedItem();
            if (evento.getButton() == MouseButton.PRIMARY && nodo instanceof ListCell<?> celda
                    && !celda.isEmpty() && item != null) {
                elegir(item);
            }
        });

        // Al entrar se selecciona el texto para reemplazarlo; al salir se cierra la lista
        campo.focusedProperty().addListener((obs, antes, enfocado) -> {
            if (enfocado) {
                Platform.runLater(campo::selectAll);
            } else {
                Platform.runLater(() -> {
                    if (!campo.isFocused() && !lista.isHover()) {
                        ventanaLista.hide();
                        if (combo.getValue() == null) {
                            T unico = coincidenciaUnica(campo.getText());
                            if (unico != null) {
                                fijarValor(unico);
                            }
                        }
                    }
                });
            }
        });
    }

    // ------------------------------------------------------------ comportamiento

    private void filtrar(String texto) {
        String filtro = normalizar(texto);
        filtrados.setPredicate(filtro.isEmpty()
                ? null
                : item -> normalizar(texto(textoBusqueda, item)).contains(filtro));
        lista.getSelectionModel().clearSelection();
        if (!filtrados.isEmpty()) {
            lista.scrollTo(0);
        }
    }

    private void mostrarLista() {
        if (campo.getScene() == null || campo.getScene().getWindow() == null
                || !campo.getScene().getWindow().isShowing()) {
            return;
        }
        int filas = Math.max(1, Math.min(filtrados.size(), FILAS_VISIBLES));
        lista.setPrefHeight(filas * ALTO_FILA + 4);
        lista.setMaxHeight(filas * ALTO_FILA + 4);
        TemaPAE.aplicar(contenedor); // colores claro/oscuro de la aplicación

        if (!ventanaLista.isShowing()) {
            Point2D esquina = campo.localToScreen(0, campo.getHeight());
            if (esquina != null) {
                ventanaLista.show(campo, esquina.getX(), esquina.getY() + 2);
            }
        }
    }

    private void mover(int paso) {
        int total = filtrados.size();
        if (total == 0) {
            return;
        }
        int actual = lista.getSelectionModel().getSelectedIndex();
        int nuevo = Math.max(0, Math.min(total - 1, actual + paso));
        lista.getSelectionModel().select(nuevo);
        lista.scrollTo(Math.max(0, nuevo - 2));
    }

    private void elegir(T item) {
        fijarValor(item);
        ventanaLista.hide();
        filtrados.setPredicate(null);
        campo.positionCaret(campo.getText().length());
    }

    /** Cambia el valor del ComboBox y el texto del campo sin disparar la búsqueda. */
    private void fijarValor(T item) {
        ajustando = true;
        try {
            combo.setValue(item);
            campo.setText(item == null ? "" : texto(textoVisible, item));
        } finally {
            ajustando = false;
        }
    }

    /** Elemento cuyo texto es igual a lo escrito, o el único que coincide parcialmente. */
    private T coincidenciaUnica(String texto) {
        String buscado = normalizar(texto);
        if (buscado.isEmpty()) {
            return null;
        }
        for (T item : todos) {
            if (normalizar(texto(textoVisible, item)).equals(buscado)) {
                return item;
            }
        }
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
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}