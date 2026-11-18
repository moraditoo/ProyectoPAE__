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

La propuesta está implementada en JavaFX y FXML. El tema se aplica con propiedades de JavaFX. La aplicación usa DM Sans cuando está instalada y recurre a Segoe UI en Windows en caso contrario.

El botón «Menú principal» minimiza la ventana activa. Puedes retomarla desde la tarjeta correspondiente. Las alertas de inventario aparecen en el menú y dan acceso al dashboard. Los reportes usan los registros de PostgreSQL.

## Novedades de la Interfaz

- **Modo Oscuro Dinámico:** Botón de alternancia rápida (Sol / Luna) disponible en la pantalla de login, en la barra del menú principal y en la barra superior compartida de todas las ventanas de módulos (`TemaPAE.barra`). Adapta fondos, tarjetas, textos, sombras, tablas y entradas con una paleta ciruela-carbón de alto contraste.
- **Centro de Control con Mini-KPIs en Vivo:** El menú principal ahora muestra estadísticas en tiempo real:
  - Ventas del día
  - Total de productos en catálogo
  - Clientes registrados
  - Citas agendadas
- **Barra de Acciones Rápidas:** Atajos directos a Nueva Venta, Registrar Cliente, Agendar Cita y Reportes desde el menú principal.
- **Resumen de Estados en Reportes:** Badges interactivos que totalizan citas programadas, completadas y canceladas al generar cualquier consulta.
- **Panel de Acceso Enriquecido:** Tarjetas de características y atajos en el login.
