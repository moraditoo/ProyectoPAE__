package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Producto;

public class InventarioController {
    @FXML private TextField txtNombre;
    @FXML private ListView<Producto> listaProductos;
    @FXML private Label lblEstado;
    private final ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() { refrescar(); }

    @FXML
    private void refrescar() {
        try {
            listaProductos.setItems(FXCollections.observableArrayList(productoDAO.listarConRelaciones()));
            lblEstado.setText("");
        } catch (Exception e) {
            lblEstado.setText("Error al cargar productos: " + e.getMessage());
        }
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty() || nombre.length() > 100) {
            lblEstado.setText("Ingrese un nombre de entre 1 y 100 caracteres.");
            return;
        }
        try {
            productoDAO.guardar(new Producto(nombre));
            txtNombre.clear();
            refrescar();
        } catch (Exception e) {
            lblEstado.setText("Error al guardar producto: " + e.getMessage());
        }
    }
}
