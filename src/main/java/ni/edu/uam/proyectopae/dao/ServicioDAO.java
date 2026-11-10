package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ServicioDAO {

    public void guardar(Servicio servicio) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(servicio);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public List<Servicio> listarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Servicio ORDER BY nombreServicio ASC", Servicio.class).list();
        }
    }
}