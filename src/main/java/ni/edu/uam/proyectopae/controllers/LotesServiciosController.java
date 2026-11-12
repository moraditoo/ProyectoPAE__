package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import ni.edu.uam.proyectopae.dao.DashboardDAO;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.EventosDatos;
import ni.edu.uam.proyectopae.util.Icono;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.Tablas;
import ni.edu.uam.proyectopae.util.Tema;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LotesServiciosController implements Refrescable {

    private static final int DIAS_ALERTA = DashboardDAO.DIAS_ALERTA_VENCIMIENTO;

    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCodigoLote;
    @FXML private DatePicker dpFechaCaducidad;
    @FXML private TextField txtCantidad;
    @FXML private Label lblModo;
    @FXML private Label lblAyuda;
    @FXML private TextField txtBuscar;

    @FXML private TableView<Lote> tblLotes;
    @FXML private TableColumn<Lote, Integer> colId;
    @FXML private TableColumn<Lote, String> colProducto;
    @FXML private TableColumn<Lote, String> colCodigo;
    @FXML private TableColumn<Lote, LocalDate> colCaducidad;
    @FXML private TableColumn<Lote, Integer> colCantidad;
    @FXML private TableColumn<Lote, Lote> colEstado;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ObservableList<Lote> listaLotes = FXCollections.observableArrayList();
    private Lote loteEnEdicion;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idLote"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoLote"));
        colCaducidad.setCellValueFactory(new PropertyValueFactory<>("fechaCaducidad"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadDisponible"));
        Tablas.formato(colCaducidad, Tablas::fecha);
        Tablas.centrar(colCantidad);
        Tablas.grafico(colEstado, LotesServiciosController::estadoLote);
        Tablas.colorearFilas(tblLotes, l -> {
            long dias = diasParaVencer(l);
            if (l.getCantidadDisponible() <= 0) return null;
            if (dias < 0) return Tema.Estado.PELIGRO;
            return dias <= DIAS_ALERTA ? Tema.Estado.ALERTA : null;
        });

        FilteredList<Lote> filtrados = new FilteredList<>(listaLotes, l -> true);
        txtBuscar.textProperty().addListener((obs, anterior, texto) -> {
            String filtro = texto == null ? "" : texto.trim().toLowerCase();
            filtrados.setPredicate(l -> filtro.isEmpty()
                    || l.getNombreProducto().toLowerCase().contains(filtro)
                    || l.getCodigoLote().toLowerCase().contains(filtro));
        });
        SortedList<Lote> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tblLotes.comparatorProperty());
        tblLotes.setItems(ordenados);

        // Bloqueo de fechas pasadas en el DatePicker de caducidad
        dpFechaCaducidad.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    Tema.diaDeshabilitado(this);
                }
            }
        });

        tblLotes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                cargarEnFormulario(sel);
            }
        });

        lblAyuda.setText("* Campos obligatorios. Los lotes que vencen en " + DIAS_ALERTA
                + " días o menos se marcan como próximos a vencer.");
        actualizarModo();
        refrescarTabla();
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
        try {
            recargarProductos();
        } catch (Exception e) {
            Alertas.errorBD("cargar los productos", e);
        }
    }

    private void recargarProductos() {
        Producto actual = cmbProducto.getValue();
        cmbProducto.setItems(FXCollections.observableArrayList(productoDAO.listarConRelaciones()));
        if (actual != null) cmbProducto.setValue(buscarProducto(actual.getIdProducto()));
    }

    @FXML
    public void refrescarTabla() {
        try {
            recargarProductos();
            listaLotes.setAll(loteDAO.listarConProducto());
        } catch (Exception e) {
            Alertas.errorBD("cargar los lotes", e);
        }
    }

    private void cargarEnFormulario(Lote lote) {
        loteEnEdicion = lote;
        txtCodigoLote.setText(lote.getCodigoLote());
        dpFechaCaducidad.setValue(lote.getFechaCaducidad());
        txtCantidad.setText(String.valueOf(lote.getCantidadDisponible()));
        cmbProducto.setValue(buscarProducto(lote.getProducto().getIdProducto()));
        actualizarModo();
    }

    /** Botón único "Guardar": inserta si no hay lote seleccionado y actualiza si lo hay. */
    @FXML
    protected void handleGuardarLote() {
        Producto producto = cmbProducto.getValue();
        String codigo = txtCodigoLote.getText() != null ? txtCodigoLote.getText().trim() : "";
        LocalDate vencimiento = dpFechaCaducidad.getValue();
        String cantidadTxt = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";

        List<String> faltantes = new ArrayList<>();
        if (producto == null) faltantes.add("Producto");
        if (codigo.isEmpty()) faltantes.add("Código de lote");
        if (cantidadTxt.isEmpty()) faltantes.add("Cantidad");
        if (vencimiento == null) faltantes.add("Fecha de caducidad");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }

        if (codigo.length() < 3 || codigo.length() > 50 || !codigo.matches("^[A-Za-z0-9_-]+$")) {
            Alertas.datoInvalido("El código de lote debe tener entre 3 y 50 caracteres alfanuméricos, guiones o guiones bajos (sin espacios).");
            return;
        }

        boolean fechaSinCambios = loteEnEdicion != null && vencimiento.equals(loteEnEdicion.getFechaCaducidad());
        if (vencimiento.isBefore(LocalDate.now()) && !fechaSinCambios) {
            Alertas.datoInvalido("La fecha de caducidad no puede ser anterior a hoy. No se pueden ingresar lotes ya vencidos.");
            return;
        }

        // Al editar se permite 0 (lote agotado); un lote nuevo debe ingresar al menos 1 unidad
        String patronCantidad = loteEnEdicion != null ? "^\\d{1,7}$" : "^[1-9]\\d{0,6}$";
        if (!cantidadTxt.matches(patronCantidad)) {
            Alertas.datoInvalido(loteEnEdicion != null
                    ? "La cantidad debe ser un número entero igual o mayor a 0."
                    : "La cantidad debe ser un número entero mayor a cero.");
            return;
        }

        try {
            Integer idActual = loteEnEdicion != null ? loteEnEdicion.getIdLote() : null;
            if (loteDAO.existeCodigo(codigo, idActual)) {
                Alertas.datoInvalido("Ya existe un lote con el código \"" + codigo + "\". El código debe ser único.");
                return;
            }

            int cantidad = Integer.parseInt(cantidadTxt);
            String accion;
            if (loteEnEdicion != null) {
                loteEnEdicion.setCodigoLote(codigo);
                loteEnEdicion.setFechaCaducidad(vencimiento);
                loteEnEdicion.setCantidadDisponible(cantidad);
                loteEnEdicion.setProducto(producto);
                loteDAO.actualizar(loteEnEdicion);
                accion = "Información actualizada correctamente";
            } else {
                loteDAO.guardar(new Lote(codigo, vencimiento, cantidad, producto));
                accion = "Registro realizado correctamente";
            }

            long dias = ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);
            String detalle = "Lote " + codigo + " de \"" + producto.getNombreProducto() + "\" con " + cantidad + " unidad(es).";
            if (dias < 0) {
                Alertas.advertencia(accion + " — producto vencido",
                        detalle + "\nEste lote venció el " + Tablas.fecha(vencimiento) + " y no debe venderse ni utilizarse.");
            } else if (dias <= DIAS_ALERTA) {
                Alertas.advertencia(accion + " — producto próximo a vencer",
                        detalle + "\nAtención: vence en " + dias + " día(s), el " + Tablas.fecha(vencimiento) + ".");
            } else {
                Alertas.exito(accion, detalle);
            }

            handleLimpiar();
            refrescarTabla();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            Alertas.errorBD("guardar el lote", e);
            refrescarTabla();
        }
    }

    @FXML
    protected void handleEliminarLote() {
        Lote sel = loteEnEdicion != null ? loteEnEdicion : tblLotes.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Ningún lote seleccionado", "Seleccione un lote de la tabla para eliminarlo.");
            return;
        }
        if (!Alertas.confirmar("¿Eliminar el lote " + sel.getCodigoLote() + "?",
                "Se eliminarán " + sel.getCantidadDisponible() + " unidad(es) de \"" + sel.getNombreProducto()
                        + "\" del inventario. Esta acción no se puede deshacer.")) {
            return;
        }

        try {
            loteDAO.eliminar(sel.getIdLote());
            Alertas.exito("Lote eliminado", "El lote " + sel.getCodigoLote() + " fue eliminado de la base de datos.");
            handleLimpiar();
            refrescarTabla();
            EventosDatos.publicar(this);
        } catch (Exception e) {
            if (Alertas.esErrorConexion(e)) {
                Alertas.errorBD("eliminar el lote", e);
            } else {
                Alertas.error("No se puede eliminar el lote", "El lote posee consumos registrados y debe conservarse para el historial.");
            }
        }
    }

    @FXML
    protected void handleLimpiar() {
        txtCodigoLote.clear();
        txtCantidad.clear();
        dpFechaCaducidad.setValue(null);
        cmbProducto.setValue(null);
        tblLotes.getSelectionModel().clearSelection();
        loteEnEdicion = null;
        actualizarModo();
    }

    private Producto buscarProducto(Integer id) {
        return cmbProducto.getItems().stream().filter(p -> p.getIdProducto().equals(id)).findFirst().orElse(null);
    }

    private void actualizarModo() {
        if (loteEnEdicion != null) {
            lblModo.setText("Editando #" + loteEnEdicion.getIdLote());
            lblModo.setGraphic(new Icono(Icono.Tipo.GUARDAR, 15));
        } else {
            lblModo.setText("Nuevo registro");
            lblModo.setGraphic(new Icono(Icono.Tipo.NUEVO, 15));
        }
        Tema.indicadorModo(lblModo, loteEnEdicion != null);
    }

    private static long diasParaVencer(Lote lote) {
        return ChronoUnit.DAYS.between(LocalDate.now(), lote.getFechaCaducidad());
    }

    private static Node estadoLote(Lote lote) {
        long dias = diasParaVencer(lote);
        if (lote.getCantidadDisponible() <= 0) {
            return Tablas.badge("Agotado", Tema.Estado.NEUTRO, Icono.Tipo.INVENTARIO);
        }
        if (dias < 0) {
            return Tablas.badge("Vencido hace " + (-dias) + " d", Tema.Estado.PELIGRO, Icono.Tipo.ERROR);
        }
        if (dias <= DIAS_ALERTA) {
            return Tablas.badge("Vence en " + dias + " d", Tema.Estado.ALERTA, Icono.Tipo.ADVERTENCIA);
        }
        return Tablas.badge("Vigente", Tema.Estado.OK, Icono.Tipo.EXITO);
    }
}
