package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.time.LocalDate;

public class LotesServiciosController implements Refrescable {

    private static final String FILTRO_TODOS = "Todos los lotes";
    private static final String FILTRO_POR_VENCER = "Por vencer (" + Lote.DIAS_ALERTA_VENCIMIENTO + " días)";
    private static final String FILTRO_VENCIDOS = "Vencidos";

    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private ComboBox<String> cmbFiltro;
    @FXML private TextField txtCodigoLote;
    @FXML private DatePicker dpFechaCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private Label lblModo;
    @FXML private Label lblTotal;
    @FXML private Label lblLeyendaPorVencer;
    @FXML private Button btnGuardar;

    @FXML private TableView<Lote> tblLotes;
    @FXML private TableColumn<Lote, Integer> colId;
    @FXML private TableColumn<Lote, String> colProducto;
    @FXML private TableColumn<Lote, String> colCodigo;
    @FXML private TableColumn<Lote, LocalDate> colCaducidad;
    @FXML private TableColumn<Lote, Long> colDias;
    @FXML private TableColumn<Lote, Integer> colCantidad;
    @FXML private TableColumn<Lote, String> colEstado;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ObservableList<Lote> listaLotes = FXCollections.observableArrayList();
    private final FilteredList<Lote> lotesFiltrados = new FilteredList<>(listaLotes, l -> true);

    /** Lote seleccionado; null = registrar uno nuevo. */
    private Lote loteEnEdicion;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idLote"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoLote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("fechaCaducidad"));
        colDias.setCellValueFactory(new PropertyValueFactory<>("diasParaVencer"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadDisponible"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoCaducidad"));
        Formato.columnaFecha(colCaducidad);

        lblLeyendaPorVencer.setText("Vence en " + Lote.DIAS_ALERTA_VENCIMIENTO + " días o menos");

