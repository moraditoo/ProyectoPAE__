package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;

import java.time.LocalDate;

public class LotesServiciosController {

    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCodigoLote;
    @FXML private DatePicker dpFechaCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private Label lblEstado;

    @FXML private TableView<Lote> tblLotes;
    @FXML private TableColumn<Lote, Integer> colId;
    @FXML private TableColumn<Lote, String> colProducto;
    @FXML private TableColumn<Lote, String> colCodigo;
    @FXML private TableColumn<Lote, LocalDate> colCaducidad;
    @FXML private TableColumn<Lote, Integer> colCantidad;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ObservableList<Lote> listaLotes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idLote"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoLote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("fechaCaducidad"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadDisponible"));

        cargarProductos();
        refrescarTabla();
    }

    private void cargarProductos() {
        try {
            cmbProducto.setItems(FXCollections.observableArrayList(productoDAO.listarConRelaciones()));
        } catch (Exception e) {
            lblEstado.setText("Error al cargar productos: " + e.getMessage());
        }
    }

    private void refrescarTabla() {
        try {
            listaLotes.setAll(loteDAO.listarConProducto());
            tblLotes.setItems(listaLotes);
        } catch (Exception e) {
            lblEstado.setText("Error al cargar lotes: " + e.getMessage());
        }
    }

    @FXML
    protected void handleGuardarLote() {
        Producto prod = cmbProducto.getValue();
        String cod = txtCodigoLote.getText().trim();
        LocalDate venc = dpFechaCaducidad.getValue();
        String cantTxt = txtCantidad.getText().trim();

        if (prod == null || cod.isEmpty() || venc == null || cantTxt.isEmpty()) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Validacion: Complete todos los campos del lote.");
            return;
        }

        if (venc.isBefore(LocalDate.now())) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Alerta sanitaria: La fecha de caducidad no puede ser anterior a hoy.");
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantTxt);
            if (cantidad <= 0) {
                lblEstado.setStyle("-fx-text-fill: red;");
                lblEstado.setText("Validacion: La cantidad disponible debe ser superior a cero.");
                return;
            }

            Lote lote = new Lote(cod, venc, cantidad, prod);
            loteDAO.guardar(lote);

            txtCodigoLote.clear();
            txtCantidad.clear();
            lblEstado.setStyle("-fx-text-fill: green;");
            lblEstado.setText("Lote registrado correctamente en el inventario.");
            refrescarTabla();
        } catch (NumberFormatException e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Formato numerico incorrecto para la cantidad.");
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Error al registrar lote (verifique codigo unico): " + e.getMessage());
        }
    }

    @FXML
    protected void handleEliminarLote() {
        Lote sel = tblLotes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("Seleccione un lote de la lista para eliminar.");
            return;
        }

        try {
            loteDAO.eliminar(sel.getIdLote());
            lblEstado.setStyle("-fx-text-fill: green;");
            lblEstado.setText("Lote eliminado con exito.");
            refrescarTabla();
        } catch (Exception e) {
            lblEstado.setStyle("-fx-text-fill: red;");
            lblEstado.setText("No se puede eliminar: El lote posee registros de consumo.");
        }
    }
}