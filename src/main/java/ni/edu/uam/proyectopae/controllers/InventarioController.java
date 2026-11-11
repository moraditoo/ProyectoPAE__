package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.math.BigDecimal;

public class InventarioController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Marca> cmbMarca;
    @FXML private Label lblEstado;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colMarca;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final MarcaDAO marcaDAO = new MarcaDAO();

    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idProducto"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("nombreCategoria"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("nombreMarca"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));

        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) -> {
            if (seleccionado != null) {
                txtNombre.setText(seleccionado.getNombreProducto());
                txtPrecio.setText(seleccionado.getPrecioVenta().toString());
                cmbCategoria.setValue(seleccionado.getCategoria());
                cmbMarca.setValue(seleccionado.getMarca());
            }
        });

        cargarCatalogosAuxiliares();
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
            mostrarMensaje("Error al sincronizar catálogos: " + e.getMessage(), true);
        }
    }

    @FXML
    public void refrescarTabla() {
        try {
            listaProductos.setAll(productoDAO.listarConRelaciones());
            tblProductos.setItems(listaProductos);
        } catch (Exception e) {
            mostrarMensaje("Error al consultar PostgreSQL con HQL: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleGuardarProducto() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String precioTxt = txtPrecio.getText() != null ? txtPrecio.getText().trim().replace(",", ".") : "";
        Categoria categoria = cmbCategoria.getValue();
        Marca marca = cmbMarca.getValue();

        // Validaciones estrictas
        if (nombre.length() < 3) {
            mostrarMensaje("Validación: El nombre del producto debe contener al menos 3 caracteres.", true);
            return;
        }

        if (categoria == null || marca == null) {
            mostrarMensaje("Validación: Seleccione una Categoría y una Marca válidas.", true);
            return;
        }

        if (precioTxt.isEmpty() || !precioTxt.matches("^\\d+(\\.\\d{1,2})?$")) {
            mostrarMensaje("Validación: Ingrese un precio numérico positivo válido con hasta 2 decimales.", true);
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(precioTxt);
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarMensaje("Validación: El precio del producto no puede ser 0 ni negativo.", true);
                return;
            }

            Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getNombreProducto().equalsIgnoreCase(nombre)) {
                seleccionado.setPrecioVenta(precio);
                seleccionado.setCategoria(categoria);
                seleccionado.setMarca(marca);
                productoDAO.actualizar(seleccionado);
                mostrarMensaje("Producto #" + seleccionado.getIdProducto() + " actualizado en PostgreSQL.", false);
            } else {
                Producto nuevo = new Producto(nombre, precio, 5, categoria, marca);
                productoDAO.guardar(nuevo);
                mostrarMensaje("Nuevo producto persistido exitosamente en PostgreSQL.", false);
            }

            handleLimpiar();
            refrescarTabla();
        } catch (Exception e) {
            mostrarMensaje("Error ORM: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleEliminarProducto() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Seleccione un producto de la tabla para eliminar.", true);
            return;
        }

        try {
            productoDAO.eliminar(seleccionado.getIdProducto());
            mostrarMensaje("Producto eliminado de la base de datos.", false);
            handleLimpiar();
            refrescarTabla();
        } catch (Exception e) {
            mostrarMensaje("Restricción referencial: No se puede eliminar, el producto tiene lotes asociados.", true);
        }
    }

    @FXML
    protected void handleLimpiar() {
        txtNombre.clear();
        txtPrecio.clear();
        cmbCategoria.setValue(null);
        cmbMarca.setValue(null);
        tblProductos.getSelectionModel().clearSelection();
    }

    private void mostrarMensaje(String texto, boolean esError) {
        lblEstado.setStyle(esError ? "-fx-text-fill: #b30000; -fx-font-weight: bold;" : "-fx-text-fill: #1b5e20; -fx-font-weight: bold;");
        lblEstado.setText(texto);
    }
}