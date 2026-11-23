package ni.edu.uam.proyectopae.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import ni.edu.uam.proyectopae.dao.LoteDAO;
import ni.edu.uam.proyectopae.dao.ProductoDAO;
import ni.edu.uam.proyectopae.models.Lote;
import ni.edu.uam.proyectopae.models.Producto;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Filas;
import ni.edu.uam.proyectopae.util.Formato;
import ni.edu.uam.proyectopae.util.Graficos;
import ni.edu.uam.proyectopae.util.Refrescable;
import ni.edu.uam.proyectopae.util.VentanaManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dashboard de inventario: lotes por vencer, lotes vencidos y productos
 * por debajo del stock mínimo, con gráficos y tablas resaltadas.
 */
public class DashboardInventarioController implements Refrescable {

    private static final int MAX_BARRAS = 10;

    @FXML private ComboBox<Integer> cmbDias;

    @FXML private Label lblKpiProductos;
    @FXML private Label lblKpiPorVencer;
    @FXML private Label lblKpiPorVencerTitulo;
    @FXML private Label lblKpiVencidos;
    @FXML private Label lblKpiBajoMinimo;
    @FXML private Label lblTituloPorVencer;

    @FXML private BarChart<String, Number> chartStock;
    @FXML private CategoryAxis ejeProductos;
    @FXML private PieChart chartEstadoLotes;

    @FXML private TableView<Lote> tblPorVencer;
    @FXML private TableColumn<Lote, String> colPvProducto;
    @FXML private TableColumn<Lote, String> colPvLote;
    @FXML private TableColumn<Lote, LocalDate> colPvFecha;
    @FXML private TableColumn<Lote, Number> colPvDias;
    @FXML private TableColumn<Lote, Number> colPvCantidad;
    @FXML private TableColumn<Lote, String> colPvEstado;

    @FXML private TableView<Producto> tblBajoMinimo;
    @FXML private TableColumn<Producto, String> colBmProducto;
    @FXML private TableColumn<Producto, Number> colBmStock;
    @FXML private TableColumn<Producto, Number> colBmMinimo;
    @FXML private TableColumn<Producto, Number> colBmFaltante;

    private final LoteDAO loteDAO = new LoteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    @FXML
    public void initialize() {
        cmbDias.setItems(
                FXCollections.observableArrayList(7, 15, 30, 60, 90)
        );

        cmbDias.setValue(Lote.DIAS_ALERTA_VENCIMIENTO);

        cmbDias.setCellFactory(lv ->
                new ListCell<>() {

                    @Override
                    protected void updateItem(Integer dias, boolean empty) {
                        super.updateItem(dias, empty);

                        setText(
                                empty || dias == null
                                        ? null
                                        : dias + " días"
                        );
                    }
                }
        );

        cmbDias.setButtonCell(
                cmbDias.getCellFactory().call(null)
        );

        cmbDias.valueProperty().addListener(
                (obs, anterior, nuevo) -> refrescar()
        );

        colPvProducto.setCellValueFactory(
                c -> new SimpleStringProperty(
                        c.getValue().getNombreProducto()
                )
        );

        colPvLote.setCellValueFactory(
                c -> new SimpleStringProperty(
                        c.getValue().getCodigoLote()
                )
        );

        colPvFecha.setCellValueFactory(
                c -> new SimpleObjectProperty<>(
                        c.getValue().getFechaCaducidad()
                )
        );

        colPvDias.setCellValueFactory(
                c -> new SimpleIntegerProperty(
                        (int) c.getValue().getDiasParaVencer()
                )
        );

        colPvCantidad.setCellValueFactory(
                c -> new SimpleIntegerProperty(
                        c.getValue().getCantidadDisponible()
                )
        );

        colPvEstado.setCellValueFactory(
                c -> new SimpleStringProperty(
                        c.getValue().getEstadoCaducidad()
                )
        );

        Formato.columnaFecha(colPvFecha);

        // Vencidos: rojo. Próximos a vencer: amarillo.
        tblPorVencer.setRowFactory(
                Filas.porEstado(
                        lote -> lote.isVencido()
                                || lote.getDiasParaVencer() <= 7,
                        lote -> true
                )
        );

        colBmProducto.setCellValueFactory(
                c -> new SimpleStringProperty(
                        c.getValue().getNombreProducto()
                )
        );

        colBmStock.setCellValueFactory(
                c -> new SimpleIntegerProperty(
                        c.getValue().getStockActual()
                )
        );

        colBmMinimo.setCellValueFactory(
                c -> new SimpleIntegerProperty(
                        c.getValue().getStockMinimo()
                )
        );

        colBmFaltante.setCellValueFactory(
                c -> new SimpleIntegerProperty(
                        c.getValue().getStockMinimo()
                                - c.getValue().getStockActual()
                )
        );

        // Productos con existencias en cero: rojo.
        tblBajoMinimo.setRowFactory(
                Filas.peligro(
                        producto -> producto.getStockActual() == 0
                )
        );

        refrescar();
    }

    @FXML
    private void handleActualizar() {
        refrescar();
    }

