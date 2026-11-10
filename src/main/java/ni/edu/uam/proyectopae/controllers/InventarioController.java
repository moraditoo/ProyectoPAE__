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

        // Al hacer clic en un registro, se cargan sus datos en el formulario para edición
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
            mostrarMensaje("Error al sincronizar catálogos de base de datos: " + e.getMessage(), true);
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
        String nombre = txtNombre.getText().trim();
        String precioTxt = txtPrecio.getText().trim();
        Categoria categoria = cmbCategoria.getValue();
        Marca marca = cmbMarca.getValue();

        if (nombre.isEmpty() || precioTxt.isEmpty() || categoria == null || marca == null) {
            mostrarMensaje("Validación: Complete todos los campos antes de guardar.", true);
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(precioTxt);
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarMensaje("Validación: El precio debe ser un número positivo.", true);
                return;
            }

            Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getNombreProducto().equalsIgnoreCase(nombre)) {
                // Actualizar registro existente vía ORM
                seleccionado.setPrecioVenta(precio);
                seleccionado.setCategoria(categoria);
                seleccionado.setMarca(marca);
                productoDAO.actualizar(seleccionado);
                mostrarMensaje("Producto #" + seleccionado.getIdProducto() + " actualizado en PostgreSQL.", false);
            } else {
                // Insertar nuevo registro vía ORM
                Producto nuevo = new Producto(nombre, precio, 5, categoria, marca);
                productoDAO.guardar(nuevo);
                mostrarMensaje("Nuevo producto persistido exitosamente en la base de datos.", false);
            }

            handleLimpiar();
            refrescarTabla();
        } catch (NumberFormatException e) {
            mostrarMensaje("Validación: El formato del precio es incorrecto.", true);
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
            mostrarMensaje("Restricción referencial: El producto tiene lotes o consumos activos.", true);
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