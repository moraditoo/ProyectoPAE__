package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ni.edu.uam.proyectopae.dao.CategoriaDAO;
import ni.edu.uam.proyectopae.dao.MarcaDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Categoria;
import ni.edu.uam.proyectopae.models.Marca;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Filas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InventarioController implements Refrescable {

    private static final String REGEX_PRECIO = "^\\d+(\\.\\d{1,2})?$";

    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtBuscar;

    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Marca> cmbMarca;

    @FXML private Label lblModo;
    @FXML private Label lblGanancia;
    @FXML private Label lblTotal;

    @FXML private Button btnGuardar;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colMarca;
    @FXML private TableColumn<Producto, BigDecimal> colPrecioCompra;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, BigDecimal> colGanancia;
    @FXML private TableColumn<Producto, BigDecimal> colMargen;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Integer> colStockMinimo;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();

    private final ObservableList<Producto> listaProductos =
            FXCollections.observableArrayList();

    private final FilteredList<Producto> productosFiltrados =
            new FilteredList<>(listaProductos, p -> true);

    /** Producto seleccionado; null significa registrar uno nuevo. */
    private Producto productoEnEdicion;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(
                new PropertyValueFactory<>("idProducto")
        );

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto")
        );

        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("nombreCategoria")
        );

        colMarca.setCellValueFactory(
                new PropertyValueFactory<>("nombreMarca")
        );

        colPrecioCompra.setCellValueFactory(
                new PropertyValueFactory<>("precioCompra")
        );

        colPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precioVenta")
        );

        colGanancia.setCellValueFactory(
                new PropertyValueFactory<>("gananciaUnitaria")
        );

        colMargen.setCellValueFactory(
                new PropertyValueFactory<>("margen")
        );

        colStock.setCellValueFactory(
                new PropertyValueFactory<>("stockActual")
        );

        colStockMinimo.setCellValueFactory(
                new PropertyValueFactory<>("stockMinimo")
        );

        Formato.columnaMoneda(colPrecioCompra);
        Formato.columnaMoneda(colPrecio);
        Formato.columnaMoneda(colGanancia);
        Formato.columnaPorcentaje(colMargen);

        // Resalta en rojo los productos por debajo del stock mínimo.
        tblProductos.setRowFactory(
                Filas.peligro(Producto::isBajoStockMinimo)
        );

        tblProductos.setItems(productosFiltrados);

        tblProductos.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, seleccionado) -> {

                    if (seleccionado != null) {
                        cargarEnFormulario(seleccionado);
                    }
                });

        txtBuscar.textProperty().addListener(
                (obs, anterior, nuevo) -> aplicarFiltro(nuevo)
        );

        txtPrecio.textProperty().addListener(
                (obs, anterior, nuevo) -> actualizarGanancia()
        );

        txtPrecioCompra.textProperty().addListener(
                (obs, anterior, nuevo) -> actualizarGanancia()
        );

        cargarCatalogosAuxiliares();
        refrescarTabla();
        actualizarModo();
    }

    @Override
    public void refrescar() {
        refrescarTabla();
    }

    @FXML
    protected void handleEnterGuardar(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleGuardarProducto();
        }
    }

    private void cargarCatalogosAuxiliares() {
        try {
            if (categoriaDAO.listarTodas().isEmpty()) {
                categoriaDAO.guardar(
                        new Categoria(
                                "Rostro",
                                "Bases, correctores y polvos"
                        )
                );

                categoriaDAO.guardar(
                        new Categoria(
                                "Ojos",
                                "Delineadores, máscaras y sombras"
                        )
                );

                categoriaDAO.guardar(
                        new Categoria(
                                "Labios",
                                "Tintas, lápices y brillos labiales"
                        )
                );
            }

            if (marcaDAO.listarTodas().isEmpty()) {
                marcaDAO.guardar(new Marca("L'Oréal Paris"));
                marcaDAO.guardar(new Marca("Maybelline"));
                marcaDAO.guardar(new Marca("Rare Beauty"));
            }

            cmbCategoria.setItems(
                    FXCollections.observableArrayList(
                            categoriaDAO.listarTodas()
                    )
            );

            cmbMarca.setItems(
                    FXCollections.observableArrayList(
                            marcaDAO.listarTodas()
                    )
            );

        } catch (Exception e) {
            Alertas.error(
                    "Error de conexión",
                    "No se pudieron cargar categorías y marcas:\n"
                            + VentanaManager.causaRaiz(e)
            );
        }
    }

    @FXML
    public void refrescarTabla() {
        try {
            listaProductos.setAll(
                    productoDAO.listarConStock()
            );

            aplicarFiltro(txtBuscar.getText());

        } catch (Exception e) {
            Alertas.error(
                    "Error de conexión",
                    "No se pudo consultar PostgreSQL:\n"
                            + VentanaManager.causaRaiz(e)
            );
        }
    }

    private void aplicarFiltro(String texto) {
        String filtro = texto == null
                ? ""
                : texto.toLowerCase().trim();

        productosFiltrados.setPredicate(producto ->
                filtro.isEmpty()
                        || producto.getNombreProducto()
                        .toLowerCase().contains(filtro)
                        || producto.getNombreCategoria()
                        .toLowerCase().contains(filtro)
                        || producto.getNombreMarca()
                        .toLowerCase().contains(filtro)
        );

        long bajos = listaProductos.stream()
                .filter(Producto::isBajoStockMinimo)
                .count();

        lblTotal.setText(
                productosFiltrados.size()
                        + " productos · "
                        + bajos
                        + " bajo el stock mínimo"
        );
    }

    private void cargarEnFormulario(Producto producto) {
        productoEnEdicion = producto;

        txtNombre.setText(producto.getNombreProducto());

        txtPrecioCompra.setText(
                producto.getPrecioCompra() != null
                        ? producto.getPrecioCompra().toPlainString()
                        : ""
        );

        txtPrecio.setText(
                producto.getPrecioVenta().toPlainString()
        );

        txtStockMinimo.setText(
                String.valueOf(producto.getStockMinimo())
        );

        seleccionarPorId(
                cmbCategoria,
                producto.getCategoria() != null
                        ? producto.getCategoria().getIdCategoria()
                        : null,
                true
        );

        seleccionarPorId(
                cmbMarca,
                producto.getMarca() != null
                        ? producto.getMarca().getIdMarca()
                        : null,
                false
        );

        actualizarModo();
    }

    /**
     * Las entidades vienen de sesiones distintas:
     * se busca por ID en lugar de utilizar equals.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void seleccionarPorId(
            ComboBox combo,
            Integer id,
            boolean esCategoria
    ) {
        combo.setValue(null);

        if (id == null) {
            return;
        }

        for (Object objeto : combo.getItems()) {
            Integer idObjeto = esCategoria
                    ? ((Categoria) objeto).getIdCategoria()
                    : ((Marca) objeto).getIdMarca();

            if (id.equals(idObjeto)) {
                combo.setValue(objeto);
                return;
            }
        }
    }

    private void actualizarModo() {
        lblModo.getStyleClass().remove("editando");

        if (productoEnEdicion == null) {
            lblModo.setText("Nuevo producto");
            btnGuardar.setText("Guardar");

        } else {
            lblModo.setText(
                    "Editando producto #" + productoEnEdicion.getIdProducto()
            );

            lblModo.getStyleClass().add("editando");
            btnGuardar.setText("Guardar cambios");
        }
    }

    private void actualizarGanancia() {
        String compraTexto = limpiarNumero(txtPrecioCompra.getText());
        String ventaTexto = limpiarNumero(txtPrecio.getText());

        if (!compraTexto.matches(REGEX_PRECIO)
                || !ventaTexto.matches(REGEX_PRECIO)) {

            lblGanancia.setText("Ganancia por unidad: -");
            lblGanancia.setTextFill(Color.GRAY);
            lblGanancia.setFont(
                    Font.font("System", FontWeight.NORMAL, 13)
            );
            return;
        }

        BigDecimal compra = new BigDecimal(compraTexto);
        BigDecimal venta = new BigDecimal(ventaTexto);

        BigDecimal ganancia = venta.subtract(compra);

        String margen = venta.signum() == 0
                ? "0"
                : ganancia.multiply(BigDecimal.valueOf(100))
                .divide(venta, 1, RoundingMode.HALF_UP)
                .toPlainString();

        lblGanancia.setText(
                "Ganancia por unidad: "
                        + Formato.moneda(ganancia)
                        + " (" + margen + " %)"
        );

        lblGanancia.setTextFill(
                ganancia.signum() < 0
                        ? Color.web("#dc2626")
                        : Color.web("#059669")
        );

        lblGanancia.setFont(
                Font.font("System", FontWeight.BOLD, 13)
        );
    }

    @FXML
    protected void handleGuardarProducto() {
        String nombre = txtNombre.getText() != null
                ? txtNombre.getText().trim()
                : "";

        String compraTexto = limpiarNumero(txtPrecioCompra.getText());
        String precioTexto = limpiarNumero(txtPrecio.getText());

        String minimoTexto = txtStockMinimo.getText() != null
                ? txtStockMinimo.getText().trim()
                : "";

        Categoria categoria = cmbCategoria.getValue();
        Marca marca = cmbMarca.getValue();

        if (nombre.length() < 3) {
            Alertas.validacion(
                    "El nombre del producto debe contener al menos 3 caracteres."
            );
            return;
        }

        if (categoria == null || marca == null) {
            Alertas.validacion(
                    "Seleccione una Categoría y una Marca."
            );
            return;
        }

        if (!compraTexto.matches(REGEX_PRECIO)
                || !precioTexto.matches(REGEX_PRECIO)) {
            Alertas.validacion(
                    "Ingrese precios de compra y venta numéricos válidos "
                            + "(hasta 2 decimales)."
            );
            return;
        }

        if (!minimoTexto.matches("^[1-9]\\d{0,4}$")) {
            Alertas.validacion(
                    "El stock mínimo es obligatorio y debe ser un entero "
                            + "entre 1 y 99999."
            );
            return;
        }

        BigDecimal compra = new BigDecimal(compraTexto);
        BigDecimal precio = new BigDecimal(precioTexto);
        int minimo = Integer.parseInt(minimoTexto);

        if (compra.signum() <= 0 || precio.signum() <= 0) {
            Alertas.validacion(
                    "Los precios de compra y venta deben ser mayores a cero."
            );
            return;
        }

        if (precio.compareTo(compra) <= 0
                && !Alertas.confirmar(
                "Precio sin ganancia",
                "El precio de venta (" + Formato.moneda(precio)
                        + ") es menor o igual al de compra ("
                        + Formato.moneda(compra)
                        + ").\n¿Desea guardarlo de todos modos?"
        )) {
            return;
        }

        try {
            if (productoEnEdicion != null) {
                productoEnEdicion.setNombreProducto(nombre);
                productoEnEdicion.setPrecioCompra(compra);
                productoEnEdicion.setPrecioVenta(precio);
                productoEnEdicion.setStockMinimo(minimo);
                productoEnEdicion.setCategoria(categoria);
                productoEnEdicion.setMarca(marca);

                productoDAO.actualizar(productoEnEdicion);

                Alertas.info(
                        "Producto actualizado",
                        "El producto #" + productoEnEdicion.getIdProducto()
                                + " se actualizó correctamente."
                );

            } else {
                productoDAO.guardar(
                        new Producto(
                                nombre,
                                compra,
                                precio,
                                minimo,
                                categoria,
                                marca
                        )
                );

                Alertas.info(
                        "Producto registrado",
                        "\"" + nombre + "\" se registró correctamente.\n"
                                + "Recuerde registrar un lote para tener existencias."
                );
            }

            handleLimpiar();
            refrescarTabla();
            VentanaManager.notificarCambios(this);

        } catch (Exception e) {
            Alertas.error(
                    "Error al guardar",
                    VentanaManager.causaRaiz(e)
            );
        }
    }

    @FXML
    protected void handleEliminarProducto() {
        Producto seleccionado =
                tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            Alertas.advertencia(
                    "Eliminar producto",
                    "Seleccione un producto de la tabla para eliminar."
            );
            return;
        }

        if (!Alertas.confirmar(
                "Eliminar producto",
                "¿Eliminar \"" + seleccionado.getNombreProducto()
                        + "\" y todos sus lotes?\n"
                        + "Esta acción no se puede deshacer."
        )) {
            return;
        }

        try {
            productoDAO.eliminar(seleccionado.getIdProducto());

            handleLimpiar();
            refrescarTabla();
            VentanaManager.notificarCambios(this);

            Alertas.info(
                    "Producto eliminado",
                    "El producto fue eliminado de la base de datos."
            );

        } catch (Exception e) {
            Alertas.error(
                    "No se puede eliminar",
                    "El producto tiene lotes con consumos o ventas registradas."
            );
        }
    }

    @FXML
    protected void handleLimpiar() {
        productoEnEdicion = null;

        txtNombre.clear();
        txtPrecioCompra.clear();
        txtPrecio.clear();
        txtStockMinimo.clear();

        cmbCategoria.setValue(null);
        cmbMarca.setValue(null);

        tblProductos.getSelectionModel().clearSelection();

        actualizarModo();
        txtNombre.requestFocus();
    }

    private static String limpiarNumero(String texto) {
        return texto == null
                ? ""
                : texto.trim().replace(",", ".");
    }
}