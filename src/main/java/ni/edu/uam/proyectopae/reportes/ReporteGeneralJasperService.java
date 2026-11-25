package ni.edu.uam.proyectopae.reportes;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Genera únicamente los apartados y campos que contiene el resultado inmutable de Hibernate. */
public final class ReporteGeneralJasperService {
    private static final String PLANTILLA = "/reportes/general.jrxml";

    public DocumentoReporte generar(DatosReporte datos) throws IOException, JRException {
        Objects.requireNonNull(datos, "Los datos del reporte son obligatorios.");
        List<JasperPrint> pdf = new ArrayList<>();
        for (SeccionReporte seccion : datos.secciones()) {
            JasperPrint apartado = generarTabla(datos, seccion.apartado().name(), seccion.apartado().titulo(),
                    seccion.apartado().alcance(), seccion.resumen(), seccion.campos(), seccion.filas());
            apartado.setName(seccion.apartado().titulo());
            pdf.add(apartado);
        }
        return new DocumentoReporte(datos, pdf);
    }

    public void exportarPdf(DocumentoReporte reporte, Path destino) throws IOException, JRException {
        Objects.requireNonNull(reporte, "Genera el reporte antes de exportarlo.");
        ExportadorJasper.pdf(reporte.apartadosPdf(), destino);
    }

    private JasperPrint generarTabla(DatosReporte datos, String nombre, String titulo, String alcance,
                                      String resumen, List<CampoReporte> campos,
                                      List<Map<String, String>> filas) throws IOException, JRException {
        JasperDesign diseno;
        try (InputStream entrada = ReporteGeneralJasperService.class.getResourceAsStream(PLANTILLA)) {
            if (entrada == null) throw new IOException("No se encontró " + PLANTILLA + ". Recompila el proyecto.");
            diseno = JRXmlLoader.load(entrada);
        }
        diseno.setName("ReporteGeneral_" + nombre);
        // Tablas extensas: A3 horizontal; las demás conservan A4 horizontal.
        if (campos.size() > 8) {
            diseno.setPageWidth(1191);
            diseno.setPageHeight(842);
            diseno.setColumnWidth(1131);
        }
        diseno.setIgnorePagination(false);
        ajustarAnchos(diseno);
        agregarColumnas(diseno, campos);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO", titulo);
        parametros.put("ALCANCE", alcance);
        parametros.put("FILTROS", datos.solicitud().descripcionFiltros());
        parametros.put("GENERADO", "Generado el " + datos.generado().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        parametros.put("RESUMEN", resumen);
        parametros.put("REPORT_LOCALE", Locale.forLanguageTag("es-NI"));
        List<Map<String, ?>> fuente = new ArrayList<>(filas);
        return JasperFillManager.fillReport(JasperCompileManager.compileReport(diseno), parametros,
                new JRMapCollectionDataSource(fuente));
    }

    private void ajustarAnchos(JasperDesign diseno) {
        for (JRBand banda : List.of(diseno.getTitle(), diseno.getPageHeader(), diseno.getPageFooter(), diseno.getSummary())) {
            for (var elemento : banda.getElements()) {
                if (elemento.getKey() != null && elemento.getKey().startsWith("ancho_")) {
                    ((JRDesignElement) elemento).setWidth(diseno.getColumnWidth());
                } else if ("pagina".equals(elemento.getKey())) {
                    ((JRDesignElement) elemento).setX(diseno.getColumnWidth() - elemento.getWidth());
                }
            }
        }
    }

    private void agregarColumnas(JasperDesign diseno, List<CampoReporte> campos) throws JRException {
        JRDesignBand cabecera = (JRDesignBand) diseno.getColumnHeader();
        JRDesignBand detalle = (JRDesignBand) diseno.getDetailSection().getBands()[0];
        int pesoTotal = campos.stream().mapToInt(CampoReporte::peso).sum();
        int minimo = Math.min(60, diseno.getColumnWidth() / campos.size());
        int distribuible = diseno.getColumnWidth() - minimo * campos.size();
        int acumulado = 0;
        int x = 0;
        int indice = 0;
        for (CampoReporte campo : campos) {
            acumulado += campo.peso();
            int fin = ++indice * minimo + distribuible * acumulado / pesoTotal;
            JRDesignField dato = new JRDesignField();
            dato.setName(campo.clave());
            dato.setValueClass(String.class);
            diseno.addField(dato);

            JRDesignStaticText titulo = new JRDesignStaticText();
            titulo.setKey("cabecera_" + campo.clave());
            titulo.setX(x);
            titulo.setY(0);
            titulo.setWidth(fin - x);
            titulo.setHeight(cabecera.getHeight());
            titulo.setStyle(diseno.getStylesMap().get("Cabecera"));
            titulo.setText(campo.titulo());
            cabecera.addElement(titulo);

            JRDesignTextField celda = new JRDesignTextField();
            celda.setKey("detalle_" + campo.clave());
            celda.setX(x);
            celda.setY(0);
            celda.setWidth(fin - x);
            celda.setHeight(detalle.getHeight());
            celda.setStyle(diseno.getStylesMap().get("Celda"));
            celda.setBlankWhenNull(true);
            celda.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
            celda.setStretchType(StretchTypeEnum.CONTAINER_HEIGHT);
            celda.setExpression(new JRDesignExpression("$F{" + campo.clave() + "}"));
            detalle.addElement(celda);
            x = fin;
        }
    }

}
