package ni.edu.uam.proyectopae.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY =
            construirSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory construirSessionFactory() {
        try {
            Configuration configuracion = new Configuration()
                    .configure("hibernate.cfg.xml");
            Properties local = new Properties();
            Path archivo = Path.of("config", "pae-local.properties");
            if (Files.exists(archivo)) {
                try (Reader entrada = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                    local.load(entrada);
                }
            }

            sobrescribirSiExiste(configuracion,
                    "hibernate.connection.url", "PAE_DB_URL", local, "db.url");

            sobrescribirSiExiste(configuracion,
                    "hibernate.connection.username", "PAE_DB_USER", local, "db.user");

            sobrescribirSiExiste(configuracion,
                    "hibernate.connection.password", "PAE_DB_PASSWORD", local, "db.password");

            return configuracion.buildSessionFactory();

        } catch (IOException | RuntimeException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    /** Prioridad: entorno, archivo personal, valores predeterminados del proyecto. */
    private static void sobrescribirSiExiste(Configuration configuracion,
                                             String propiedad, String variable,
                                             Properties local, String clave) {
        String valor = System.getenv(variable);
        if (valor == null) {
            valor = local.getProperty(clave);
        }
        if (valor != null) {
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
