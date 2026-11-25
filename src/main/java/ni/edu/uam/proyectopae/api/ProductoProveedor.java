package ni.edu.uam.proyectopae.api;

import java.math.BigDecimal;

/**
 * Producto del catálogo del proveedor tal como llega en el JSON de la API.
 * Cada atributo coincide con una clave del JSON:
 * {"id":1,"codigo":"PRV-001","nombre":"...","marca":"...","categoria":"...",
 *  "precio":520.00,"existencias":24,"descripcion":"..."}
 * Jackson usa el constructor vacío y los setters; la tabla de JavaFX usa los getters.
 */
public class ProductoProveedor {

    private Long id;
    private String codigo;
    private String nombre;
    private String marca;
    private String categoria;
    private BigDecimal precio;
    private Integer existencias;
    private String descripcion;

    public ProductoProveedor() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getExistencias() { return existencias; }
    public void setExistencias(Integer existencias) { this.existencias = existencias; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}