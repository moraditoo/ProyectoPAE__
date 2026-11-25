package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.SimplePrintPart;
import net.sf.jasperreports.pdf.view.save.JRPdfSaveContributor;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JRSaveContributor;
import net.sf.jasperreports.view.JasperViewer;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Objects;

/** Abre el visor original de JasperReports con todos los apartados del reporte. */
public final class ImpresionReporteService {
    public JasperPrint preparar(DocumentoReporte documento) throws JRException {
        Objects.requireNonNull(documento, "Genera el reporte antes de imprimirlo.");
        if (documento.paginasPdf() == 0) {
            throw new IllegalArgumentException("El reporte no contiene páginas.");
        }

        JasperPrint completo = new JasperPrint();
        completo.copyFrom(documento.apartadosPdf().getFirst());
        completo.getPages().clear();
        completo.setName("Reporte general - Proyecto PAE");

        for (JasperPrint apartado : documento.apartadosPdf()) {
            if (apartado.getPages().isEmpty()) continue;
            completo.addPart(completo.getPages().size(),
                    SimplePrintPart.fromJasperPrint(apartado, apartado.getName()));
            for (var estilo : apartado.getStylesList()) completo.addStyle(estilo, true);
            for (var origen : apartado.getOriginsList()) completo.addOrigin(origen);
            for (var pagina : apartado.getPages()) completo.addPage(pagina);
        }
        return completo;
    }

    /** La preparación se llama desde la tarea del controlador; Swing abre su propio visor. */
    public void mostrar(DocumentoReporte documento)
            throws JRException, InvocationTargetException, InterruptedException {
        JasperPrint completo = preparar(documento);
        Runnable abrir = () -> {
            JasperViewer visor = new JasperViewer(completo, false, Locale.forLanguageTag("es"));
            if (!limitarGuardadoAPdf(visor.getContentPane())) {
                visor.dispose();
                throw new IllegalStateException("No se pudo configurar el guardado PDF de JasperReports.");
            }
            // Evita el fallo de limpieza de pestañas de JasperReports 7.0.6 al cerrar un libro.
            for (var cierre : visor.getWindowListeners()) visor.removeWindowListener(cierre);
            visor.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            visor.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) abrir.run();
        else SwingUtilities.invokeAndWait(abrir);
    }

    private boolean limitarGuardadoAPdf(Container panel) {
        for (Component componente : panel.getComponents()) {
            if (componente instanceof JRViewerToolbar barra) {
                for (JRSaveContributor formato : barra.getSaveContributors()) {
                    if (formato instanceof JRPdfSaveContributor) {
                        barra.setSaveContributors(new JRSaveContributor[]{formato});
                        return true;
                    }
                }
                return false;
            }
            if (componente instanceof Container contenido && limitarGuardadoAPdf(contenido)) return true;
        }
        return false;
    }
}
