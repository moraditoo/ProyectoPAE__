module ni.edu.uam.proyectopae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.naming;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires net.sf.jasperreports.core;
    requires net.sf.jasperreports.pdf;
    requires net.sf.jasperreports.jdt;
    requires net.sf.jasperreports.fonts;

    opens ni.edu.uam.proyectopae to javafx.fxml;
    opens ni.edu.uam.proyectopae.controllers to javafx.fxml;
    opens ni.edu.uam.proyectopae.models to org.hibernate.orm.core, javafx.base;

    exports ni.edu.uam.proyectopae;
    exports ni.edu.uam.proyectopae.controllers;
    exports ni.edu.uam.proyectopae.models;
    exports ni.edu.uam.proyectopae.dao;
    exports ni.edu.uam.proyectopae.util;
    exports ni.edu.uam.proyectopae.reportes;
}
