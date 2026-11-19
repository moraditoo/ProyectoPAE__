package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Marca;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Transacciones;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MarcaDAO {

    public void guardar(Marca marca) {
        Transacciones.escribir(session -> session.persist(marca));
    }

    public List<Marca> listarTodas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Marca ORDER BY nombreMarca ASC", Marca.class).list();
        }
    }
}