package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Transacciones;
import org.hibernate.Session;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ServicioDAO {

    public void guardar(Servicio servicio) {
        Transacciones.escribir(session -> session.persist(servicio));
    }

    public List<Servicio> listarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Servicio ORDER BY nombreServicio ASC", Servicio.class).list();
        }
    }

    /**
     * Busca un servicio con sus productos ya cargados (LEFT JOIN FETCH), para poder
     * recorrerlos fuera de la sesión sin LazyInitializationException.
     */
    public Servicio buscarConProductos(Integer idServicio) {
        if (idServicio == null) {
            return null;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT s FROM Servicio s " +
                            "LEFT JOIN FETCH s.productos " +
                            "WHERE s.idServicio = :id",
                    Servicio.class
            ).setParameter("id", idServicio).uniqueResult();
        }
    }

    /**
     * Reemplaza los productos asociados a un servicio en una sola transacción.
     * Quita ids repetidos y verifica que el servicio y todos los productos existan.
     */
    public void actualizarProductos(Integer idServicio, List<Integer> idsProductos) {
        if (idServicio == null) {
            throw new IllegalArgumentException("El servicio es obligatorio.");
        }

        Set<Integer> ids = new LinkedHashSet<>();
        if (idsProductos != null) {
            for (Integer id : idsProductos) {
                if (id != null) {
                    ids.add(id);
                }
            }
        }

        Transacciones.escribir(session -> {
            Servicio servicio = session.get(Servicio.class, idServicio);

            if (servicio == null) {
                throw new IllegalArgumentException("El servicio ya no existe.");
            }

            servicio.getProductos().clear();

            for (Integer id : ids) {
                Producto producto = session.get(Producto.class, id);

                if (producto == null) {
                    throw new IllegalArgumentException(
                            "El producto con id " + id + " ya no existe."
                    );
                }

                servicio.agregarProducto(producto);
            }
        });
    }
}
