package ni.edu.uam.proyectopae.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombres", nullable = false, length = 60)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 60)
    private String apellidos;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "alergias_piel", columnDefinition = "TEXT")
    private String alergiasPiel;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "sexo", length = 20)
    private String sexo;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cita> citas = new ArrayList<>();

    public Cliente() {}

    public Cliente(String nombres, String apellidos, String telefono, String alergiasPiel) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.alergiasPiel = alergiasPiel;
    }

    public Cliente(String nombres, String apellidos, String telefono, String email, String sexo, String alergiasPiel) {
        this(nombres, apellidos, telefono, alergiasPiel);
        this.email = email;
        this.sexo = sexo;
    }

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getAlergiasPiel() { return alergiasPiel; }
    public void setAlergiasPiel(String alergiasPiel) { this.alergiasPiel = alergiasPiel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public List<Cita> getCitas() { return citas; }
    public void setCitas(List<Cita> citas) { this.citas = citas; }

    @Override
    public String toString() {
        return nombres + " " + apellidos;
    }
}