    @Override
    public void refrescar() {
        try {
            int dias = cmbDias.getValue() != null
                    ? cmbDias.getValue()
                    : Lote.DIAS_ALERTA_VENCIMIENTO;

            List<Producto> productos =
                    productoDAO.listarConStock();

            List<Lote> porVencer =
                    loteDAO.listarPorVencer(dias);

            List<Lote> vencidos =
                    loteDAO.listarVencidosConStock();

            List<Lote> todos =
                    loteDAO.listarConProducto();

            List<Producto> bajoMinimo = productos.stream()
                    .filter(Producto::isBajoStockMinimo)
                    .sorted(
                            Comparator.comparingInt(
                                    p -> p.getStockActual()
                                            - p.getStockMinimo()
                            )
                    )
                    .toList();

            // Indicadores principales.
            lblKpiProductos.setText(
                    String.valueOf(productos.size())
            );

            lblKpiPorVencer.setText(
                    String.valueOf(porVencer.size())
            );

            lblKpiPorVencerTitulo.setText(
                    "Lotes que vencen en " + dias + " días"
            );

            lblKpiVencidos.setText(
                    String.valueOf(vencidos.size())
            );

            lblKpiBajoMinimo.setText(
                    String.valueOf(bajoMinimo.size())
            );

            lblTituloPorVencer.setText(
                    "Productos por vencer (próximos "
                            + dias + " días) y vencidos"
            );

            // Los vencidos aparecen primero en la tabla.
            var filas = FXCollections.<Lote>observableArrayList(vencidos);
            filas.addAll(porVencer);

            tblPorVencer.setItems(filas);

            tblBajoMinimo.setItems(
                    FXCollections.observableArrayList(bajoMinimo)
            );

            cargarGraficoStock(productos, bajoMinimo);
            cargarGraficoLotes(todos, dias);

        } catch (Exception e) {
            Alertas.error(
                    "Error de conexión",
                    "No se pudo cargar el dashboard:\n"
                            + VentanaManager.causaRaiz(e)
            );
        }
    }

    /**
     * Barras: productos bajo el mínimo o los de menor stock
     * si no hay productos por debajo del mínimo.
     */
    private void cargarGraficoStock(
            List<Producto> productos,
            List<Producto> bajoMinimo
    ) {
        List<Producto> mostrar = !bajoMinimo.isEmpty()
                ? bajoMinimo
                : productos.stream()
                .sorted(
                        Comparator.comparingInt(Producto::getStockActual)
                )
                .toList();

        mostrar = mostrar.stream()
                .limit(MAX_BARRAS)
                .toList();

        XYChart.Series<String, Number> actual =
                new XYChart.Series<>();

        actual.setName("Stock actual");

        XYChart.Series<String, Number> minimo =
                new XYChart.Series<>();

        minimo.setName("Stock mínimo");

        Set<String> usadas = new HashSet<>();

        for (Producto producto : mostrar) {
            String nombre = Graficos.etiqueta(
                    producto.getNombreProducto(),
                    usadas
            );

            actual.getData().add(
                    new XYChart.Data<>(
                            nombre,
                            producto.getStockActual()
                    )
            );

            minimo.getData().add(
                    new XYChart.Data<>(
                            nombre,
                            producto.getStockMinimo()
                    )
            );
        }

        chartStock.getData().clear();
        ejeProductos.getCategories().clear();

        chartStock.getData().add(actual);
        chartStock.getData().add(minimo);

        chartStock.setTitle(
                bajoMinimo.isEmpty()
                        ? "Productos con menor stock"
                        : "Productos bajo el stock mínimo"
        );
    }

    /**
     * Gráfico circular: lotes con existencias según su estado
     * de caducidad.
     */
    private void cargarGraficoLotes(
            List<Lote> lotes,
            int dias
    ) {
        long vigentes = 0;
        long porVencer = 0;
        long vencidos = 0;

        LocalDate limite = LocalDate.now().plusDays(dias);

        for (Lote lote : lotes) {
            if (lote.getCantidadDisponible() == null
                    || lote.getCantidadDisponible() <= 0) {
                continue;
            }

            if (lote.isVencido()) {
                vencidos++;

            } else if (!lote.getFechaCaducidad().isAfter(limite)) {
                porVencer++;

            } else {
                vigentes++;
            }
        }

        var datos = FXCollections.<PieChart.Data>observableArrayList();

        List<String> colores = new ArrayList<>();

        if (vigentes > 0) {
            datos.add(
                    new PieChart.Data(
                            "Vigentes (" + vigentes + ")",
                            vigentes
                    )
            );

            colores.add("#059669");
        }

        if (porVencer > 0) {
            datos.add(
                    new PieChart.Data(
                            "Por vencer (" + porVencer + ")",
                            porVencer
                    )
            );

            colores.add("#d97706");
        }

        if (vencidos > 0) {
            datos.add(
                    new PieChart.Data(
                            "Vencidos (" + vencidos + ")",
                            vencidos
                    )
            );

            colores.add("#dc2626");
        }

        chartEstadoLotes.setData(datos);
    }
}