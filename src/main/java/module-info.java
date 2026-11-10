module ni.edu.uam.proyectopae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.naming;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;


    opens ni.edu.uam.proyectopae to javafx.fxml;
    opens ni.edu.uam.proyectopae.controllers to javafx.fxml;
    opens ni.edu.uam.proyectopae.models to org.hibernate.orm.core, javafx.base;
    exports ni.edu.uam.proyectopae;
}
