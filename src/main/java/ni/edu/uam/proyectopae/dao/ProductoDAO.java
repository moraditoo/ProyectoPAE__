package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductoDAO {

    public void guardar(Producto producto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(producto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void actualizar(Producto producto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(producto);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void eliminar(Integer idProducto) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Producto p = session.get(Producto.class, idProducto);
            if (p != null) session.remove(p);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    // Consulta HQL 1 Requerida: Recupera informacion relacionada entre Producto, Categoria y Marca con JOIN FETCH
    public List<Producto> listarConRelaciones() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT p FROM Producto p " +
                            "JOIN FETCH p.categoria c " +
                            "JOIN FETCH p.marca m " +
                            "ORDER BY p.idProducto DESC",
                    Producto.class
            ).list();
        }
    }

    /** Existencias vigentes (lotes no vencidos) agrupadas por producto: idProducto -> unidades. */
    public Map<Integer, Integer> stockVigentePorProducto() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> filas = session.createQuery(
                    "SELECT l.producto.idProducto, SUM(l.cantidadDisponible) FROM Lote l " +
                            "WHERE l.fechaCaducidad >= :hoy GROUP BY l.producto.idProducto",
                    Object[].class
            ).setParameter("hoy", LocalDate.now()).list();

            Map<Integer, Integer> stock = new HashMap<>();
            for (Object[] f : filas) {
                stock.put((Integer) f[0], ((Number) f[1]).intValue());
            }
            return stock;
        }
    }

    /** Productos con categoria/marca y su stock vigente calculado. */
    public List<Producto> listarConStock() {
        List<Producto> productos = listarConRelaciones();
        Map<Integer, Integer> stock = stockVigentePorProducto();
        for (Producto p : productos) {
            p.setStockActual(stock.getOrDefault(p.getIdProducto(), 0));
        }
        return productos;
    }
}
