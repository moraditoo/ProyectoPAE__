package ni.edu.uam.proyectopae.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.proyectopae.dao.ClienteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.dao.VentaDAO;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.DetalleVenta;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.models.Venta;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class VentasController implements Refrescable {

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCantidad;
    @FXML private Label lblDisponible;
    @FXML private Label lblTotal;
    @FXML private Label lblGananciaEstimada;
    @FXML private Label lblVentasHoy;

    @FXML private TableView<DetalleVenta> tblCarrito;
    @FXML private TableColumn<DetalleVenta, String> colCarProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colCarCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colCarPrecio;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colCarSubtotal;

    @FXML private TableView<Venta> tblVentas;
    @FXML private TableColumn<Venta, Integer> colVenId;
    @FXML private TableColumn<Venta, LocalDateTime> colVenFecha;
    @FXML private TableColumn<Venta, String> colVenCliente;
    @FXML private TableColumn<Venta, BigDecimal> colVenTotal;
    @FXML private TableColumn<Venta, BigDecimal> colVenGanancia;

    @FXML private TableView<DetalleVenta> tblDetalle;
    @FXML private TableColumn<DetalleVenta, String> colDetProducto;
    @FXML private TableColumn<DetalleVenta, Integer> colDetCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colDetPrecio;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colDetCosto;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colDetSubtotal;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    private final ObservableList<DetalleVenta> carrito = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colCarProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCarCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCarPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colCarSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        Formato.columnaMoneda(colCarPrecio);
        Formato.columnaMoneda(colCarSubtotal);
        tblCarrito.setItems(carrito);

        colVenId.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        colVenFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colVenCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colVenTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colVenGanancia.setCellValueFactory(new PropertyValueFactory<>("ganancia"));
        Formato.columnaFechaHora(colVenFecha);
        Formato.columnaMoneda(colVenTotal);
        Formato.columnaMoneda(colVenGanancia);

        colDetProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colDetPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colDetCosto.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
        colDetSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        Formato.columnaMoneda(colDetPrecio);
        Formato.columnaMoneda(colDetCosto);
        Formato.columnaMoneda(colDetSubtotal);

        // El combo de productos muestra precio y existencias
        cmbProducto.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null
                        : p.getNombreProducto() + "  ·  " + Formato.moneda(p.getPrecioVenta()) + "  ·  stock " + p.getStockActual());
            }
        });
        cmbProducto.valueProperty().addListener((obs, a, p) -> actualizarDisponible());

        tblVentas.getSelectionModel().selectedItemProperty().addListener((obs, a, v) -> cargarDetalle(v));

        refrescar();
        actualizarTotales();
    }

    @Override
    public void refrescar() {
        try {
            Integer idCliente = cmbCliente.getValue() != null ? cmbCliente.getValue().getIdCliente() : null;
            cmbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));
            if (idCliente != null) {
                cmbCliente.getItems().stream().filter(c -> idCliente.equals(c.getIdCliente()))
                        .findFirst().ifPresent(cmbCliente::setValue);
            }

            Integer idProducto = cmbProducto.getValue() != null ? cmbProducto.getValue().getIdProducto() : null;
            cmbProducto.setItems(FXCollections.observableArrayList(
                    productoDAO.listarConStock().stream().filter(p -> p.getStockActual() > 0).toList()));
            if (idProducto != null) {
                cmbProducto.getItems().stream().filter(p -> idProducto.equals(p.getIdProducto()))
                        .findFirst().ifPresent(cmbProducto::setValue);
            }

            tblVentas.setItems(FXCollections.observableArrayList(ventaDAO.listarConCliente()));
            BigDecimal hoy = tblVentas.getItems().stream()
                    .filter(v -> v.getFecha().toLocalDate().equals(LocalDate.now()))
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblVentasHoy.setText(Formato.moneda(hoy));
            actualizarDisponible();
        } catch (Exception e) {
            Alertas.error("Error de conexión", "No se pudo cargar la información de ventas:\n" + VentanaManager.causaRaiz(e));
        }
    }

    private int enCarrito(Producto p) {
        return carrito.stream()
                .filter(d -> d.getProducto().getIdProducto().equals(p.getIdProducto()))
                .mapToInt(DetalleVenta::getCantidad).sum();
    }

    private void actualizarDisponible() {
        Producto p = cmbProducto.getValue();
        if (p == null) {
            lblDisponible.setText("");
            return;
        }
        int disponible = p.getStockActual() - enCarrito(p);
        lblDisponible.setText("Disponible: " + disponible + " unidades (mínimo exigido: " + p.getStockMinimo() + ")");
    }

    @FXML
    protected void handleAgregar() {
        Producto p = cmbProducto.getValue();
        String cantTxt = txtCantidad.getText() == null ? "" : txtCantidad.getText().trim();

        if (p == null) {
            Alertas.validacion("Seleccione un producto.");
            return;
        }
        if (!cantTxt.matches("^[1-9]\\d{0,4}$")) {
            Alertas.validacion("La cantidad debe ser un número entero mayor a cero.");
            return;
        }
        int cantidad = Integer.parseInt(cantTxt);
        int disponible = p.getStockActual() - enCarrito(p);
        if (cantidad > disponible) {
            Alertas.advertencia("Stock insuficiente", "Solo hay " + disponible + " unidades disponibles de \"" + p.getNombreProducto() + "\".");
            return;
        }

        // Si el producto ya esta en el carrito se suma la cantidad
        DetalleVenta existente = carrito.stream()
                .filter(d -> d.getProducto().getIdProducto().equals(p.getIdProducto()))
                .findFirst().orElse(null);
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
            tblCarrito.refresh();
        } else {
            carrito.add(new DetalleVenta(p, cantidad));
        }

        int restante = disponible - cantidad;
        if (restante < p.getStockMinimo()) {
            Alertas.advertencia("Stock mínimo", "Después de esta venta quedarán " + restante + " unidades de \""
                    + p.getNombreProducto() + "\", por debajo del mínimo exigido (" + p.getStockMinimo() + ").");
        }

        txtCantidad.clear();
        actualizarDisponible();
        actualizarTotales();
    }

    @FXML
    protected void handleQuitar() {
        DetalleVenta sel = tblCarrito.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alertas.advertencia("Quitar producto", "Seleccione un producto del carrito.");
            return;
        }
        carrito.remove(sel);
        actualizarDisponible();
        actualizarTotales();
    }

    @FXML
    protected void handleVaciar() {
        if (!carrito.isEmpty() && !Alertas.confirmar("Vaciar carrito", "¿Quitar todos los productos del carrito?")) {
            return;
        }
        limpiarVenta();
    }

    @FXML
    protected void handleRegistrarVenta() {
        Cliente cliente = cmbCliente.getValue();
        if (cliente == null) {
            Alertas.validacion("Seleccione el cliente de la venta.");
            return;
        }
        if (carrito.isEmpty()) {
            Alertas.validacion("Agregue al menos un producto al carrito.");
            return;
        }

        Venta venta = new Venta(cliente);
        carrito.forEach(venta::agregarDetalle);

        if (!Alertas.confirmar("Confirmar venta", "Cliente: " + cliente.getNombreCompleto()
                + "\nProductos: " + carrito.size() + "\nTotal a cobrar: " + Formato.moneda(venta.getTotal()))) {
            return;
        }

        try {
            ventaDAO.registrar(venta);
            Alertas.info("Venta registrada", "Venta N° " + venta.getIdVenta() + " por " + Formato.moneda(venta.getTotal())
                    + " registrada correctamente.\nGanancia: " + Formato.moneda(venta.getGanancia()));
            limpiarVenta();
            refrescar();
            VentanaManager.notificarCambios(this);
        } catch (Exception e) {
            carrito.forEach(d -> d.setIdDetalle(null));
            Alertas.error("No se pudo registrar la venta", VentanaManager.causaRaiz(e));
            refrescar();
        }
    }

    private void limpiarVenta() {
        carrito.clear();
        cmbProducto.setValue(null);
        txtCantidad.clear();
        actualizarDisponible();
        actualizarTotales();
    }

    private void actualizarTotales() {
        BigDecimal total = carrito.stream().map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ganancia = carrito.stream().map(DetalleVenta::getGanancia).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total: " + Formato.moneda(total));
        lblGananciaEstimada.setText(carrito.isEmpty() ? "" : "Ganancia estimada: " + Formato.moneda(ganancia));
    }

    private void cargarDetalle(Venta v) {
        if (v == null) {
            tblDetalle.getItems().clear();
            return;
        }
        try {
            tblDetalle.setItems(FXCollections.observableArrayList(ventaDAO.listarDetalles(v.getIdVenta())));
        } catch (Exception e) {
            Alertas.error("Error", "No se pudo cargar el detalle: " + VentanaManager.causaRaiz(e));
        }
    }
}
