package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ni.edu.uam.proyectopae.dao.CategoriaDAO;
import ni.edu.uam.proyectopae.dao.MarcaDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Categoria;
import ni.edu.uam.proyectopae.models.Marca;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
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

    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private final FilteredList<Producto> productosFiltrados = new FilteredList<>(listaProductos, p -> true);

    /** Producto seleccionado; null = registrar uno nuevo. */
    private Producto productoEnEdicion;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("nombreCategoria"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("nombreMarca"));
        colPrecioCompra.setCellValueFactory(new PropertyValueFactory<>("precioCompra"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colGanancia.setCellValueFactory(new PropertyValueFactory<>("gananciaUnitaria"));
        colMargen.setCellValueFactory(new PropertyValueFactory<>("margen"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colStockMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        Formato.columnaMoneda(colPrecioCompra);
        Formato.columnaMoneda(colPrecio);
        Formato.columnaMoneda(colGanancia);
        Formato.columnaPorcentaje(colMargen);

        // Resalta en rojo los productos por debajo del stock minimo
        tblProductos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("fila-peligro");
                if (!empty && item != null && item.isBajoStockMinimo()) {
                    getStyleClass().add("fila-peligro");
                }
            }
        });

        tblProductos.setItems(productosFiltrados);
        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) cargarEnFormulario(sel);
        });

        txtBuscar.textProperty().addListener((obs, a, t) -> aplicarFiltro(t));
        txtPrecio.textProperty().addListener((obs, a, b) -> actualizarGanancia());
        txtPrecioCompra.textProperty().addListener((obs, a, b) -> actualizarGanancia());

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
                categoriaDAO.guardar(new Categoria("Rostro", "Bases, correctores y polvos"));
                categoriaDAO.guardar(new Categoria("Ojos", "Delineadores, máscaras y sombras"));
                categoriaDAO.guardar(new Categoria("Labios", "Tintas, lápices y brillos labiales"));
            }
            if (marcaDAO.listarTodas().isEmpty()) {
                marcaDAO.guardar(new Marca("L'Oréal Paris"));
                marcaDAO.guardar(new Marca("Maybelline"));
                marcaDAO.guardar(new Marca("Rare Beauty"));
            }
            cmbCategoria.setItems(FXCollections.observableArrayList(categoriaDAO.listarTodas()));
            cmbMarca.setItems(FXCollections.observableArrayList(marcaDAO.listarTodas()));
        } catch (Exception e) {
            Alertas.error("Error de conexión", "No se pudieron cargar categorías y marcas:\n" + VentanaManager.causaRaiz(e));
        }
    }

    @FXML
    public void refrescarTabla() {
        try {
            listaProductos.setAll(productoDAO.listarConStock());
            aplicarFiltro(txtBuscar.getText());
        } catch (Exception e) {
            Alertas.error("Error de conexión", "No se pudo consultar PostgreSQL:\n" + VentanaManager.causaRaiz(e));
        }
    }

    private void aplicarFiltro(String texto) {
        String f = texto == null ? "" : texto.toLowerCase().trim();
        productosFiltrados.setPredicate(p -> f.isEmpty()
                || p.getNombreProducto().toLowerCase().contains(f)
                || p.getNombreCategoria().toLowerCase().contains(f)
                || p.getNombreMarca().toLowerCase().contains(f));
        long bajos = listaProductos.stream().filter(Producto::isBajoStockMinimo).count();
        lblTotal.setText(productosFiltrados.size() + " productos · " + bajos + " bajo el stock mínimo");
    }

    private void cargarEnFormulario(Producto p) {
        productoEnEdicion = p;
        txtNombre.setText(p.getNombreProducto());
        txtPrecioCompra.setText(p.getPrecioCompra() != null ? p.getPrecioCompra().toPlainString() : "");
        txtPrecio.setText(p.getPrecioVenta().toPlainString());
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        seleccionarPorId(cmbCategoria, p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null, true);
        seleccionarPorId(cmbMarca, p.getMarca() != null ? p.getMarca().getIdMarca() : null, false);
        actualizarModo();
    }

    /** Las entidades vienen de sesiones distintas: se busca por ID en lugar de equals. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void seleccionarPorId(ComboBox combo, Integer id, boolean esCategoria) {
        combo.setValue(null);
        if (id == null) return;
        for (Object o : combo.getItems()) {
            Integer oid = esCategoria ? ((Categoria) o).getIdCategoria() : ((Marca) o).getIdMarca();
            if (id.equals(oid)) {
                combo.setValue(o);
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
            lblModo.setText("Editando producto #" + productoEnEdicion.getIdProducto());
            lblModo.getStyleClass().add("editando");
            btnGuardar.setText("Guardar cambios");
        }
    }

    private void actualizarGanancia() {
        String c = limpiarNumero(txtPrecioCompra.getText());
        String v = limpiarNumero(txtPrecio.getText());
        if (!c.matches(REGEX_PRECIO) || !v.matches(REGEX_PRECIO)) {
            lblGanancia.setText("Ganancia por unidad: -");
            lblGanancia.setStyle("");
            return;
        }
        BigDecimal compra = new BigDecimal(c);
        BigDecimal venta = new BigDecimal(v);
        BigDecimal ganancia = venta.subtract(compra);
        String margen = venta.signum() == 0 ? "0" :
                ganancia.multiply(BigDecimal.valueOf(100)).divide(venta, 1, RoundingMode.HALF_UP).toPlainString();
        lblGanancia.setText("Ganancia por unidad: " + Formato.moneda(ganancia) + "  (" + margen + " %)");
        lblGanancia.setStyle(ganancia.signum() < 0 ? "-fx-text-fill: #dc2626; -fx-font-weight: bold;"
                : "-fx-text-fill: #059669; -fx-font-weight: bold;");
    }

    /** Un solo boton: registra si es nuevo o actualiza el producto seleccionado. */
    @FXML
    protected void handleGuardarProducto() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String compraTxt = limpiarNumero(txtPrecioCompra.getText());
        String precioTxt = limpiarNumero(txtPrecio.getText());
        String minimoTxt = txtStockMinimo.getText() != null ? txtStockMinimo.getText().trim() : "";
        Categoria categoria = cmbCategoria.getValue();
        Marca marca = cmbMarca.getValue();

        if (nombre.length() < 3) {
            Alertas.validacion("El nombre del producto debe contener al menos 3 caracteres.");
            return;
        }
        if (categoria == null || marca == null) {
            Alertas.validacion("Seleccione una Categoría y una Marca.");
            return;
        }
        if (!compraTxt.matches(REGEX_PRECIO) || !precioTxt.matches(REGEX_PRECIO)) {
            Alertas.validacion("Ingrese precios de compra y venta numéricos válidos (hasta 2 decimales).");
            return;
        }
        // Se exige un stock minimo para poder generar alertas de reabastecimiento
        if (!minimoTxt.matches("^[1-9]\\d{0,4}$")) {
            Alertas.validacion("El stock mínimo es obligatorio y debe ser un entero entre 1 y 99999.");
            return;
        }

        BigDecimal compra = new BigDecimal(compraTxt);
        BigDecimal precio = new BigDecimal(precioTxt);
        int minimo = Integer.parseInt(minimoTxt);

        if (compra.signum() <= 0 || precio.signum() <= 0) {
            Alertas.validacion("Los precios de compra y venta deben ser mayores a cero.");
            return;
        }
        if (precio.compareTo(compra) <= 0 && !Alertas.confirmar("Precio sin ganancia",
                "El precio de venta (" + Formato.moneda(precio) + ") es menor o igual al de compra ("
                        + Formato.moneda(compra) + ").\n¿Desea guardarlo de todos modos?")) {
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
                Alertas.info("Producto actualizado", "El producto #" + productoEnEdicion.getIdProducto() + " se actualizó correctamente.");
            } else {
                productoDAO.guardar(new Producto(nombre, compra, precio, minimo, categoria, marca));
                Alertas.info("Producto registrado", "\"" + nombre + "\" se registró correctamente.\n"
                        + "Recuerde registrar un lote para tener existencias.");
            }

            handleLimpiar();
            refrescarTabla();
            VentanaManager.notificarCambios(this);
        } catch (Exception e) {
            Alertas.error("Error al guardar", VentanaManager.causaRaiz(e));
        }
    }

    @FXML
    protected void handleEliminarProducto() {
        Producto sel = tblProductos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Eliminar producto", "Seleccione un producto de la tabla para eliminar.");
            return;
        }
        if (!Alertas.confirmar("Eliminar producto", "¿Eliminar \"" + sel.getNombreProducto() + "\" y todos sus lotes?\nEsta acción no se puede deshacer.")) {
            return;
        }

        try {
            productoDAO.eliminar(sel.getIdProducto());
            handleLimpiar();
            refrescarTabla();
            VentanaManager.notificarCambios(this);
            Alertas.info("Producto eliminado", "El producto fue eliminado de la base de datos.");
        } catch (Exception e) {
            Alertas.error("No se puede eliminar", "El producto tiene lotes con consumos o ventas registradas.");
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

    private static String limpiarNumero(String t) {
        return t == null ? "" : t.trim().replace(",", ".");
    }
}
