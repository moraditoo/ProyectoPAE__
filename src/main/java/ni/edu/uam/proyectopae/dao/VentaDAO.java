package ni.edu.uam.proyectopae.dao;

import jakarta.persistence.LockModeType;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.DetalleVenta;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.models.Venta;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class VentaDAO {

    /**
     * Registra una venta en una sola transacción: descuenta las unidades de los lotes vigentes
     * del producto (primero los que vencen antes) y guarda los precios y costos del momento.
     * Si algún producto no tiene existencias suficientes, no se guarda nada.
     */
    public Venta registrar(Cliente cliente, List<DetalleVenta> items) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            LocalDate hoy = LocalDate.now();

            Venta venta = new Venta();
            venta.setCliente(session.get(Cliente.class, cliente.getIdCliente()));
            venta.setFecha(LocalDateTime.now());

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal costo = BigDecimal.ZERO;

            for (DetalleVenta item : items) {
                Producto producto = session.get(Producto.class, item.getProducto().getIdProducto());
                List<Lote> lotes = session.createQuery(
                                "FROM Lote l WHERE l.producto.idProducto = :id AND l.cantidadDisponible > 0 " +
                                        "AND l.fechaCaducidad >= :hoy ORDER BY l.fechaCaducidad ASC", Lote.class)
                        .setParameter("id", producto.getIdProducto())
                        .setParameter("hoy", hoy)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .list();

                int disponible = lotes.stream().mapToInt(Lote::getCantidadDisponible).sum();
                if (disponible < item.getCantidad()) {
                    throw new StockInsuficienteException(producto.getNombreProducto(), disponible, item.getCantidad());
                }

                int pendiente = item.getCantidad();
                for (Lote lote : lotes) {
                    if (pendiente == 0) break;
                    int tomar = Math.min(pendiente, lote.getCantidadDisponible());
                    lote.setCantidadDisponible(lote.getCantidadDisponible() - tomar);
                    pendiente -= tomar;
                }

                DetalleVenta detalle = new DetalleVenta(producto, item.getCantidad());
                detalle.setVenta(venta);
                venta.getDetalles().add(detalle);

                total = total.add(detalle.getSubtotal());
                costo = costo.add(detalle.getCostoUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            }

            venta.setTotal(total);
            venta.setCostoTotal(costo);
            session.persist(venta);
            tx.commit();
            return venta;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    public List<Venta> listarConCliente() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT v FROM Venta v JOIN FETCH v.cliente ORDER BY v.fecha DESC", Venta.class
            ).list();
        }
    }

    public List<DetalleVenta> listarDetalles(Integer idVenta) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT d FROM DetalleVenta d JOIN FETCH d.producto WHERE d.venta.idVenta = :id", DetalleVenta.class
            ).setParameter("id", idVenta).list();
        }
    }
}
