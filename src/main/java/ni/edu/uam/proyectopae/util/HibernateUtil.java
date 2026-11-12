package ni.edu.uam.proyectopae.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    /**
     * Se inicializa de forma diferida: si PostgreSQL no está disponible se lanza una excepción
     * controlable (en lugar de un ExceptionInInitializerError) para que las ventanas muestren
     * una alerta de conexión y se pueda reintentar más tarde sin reiniciar la aplicación.
     */
    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
            } catch (Throwable ex) {
                System.err.println("Error critico al inicializar SessionFactory: " + ex.getMessage());
                throw new ErrorConexionException(
                        "No se pudo conectar con la base de datos PostgreSQL. Verifique que el servidor esté en ejecución "
                                + "y que los datos de conexión de hibernate.cfg.xml sean correctos.", ex);
            }
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

    /** Error al establecer la conexión inicial con la base de datos. */
    public static class ErrorConexionException extends RuntimeException {
        public ErrorConexionException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
