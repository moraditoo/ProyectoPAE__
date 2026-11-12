package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CitaDAO {

    public void agendar(Cita cita) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(cita);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void cancelar(Integer idCita) {
        cambiarEstado(idCita, "CANCELADA");
    }

    /** Marca la cita como REALIZADA: el servicio se considera cobrado en los reportes financieros. */
    public void marcarRealizada(Integer idCita) {
        cambiarEstado(idCita, "REALIZADA");
    }

    private void cambiarEstado(Integer idCita, String estado) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Cita c = session.get(Cita.class, idCita);
            if (c != null) {
                c.setEstado(estado);
                session.merge(c);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public List<Cita> listarCitasConRelaciones() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT c FROM Cita c " +
                            "JOIN FETCH c.cliente cl " +
                            "JOIN FETCH c.servicio s " +
                            "ORDER BY c.fechaHora ASC",
                    Cita.class
            ).list();
        }
    }

    // Consulta HQL 2 Requerida: Reporte agregado de citas e ingresos proyectados agrupados por Servicio
    public List<Object[]> reporteIngresosPorServicio() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT s.nombreServicio, COUNT(c.idCita), SUM(s.tarifaBase) " +
                            "FROM Cita c " +
                            "JOIN c.servicio s " +
                            "WHERE c.estado = 'PROGRAMADA' " +
                            "GROUP BY s.idServicio, s.nombreServicio " +
                            "ORDER BY SUM(s.tarifaBase) DESC",
                    Object[].class
            ).list();
        }
    }
}