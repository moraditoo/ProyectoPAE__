package ni.edu.uam.catalogo.controller;

import jakarta.validation.Valid;
import ni.edu.uam.catalogo.model.ProductoProveedor;
import ni.edu.uam.catalogo.repository.ProductoProveedorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Endpoints del catálogo del proveedor.
 *
 * GET    /api/productos?buscar=&categoria=  lista con filtros        200
 * GET    /api/productos/categorias           categorías disponibles   200
 * GET    /api/productos/{id}                 detalle de un producto   200 / 404
 * POST   /api/productos                      registra un producto     201 / 400 / 409
 * PUT    /api/productos/{id}                 actualiza un producto    200 / 400 / 404 / 409
 * DELETE /api/productos/{id}                 elimina un producto      204 / 404
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoProveedorController {

    private final ProductoProveedorRepository repo;

    public ProductoProveedorController(ProductoProveedorRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ProductoProveedor> listar(@RequestParam(name = "buscar", required = false) String buscar,
                                          @RequestParam(name = "categoria", required = false) String categoria) {
        String texto = normalizar(buscar);
        String categoriaBuscada = normalizar(categoria);

        return repo.findAllByOrderByNombreAsc().stream()
                .filter(p -> texto.isEmpty()
                        || normalizar(p.getNombre()).contains(texto)
                        || normalizar(p.getMarca()).contains(texto)
                        || normalizar(p.getCodigo()).contains(texto))
                .filter(p -> categoriaBuscada.isEmpty()
                        || normalizar(p.getCategoria()).equals(categoriaBuscada))
                .toList();
    }

    @GetMapping("/categorias")
    public List<String> categorias() {
        return repo.findAll().stream()
                .map(ProductoProveedor::getCategoria)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductoProveedor obtener(@PathVariable("id") Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoProveedor crear(@Valid @RequestBody ProductoProveedor datos) {
        datos.setId(null); // el ID siempre lo asigna la base de datos
        datos.setCodigo(datos.getCodigo().trim().toUpperCase(Locale.ROOT));
        if (repo.existsByCodigoIgnoreCase(datos.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un producto con el código " + datos.getCodigo());
        }
        return repo.save(datos);
    }

    @PutMapping("/{id}")
    public ProductoProveedor actualizar(@PathVariable("id") Long id, @Valid @RequestBody ProductoProveedor datos) {
        ProductoProveedor existente = obtener(id);
        String codigo = datos.getCodigo().trim().toUpperCase(Locale.ROOT);
        if (repo.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe otro producto con el código " + codigo);
        }
        existente.setCodigo(codigo);
        existente.setNombre(datos.getNombre());
        existente.setMarca(datos.getMarca());
        existente.setCategoria(datos.getCategoria());
        existente.setPrecio(datos.getPrecio());
        existente.setExistencias(datos.getExistencias());
        existente.setDescripcion(datos.getDescripcion());
        return repo.save(existente);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado: " + id);
        }
        repo.deleteById(id);
    }

    /** Minúsculas y sin tildes, para que "labial" encuentre "Labial" y "liquida" encuentre "líquida". */
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}