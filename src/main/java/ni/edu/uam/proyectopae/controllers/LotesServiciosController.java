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

        tblLotes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                txtCodigoLote.setText(sel.getCodigoLote());
                dpFechaCaducidad.setValue(sel.getFechaCaducidad());
                txtCantidad.setText(String.valueOf(sel.getCantidadDisponible()));
                cmbProducto.setValue(sel.getProducto());
            }
        });

        cargarProductos();
        refrescarTabla();
    }

    public void cargarProductos() {
        try {
            cmbProducto.setItems(FXCollections.observableArrayList(productoDAO.listarConRelaciones()));
        } catch (Exception e) {
            mostrarMensaje("Error al sincronizar productos desde PostgreSQL: " + e.getMessage(), true);
        }
    }

    @FXML
    public void refrescarTabla() {
        try {
            cargarProductos();
            listaLotes.setAll(loteDAO.listarConProducto());
            tblLotes.setItems(listaLotes);
        } catch (Exception e) {
            mostrarMensaje("Error al cargar lotes: " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleGuardarLote() {
        Producto producto = cmbProducto.getValue();
        String codigo = txtCodigoLote.getText().trim();
        LocalDate vencimiento = dpFechaCaducidad.getValue();
        String cantidadTxt = txtCantidad.getText().trim();

        if (producto == null || codigo.isEmpty() || vencimiento == null || cantidadTxt.isEmpty()) {
            mostrarMensaje("Validación: Debe completar todos los campos del lote.", true);
            return;
        }

        if (vencimiento.isBefore(LocalDate.now())) {
            mostrarMensaje("Alerta Sanitaria: No se permite registrar productos con fecha de vencimiento expirada.", true);
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTxt);
            if (cantidad <= 0) {
                mostrarMensaje("Validación: El stock disponible debe ser mayor a 0.", true);
                return;
            }

            Lote seleccionado = tblLotes.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getCodigoLote().equalsIgnoreCase(codigo)) {
                // Actualizar lote existente vía Hibernate merge
                seleccionado.setFechaCaducidad(vencimiento);
                seleccionado.setCantidadDisponible(cantidad);
                seleccionado.setProducto(producto);
                loteDAO.actualizar(seleccionado);
                mostrarMensaje("Lote #" + seleccionado.getIdLote() + " actualizado en PostgreSQL.", false);
            } else {
                // Persistir nuevo lote vía Hibernate persist
                Lote nuevo = new Lote(codigo, vencimiento, cantidad, producto);
                loteDAO.guardar(nuevo);
                mostrarMensaje("Lote registrado exitosamente con Hibernate.", false);
            }

            handleLimpiar();
            refrescarTabla();
        } catch (NumberFormatException e) {
            mostrarMensaje("Validación: La cantidad debe ser un número entero.", true);
        } catch (Exception e) {
            mostrarMensaje("Error en base de datos (posible código de lote duplicado): " + e.getMessage(), true);
        }
    }

    @FXML
    protected void handleEliminarLote() {
        Lote sel = tblLotes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarMensaje("Seleccione un lote para eliminar.", true);
            return;
        }

        try {
            loteDAO.eliminar(sel.getIdLote());
            mostrarMensaje("Lote eliminado de la base de datos.", false);
            handleLimpiar();
            refrescarTabla();
        } catch (Exception e) {
            mostrarMensaje("Restricción referencial: No se puede eliminar, el lote tiene registros de consumo.", true);
        }
    }

    @FXML
    protected void handleLimpiar() {
        txtCodigoLote.clear();
        txtCantidad.clear();
        dpFechaCaducidad.setValue(null);
        cmbProducto.setValue(null);
        tblLotes.getSelectionModel().clearSelection();
    }

    private void mostrarMensaje(String texto, boolean esError) {
        lblEstado.setStyle(esError ? "-fx-text-fill: #b30000; -fx-font-weight: bold;" : "-fx-text-fill: #1b5e20; -fx-font-weight: bold;");
        lblEstado.setText(texto);
    }
}