package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;

@Entity
@Table(name = "consumo_insumos")
public class ConsumoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consumo")
    private Integer idConsumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @Column(name = "cantidad_descontada", nullable = false)
    private Integer cantidadDescontada;

    public ConsumoInsumo() {}

    public ConsumoInsumo(Cita cita, Lote lote, Integer cantidadDescontada) {
        this.cita = cita;
        this.lote = lote;
        this.cantidadDescontada = cantidadDescontada;
    }

    public Integer getIdConsumo() { return idConsumo; }
    public void setIdConsumo(Integer idConsumo) { this.idConsumo = idConsumo; }

    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }

    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }

    public Integer getCantidadDescontada() { return cantidadDescontada; }
    public void setCantidadDescontada(Integer cantidadDescontada) { this.cantidadDescontada = cantidadDescontada; }
}