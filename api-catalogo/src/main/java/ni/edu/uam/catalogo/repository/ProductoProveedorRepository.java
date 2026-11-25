package ni.edu.uam.catalogo.repository;

import ni.edu.uam.catalogo.model.ProductoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data genera automáticamente findAll, findById, save, deleteById, existsById y count. */
public interface ProductoProveedorRepository extends JpaRepository<ProductoProveedor, Long> {

    List<ProductoProveedor> findAllByOrderByNombreAsc();

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}