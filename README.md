# Proyecto PAE

Requiere JDK 21. Configure `JAVA_HOME` con la ruta del JDK y ejecute en PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
```

La aplicación abre Inventario y Lotes. Primero registre un producto en Inventario;
al seleccionar Lotes se actualiza la lista de productos disponibles.

Hibernate crea una base H2 local en `data/proyectopae.mv.db`; los datos se conservan
al cerrar la aplicación. Las pruebas utilizan una base independiente en memoria.
No se requiere un servidor de base de datos.

El registro de productos admite el nombre y la gestión de lotes permite registrar
y eliminar lotes. Login y Agenda siguen siendo plantillas sin funcionalidad y no
forman parte de la pantalla inicial. Los modelos y DAO restantes requieren completar
las reglas del negocio antes de habilitar esas funciones.
