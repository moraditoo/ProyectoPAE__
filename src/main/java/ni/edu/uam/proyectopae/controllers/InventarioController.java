package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ni.edu.uam.proyectopae.dao.CategoriaDAO;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.MarcaDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Categoria;
import ni.edu.uam.proyectopae.models.Marca;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.EventosDatos;
import ni.edu.uam.proyectopae.util.Icono;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.Tablas;
import ni.edu.uam.proyectopae.util.Tema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class InventarioController implements Refrescable {

    private static final String REGEX_PRECIO = "^\\d{1,8}(\\.\\d{1,2})?$";

    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtStockMinimo;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private Label lblModo;
    @FXML private Label lblGanancia;
    @FXML private TextField txtBuscar;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colMarca;
    @FXML private TableColumn<Producto, BigDecimal> colPrecioCompra;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, BigDecimal> colGanancia;
    @FXML private TableColumn<Producto, Long> colStock;
    @FXML private TableColumn<Producto, Producto> colEstado;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();
    private final LoteDAO loteDAO = new LoteDAO();

    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private Map<Integer, Long> stockVigente = new HashMap<>();
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
        Tablas.formatoDinero(colPrecioCompra);
        Tablas.formatoDinero(colPrecio);
        Tablas.formatoDinero(colGanancia);
        Tablas.valor(colStock, this::stockDe);
        Tablas.centrar(colStock);
        Tablas.grafico(colEstado, p -> stockDe(p) <= p.getStockMinimo()
                ? Tablas.badge("Reponer (mín. " + p.getStockMinimo() + ")", Tema.Estado.PELIGRO, Icono.Tipo.ADVERTENCIA)
                : Tablas.badge("Suficiente (mín. " + p.getStockMinimo() + ")", Tema.Estado.OK, Icono.Tipo.EXITO));
        Tablas.colorearFilas(tblProductos, p -> stockDe(p) <= p.getStockMinimo() ? Tema.Estado.PELIGRO : null);

        FilteredList<Producto> filtrados = new FilteredList<>(listaProductos, p -> true);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> {
            String filtro = texto == null ? "" : texto.trim().toLowerCase();
            filtrados.setPredicate(p -> filtro.isEmpty()
                    || p.getNombreProducto().toLowerCase().contains(filtro)
                    || p.getNombreCategoria().toLowerCase().contains(filtro)
                    || p.getNombreMarca().toLowerCase().contains(filtro));
        });
        SortedList<Producto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblProductos.comparatorProperty());
        tblProductos.setItems(ordenados);

        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                cargarEnFormulario(seleccionado);
            }
        });

        txtPrecio.textProperty().addListener((o, a, b) -> actualizarGanancia());
        txtPrecioCompra.textProperty().addListener((o, a, b) -> actualizarGanancia());

        actualizarModo();
        if (cargarCatalogosAuxiliares()) {
            refrescarTabla();
        }
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

    /** Devuelve false si no se pudo consultar la base de datos (ya se mostró la alerta). */
    private boolean cargarCatalogosAuxiliares() {
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
            return true;
        } catch (Exception e) {
            Alertas.errorBD("cargar las categorías y marcas", e);
            return false;
        }
    }

    @FXML
    public void refrescarTabla() {
        try {
            stockVigente = loteDAO.stockVigentePorProducto();
            listaProductos.setAll(productoDAO.listarConRelaciones());
            tblProductos.refresh();
        } catch (Exception e) {
            Alertas.errorBD("cargar los productos", e);
        }
    }

    private void cargarEnFormulario(Producto p) {
        productoEnEdicion = p;
        txtNombre.setText(p.getNombreProducto());
        txtPrecioCompra.setText(p.getPrecioCompra().toPlainString());
        txtPrecio.setText(p.getPrecioVenta().toPlainString());
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        cmbCategoria.setValue(buscarPorId(cmbCategoria.getItems(), p.getCategoria(), Categoria::getIdCategoria));
        cmbMarca.setValue(buscarPorId(cmbMarca.getItems(), p.getMarca(), Marca::getIdMarca));
        actualizarModo();
    }

    /** Botón único "Guardar": inserta si no hay producto seleccionado y actualiza si lo hay. */
    @FXML
    protected void handleGuardarProducto() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String precioTxt = normalizarNumero(txtPrecio.getText());
        String compraTxt = normalizarNumero(txtPrecioCompra.getText());
        String minimoTxt = txtStockMinimo.getText() != null ? txtStockMinimo.getText().trim() : "";
        Categoria categoria = cmbCategoria.getValue();
        Marca marca = cmbMarca.getValue();

        List<String> faltantes = new ArrayList<>();
        if (nombre.isEmpty()) faltantes.add("Nombre del producto");
        if (categoria == null) faltantes.add("Categoría");
        if (marca == null) faltantes.add("Marca");
        if (compraTxt.isEmpty()) faltantes.add("Precio de compra");
        if (precioTxt.isEmpty()) faltantes.add("Precio de venta");
        if (minimoTxt.isEmpty()) faltantes.add("Existencia mínima");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }

        if (nombre.length() < 3 || nombre.length() > 100) {
            Alertas.datoInvalido("El nombre del producto debe tener entre 3 y 100 caracteres.");
            return;
        }
        if (!compraTxt.matches(REGEX_PRECIO) || !precioTxt.matches(REGEX_PRECIO)) {
            Alertas.datoInvalido("Los precios deben ser números positivos con hasta 2 decimales. Ejemplo: 380.50");
            return;
        }
        if (!minimoTxt.matches("^\\d{1,6}$")) {
            Alertas.datoInvalido("La existencia mínima debe ser un número entero igual o mayor a 0.");
            return;
        }

        BigDecimal precio = new BigDecimal(precioTxt);
        BigDecimal compra = new BigDecimal(compraTxt);
        int minimo = Integer.parseInt(minimoTxt);
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            Alertas.datoInvalido("El precio de venta no puede ser 0 ni negativo.");
            return;
        }
        if (precio.compareTo(compra) < 0) {
            Alertas.datoInvalido("El precio de venta (" + Tablas.dinero(precio) + ") no puede ser menor que el precio de compra ("
                    + Tablas.dinero(compra) + "), porque el producto se vendería con pérdida.");
            return;
        }
        boolean duplicado = listaProductos.stream().anyMatch(p -> p.getNombreProducto().equalsIgnoreCase(nombre)
                && (productoEnEdicion == null || !p.getIdProducto().equals(productoEnEdicion.getIdProducto())));
        if (duplicado) {
            Alertas.datoInvalido("Ya existe un producto con el nombre \"" + nombre + "\".");
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
                Alertas.exito("Información actualizada correctamente",
                        "El producto \"" + nombre + "\" se actualizó en la base de datos.");
            } else {
                productoDAO.guardar(new Producto(nombre, compra, precio, minimo, categoria, marca));
                Alertas.exito("Registro realizado correctamente",
                        "El producto \"" + nombre + "\" fue registrado. Registre sus lotes en el módulo Lotes y Caducidad.");
            }

            handleLimpiar();
            refrescarTabla();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            Alertas.errorBD("guardar el producto", e);
            refrescarTabla();
        }
    }

    @FXML
    protected void handleEliminarProducto() {
        Producto seleccionado = productoEnEdicion != null ? productoEnEdicion : tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.advertencia("Ningún producto seleccionado", "Seleccione un producto de la tabla para eliminarlo.");
            return;
        }
        if (!Alertas.confirmar("¿Eliminar el producto \"" + seleccionado.getNombreProducto() + "\"?",
                "También se eliminarán sus lotes registrados. Esta acción no se puede deshacer.")) {
            return;
        }

        try {
            productoDAO.eliminar(seleccionado.getIdProducto());
            Alertas.exito("Producto eliminado", "El producto fue eliminado de la base de datos.");
            handleLimpiar();
            refrescarTabla();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            if (Alertas.esErrorConexion(e)) {
                Alertas.errorBD("eliminar el producto", e);
            } else {
                Alertas.error("No se puede eliminar el producto",
                        "El producto tiene ventas o consumos registrados y debe conservarse para el historial.");
            }
        }
    }

    @FXML
    protected void handleLimpiar() {
        txtNombre.clear();
        txtPrecio.clear();
        txtPrecioCompra.clear();
        txtStockMinimo.setText("5");
        cmbCategoria.setValue(null);
        cmbMarca.setValue(null);
        tblProductos.getSelectionModel().clearSelection();
        productoEnEdicion = null;
        actualizarModo();
    }

    private long stockDe(Producto p) {
        return stockVigente.getOrDefault(p.getIdProducto(), 0L);
    }

    private void actualizarGanancia() {
        try {
            BigDecimal venta = new BigDecimal(normalizarNumero(txtPrecio.getText()));
            BigDecimal compra = new BigDecimal(normalizarNumero(txtPrecioCompra.getText()));
            BigDecimal ganancia = venta.subtract(compra);
            lblGanancia.setText(Tablas.dinero(ganancia));
            Tema.textoMonto(lblGanancia, ganancia.signum() < 0);
        } catch (NumberFormatException e) {
            lblGanancia.setText("C$ 0.00");
            Tema.textoMonto(lblGanancia, false);
        }
    }

    private void actualizarModo() {
        if (productoEnEdicion != null) {
            lblModo.setText("Editando #" + productoEnEdicion.getIdProducto());
            lblModo.setGraphic(new Icono(Icono.Tipo.GUARDAR, 15));
        } else {
            lblModo.setText("Nuevo registro");
            lblModo.setGraphic(new Icono(Icono.Tipo.NUEVO, 15));
            if (txtStockMinimo.getText() == null || txtStockMinimo.getText().isBlank()) txtStockMinimo.setText("5");
        }
        Tema.indicadorModo(lblModo, productoEnEdicion != null);
    }

    private static String normalizarNumero(String texto) {
        return texto != null ? texto.trim().replace(",", ".") : "";
    }

    /** Las entidades de cada consulta son instancias distintas: se busca la opción equivalente por su id. */
    private static <T> T buscarPorId(List<T> opciones, T valor, Function<T, Integer> id) {
        if (valor == null) return null;
        for (T opcion : opciones) {
            if (id.apply(opcion).equals(id.apply(valor))) return opcion;
        }
        return valor;
    }
}
