package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ni.edu.uam.proyectopae.util.Transacciones;

import java.util.List;

public class ClienteDAO {

    public void guardar(Cliente cliente) {
        Transacciones.escribir(session -> session.persist(cliente));
    }

    public void actualizar(Cliente cliente) {
        Transacciones.escribir(session -> session.merge(cliente));
    }

    public void eliminar(Integer idCliente) {
        Transacciones.escribir(session -> {
            Cliente cliente = session.get(Cliente.class, idCliente);

            if (cliente != null) {
                session.remove(cliente);
            }
        });
    }

    public List<Cliente> listarTodos() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Cliente ORDER BY nombres ASC", Cliente.class).list();
        }
    }

    public List<Cliente> buscarPorFiltro(String criterio) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Cliente c WHERE LOWER(c.nombres) LIKE :crit " +
                            "OR LOWER(c.apellidos) LIKE :crit " +
                            "OR c.telefono LIKE :crit " +
                            "OR LOWER(c.correo) LIKE :crit ORDER BY c.nombres ASC",
                    Cliente.class
            ).setParameter("crit", "%" + criterio.toLowerCase().trim() + "%").list();
        }
    }

    /** Verifica que el correo no este registrado por otro cliente (idExcluir = cliente en edicion). */
    public boolean existeCorreo(String correo, Integer idExcluir) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long total = session.createQuery(
                            "SELECT COUNT(c) FROM Cliente c WHERE LOWER(c.correo) = :correo " +
                                    "AND c.idCliente <> :idExcluir",
                            Long.class
                    ).setParameter("correo", correo.toLowerCase().trim())
                    .setParameter("idExcluir", idExcluir == null ? -1 : idExcluir)
                    .uniqueResult();
            return total != null && total > 0;
        }
    }
}
