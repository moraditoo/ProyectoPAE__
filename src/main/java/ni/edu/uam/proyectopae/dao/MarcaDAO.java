package ni.edu.uam.proyectofinalpae.dao;

import ni.edu.uam.proyectofinalpae.models.Marca;
import ni.edu.uam.proyectofinalpae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MarcaDAO {

    public void guardar(Marca marca) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(marca);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public List<Marca> listarTodas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Marca ORDER BY nombreMarca ASC", Marca.class).list();
        }
    }
}