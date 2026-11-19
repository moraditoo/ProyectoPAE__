package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ni.edu.uam.proyectopae.util.Transacciones;

import java.time.LocalDate;
import java.util.List;

public class LoteDAO {

    public void guardar(Lote lote) {
        Transacciones.escribir(session -> session.persist(lote));
    }

    public void actualizar(Lote lote) {
        Transacciones.escribir(session -> session.merge(lote));
    }

    public void eliminar(Integer idLote) {
        Transacciones.escribir(session -> {
            Lote lote = session.get(Lote.class, idLote);

            if (lote != null) {
                session.remove(lote);
            }
        });
    }

    public List<Lote> listarConProducto() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT l FROM Lote l JOIN FETCH l.producto ORDER BY l.fechaCaducidad ASC",
                    Lote.class
            ).list();
        }
    }

    // Metodo a masapul ti InventarioController
    public List<Lote> listarPorProducto(Integer idProducto) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT l FROM Lote l WHERE l.producto.idProducto = :prodId ORDER BY l.fechaCaducidad ASC",
                    Lote.class
            ).setParameter("prodId", idProducto).list();
        }
    }

    /** Lotes con existencias que vencen entre hoy y hoy + dias (para el dashboard y las alertas). */
    public List<Lote> listarPorVencer(int dias) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT l FROM Lote l JOIN FETCH l.producto " +
                                    "WHERE l.cantidadDisponible > 0 AND l.fechaCaducidad BETWEEN :hoy AND :limite " +
                                    "ORDER BY l.fechaCaducidad ASC",
                            Lote.class
                    ).setParameter("hoy", LocalDate.now())
                    .setParameter("limite", LocalDate.now().plusDays(dias))
                    .list();
        }
    }

    /** Lotes ya vencidos que todavia tienen unidades (deben retirarse del inventario). */
    public List<Lote> listarVencidosConStock() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT l FROM Lote l JOIN FETCH l.producto " +
                            "WHERE l.cantidadDisponible > 0 AND l.fechaCaducidad < :hoy " +
                            "ORDER BY l.fechaCaducidad ASC",
                    Lote.class
            ).setParameter("hoy", LocalDate.now()).list();
        }
    }
}