        // Colores segun la caducidad
        tblLotes.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Lote item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("fila-peligro", "fila-advertencia");
                if (!empty && item != null) {
                    if (item.isVencido()) getStyleClass().add("fila-peligro");
                    else if (item.isPorVencer()) getStyleClass().add("fila-advertencia");
                }
            }
        });

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

        cmbFiltro.setItems(FXCollections.observableArrayList(FILTRO_TODOS, FILTRO_POR_VENCER, FILTRO_VENCIDOS));
        cmbFiltro.setValue(FILTRO_TODOS);
        cmbFiltro.valueProperty().addListener((obs, a, b) -> aplicarFiltro());

        tblLotes.setItems(lotesFiltrados);
        tblLotes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) cargarEnFormulario(sel);
        });

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
            handleGuardarLote();
        }
    }

    public void cargarProductos() {
        Producto actual = cmbProducto.getValue();
        cmbProducto.setItems(FXCollections.observableArrayList(productoDAO.listarConRelaciones()));
        if (actual != null) seleccionarProducto(actual.getIdProducto());
    }

    @FXML
    public void refrescarTabla() {
        try {
            cargarProductos();
            listaLotes.setAll(loteDAO.listarConProducto());
            aplicarFiltro();
        } catch (Exception e) {
            Alertas.error("Error de conexión", "No se pudieron cargar los lotes:\n" + VentanaManager.causaRaiz(e));
        }
    }

    private void aplicarFiltro() {
        String f = cmbFiltro.getValue();
        lotesFiltrados.setPredicate(l -> {
            if (FILTRO_POR_VENCER.equals(f)) return l.isPorVencer();
            if (FILTRO_VENCIDOS.equals(f)) return l.isVencido();
            return true;
        });
        long porVencer = listaLotes.stream().filter(Lote::isPorVencer).count();
        long vencidos = listaLotes.stream().filter(Lote::isVencido).count();
        lblTotal.setText(lotesFiltrados.size() + " lotes · " + porVencer + " por vencer · " + vencidos + " vencidos");
    }

    private void cargarEnFormulario(Lote l) {
        loteEnEdicion = l;
        txtCodigoLote.setText(l.getCodigoLote());
        dpFechaCaducidad.setValue(l.getFechaCaducidad());
        txtCantidad.setText(String.valueOf(l.getCantidadDisponible()));
        seleccionarProducto(l.getProducto() != null ? l.getProducto().getIdProducto() : null);
        actualizarModo();
    }

    private void seleccionarProducto(Integer idProducto) {
        cmbProducto.setValue(null);
        if (idProducto == null) return;
        cmbProducto.getItems().stream()
                .filter(p -> idProducto.equals(p.getIdProducto()))
                .findFirst()
                .ifPresent(cmbProducto::setValue);
    }

    private void actualizarModo() {
        lblModo.getStyleClass().remove("editando");
        if (loteEnEdicion == null) {
            lblModo.setText("Nuevo lote");
            btnGuardar.setText("Guardar");
        } else {
            lblModo.setText("Editando lote #" + loteEnEdicion.getIdLote());
            lblModo.getStyleClass().add("editando");
            btnGuardar.setText("Guardar cambios");
        }
    }

    /** Un solo boton: registra un lote nuevo o actualiza el seleccionado. */
    @FXML
    protected void handleGuardarLote() {
        Producto producto = cmbProducto.getValue();
        String codigo = txtCodigoLote.getText() != null ? txtCodigoLote.getText().trim().toUpperCase() : "";
        LocalDate vencimiento = dpFechaCaducidad.getValue();
        String cantidadTxt = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";

        if (producto == null) {
            Alertas.validacion("Debe seleccionar el producto correspondiente.");
            return;
        }
        if (codigo.length() < 3 || !codigo.matches("^[A-Za-z0-9_-]+$")) {
            Alertas.validacion("El código debe tener mínimo 3 caracteres alfanuméricos (sin espacios).");
            return;
        }
        if (vencimiento == null || vencimiento.isBefore(LocalDate.now())) {
            Alertas.advertencia("Alerta sanitaria", "Debe seleccionar una fecha de caducidad futura.");
            return;
        }
        // Un lote nuevo debe traer existencias; al editar se permite dejarlo en 0 (agotado)
        String regexCantidad = loteEnEdicion == null ? "^[1-9]\\d*$" : "^\\d+$";
        if (!cantidadTxt.matches(regexCantidad)) {
            Alertas.validacion("La cantidad debe ser un número entero " + (loteEnEdicion == null ? "mayor a cero." : "igual o mayor a cero."));
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadTxt);

            if (loteEnEdicion != null) {
                loteEnEdicion.setCodigoLote(codigo);
                loteEnEdicion.setFechaCaducidad(vencimiento);
                loteEnEdicion.setCantidadDisponible(cantidad);
                loteEnEdicion.setProducto(producto);
                loteDAO.actualizar(loteEnEdicion);
                Alertas.info("Lote actualizado", "El lote " + codigo + " se actualizó correctamente.");
            } else {
                loteDAO.guardar(new Lote(codigo, vencimiento, cantidad, producto));
                Alertas.info("Lote registrado", "Se registraron " + cantidad + " unidades de " + producto.getNombreProducto() + ".");
            }

            if (vencimiento.isBefore(LocalDate.now().plusDays(Lote.DIAS_ALERTA_VENCIMIENTO + 1))) {
                Alertas.advertencia("Lote próximo a vencer", "El lote " + codigo + " vence el " + Formato.fecha(vencimiento)
                        + ".\nPriorice su venta o uso.");
            }

            handleLimpiar();
            refrescarTabla();
            VentanaManager.notificarCambios(this);
        } catch (Exception e) {
            Alertas.error("Error al guardar el lote", "Verifique que el código no esté repetido.\n" + VentanaManager.causaRaiz(e));
        }
    }

    @FXML
    protected void handleEliminarLote() {
        Lote sel = tblLotes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Eliminar lote", "Seleccione un lote para eliminar.");
            return;
        }
        if (!Alertas.confirmar("Eliminar lote", "¿Eliminar el lote " + sel.getCodigoLote() + " (" + sel.getCantidadDisponible() + " unidades)?")) {
            return;
        }

        try {
            loteDAO.eliminar(sel.getIdLote());
            handleLimpiar();
            refrescarTabla();
            VentanaManager.notificarCambios(this);
            Alertas.info("Lote eliminado", "El lote fue eliminado de la base de datos.");
        } catch (Exception e) {
            Alertas.error("No se puede eliminar", "El lote posee consumos o ventas registradas.");
        }
    }

    @FXML
    protected void handleLimpiar() {
        loteEnEdicion = null;
        txtCodigoLote.clear();
        txtCantidad.clear();
        dpFechaCaducidad.setValue(null);
        cmbProducto.setValue(null);
        tblLotes.getSelectionModel().clearSelection();
        actualizarModo();
    }
}
