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

            sobrescribirSiExiste(configuracion,
                    "hibernate.connection.url", "PAE_DB_URL");

            sobrescribirSiExiste(configuracion,
                    "hibernate.connection.username", "PAE_DB_USER");

            sobrescribirSiExiste(configuracion,
                    "hibernate.connection.password", "PAE_DB_PASSWORD");

            return configuracion.buildSessionFactory();

        } catch (RuntimeException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    /** Usa la variable de entorno si existe; si no, conserva el valor de hibernate.cfg.xml. */
    private static void sobrescribirSiExiste(Configuration configuracion,
                                             String propiedad, String variable) {
        String valor = System.getenv(variable);
        if (valor != null && !valor.isBlank()) {
            configuracion.setProperty(propiedad, valor);
        }
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