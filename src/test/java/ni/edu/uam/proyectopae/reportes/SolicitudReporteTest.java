package ni.edu.uam.proyectopae.reportes;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudReporteTest {
    @Test
    void rechazaSeleccionVaciaCamposAjenosDuplicadosYFechasInvertidas() {
        assertThrows(IllegalArgumentException.class, () -> new SolicitudReporte(null, null, null, null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new SolicitudReporte(null, null, null, null, Map.of(ApartadoReporte.CLIENTES, List.of())));
        assertThrows(IllegalArgumentException.class, () -> new SolicitudReporte(null, null, null, null,
                Map.of(ApartadoReporte.CLIENTES, List.of(new CampoReporte("password", "Contraseña", 100)))));
        CampoReporte nombre = ApartadoReporte.CLIENTES.campos().get(1);
        assertThrows(IllegalArgumentException.class, () -> new SolicitudReporte(null, null, null, null,
                Map.of(ApartadoReporte.CLIENTES, List.of(nombre, nombre))));
        assertThrows(IllegalArgumentException.class, () -> new SolicitudReporte(LocalDate.now().plusDays(1), LocalDate.now(),
                null, null, SolicitudReporte.todos().seleccion()));
    }

    @Test
    void conservaLaSeleccionAunqueCambienLasListasDeLaPantallaYOrdenaLosApartados() {
        var campos = new ArrayList<>(ApartadoReporte.CLIENTES.campos());
        Map<ApartadoReporte, List<CampoReporte>> seleccion = new EnumMap<>(ApartadoReporte.class);
        seleccion.put(ApartadoReporte.CLIENTES, campos);
        seleccion.put(ApartadoReporte.PRODUCTOS, ApartadoReporte.PRODUCTOS.campos());
        SolicitudReporte solicitud = new SolicitudReporte(null, null, null, null, seleccion);
        campos.clear();
        seleccion.clear();
        assertEquals(7, solicitud.seleccion().get(ApartadoReporte.CLIENTES).size());
        assertEquals(List.of(ApartadoReporte.PRODUCTOS, ApartadoReporte.CLIENTES), List.copyOf(solicitud.seleccion().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> solicitud.seleccion().clear());
        assertThrows(UnsupportedOperationException.class, () -> solicitud.seleccion().get(ApartadoReporte.CLIENTES).clear());
    }

    @Test
    void noPermiteAgregarApartadosOCamposFueraDeLaSolicitudAlResultado() {
        var campo = ApartadoReporte.CLIENTES.campos().get(1);
        SolicitudReporte solicitud = new SolicitudReporte(null, null, null, null, Map.of(ApartadoReporte.CLIENTES, List.of(campo)));
        SeccionReporte clientes = new SeccionReporte(ApartadoReporte.CLIENTES, List.of(campo),
                List.of(Map.of("nombres", "Camila", "correo", "no-incluir@example.com")), null);
        assertEquals(Map.of("nombres", "Camila"), clientes.filas().getFirst());
        assertThrows(IllegalArgumentException.class, () -> new DatosReporte(solicitud, java.time.LocalDateTime.now(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DatosReporte(solicitud, java.time.LocalDateTime.now(), List.of(clientes, clientes)));
    }
}
