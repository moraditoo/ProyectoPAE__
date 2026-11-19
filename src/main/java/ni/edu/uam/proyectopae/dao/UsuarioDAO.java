package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Usuario;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Transacciones;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class UsuarioDAO {

    public void guardar(Usuario usuario) {
        Transacciones.escribir(session -> session.persist(usuario));
    }

    public Usuario autenticar(String username, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Usuario> query = session.createQuery(
                    "FROM Usuario u WHERE u.username = :usr AND u.password = :pwd", Usuario.class);
            query.setParameter("usr", username);
            query.setParameter("pwd", password);
            return query.uniqueResult();
        }
    }

    public List<Usuario> listarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Usuario", Usuario.class).list();
        }
    }
}