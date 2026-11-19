package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Encabezado de una venta de productos a un cliente. */
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // Suma de los subtotales (precio de venta * cantidad)
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Suma de (precio de compra * cantidad): permite calcular la ganancia de la venta
    @Column(name = "costo_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {}

    public Venta(Cliente cliente) {
        this.cliente = cliente;
    }

    /** Agrega una linea, sincroniza ambos lados y recalcula los totales. */
    public void agregarDetalle(DetalleVenta detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El detalle es obligatorio."
            );
        }

        if (detalle.getVenta() != null
                && detalle.getVenta() != this) {

            throw new IllegalArgumentException(
                    "El detalle ya pertenece a otra venta."
            );
        }

        if (!detalles.contains(detalle)) {
            detalles.add(detalle);
        }

        detalle.setVenta(this);
        recalcularTotales();
    }

    /** Quita una linea, rompe el enlace en ambos lados y recalcula los totales. */
    public void quitarDetalle(DetalleVenta detalle) {
        if (detalles.remove(detalle)) {
            detalle.setVenta(null);
            recalcularTotales();
        }
    }

    public void recalcularTotales() {
        total = BigDecimal.ZERO;
        costoTotal = BigDecimal.ZERO;
        for (DetalleVenta d : detalles) {
            total = total.add(d.getSubtotal());
            costoTotal = costoTotal.add(d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
        }
    }

    public BigDecimal getGanancia() {
        return total.subtract(costoTotal);
    }

    public String getNombreCliente() {
        return cliente != null ? cliente.getNombres() + " " + cliente.getApellidos() : "";
    }

    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal costoTotal) { this.costoTotal = costoTotal; }

    public List<DetalleVenta> getDetalles() { return detalles; }

    public void setDetalles(List<DetalleVenta> nuevosDetalles) {
        List<DetalleVenta> copia = nuevosDetalles == null
                ? List.of()
                : new ArrayList<>(nuevosDetalles);

        for (DetalleVenta detalle : new ArrayList<>(detalles)) {
            quitarDetalle(detalle);
        }

        for (DetalleVenta detalle : copia) {
            agregarDetalle(detalle);
        }
    }
}