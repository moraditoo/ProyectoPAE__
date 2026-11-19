package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Linea de una venta. Guarda una "foto" del precio de venta y del precio de compra
 * al momento de vender, para que la ganancia historica no cambie si luego se
 * modifican los precios del producto.
 */
@Entity
@Table(name = "detalle_ventas")
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "ganancia", nullable = false, precision = 12, scale = 2)
    private BigDecimal ganancia;

    public DetalleVenta() {}

    public DetalleVenta(Producto producto, int cantidad) {
        this.producto = producto;
        this.precioUnitario = producto.getPrecioVenta();
        this.costoUnitario = producto.getPrecioCompra() != null ? producto.getPrecioCompra() : BigDecimal.ZERO;
        setCantidad(cantidad);
    }

    /** Cambia la cantidad y recalcula subtotal y ganancia. */
    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
        BigDecimal cant = BigDecimal.valueOf(cantidad);
        this.subtotal = precioUnitario.multiply(cant);
        this.ganancia = subtotal.subtract(costoUnitario.multiply(cant));
    }

    public String getNombreProducto() {
        return producto != null ? producto.getNombreProducto() : "";
    }

    public Integer getIdDetalle() { return idDetalle; }
    public void setIdDetalle(Integer idDetalle) { this.idDetalle = idDetalle; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getGanancia() { return ganancia; }
    public void setGanancia(BigDecimal ganancia) { this.ganancia = ganancia; }
}
