package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;

@Entity
@Table(name = "consumos_insumos")
public class ConsumoInsumo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idConsumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    public Integer getIdConsumo() { return idConsumo; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
}
