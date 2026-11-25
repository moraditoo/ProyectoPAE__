package ni.edu.uam.proyectopae.reportes;

import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Usa las mismas páginas que se exportan al PDF, sin volver a consultar la base. */
public final class ImpresionReporteService {
    private record Pagina(JasperPrint apartado, int indice) { }

    private final List<Pagina> paginas = new ArrayList<>();

    public ImpresionReporteService(DocumentoReporte documento) {
        Objects.requireNonNull(documento, "Genera el reporte antes de imprimirlo.");
        for (JasperPrint apartado : documento.apartadosPdf()) {
            for (int i = 0; i < apartado.getPages().size(); i++) {
                paginas.add(new Pagina(apartado, i));
            }
        }
        if (paginas.isEmpty()) throw new IllegalArgumentException("El reporte no contiene páginas.");
    }

    public int cantidadPaginas() {
        return paginas.size();
    }

    public String tituloPagina(int indice) {
        return paginas.get(indice).apartado().getName();
    }

    public Image renderizarPagina(int indice, float escala) throws JRException {
        if (!Float.isFinite(escala) || escala <= 0 || escala > 4) {
            throw new IllegalArgumentException("La escala debe ser mayor que cero y no superar 4.");
        }
        Pagina pagina = paginas.get(indice);
        BufferedImage original = (BufferedImage) JasperPrintManager.printPageToImage(
                pagina.apartado(), pagina.indice(), escala);
        int ancho = original.getWidth();
        int alto = original.getHeight();
        int[] pixeles = original.getRGB(0, 0, ancho, alto, null, 0, ancho);
        WritableImage imagen = new WritableImage(ancho, alto);
        imagen.getPixelWriter().setPixels(0, 0, ancho, alto,
                PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        original.flush();
        return imagen;
    }

    /** Se ejecuta en segundo plano con nodos independientes de la vista previa. */
    public void imprimir(PrinterJob trabajo) throws JRException {
        Objects.requireNonNull(trabajo, "Selecciona una impresora.");
        try {
            PageLayout papel = trabajo.getJobSettings().getPageLayout();
            // JavaFX aplica los rangos del diálogo a la numeración global del trabajo.
            for (int i = 0; i < paginas.size(); i++) {
                ImageView hoja = new ImageView(renderizarPagina(i, 3f));
                hoja.setPreserveRatio(true);
                hoja.setFitWidth(papel.getPrintableWidth());
                hoja.setFitHeight(papel.getPrintableHeight());
                if (!trabajo.printPage(papel, hoja)) {
                    throw new IllegalStateException("La impresión se canceló o la impresora no pudo recibir una página.");
                }
            }
            if (!trabajo.endJob()) {
                throw new IllegalStateException("No se pudo completar el envío a la impresora.");
            }
        } catch (JRException | RuntimeException error) {
            trabajo.cancelJob();
            throw error;
        }
    }
}
