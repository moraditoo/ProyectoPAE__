package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.api.ApiException;
import ni.edu.uam.proyectopae.api.CatalogoApiClient;
import ni.edu.uam.proyectopae.api.ProductoProveedor;
import ni.edu.uam.proyectopae.api.RespuestaApi;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Refrescable;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Módulo "Catálogo del proveedor": consume la API REST api-catalogo.
 * Toda la comunicación HTTP está en CatalogoApiClient; aquí solo se maneja la interfaz.
 * Cada llamada se ejecuta en un Task (hilo de fondo) para no congelar la ventana.
 */
public class CatalogoProveedorController implements Refrescable {

    private static final String TODAS = "Todas las categorías";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField txtIdConsulta;

    @FXML private TableView<ProductoProveedor> tblProductos;
    @FXML private TableColumn<ProductoProveedor, Long> colId;
    @FXML private TableColumn<ProductoProveedor, String> colCodigo;
    @FXML private TableColumn<ProductoProveedor, String> colNombre;
    @FXML private TableColumn<ProductoProveedor, String> colMarca;
    @FXML private TableColumn<ProductoProveedor, String> colCategoria;
    @FXML private TableColumn<ProductoProveedor, BigDecimal> colPrecio;
    @FXML private TableColumn<ProductoProveedor, Integer> colExistencias;
    @FXML private Label lblResultados;

    @FXML private Label lblModo;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtMarca;
    @FXML private TextField txtCategoria;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtExistencias;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnGuardar;

    @FXML private Label lblApiUrl;
    @FXML private Label lblEstadoHttp;
    @FXML private ProgressIndicator indicadorCarga;

    private final CatalogoApiClient api = new CatalogoApiClient();
    private final ObservableList<ProductoProveedor> productos = FXCollections.observableArrayList();

    /** Producto cargado en el formulario; null significa que se registrará uno nuevo. */
    private ProductoProveedor enEdicion;

    /** Cantidad de consultas en curso, para mostrar u ocultar el indicador de carga. */
    private int consultasActivas;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colExistencias.setCellValueFactory(new PropertyValueFactory<>("existencias"));
        Formato.columnaMoneda(colPrecio);
        tblProductos.setItems(productos);

