package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.DetalleVenta;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Venta;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.util.List;

public class VentaDAO {

    /**
     * Registra la venta y descuenta las existencias de los lotes vigentes del producto,
     * empezando por el lote que vence primero (FEFO: First Expired, First Out).
     * Todo ocurre en una sola transaccion: si falta stock no se guarda nada.
     */
    public void registrar(Venta venta) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            for (DetalleVenta d : venta.getDetalles()) {
                int pendiente = d.getCantidad();
                List<Lote> lotes = session.createQuery(
                                "SELECT l FROM Lote l WHERE l.producto.idProducto = :idProd " +
                                        "AND l.cantidadDisponible > 0 AND l.fechaCaducidad >= :hoy " +
                                        "ORDER BY l.fechaCaducidad ASC",
                                Lote.class
                        ).setParameter("idProd", d.getProducto().getIdProducto())
                        .setParameter("hoy", LocalDate.now())
                        .list();

                for (Lote lote : lotes) {
                    if (pendiente == 0) break;
                    int usar = Math.min(pendiente, lote.getCantidadDisponible());
                    lote.setCantidadDisponible(lote.getCantidadDisponible() - usar);
                    pendiente -= usar;
                }

                if (pendiente > 0) {
                    throw new IllegalStateException("Stock insuficiente para \"" + d.getNombreProducto()
                            + "\" (faltan " + pendiente + " unidades en lotes vigentes).");
                }
            }

            venta.recalcularTotales();
            session.persist(venta);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public List<Venta> listarConCliente() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT v FROM Venta v JOIN FETCH v.cliente ORDER BY v.fecha DESC",
                    Venta.class
            ).list();
        }
    }

    public List<DetalleVenta> listarDetalles(Integer idVenta) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT d FROM DetalleVenta d JOIN FETCH d.producto WHERE d.venta.idVenta = :id",
                    DetalleVenta.class
            ).setParameter("id", idVenta).list();
        }
    }
}
