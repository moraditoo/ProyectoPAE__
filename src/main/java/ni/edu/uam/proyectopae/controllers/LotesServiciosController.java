package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

        // Bloqueo de fechas pasadas en el DatePicker de caducidad
        dpFechaCaducidad.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #a0a0a0;");
                }
            }
        });

        tblLotes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                txtCodigoLote.setText(sel.getCodigoLote());
                dpFechaCaducidad.setValue(sel.getFechaCaducidad());
                txtCantidad.setText(String.valueOf(sel.getCantidadDisponible()));
                cmbProducto.setValue(sel.getProducto());
            }
        });

        refrescarTabla();
    }

    @FXML
    protected void handleEnterGuardar(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleGuardarLote();
        }
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
        String codigo = txtCodigoLote.getText() != null ? txtCodigoLote.getText().trim() : "";
        LocalDate vencimiento = dpFechaCaducidad.getValue();
        String cantidadTxt = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";

        if (producto == null) {
            mostrarMensaje("Validación: Debe seleccionar el producto correspondiente.", true);
            return;
        }

        if (codigo.length() < 3 || !codigo.matches("^[A-Za-z0-9_-]+$")) {
            mostrarMensaje("Validación: El código debe tener mínimo 3 caracteres alfanuméricos (sin espacios).", true);
            return;
        }

        if (vencimiento == null || vencimiento.isBefore(LocalDate.now())) {
            mostrarMensaje("Alerta Sanitaria: Debe seleccionar una fecha de caducidad futura.", true);
            return;
        }

        if (!cantidadTxt.matches("^[1-9]\\d*$")) {
            mostrarMensaje("Validación: La cantidad ingresada debe ser un entero positivo mayor a cero.", true);
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTxt);

            Lote seleccionado = tblLotes.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getCodigoLote().equalsIgnoreCase(codigo)) {
                seleccionado.setFechaCaducidad(vencimiento);
                seleccionado.setCantidadDisponible(cantidad);
                seleccionado.setProducto(producto);
                loteDAO.actualizar(seleccionado);
                mostrarMensaje("Lote #" + seleccionado.getIdLote() + " actualizado correctamente.", false);
            } else {
                Lote nuevo = new Lote(codigo, vencimiento, cantidad, producto);
                loteDAO.guardar(nuevo);
                mostrarMensaje("Lote registrado en PostgreSQL con Hibernate.", false);
            }

            handleLimpiar();
            refrescarTabla();
        } catch (Exception e) {
            mostrarMensaje("Error al persistir lote (código duplicado o fallo BD): " + e.getMessage(), true);
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
            mostrarMensaje("Restricción referencial: No se puede eliminar, el lote posee consumos registrados.", true);
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