        // Al seleccionar una fila se consulta su detalle con GET /api/productos/{id}
        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null && seleccionado.getId() != null) {
                consultarDetalle(seleccionado.getId());
            }
        });

        cmbCategoria.setItems(FXCollections.observableArrayList(TODAS));
        cmbCategoria.setValue(TODAS);
        cmbCategoria.setOnAction(evento -> handleBuscar());

        indicadorCarga.setVisible(false);
        lblApiUrl.setText("API: " + api.getBaseUrl());
        lblEstadoHttp.setText("Sin consultas todavía");
        actualizarModo();

        cargarCategoriasYProductos();
    }

    @Override
    public void refrescar() {
        cargarCategoriasYProductos();
    }

    // ------------------------------------------------------------ consultas (GET)

    /** Carga las categorías del filtro y después la lista completa. */
    private void cargarCategoriasYProductos() {
        ejecutar(api::categorias, respuesta -> {
            String actual = cmbCategoria.getValue();
            cmbCategoria.setOnAction(null); // evita una búsqueda doble mientras se llenan las opciones
            cmbCategoria.getItems().setAll(TODAS);
            cmbCategoria.getItems().addAll(respuesta.datos());
            cmbCategoria.setValue(cmbCategoria.getItems().contains(actual) ? actual : TODAS);
            cmbCategoria.setOnAction(evento -> handleBuscar());
            handleBuscar();
        });
    }

    /** GET /api/productos?buscar=...&categoria=... */
    @FXML
    protected void handleBuscar() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        String categoria = TODAS.equals(cmbCategoria.getValue()) ? null : cmbCategoria.getValue();

        if (texto.length() == 1) {
            Alertas.validacion("Escriba al menos 2 caracteres para buscar, o deje el campo vacío para ver todo.");
            return;
        }

        ejecutar(() -> api.listar(texto, categoria), respuesta -> {
            List<ProductoProveedor> lista = respuesta.datos();
            productos.setAll(lista);
            if (lista.isEmpty()) {
                lblResultados.setText("No se encontraron productos con esos filtros.");
            } else {
                lblResultados.setText(lista.size() + (lista.size() == 1 ? " producto encontrado" : " productos encontrados"));
            }
        });
    }

    @FXML
    protected void handleLimpiarFiltros() {
        txtBuscar.clear();
        if (TODAS.equals(cmbCategoria.getValue())) {
            handleBuscar();
        } else {
            cmbCategoria.setValue(TODAS); // cambiar el valor dispara la búsqueda (setOnAction)
        }
    }

    /** GET /api/productos/{id} escrito por el usuario (sirve para demostrar el error 404). */
    @FXML
    protected void handleConsultarPorId() {
        String texto = txtIdConsulta.getText() == null ? "" : txtIdConsulta.getText().trim();
        if (!texto.matches("\\d{1,9}")) {
            Alertas.validacion("Escriba un ID numérico válido (por ejemplo 1).");
            return;
        }
        consultarDetalle(Long.parseLong(texto));
    }

    private void consultarDetalle(long id) {
        ejecutar(() -> api.obtener(id), respuesta -> cargarEnFormulario(respuesta.datos()));
    }

    // ------------------------------------------------------------ cambios (POST, PUT, DELETE)

    @FXML
    protected void handleGuardar() {
        ProductoProveedor formulario = leerFormulario();
        if (formulario == null) {
            return; // ya se mostró la validación
        }

        if (enEdicion == null) {
            ejecutar(() -> api.crear(formulario), respuesta -> {
                Alertas.info("Producto registrado", "La API asignó el ID " + respuesta.datos().getId()
                        + " a \"" + respuesta.datos().getNombre() + "\".");
                limpiarFormulario();
                cargarCategoriasYProductos();
            });
        } else {
            long id = enEdicion.getId();
            ejecutar(() -> api.actualizar(id, formulario), respuesta -> {
                Alertas.info("Producto actualizado", "Se guardaron los cambios del producto #" + id + ".");
                limpiarFormulario();
                cargarCategoriasYProductos();
            });
        }
    }

    @FXML
    protected void handleEliminar() {
        if (enEdicion == null) {
            Alertas.advertencia("Eliminar producto", "Seleccione un producto de la tabla o consúltelo por ID.");
            return;
        }
        long id = enEdicion.getId();
        if (!Alertas.confirmar("Eliminar producto", "¿Eliminar \"" + enEdicion.getNombre()
                + "\" del catálogo del proveedor?")) {
            return;
        }
        ejecutar(() -> api.eliminar(id), respuesta -> {
            Alertas.info("Producto eliminado", "El producto #" + id + " se eliminó del catálogo.");
            limpiarFormulario();
            cargarCategoriasYProductos();
        });
    }

    @FXML
    protected void handleNuevo() {
        limpiarFormulario();
        txtCodigo.requestFocus();
    }

    // ------------------------------------------------------------ formulario

    private void cargarEnFormulario(ProductoProveedor p) {
        enEdicion = p;
        txtCodigo.setText(texto(p.getCodigo()));
        txtNombre.setText(texto(p.getNombre()));
        txtMarca.setText(texto(p.getMarca()));
        txtCategoria.setText(texto(p.getCategoria()));
        txtPrecio.setText(p.getPrecio() == null ? "" : p.getPrecio().toPlainString());
        txtExistencias.setText(p.getExistencias() == null ? "" : String.valueOf(p.getExistencias()));
        txtDescripcion.setText(texto(p.getDescripcion()));
        actualizarModo();
    }

    /** Valida lo que se puede revisar en la interfaz; los rangos (precio > 0, etc.) los valida la API. */
    private ProductoProveedor leerFormulario() {
        String codigo = texto(txtCodigo.getText()).trim();
        String nombre = texto(txtNombre.getText()).trim();
        String marca = texto(txtMarca.getText()).trim();
        String categoria = texto(txtCategoria.getText()).trim();
        String precioTxt = texto(txtPrecio.getText()).trim().replace(',', '.');
        String existenciasTxt = texto(txtExistencias.getText()).trim();

        if (codigo.isEmpty() || nombre.isEmpty() || marca.isEmpty() || categoria.isEmpty()
                || precioTxt.isEmpty() || existenciasTxt.isEmpty()) {
            Alertas.validacion("Complete todos los campos marcados con *.");
            return null;
        }

        ProductoProveedor p = new ProductoProveedor();
        try {
            p.setPrecio(new BigDecimal(precioTxt));
            p.setExistencias(Integer.parseInt(existenciasTxt));
        } catch (NumberFormatException e) {
            Alertas.validacion("El precio debe ser un número (ej. 450.50) y las existencias un entero (ej. 10).");
            return null;
        }
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setMarca(marca);
        p.setCategoria(categoria);
        p.setDescripcion(texto(txtDescripcion.getText()).trim());
        return p;
    }

    private void limpiarFormulario() {
        enEdicion = null;
        tblProductos.getSelectionModel().clearSelection();
        txtCodigo.clear();
        txtNombre.clear();
        txtMarca.clear();
        txtCategoria.clear();
        txtPrecio.clear();
        txtExistencias.clear();
        txtDescripcion.clear();
        actualizarModo();
    }

    private void actualizarModo() {
        lblModo.getStyleClass().remove("editando");
        if (enEdicion == null) {
            lblModo.setText("Nuevo producto del proveedor");
            btnGuardar.setText("Registrar (POST)");
        } else {
            lblModo.setText("Detalle del producto #" + enEdicion.getId());
            lblModo.getStyleClass().add("editando");
            btnGuardar.setText("Guardar cambios (PUT)");
        }
    }

    // ------------------------------------------------------------ hilo de fondo

    /**
     * Ejecuta la llamada HTTP en un hilo de fondo y, al terminar, vuelve al hilo de JavaFX
     * para actualizar la interfaz (onSucceeded / onFailed siempre corren en el hilo de JavaFX).
     */
    private <T> void ejecutar(Callable<RespuestaApi<T>> trabajo, Consumer<RespuestaApi<T>> alExito) {
        Task<RespuestaApi<T>> tarea = new Task<>() {
            @Override
            protected RespuestaApi<T> call() throws Exception {
                return trabajo.call();
            }
        };

        consultasActivas++;
        indicadorCarga.setVisible(true);
        lblEstadoHttp.setText("Consultando la API...");

        tarea.setOnSucceeded(evento -> {
            terminarCarga();
            RespuestaApi<T> respuesta = tarea.getValue();
            lblEstadoHttp.setText(respuesta.resumen());
            alExito.accept(respuesta);
        });

        tarea.setOnFailed(evento -> {
            terminarCarga();
            mostrarError(tarea.getException());
        });

        Thread hilo = new Thread(tarea, "proyectopae-api-catalogo");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void terminarCarga() {
        consultasActivas = Math.max(0, consultasActivas - 1);
        indicadorCarga.setVisible(consultasActivas > 0);
    }

    private void mostrarError(Throwable error) {
        if (error instanceof ApiException apiError) {
            if (apiError.isSinConexion()) {
                lblEstadoHttp.setText("Sin conexión con " + api.getBaseUrl());
                lblResultados.setText("No se pudo consultar la API.");
                Alertas.error("API no disponible", apiError.getMessage());
            } else {
                lblEstadoHttp.setText("Respuesta HTTP " + apiError.getCodigoHttp());
                Alertas.advertencia("La API rechazó la solicitud", apiError.getMessage());
            }
        } else {
            lblEstadoHttp.setText("Error inesperado");
            Alertas.error("Error inesperado", error == null ? "Error desconocido" : String.valueOf(error.getMessage()));
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}