package ni.edu.uam.proyectopae.reportes;

import ni.edu.uam.proyectopae.dao.ReporteGeneralDAO;
import ni.edu.uam.proyectopae.util.HibernateUtil;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Comprueba el mismo module-path de la aplicación; los ejemplos nunca se persisten. */
public final class DiagnosticoReportes {
    private DiagnosticoReportes() { }

    public static void main(String[] args) throws Exception {
        Path carpeta = Path.of("target", "diagnostico-reportes");
        Files.createDirectories(carpeta);
        ReporteGeneralJasperService jasper = new ReporteGeneralJasperService();
        DocumentoReporte documento = jasper.generar(ejemplo());
        jasper.exportarPdf(documento, carpeta.resolve("reporte-general-ejemplo.pdf"));
        System.out.println("[OK] JasperReports: 12 apartados, JRXML, fuentes y PDF en " + carpeta.toAbsolutePath());
        if (Arrays.asList(args).contains("--sin-bd")) return;

        try {
            DatosReporte datos = new ReporteGeneralDAO().consultar(SolicitudReporte.todos());
            System.out.println("[OK] PostgreSQL e Hibernate: consulta de los " + datos.secciones().size()
                    + " apartados correcta (" + datos.totalRegistros() + " filas).");
        } catch (Throwable error) {
            System.err.println("[ERROR] Base de datos: " + VentanaManager.causaRaiz(error));
            System.err.println("Revisa config/pae-local.properties, el servicio PostgreSQL y la existencia de tienda_maquillaje_db.");
            throw new IllegalStateException("No se pudo completar el diagnostico de base de datos.", error);
        } finally {
            try { HibernateUtil.shutdown(); } catch (LinkageError ignored) { }
        }
    }

    private static DatosReporte ejemplo() {
        Map<ApartadoReporte, List<String>> valores = new EnumMap<>(ApartadoReporte.class);
        valores.put(ApartadoReporte.RESUMEN, List.of("Total de ventas del período", "C$ 690.00"));
        valores.put(ApartadoReporte.PRODUCTOS, List.of("1", "Base Fit Me tono 128", "1", "Rostro", "1",
                "Maybelline", "C$ 450.00", "C$ 690.00", "C$ 240.00", "34.78 %", "18", "5", "SUFICIENTE"));
        valores.put(ApartadoReporte.LOTES, List.of("1", "1", "Base Fit Me tono 128", "FM128-2026",
                "01/11/2026", "01/11/2027", "18", "343", "VIGENTE"));
        valores.put(ApartadoReporte.CLIENTES, List.of("1", "María", "Pérez", "Femenino",
                "88881001", "maria.perez@example.com", "Sin alergias declaradas"));
        valores.put(ApartadoReporte.SERVICIOS, List.of("1", "Maquillaje social", "60", "C$ 900.00",
                "1 - Base Fit Me tono 128"));
        valores.put(ApartadoReporte.CITAS, List.of("1", "1", "María Pérez", "1", "Maquillaje social",
                "23/11/2026 14:30", "COMPLETADA", "C$ 900.00"));
        valores.put(ApartadoReporte.VENTAS, List.of("1", "1", "María Pérez", "23/11/2026 15:30",
                "C$ 690.00", "C$ 450.00", "C$ 240.00"));
        valores.put(ApartadoReporte.DETALLE_VENTAS, List.of("1", "1", "23/11/2026 15:30", "María Pérez",
                "1", "Base Fit Me tono 128", "1", "C$ 690.00", "C$ 450.00", "C$ 690.00", "C$ 240.00"));
        valores.put(ApartadoReporte.CONSUMOS, List.of("1", "1", "23/11/2026 14:30", "María Pérez",
                "Maquillaje social", "1", "FM128-2026", "1", "Base Fit Me tono 128", "1"));
        valores.put(ApartadoReporte.CATEGORIAS, List.of("1", "Rostro", "Bases y correctores"));
        valores.put(ApartadoReporte.MARCAS, List.of("1", "Maybelline"));
        valores.put(ApartadoReporte.USUARIOS, List.of("1", "demo.admin", "ADMIN"));
        List<SeccionReporte> secciones = new ArrayList<>();
        for (ApartadoReporte apartado : ApartadoReporte.values()) {
            Map<String, String> fila = new LinkedHashMap<>();
            for (int i = 0; i < apartado.campos().size(); i++) {
                fila.put(apartado.campos().get(i).clave(), valores.get(apartado).get(i));
            }
            secciones.add(new SeccionReporte(apartado, apartado.campos(), List.of(fila), "Registros de ejemplo: 1"));
        }
        return new DatosReporte(SolicitudReporte.todos(), LocalDateTime.of(2026, 11, 23, 17, 30), secciones);
    }
}
