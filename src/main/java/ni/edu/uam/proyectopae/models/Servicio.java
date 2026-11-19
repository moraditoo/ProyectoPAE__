package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "nombre_servicio", nullable = false, length = 100)
    private String nombreServicio;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "tarifa_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaBase;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cita> citas = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "servicio_productos",
            joinColumns = @JoinColumn(name = "id_servicio"),
            inverseJoinColumns = @JoinColumn(name = "id_producto"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_servicio_producto",
                    columnNames = {"id_servicio", "id_producto"}
            )
    )
    private List<Producto> productos = new ArrayList<>();

    public Servicio() {}

    public Servicio(String nombreServicio, Integer duracionMinutos, BigDecimal tarifaBase) {
        this.nombreServicio = nombreServicio;
        this.duracionMinutos = duracionMinutos;
        this.tarifaBase = tarifaBase;
    }

    public Integer getIdServicio() { return idServicio; }
    public void setIdServicio(Integer idServicio) { this.idServicio = idServicio; }

    public String getNombreServicio() { return nombreServicio; }
    public void setNombreServicio(String nombreServicio) { this.nombreServicio = nombreServicio; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public BigDecimal getTarifaBase() { return tarifaBase; }
    public void setTarifaBase(BigDecimal tarifaBase) { this.tarifaBase = tarifaBase; }

    public List<Cita> getCitas() { return citas; }
    public void setCitas(List<Cita> citas) { this.citas = citas; }

    public List<Producto> getProductos() {
        return productos;
    }

    public void agregarProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException(
                    "El producto es obligatorio."
            );
        }

        if (!productos.contains(producto)) {
            productos.add(producto);
        }
    }

    public void quitarProducto(Producto producto) {
        productos.remove(producto);
    }

    @Override
    public String toString() {
        return nombreServicio + " (C$" + tarifaBase + ")";
    }
}