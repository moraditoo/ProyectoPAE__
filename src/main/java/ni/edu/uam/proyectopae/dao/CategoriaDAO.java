package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Categoria;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Transacciones;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CategoriaDAO {

    public void guardar(Categoria categoria) {
        Transacciones.escribir(session -> session.persist(categoria));
    }

    public List<Categoria> listarTodas() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Categoria ORDER BY nombre ASC", Categoria.class).list();
        }
    }
}