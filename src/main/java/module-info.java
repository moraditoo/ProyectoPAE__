module ni.edu.uam.proyectopae {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.proyectopae to javafx.fxml;
    exports ni.edu.uam.proyectopae;
}