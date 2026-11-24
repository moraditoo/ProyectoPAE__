package ni.edu.uam.proyectopae.controllers;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import ni.edu.uam.proyectopae.models.Servicio;
import ni.edu.uam.proyectopae.reportes.*;
import ni.edu.uam.proyectopae.reportes.ReporteJasperService.Formato;
import ni.edu.uam.proyectopae.util.TemaPAE;
import ni.edu.uam.proyectopae.util.VentanaManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** Carga el FXML real, con consultas sustituidas: no abre ventanas ni accede a PostgreSQL. */
class ReporteControllerTest {
    @TempDir Path temporal;
    private Parent raiz;
    private Scene escena;
    private ReporteController controlador;
    private final AtomicInteger consultas = new AtomicInteger();
    private final AtomicReference<SolicitudReporte> ultimaSolicitud = new AtomicReference<>();

    @BeforeAll static void iniciarJavaFx() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            inicio.countDown();
        });
        assertTrue(inicio.await(15, TimeUnit.SECONDS), "JavaFX debe iniciar");
    }

    @AfterAll static void cerrarJavaFx() { Platform.exit(); }

    @BeforeEach void cargarPantalla() throws Exception { cargar(this::datos); }

    private void cargar(Function<SolicitudReporte, DatosReporte> consulta) throws Exception {
        fx(() -> {
            Servicio servicio = new Servicio();
            servicio.setIdServicio(4);
            servicio.setNombreServicio("Maquillaje social");
            controlador = new ReporteController(solicitud -> {
                consultas.incrementAndGet();
                ultimaSolicitud.set(solicitud);
                return consulta.apply(solicitud);
            }, () -> List.of(servicio), new ReporteGeneralJasperService());
            FXMLLoader loader = VentanaManager.loader("reportes-view.fxml");
            loader.setControllerFactory(tipo -> controlador);
            raiz = loader.load();
            escena = VentanaManager.crearEscena(raiz, 1280, 820);
            raiz.applyCss();
            raiz.layout();
            return null;
        });
        esperar(() -> !nodo("configuracion", VBox.class).isDisabled());
    }

    @Test void muestraTodosLosApartadosYCamposYValidaLaSeleccion() throws Exception {
        fx(() -> {
            SolicitudReporte completa = controlador.crearSolicitud();
            assertEquals(SolicitudReporte.todos().seleccion(), completa.seleccion());
            assertTrue(nodo("btnExportar", Button.class).isDisabled());
            capturar("interfaz-inicial");
            nodo("btnNinguno", Button.class).fire();
            assertThrows(IllegalArgumentException.class, controlador::crearSolicitud);
            seleccionar(ApartadoReporte.CLIENTES);
            for (CampoReporte campo : ApartadoReporte.CLIENTES.campos()) {
                casilla("campo_CLIENTES_" + campo.clave()).setSelected(false);
            }
            assertTrue(assertThrows(IllegalArgumentException.class, controlador::crearSolicitud)
                    .getMessage().contains("Clientes"));
            nodo("btnTodos", Button.class).fire();
            assertEquals(completa.seleccion(), controlador.crearSolicitud().seleccion());
            nodo("dpDesde", DatePicker.class).setValue(LocalDate.of(2026, 11, 24));
            nodo("dpHasta", DatePicker.class).setValue(LocalDate.of(2026, 11, 23));
            assertThrows(IllegalArgumentException.class, controlador::crearSolicitud);
            return null;
        });
        assertEquals(0, consultas.get());
    }

    @Test void consultaSoloLaSeleccionYMuestraYExportaLosMismosCampos() throws Exception {
        fx(() -> {
            seleccionar(ApartadoReporte.CLIENTES, ApartadoReporte.VENTAS);
            elegirCampos(ApartadoReporte.CLIENTES, "nombres", "apellidos");
            elegirCampos(ApartadoReporte.VENTAS, "total");
            nodo("dpDesde", DatePicker.class).setValue(LocalDate.of(2026, 11, 1));
            nodo("dpHasta", DatePicker.class).setValue(LocalDate.of(2026, 11, 23));
            nodo("cmbServicio", ComboBox.class).getSelectionModel().select(1);
            nodo("panel_CLIENTES", TitledPane.class).setExpanded(true);
            generar();
            return null;
        });
        esperarReporte();
        Path csv = temporal.resolve("seleccion.csv");
        // Las cabeceras nuevas tienen una breve animación nativa de JavaFX.
        Thread.sleep(400);
        fx(() -> {
            SolicitudReporte solicitud = ultimaSolicitud.get();
            assertEquals(2, solicitud.seleccion().size());
            assertEquals(4, solicitud.idServicio());
            assertEquals(LocalDate.of(2026, 11, 1), solicitud.desde());
            assertEquals(LocalDate.of(2026, 11, 23), solicitud.hasta());
            TabPane tabs = nodo("vistaPrevia", TabPane.class);
            assertEquals(List.of("CLIENTES", "VENTAS"), tabs.getTabs().stream().map(Tab::getId).toList());
            TableView<?> tabla = (TableView<?>) tabs.getTabs().getFirst().getContent().lookup("#tabla_CLIENTES");
            assertEquals(List.of("Nombres", "Apellidos"), tabla.getColumns().stream().map(TableColumn::getText).toList());
            assertEquals(1, tabla.getItems().size());
            assertTrue(tabla.getColumns().stream().noneMatch(TableColumn::isSortable));
            assertTrue(tabla.getColumns().stream().noneMatch(TableColumn::isReorderable));
            raiz.applyCss();
            raiz.layout();
            return null;
        });
        fx(() -> {
            capturar("interfaz-clientes");
            for (var nodo : nodo("vistaPrevia", TabPane.class).lookupAll(".tab-label")) {
                if (nodo instanceof Labeled etiqueta) {
                    assertTrue(etiqueta.getWidth() >= etiqueta.prefWidth(-1) - 1,
                            "La cabecera debe mostrar su nombre: " + etiqueta.getText());
                }
            }
            TemaPAE.setModoOscuro(true);
            TemaPAE.aplicar(raiz);
            return null;
        });
        fx(() -> { capturar("interfaz-oscura"); return null; });
        fx(() -> {
            TemaPAE.setModoOscuro(false);
            TemaPAE.aplicar(raiz);
            controlador.guardarReporte(csv, Formato.CSV);
            return null;
        });
        esperar(() -> nodo("lblEstado", Label.class).getText().startsWith("Archivo guardado:"));
        String contenido = Files.readString(csv);
        assertTrue(contenido.contains("María"));
        assertTrue(contenido.contains("Pérez"));
        assertTrue(contenido.contains("C$ 690.00"));
        assertFalse(contenido.contains("oculto@example.com"));
        assertFalse(contenido.contains("ID cliente"));
        assertEquals(1, consultas.get(), "Exportar no debe volver a consultar la BD");
    }

    @Test void cambiosDeCamposApartadosFechasYServicioInvalidanLaExportacion() throws Exception {
        fx(() -> { seleccionar(ApartadoReporte.CLIENTES); generar(); return null; });
        esperarReporte();
        List<Runnable> cambios = List.of(
                () -> casilla("campo_CLIENTES_correo").setSelected(false),
                () -> nodo("dpDesde", DatePicker.class).setValue(LocalDate.of(2026, 11, 1)),
                () -> nodo("cmbServicio", ComboBox.class).getSelectionModel().select(1),
                () -> casilla("apartado_MARCAS").setSelected(true));
        for (Runnable cambio : cambios) {
            fx(() -> {
                cambio.run();
                assertTrue(nodo("btnExportar", Button.class).isDisabled());
                assertTrue(nodo("vistaPrevia", TabPane.class).getTabs().isEmpty());
                assertTrue(nodo("panelSinResultados", VBox.class).isVisible());
                generar();
                return null;
            });
            esperarReporte();
        }
        fx(() -> {
            nodo("btnLimpiar", Button.class).fire();
            assertNull(nodo("dpDesde", DatePicker.class).getValue());
            assertNull(controlador.crearSolicitud().idServicio());
            assertTrue(nodo("btnExportar", Button.class).isDisabled());
            assertEquals(2, controlador.crearSolicitud().seleccion().size(), "Limpiar filtros conserva las casillas");
            return null;
        });
    }

    @Test void permiteExportarUnApartadoSinRegistros() throws Exception {
        cargar(solicitud -> new DatosReporte(solicitud, LocalDateTime.now(), solicitud.seleccion().entrySet().stream()
                .map(e -> new SeccionReporte(e.getKey(), e.getValue(), List.of(), "Registros: 0")).toList()));
        fx(() -> { seleccionar(ApartadoReporte.CITAS); generar(); return null; });
        esperarReporte();
        Path pdf = temporal.resolve("sin-registros.pdf");
        Path csv = temporal.resolve("sin-registros.csv");
        fx(() -> {
            assertEquals(1, nodo("vistaPrevia", TabPane.class).getTabs().size());
            controlador.guardarReporte(pdf, Formato.PDF);
            return null;
        });
        esperar(() -> nodo("lblEstado", Label.class).getText().startsWith("Archivo guardado:"));
        assertEquals("%PDF", new String(Files.readAllBytes(pdf), 0, 4, StandardCharsets.US_ASCII));
        fx(() -> { controlador.guardarReporte(csv, Formato.CSV); return null; });
        esperar(() -> nodo("lblEstado", Label.class).getText().equals("Archivo guardado: sin-registros.csv"));
        assertTrue(Files.readString(csv).contains("Sin registros con los filtros seleccionados."));
        assertEquals(1, consultas.get());
    }

    @Test void generaLosDoceApartadosYConservaElAccesoEnUnaPantallaPequena() throws Exception {
        fx(() -> { generar(); return null; });
        esperarReporte();
        Thread.sleep(400);
        fx(() -> {
            TabPane tabs = nodo("vistaPrevia", TabPane.class);
            assertEquals(12, tabs.getTabs().size());
            for (Tab tab : tabs.getTabs()) {
                ApartadoReporte apartado = ApartadoReporte.valueOf(tab.getId());
                TableView<?> tabla = (TableView<?>) tab.getContent().lookup("#tabla_" + tab.getId());
                assertEquals(apartado.campos().size(), tabla.getColumns().size());
            }
            ((javafx.scene.layout.Region) raiz).resize(900, 600);
            raiz.applyCss();
            raiz.layout();
            return null;
        });
        fx(() -> { capturar("interfaz-900"); return null; });
        assertEquals(1, consultas.get());
    }

    @Test void bloqueaLosControlesMientrasConsultaYLosRecuperaTrasUnError() throws Exception {
        CountDownLatch continuar = new CountDownLatch(1);
        AtomicInteger intentos = new AtomicInteger();
        cargar(solicitud -> {
            if (intentos.getAndIncrement() == 0) {
                try {
                    if (!continuar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Espera agotada");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                throw new IllegalStateException("Conexión de prueba no disponible");
            }
            return datos(solicitud);
        });
        try {
            fx(() -> {
                seleccionar(ApartadoReporte.MARCAS);
                generar();
                assertTrue(nodo("configuracion", VBox.class).isDisabled());
                assertTrue(nodo("btnExportar", Button.class).isDisabled());
                assertTrue(nodo("progreso", ProgressIndicator.class).isVisible());
                return null;
            });
        } finally { continuar.countDown(); }
        esperar(() -> nodo("lblEstado", Label.class).getText().contains("Conexión de prueba no disponible"));
        fx(() -> {
            assertFalse(nodo("configuracion", VBox.class).isDisabled());
            assertTrue(nodo("btnExportar", Button.class).isDisabled());
            generar();
            return null;
        });
        esperarReporte();
        assertEquals(2, consultas.get());
    }

    private DatosReporte datos(SolicitudReporte solicitud) {
        return new DatosReporte(solicitud, LocalDateTime.of(2026, 11, 23, 17, 30),
                solicitud.seleccion().entrySet().stream().map(e -> {
                    Map<String, String> fila = new LinkedHashMap<>();
                    e.getKey().campos().forEach(c -> fila.put(c.clave(), c.titulo() + " de prueba"));
                    if (e.getKey() == ApartadoReporte.CLIENTES) {
                        fila.putAll(Map.of("id", "7", "nombres", "María", "apellidos", "Pérez",
                                "correo", "oculto@example.com", "alergias", "Sin alergias declaradas"));
                    }
                    if (e.getKey() == ApartadoReporte.VENTAS) fila.put("total", "C$ 690.00");
                    return new SeccionReporte(e.getKey(), e.getValue(), List.of(fila), "Registros: 1");
                }).toList());
    }

    private void seleccionar(ApartadoReporte... incluidos) {
        nodo("btnNinguno", Button.class).fire();
        for (ApartadoReporte apartado : incluidos) casilla("apartado_" + apartado.name()).setSelected(true);
    }

    private void elegirCampos(ApartadoReporte apartado, String... claves) {
        List<String> incluidos = List.of(claves);
        apartado.campos().forEach(c -> casilla("campo_" + apartado.name() + "_" + c.clave())
                .setSelected(incluidos.contains(c.clave())));
    }

    private void generar() { nodo("btnGenerar", Button.class).fire(); }

    private void esperarReporte() throws Exception {
        esperar(() -> !nodo("btnExportar", Button.class).isDisabled());
    }

    private void esperar(BooleanSupplier condicion) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite) {
            if (fx(condicion::getAsBoolean)) return;
            Thread.sleep(25);
        }
        fail("La operación no terminó. Estado: " + fx(() -> nodo("lblEstado", Label.class).getText()));
    }

    private CheckBox casilla(String id) { return nodo(id, CheckBox.class); }

    private <T> T nodo(String id, Class<T> tipo) {
        // El contenido de un TitledPane cerrado puede no pertenecer aún al árbol de la escena.
        Object encontrado = raiz.lookup("#" + id);
        if (encontrado == null) {
            VBox lista = (VBox) raiz.lookup("#listaApartados");
            for (var hijo : lista.getChildren()) {
                TitledPane panel = (TitledPane) hijo;
                if (id.equals(panel.getGraphic().getId())) encontrado = panel.getGraphic();
                if (encontrado == null) encontrado = panel.getContent().lookup("#" + id);
                if (encontrado != null) break;
            }
        }
        return tipo.cast(java.util.Objects.requireNonNull(encontrado, "Control " + id));
    }

    private static <T> T fx(Callable<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion);
        Platform.runLater(tarea);
        try { return tarea.get(25, TimeUnit.SECONDS); }
        catch (ExecutionException error) {
            if (error.getCause() instanceof Exception causa) throw causa;
            if (error.getCause() instanceof Error causa) throw causa;
            throw error;
        }
    }

    /** Captura de la escena real para revisión visual, sin mostrar una ventana. */
    private void capturar(String nombre) throws Exception {
        raiz.applyCss();
        raiz.layout();
        WritableImage imagen = raiz.snapshot(null, null);
        int ancho = (int) imagen.getWidth(), alto = (int) imagen.getHeight();
        ByteArrayOutputStream ppm = new ByteArrayOutputStream();
        ppm.write(("P6\n" + ancho + " " + alto + "\n255\n").getBytes(StandardCharsets.US_ASCII));
        var lector = imagen.getPixelReader();
        for (int y = 0; y < alto; y++) for (int x = 0; x < ancho; x++) {
            int color = lector.getArgb(x, y);
            ppm.write((color >> 16) & 255);
            ppm.write((color >> 8) & 255);
            ppm.write(color & 255);
        }
        Path carpeta = Path.of("target", "pruebas-reportes");
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve(nombre + ".ppm"), ppm.toByteArray());
    }
}
