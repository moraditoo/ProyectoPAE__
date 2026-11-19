package ni.edu.uam.proyectopae.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Consumer;
import java.util.function.Function;

public final class Transacciones {

    private Transacciones() {
    }

    public static <T> T ejecutar(Function<Session, T> operacion) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaccion = session.beginTransaction();

            try {
                T resultado = operacion.apply(session);

                transaccion.commit();
                return resultado;

            } catch (RuntimeException | Error error) {
                try {
                    if (transaccion.isActive()) {
                        transaccion.rollback();
                    }
                } catch (RuntimeException errorRollback) {
                    error.addSuppressed(errorRollback);
                }

                throw error;
            }
        }
    }

    public static void escribir(Consumer<Session> operacion) {
        ejecutar(session -> {
            operacion.accept(session);
            return null;
        });
    }
}