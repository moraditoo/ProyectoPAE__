# Propuesta de interfaz de ProyectoPAE

[Abrir el diseño editable en Figma](https://www.figma.com/design/Rs6ZnD4uUWSJVA2WjKGe1k)

La propuesta usa una interfaz de escritorio de 1280 × 820 px, una paleta ciruela y la tipografía DM Sans. Después del inicio de sesión, el menú presenta siete módulos que se abren en ventanas independientes en JavaFX. La acción «Cerrar sesión» está al pie del menú.

| Pantalla | Vista FXML |
| --- | --- |
| Inicio de sesión | login-view.fxml |
| Menú principal | menu-view.fxml |
| Dashboard de inventario | dashboard-inventario-view.fxml |
| Ventas | ventas-view.fxml |
| Clientes | clientes-view.fxml |
| Productos | inventario-view.fxml |
| Lotes y caducidad | lotes-servicios-view.fxml |
| Agenda de citas | agenda-view.fxml |
| Reportes de citas | reportes-view.fxml |

En Reportes, el usuario filtra por fecha y servicio, elige las columnas, genera una vista previa y puede exportar a CSV. Figma muestra el estado inicial y el resultado generado. Los nombres, montos y registros del diseño son ilustrativos.

El alcance de esta rama es la documentación de la propuesta. Su implementación en JavaFX queda pendiente.
