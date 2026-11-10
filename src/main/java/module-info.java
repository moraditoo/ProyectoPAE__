module ni.edu.uam.proyectofinalpae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    opens ni.edu.uam.proyectofinalpae to javafx.fxml;
    opens ni.edu.uam.proyectofinalpae.controllers to javafx.fxml;
    opens ni.edu.uam.proyectofinalpae.models to org.hibernate.orm.core, javafx.base;

    exports ni.edu.uam.proyectofinalpae;
    exports ni.edu.uam.proyectofinalpae.controllers;
    exports ni.edu.uam.proyectofinalpae.models;
    exports ni.edu.uam.proyectofinalpae.dao;
    exports ni.edu.uam.proyectofinalpae.util;
}