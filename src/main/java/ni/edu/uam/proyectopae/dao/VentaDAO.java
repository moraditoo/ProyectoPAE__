package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.DetalleVenta;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Venta;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.Transacciones;
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
        if (venta.getDetalles().isEmpty()) {
            throw new IllegalArgumentException(
                    "Agrega al menos un producto a la venta."
            );
        }

        Transacciones.escribir(session -> {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getCantidad() == null
                        || detalle.getCantidad() <= 0) {

                    throw new IllegalArgumentException(
                            "La cantidad debe ser mayor que cero."
                    );
                }

                int pendiente = detalle.getCantidad();

                List<Lote> lotes = session.createQuery("""
                    SELECT l
                    FROM Lote l
                    WHERE l.producto.idProducto = :producto
                      AND l.cantidadDisponible > 0
                      AND l.fechaCaducidad >= :hoy
                    ORDER BY l.fechaCaducidad, l.idLote
                    """, Lote.class)
                        .setParameter(
                                "producto",
                                detalle.getProducto().getIdProducto()
                        )
                        .setParameter("hoy", LocalDate.now())
                        .list();

                for (Lote lote : lotes) {
                    if (pendiente == 0) {
                        break;
                    }

                    int cantidad = Math.min(
                            pendiente,
                            lote.getCantidadDisponible()
                    );

                    lote.setCantidadDisponible(
                            lote.getCantidadDisponible() - cantidad
                    );

                    pendiente -= cantidad;
                }

                if (pendiente > 0) {
                    throw new IllegalStateException(
                            "Stock insuficiente para "
                                    + detalle.getNombreProducto()
                    );
                }
            }

            venta.recalcularTotales();
            session.persist(venta);
        });
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
