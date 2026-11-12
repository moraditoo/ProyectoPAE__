package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoteDAO {

    public void guardar(Lote lote) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(lote);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void actualizar(Lote lote) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(lote);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void eliminar(Integer idLote) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Lote lote = session.get(Lote.class, idLote);
            if (lote != null) {
                session.remove(lote);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
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

    /** Existencias por producto sumando solo los lotes que aún no han vencido. */
    public Map<Integer, Long> stockVigentePorProducto() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> filas = session.createQuery(
                    "SELECT l.producto.idProducto, SUM(l.cantidadDisponible) FROM Lote l " +
                            "WHERE l.fechaCaducidad >= :hoy GROUP BY l.producto.idProducto",
                    Object[].class
            ).setParameter("hoy", LocalDate.now()).list();
            Map<Integer, Long> stock = new HashMap<>();
            for (Object[] fila : filas) {
                stock.put((Integer) fila[0], ((Number) fila[1]).longValue());
            }
            return stock;
        }
    }

    /** Indica si ya existe otro lote con el mismo código (sin distinguir mayúsculas). */
    public boolean existeCodigo(String codigo, Integer idExcluido) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int excluir = idExcluido != null ? idExcluido : -1;
            Long total = session.createQuery(
                    "SELECT COUNT(l) FROM Lote l WHERE LOWER(l.codigoLote) = :codigo AND l.idLote <> :id", Long.class
            ).setParameter("codigo", codigo.toLowerCase()).setParameter("id", excluir).uniqueResult();
            return total != null && total > 0;
        }
    }
}
