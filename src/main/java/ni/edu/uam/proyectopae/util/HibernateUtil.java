package ni.edu.uam.proyectopae.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY =
            construirSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory construirSessionFactory() {
        try {
            Configuration configuracion = new Configuration()
                    .configure("hibernate.cfg.xml");

            configuracion.setProperty(
                    "hibernate.connection.url",
                    variableObligatoria("PAE_DB_URL")
            );

            configuracion.setProperty(
                    "hibernate.connection.username",
                    variableObligatoria("PAE_DB_USER")
            );

            configuracion.setProperty(
                    "hibernate.connection.password",
                    variableObligatoria("PAE_DB_PASSWORD")
            );

            return configuracion.buildSessionFactory();

        } catch (RuntimeException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String variableObligatoria(String nombre) {
        String valor = System.getenv(nombre);

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la variable " + nombre
            );
        }

        return valor;
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        if (!SESSION_FACTORY.isClosed()) {
            SESSION_FACTORY.close();
        }
    }
}