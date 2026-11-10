package ni.edu.uam.proyectopae.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private HibernateUtil() {}

    private static class Holder {
        private static final SessionFactory SESSION_FACTORY = new Configuration()
                .configure()
                .setProperty("hibernate.connection.url", System.getProperty(
                        "proyectopae.jdbc.url", "jdbc:h2:./data/proyectopae"))
                .buildSessionFactory();
    }

    public static SessionFactory getSessionFactory() {
        return Holder.SESSION_FACTORY;
    }

    public static void shutdown() {
        if (!Holder.SESSION_FACTORY.isClosed()) Holder.SESSION_FACTORY.close();
    }
}
