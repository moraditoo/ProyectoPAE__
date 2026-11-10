package ni.edu.uam.proyectopae.dao;

import java.util.List;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;



public class ProductoDAO {
    public List<Producto> listarConRelaciones() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Producto ORDER BY nombreProducto", Producto.class).list();
        }
    }

    public void guardar(Producto producto) {
        HibernateUtil.getSessionFactory().inTransaction(session -> session.persist(producto));
    }
}
