package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

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
}