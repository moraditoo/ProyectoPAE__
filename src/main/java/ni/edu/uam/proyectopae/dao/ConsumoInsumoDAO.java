package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.ConsumoInsumo;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConsumoInsumoDAO {

    public void registrarConsumo(ConsumoInsumo consumo) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(consumo);
            // Actualizar stock del lote
            consumo.getLote().setCantidadDisponible(
                    consumo.getLote().getCantidadDisponible() - consumo.getCantidadDescontada()
            );
            session.merge(consumo.getLote());
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}