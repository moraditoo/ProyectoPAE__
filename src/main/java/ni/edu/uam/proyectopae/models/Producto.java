package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    // Precio al que la tienda compra el producto al proveedor (nullable para no romper registros antiguos)
    @Column(name = "precio_compra", precision = 10, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_marca", nullable = false)
    private Marca marca;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Lote> lotes = new ArrayList<>();

    // Existencias vigentes (lotes no vencidos). Se calcula en ProductoDAO, no se guarda en la BD.
    @Transient
    private int stockActual;

    public Producto() {}

    public Producto(String nombreProducto, BigDecimal precioCompra, BigDecimal precioVenta, Integer stockMinimo, Categoria categoria, Marca marca) {
        this(nombreProducto, precioVenta, stockMinimo, categoria, marca);
        this.precioCompra = precioCompra;
    }

    public Producto(String nombreProducto, BigDecimal precioVenta, Integer stockMinimo, Categoria categoria, Marca marca) {
        this.nombreProducto = nombreProducto;
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
        this.marca = marca;
    }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public BigDecimal getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(BigDecimal precioCompra) { this.precioCompra = precioCompra; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }

    public List<Lote> getLotes() { return lotes; }
    public void setLotes(List<Lote> lotes) { this.lotes = lotes; }

    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }

    public boolean isBajoStockMinimo() {
        return stockMinimo != null && stockActual < stockMinimo;
    }

    /** Ganancia por unidad = precio de venta - precio de compra. */
    public BigDecimal getGananciaUnitaria() {
        BigDecimal venta = precioVenta != null ? precioVenta : BigDecimal.ZERO;
        BigDecimal compra = precioCompra != null ? precioCompra : BigDecimal.ZERO;
        return venta.subtract(compra);
    }

    /** Margen de ganancia sobre el precio de venta, en porcentaje. */
    public BigDecimal getMargen() {
        if (precioVenta == null || precioVenta.signum() == 0) return BigDecimal.ZERO;
        return getGananciaUnitaria().multiply(BigDecimal.valueOf(100)).divide(precioVenta, 2, RoundingMode.HALF_UP);
    }

    public String getNombreCategoria() {
        return categoria != null ? categoria.getNombre() : "";
    }

    public String getNombreMarca() {
        return marca != null ? marca.getNombreMarca() : "";
    }

    @Override
    public String toString() { return nombreProducto; }
}