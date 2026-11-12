package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.dao.StockInsuficienteException;
import ni.edu.uam.proyectopae.dao.VentaDAO;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.DetalleVenta;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.models.Venta;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.EventosDatos;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.Tablas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VentasController implements Refrescable {

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCantidad;
    @FXML private Label lblDisponible;
    @FXML private Label lblTotal;

    @FXML private TableView<DetalleVenta> tblDetalle;
    @FXML private TableColumn<DetalleVenta, String> colDetProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colDetCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colDetPrecio;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colDetSubtotal;

    @FXML private TableView<Venta> tblVentas;
    @FXML private TableColumn<Venta, Integer> colVentaId;
    @FXML private TableColumn<Venta, LocalDateTime> colVentaFecha;
    @FXML private TableColumn<Venta, String> colVentaCliente;
    @FXML private TableColumn<Venta, BigDecimal> colVentaTotal;
    @FXML private TableColumn<Venta, BigDecimal> colVentaCosto;
    @FXML private TableColumn<Venta, BigDecimal> colVentaGanancia;

    @FXML private TableView<DetalleVenta> tblDetalleHistorial;
    @FXML private TableColumn<DetalleVenta, String> colHistProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colHistCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colHistPrecio;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colHistCosto;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colHistSubtotal;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final LoteDAO loteDAO = new LoteDAO();

    private final ObservableList<DetalleVenta> carrito = FXCollections.observableArrayList();
    private final ObservableList<Venta> ventas = FXCollections.observableArrayList();
    private Map<Integer, Long> stockVigente = new HashMap<>();

    @FXML
    public void initialize() {
        colDetProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colDetPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colDetSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        Tablas.formatoDinero(colDetPrecio);
        Tablas.formatoDinero(colDetSubtotal);
        Tablas.centrar(colDetCantidad);
        tblDetalle.setItems(carrito);

        colVentaId.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        colVentaFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colVentaCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colVentaTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colVentaCosto.setCellValueFactory(new PropertyValueFactory<>("costoTotal"));
        colVentaGanancia.setCellValueFactory(new PropertyValueFactory<>("ganancia"));
        Tablas.formato(colVentaFecha, Tablas::fechaHora);
        Tablas.formatoDinero(colVentaTotal);
        Tablas.formatoDinero(colVentaCosto);
        Tablas.formatoDinero(colVentaGanancia);
        tblVentas.setItems(ventas);

        colHistProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colHistCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colHistPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colHistCosto.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
        colHistSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        Tablas.formatoDinero(colHistPrecio);
        Tablas.formatoDinero(colHistCosto);
        Tablas.formatoDinero(colHistSubtotal);
        Tablas.centrar(colHistCantidad);

        cmbProducto.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null
                        : p.getNombreProducto() + "  —  " + Tablas.dinero(p.getPrecioVenta()) + "  (" + stockDe(p) + " disp.)");
            }
        });
        cmbProducto.valueProperty().addListener((obs, anterior, p) -> mostrarDisponible(p));

        tblVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, venta) -> cargarDetalleHistorial(venta));

        actualizarTotal();
        refrescarTabla();
    }

    @Override
    public void refrescar() {
        refrescarTabla();
    }

    @FXML
    public void refrescarTabla() {
        try {
            Cliente clienteActual = cmbCliente.getValue();
            Producto productoActual = cmbProducto.getValue();

            stockVigente = loteDAO.stockVigentePorProducto();
            cmbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));
            cmbProducto.setItems(FXCollections.observableArrayList(productoDAO.listarConRelaciones()));
            ventas.setAll(ventaDAO.listarConCliente());

            if (clienteActual != null) {
                cmbCliente.getItems().stream().filter(c -> c.getIdCliente().equals(clienteActual.getIdCliente()))
                        .findFirst().ifPresentOrElse(cmbCliente::setValue, () -> cmbCliente.setValue(null));
            }
            if (productoActual != null) {
                cmbProducto.getItems().stream().filter(p -> p.getIdProducto().equals(productoActual.getIdProducto()))
                        .findFirst().ifPresentOrElse(cmbProducto::setValue, () -> cmbProducto.setValue(null));
            }
            mostrarDisponible(cmbProducto.getValue());
        } catch (Exception e) {
            Alertas.errorBD("cargar la información de ventas", e);
        }
    }

    @FXML
    protected void handleAgregarProducto() {
        Producto producto = cmbProducto.getValue();
        String cantidadTxt = txtCantidad.getText() != null ? txtCantidad.getText().trim() : "";

        List<String> faltantes = new ArrayList<>();
        if (producto == null) faltantes.add("Producto");
        if (cantidadTxt.isEmpty()) faltantes.add("Cantidad");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }
        if (!cantidadTxt.matches("^[1-9]\\d{0,5}$")) {
            Alertas.datoInvalido("La cantidad debe ser un número entero mayor a cero.");
            return;
        }

        int cantidad = Integer.parseInt(cantidadTxt);
        Optional<DetalleVenta> existente = carrito.stream()
                .filter(d -> d.getProducto().getIdProducto().equals(producto.getIdProducto())).findFirst();
        int totalSolicitado = cantidad + existente.map(DetalleVenta::getCantidad).orElse(0);
        long disponible = stockDe(producto);

        if (disponible <= 0) {
            Alertas.advertencia("Producto sin existencias",
                    "\"" + producto.getNombreProducto() + "\" no tiene unidades en lotes vigentes. Registre un lote nuevo antes de venderlo.");
            return;
        }
        if (totalSolicitado > disponible) {
            Alertas.advertencia("Existencias insuficientes",
                    "Solo hay " + disponible + " unidad(es) vigentes de \"" + producto.getNombreProducto() + "\" y la venta pide " + totalSolicitado + ".");
            return;
        }

        if (existente.isPresent()) {
            existente.get().setCantidad(totalSolicitado);
            existente.get().recalcular();
            tblDetalle.refresh();
        } else {
            carrito.add(new DetalleVenta(producto, cantidad));
        }
        txtCantidad.clear();
        cmbProducto.setValue(null);
        actualizarTotal();
    }

    @FXML
    protected void handleQuitarProducto() {
        DetalleVenta seleccionado = tblDetalle.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.advertencia("Ningún producto seleccionado", "Seleccione un producto del detalle para quitarlo de la venta.");
            return;
        }
        carrito.remove(seleccionado);
        actualizarTotal();
    }

    @FXML
    protected void handleNuevaVenta() {
        if (!carrito.isEmpty() && !Alertas.confirmar("¿Descartar la venta en curso?",
                "Se quitarán los " + carrito.size() + " producto(s) agregados.")) {
            return;
        }
        limpiarVenta();
    }

    @FXML
    protected void handleGuardarVenta() {
        Cliente cliente = cmbCliente.getValue();
        List<String> faltantes = new ArrayList<>();
        if (cliente == null) faltantes.add("Cliente");
        if (carrito.isEmpty()) faltantes.add("Al menos un producto en el detalle");
        if (!faltantes.isEmpty()) {
            Alertas.camposObligatorios(faltantes);
            return;
        }

        try {
            Venta venta = ventaDAO.registrar(cliente, new ArrayList<>(carrito));
            List<DetalleVenta> vendidos = new ArrayList<>(carrito);
            limpiarVenta();
            refrescarTabla();
            EventosDatos.publicar(this);

            Alertas.exito("Registro realizado correctamente",
                    "Venta N.º " + venta.getIdVenta() + " a " + cliente + " por " + Tablas.dinero(venta.getTotal())
                            + ".\nGanancia de la venta: " + Tablas.dinero(venta.getGanancia()) + ".");
            avisarStockBajo(vendidos);
        } catch (StockInsuficienteException e) {
            Alertas.advertencia("Existencias insuficientes", e.getMessage());
            refrescarTabla();
        } catch (Exception e) {
            Alertas.errorBD("registrar la venta", e);
        }
    }

    /** Tras una venta, avisa si algún producto vendido quedó con existencias en o por debajo del mínimo. */
    private void avisarStockBajo(List<DetalleVenta> vendidos) {
        StringBuilder sb = new StringBuilder();
        for (DetalleVenta d : vendidos) {
            Producto p = d.getProducto();
            long stock = stockDe(p);
            if (stock <= p.getStockMinimo()) {
                sb.append("\n  •  ").append(p.getNombreProducto()).append(": ").append(stock)
                        .append(" unidad(es) (mínimo ").append(p.getStockMinimo()).append(")");
            }
        }
        if (!sb.isEmpty()) {
            Alertas.advertencia("Existencias por debajo del mínimo",
                    "Los siguientes productos necesitan reposición:\n" + sb);
        }
    }

    private void cargarDetalleHistorial(Venta venta) {
        if (venta == null) {
            tblDetalleHistorial.getItems().clear();
            return;
        }
        try {
            tblDetalleHistorial.setItems(FXCollections.observableArrayList(ventaDAO.listarDetalles(venta.getIdVenta())));
        } catch (Exception e) {
            Alertas.errorBD("cargar el detalle de la venta", e);
        }
    }

    private void limpiarVenta() {
        carrito.clear();
        cmbCliente.setValue(null);
        cmbProducto.setValue(null);
        txtCantidad.clear();
        actualizarTotal();
    }

    private void mostrarDisponible(Producto p) {
        if (p == null) {
            lblDisponible.setText("Seleccione un producto para ver sus existencias vigentes.");
            return;
        }
        long stock = stockDe(p);
        lblDisponible.setText("Precio: " + Tablas.dinero(p.getPrecioVenta()) + "   ·   Disponible: " + stock + " unidad(es)"
                + (stock <= p.getStockMinimo() ? "   ·   ⚠ en o bajo el mínimo (" + p.getStockMinimo() + ")" : ""));
    }

    private void actualizarTotal() {
        BigDecimal total = carrito.stream().map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText(Tablas.dinero(total));
    }

    private long stockDe(Producto p) {
        return stockVigente.getOrDefault(p.getIdProducto(), 0L);
    }
}
