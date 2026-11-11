package ni.edu.uam.proyectopae.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainViewController {

    @FXML private TabPane mainTabPane;
    @FXML private Tab tabClientes;
    @FXML private Tab tabProductos;
    @FXML private Tab tabLotes;
    @FXML private Tab tabAgenda;

    @FXML private ClienteController clientesViewController;
    @FXML private InventarioController inventarioViewController;
    @FXML private LotesServiciosController lotesViewController;
    @FXML private AgendaController agendaViewController;

    @FXML
    public void initialize() {

        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tabClientes && clientesViewController != null) {
                clientesViewController.refrescarTabla();
            } else if (newTab == tabProductos && inventarioViewController != null) {
                inventarioViewController.refrescarTabla();
            } else if (newTab == tabLotes && lotesViewController != null) {
                lotesViewController.refrescarTabla();
            } else if (newTab == tabAgenda && agendaViewController != null) {
                agendaViewController.handleRecargarTodo();
            }
        });
    }
}