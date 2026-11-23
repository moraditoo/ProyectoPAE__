package ni.edu.uam.proyectopae.reportes;

import ni.edu.uam.proyectopae.dao.ReporteDAO;
import ni.edu.uam.proyectopae.models.Cita;
import ni.edu.uam.proyectopae.models.Cliente;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Comprobación manual en el mismo module-path que utiliza la aplicación. */
public final class DiagnosticoReportes {
    private DiagnosticoReportes() { }

    public static void main(String[] args) throws Exception {
        Path carpeta = Path.of("target", "diagnostico-reportes");
        Files.createDirectories(carpeta);
        ReporteJasperService jasper = new ReporteJasperService();
        Cliente cliente = new Cliente();
        cliente.setNombres("María");
        cliente.setApellidos("Pérez");
        Servicio servicio = new Servicio();
        servicio.setNombreServicio("Maquillaje de prueba");
        Cita cita = new Cita(cliente, servicio, LocalDateTime.of(2026, 11, 23, 14, 30), "COMPLETADA");
        cita.setIdCita(1);
        var reporte = jasper.generar(List.of(cita), Arrays.asList(ColumnaCita.values()), null, null, null);
        jasper.exportar(reporte, carpeta.resolve("citas-ejemplo.pdf"), ReporteJasperService.Formato.PDF);
        jasper.exportar(reporte, carpeta.resolve("citas-ejemplo.csv"), ReporteJasperService.Formato.CSV);
        System.out.println("[OK] JasperReports: compilacion JRXML, fuentes, PDF y CSV en " + carpeta.toAbsolutePath());

        try {
            // Solo consulta citas: los datos ficticios anteriores nunca se guardan en la BD.
            int cantidad = new ReporteDAO().buscarCitas(null, null, null).size();
            System.out.println("[OK] PostgreSQL e Hibernate: consulta de reportes correcta (" + cantidad + " citas).");
        } catch (Throwable error) {
            System.err.println("[ERROR] Base de datos: " + VentanaManager.causaRaiz(error));
            System.err.println("Revisa config/pae-local.properties, el servicio PostgreSQL y la existencia de tienda_maquillaje_db.");
            throw new IllegalStateException("No se pudo completar el diagnostico de base de datos.", error);
        } finally {
            // Evita reinicializar Hibernate si el primer intento falló.
            try { HibernateUtil.shutdown(); } catch (LinkageError ignored) { }
        }
    }
}
