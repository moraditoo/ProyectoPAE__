package ni.edu.uam.proyectopae.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import ni.edu.uam.proyectopae.dao.DashboardDAO;
import ni.edu.uam.proyectopae.models.Usuario;
import ni.edu.uam.proyectopae.util.Alertas;
import ni.edu.uam.proyectopae.util.Icono;
import ni.edu.uam.proyectopae.util.Navegador;
import ni.edu.uam.proyectopae.util.Navegador.Modulo;
import ni.edu.uam.proyectopae.util.Tema;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Menú principal posterior al inicio de sesión: una tarjeta de color por cada módulo del sistema. */
public class MenuPrincipalController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblFecha;
    @FXML private TilePane panelModulos;

    @FXML
    public void initialize() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es")));
        lblFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));

        agregarModulo(Modulo.DASHBOARD, "Indicadores, alertas, finanzas y gráficos", "#4f46e5");
        agregarModulo(Modulo.VENTAS, "Venta de productos a clientes", "#059669");
        agregarModulo(Modulo.PRODUCTOS, "Catálogo, precios y existencias mínimas", "#2563eb");
        agregarModulo(Modulo.LOTES, "Lotes y fechas de vencimiento", "#d97706");
        agregarModulo(Modulo.CLIENTES, "Registro y datos de contacto de clientes", "#7c3aed");
        agregarModulo(Modulo.AGENDA, "Citas y servicios de maquillaje", "#0891b2");

        panelModulos.getChildren().add(Tema.tarjetaMenu("Cerrar Sesión", "Regresar a la pantalla de inicio de sesión",
                Icono.Tipo.SALIR, Color.web("#dc2626"), this::cerrarSesion));
        panelModulos.getChildren().add(Tema.tarjetaMenu("Salir", "Cerrar todas las ventanas del sistema",
                Icono.Tipo.CANCELAR, Color.web("#475569"), this::salir));
    }

    public void setUsuario(Usuario usuario) {
        lblBienvenida.setText("Bienvenido(a), " + usuario.getUsername() + "  ·  " + usuario.getRol());
    }

    /** Al iniciar sesión avisa de productos vencidos, próximos a vencer y existencias bajo el mínimo. */
    public void mostrarAlertasIniciales() {
        try {
            DashboardController.mostrarAlertasInventario(new DashboardDAO().cargar());
        } catch (Exception e) {
            Alertas.errorBD("revisar el estado del inventario", e);
        }
    }

    private void agregarModulo(Modulo modulo, String descripcion, String color) {
        panelModulos.getChildren().add(Tema.tarjetaMenu(modulo.getTitulo(), descripcion, modulo.getIcono(),
                Color.web(color), () -> Navegador.abrir(modulo)));
    }

    private void cerrarSesion() {
        if (Alertas.confirmar("¿Cerrar la sesión?", "Se cerrarán todas las ventanas abiertas del sistema.")) {
            Navegador.cerrarSesion();
        }
    }

    private void salir() {
        if (Alertas.confirmar("¿Salir del sistema?", "Se cerrarán todas las ventanas de la aplicación.")) {
            Platform.exit();
        }
    }
}
