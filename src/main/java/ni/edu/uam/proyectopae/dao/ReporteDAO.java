package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

public class ReporteDAO {

    public List<Cita> buscarCitas(LocalDate desde, LocalDate hasta, Integer idServicio) {
        StringBuilder hql = new StringBuilder("""
                SELECT c
                FROM Cita c
                JOIN FETCH c.cliente
                JOIN FETCH c.servicio
                WHERE 1 = 1
                """);

        if (desde != null) {
            hql.append(" AND c.fechaHora >= :inicio");
        }
        if (hasta != null) {
            hql.append(" AND c.fechaHora < :fin");
        }
        if (idServicio != null) {
            hql.append(" AND c.servicio.idServicio = :idServicio");
        }
        hql.append(" ORDER BY c.fechaHora DESC, c.idCita DESC");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Query<Cita> consulta = session.createQuery(hql.toString(), Cita.class);

                if (desde != null) {
                    consulta.setParameter("inicio", desde.atStartOfDay());
                }
                if (hasta != null) {
                    consulta.setParameter("fin", hasta.plusDays(1).atStartOfDay());
                }
                if (idServicio != null) {
                    consulta.setParameter("idServicio", idServicio);
                }

                List<Cita> citas = consulta.getResultList();
                tx.commit();
                return citas;
            } catch (RuntimeException error) {
                try {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                } catch (RuntimeException errorRollback) {
                    error.addSuppressed(errorRollback);
                }
                throw error;
            }
        }
    }
}