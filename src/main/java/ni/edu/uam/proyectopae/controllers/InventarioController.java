package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

        precargarCatalogos();
        refrescarTabla();
    }

    private void precargarCatalogos() {
        try {
            if (categoriaDAO.listarTodas().isEmpty()) {
                categoriaDAO.guardar(new Categoria("Rostro", "Bases, correctores y polvos"));
                categoriaDAO.guardar(new Categoria("Labios", "Labiales matte y tintas"));
            }
            if (marcaDAO.listarTodas().isEmpty()) {
                marcaDAO.guardar(new Marca("Maybelline"));
                marcaDAO.guardar(new Marca("Rare Beauty"));
            }
            cmbCategoria.setItems(FXCollections.observableArrayList(categoriaDAO.listarTodas()));
            cmbMarca.setItems(FXCollections.observableArrayList(marcaDAO.listarTodas()));
        } catch (Exception e) {
            lblEstado.setText("Error al cargar catalogos auxiliares: " + e.getMessage());
        }
    }

    private void refrescarTabla() {
        try {
            listaProductos.setAll(productoDAO.listarConRelaciones());
            tblProductos.setItems(listaProductos);
        } catch (Exception e) {
            lblEstado.setText("Error al cargar listado de productos: " + e.getMessage());
        }
    }

    @FXML
    protected void handleGuardarProducto() {
        String nom = txtNombre.getText().trim();
        String pre = txtPrecio.getText().trim();
        Categoria cat = cmbCategoria.getValue();
        Marca mar = cmbMarca.getValue();

        if (nom.isEmpty() || pre.isEmpty() || cat == null || mar == null) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Validacion: Complete todos los datos solicitados.");
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(pre);
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                lblEstado.setStyle("-fx-text-fill: red;");
                lblEstado.setText("Validacion: El precio debe ser un valor positivo.");
                return;
            }

            Producto nuevo = new Producto(nom, precio, 5, cat, mar);
            productoDAO.guardar(nuevo);

            txtNombre.clear();
            txtPrecio.clear();
            lblEstado.setStyle("-fx-text-fill: green;");
            lblEstado.setText("Producto registrado mediante Hibernate con exito.");
            refrescarTabla();
        } catch (NumberFormatException e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Formato invalido en precio. Utilice formato decimal.");
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Error de persistencia: " + e.getMessage());
        }
    }

    @FXML
    protected void handleEliminarProducto() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Seleccione un producto en la tabla para eliminar.");
            return;
        }

        try {
            productoDAO.eliminar(seleccionado.getIdProducto());
            lblEstado.setStyle("-fx-text-fill: green;");
            lblEstado.setText("Producto eliminado correctamente.");
            refrescarTabla();
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("No es posible eliminar el producto: Posee lotes o insumos asociados.");
        }
    }
}