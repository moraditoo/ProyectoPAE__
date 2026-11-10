module ni.edu.uam.proyectopae {
    requires javafx.controls;
    requires javafx.fxml;

<<<<<<< Updated upstream

    opens ni.edu.uam.proyectopae to javafx.fxml;
    exports ni.edu.uam.proyectopae;
=======
    opens ni.edu.uam.proyectopae to javafx.fxml;
    opens ni.edu.uam.proyectopae.controllers to javafx.fxml;
    opens ni.edu.uam.proyectopae.models to org.hibernate.orm.core, javafx.base;

    exports ni.edu.uam.proyectopae;
    exports ni.edu.uam.proyectopae.controllers;
    exports ni.edu.uam.proyectopae.models;
    exports ni.edu.uam.proyectopae.dao;
    exports ni.edu.uam.proyectopae.util;
>>>>>>> Stashed changes
}