package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Transacciones;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ServicioDAO {

    public void guardar(Servicio servicio) {
        Transacciones.escribir(session -> session.persist(servicio));
    }

    public List<Servicio> listarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Servicio ORDER BY nombreServicio ASC", Servicio.class).list();
        }
    }
}