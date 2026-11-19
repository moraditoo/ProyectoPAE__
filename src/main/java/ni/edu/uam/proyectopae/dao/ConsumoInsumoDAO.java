package ni.edu.uam.proyectopae.dao;

import ni.edu.uam.proyectopae.models.ConsumoInsumo;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.util.Transacciones;

public class ConsumoInsumoDAO {

    public void registrarConsumo(ConsumoInsumo consumo) {
        if (consumo.getLote() == null
                || consumo.getCantidadDescontada() == null
                || consumo.getCantidadDescontada() <= 0) {

            throw new IllegalArgumentException(
                    "Selecciona un lote y una cantidad mayor que cero."
            );
        }

        Transacciones.escribir(session -> {
            Lote lote = session.get(
                    Lote.class,
                    consumo.getLote().getIdLote()
            );

            if (lote == null) {
                throw new IllegalArgumentException(
                        "El lote no existe."
                );
            }

            int cantidad = consumo.getCantidadDescontada();

            if (lote.getCantidadDisponible() < cantidad) {
                throw new IllegalArgumentException(
                        "El lote no tiene suficientes existencias."
                );
            }

            consumo.setLote(lote);

            lote.setCantidadDisponible(
                    lote.getCantidadDisponible() - cantidad
            );

            session.persist(consumo);
        });
    }
}