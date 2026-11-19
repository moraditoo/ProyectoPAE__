# Relaciones del modelo

## Venta y detalle de venta

**Multiplicidad 1–N.** Una venta tiene uno o varios detalles (una línea por producto vendido) y cada detalle pertenece a una sola venta. En la base de datos esto se ve en la tabla `detalle_ventas`, que tiene la columna `id_venta` apuntando a `ventas`.

**`mappedBy` en Venta.** En `Venta.detalles` se usa `@OneToMany(mappedBy = "venta", ...)`. El `mappedBy` indica que `Venta` no es el dueño de la relación: la clave foránea no está en la tabla `ventas`, sino en `detalle_ventas`, y la controla el atributo `venta` de `DetalleVenta`. Sin `mappedBy`, Hibernate crearía una tabla intermedia innecesaria.

**`@JoinColumn` en DetalleVenta.** En `DetalleVenta.venta` se usa `@ManyToOne` con `@JoinColumn(name = "id_venta", nullable = false)`. Este es el lado dueño: define el nombre de la columna de la clave foránea y obliga a que todo detalle tenga una venta.

**Métodos de ayuda.** Como la relación es bidireccional, hay que mantener los dos lados iguales en memoria; si solo se agrega el detalle a la lista y no se asigna `detalle.setVenta(this)`, Hibernate guardaría `id_venta` en nulo.

- `agregarDetalle(detalle)`: valida que el detalle no sea nulo y que no pertenezca a otra venta, lo agrega a la lista (sin duplicarlo), le asigna esta venta y recalcula `total` y `costoTotal`.
- `quitarDetalle(detalle)`: si el detalle estaba en la lista, lo quita, le pone la venta en `null` y recalcula los totales. Como `Venta.detalles` tiene `orphanRemoval = true`, el detalle quitado se elimina de la base al guardar.
- `setDetalles(lista)`: copia la lista recibida, quita uno por uno los detalles actuales con `quitarDetalle` y agrega los nuevos con `agregarDetalle`. Así no se reemplaza la colección que Hibernate administra y los dos lados quedan sincronizados.

## Servicio y producto

**Multiplicidad N–M.** Un servicio (por ejemplo, maquillaje de gala) puede usar varios productos, y un mismo producto (por ejemplo, una base) puede usarse en varios servicios.

**`@ManyToMany`.** Se declaró en `Servicio.productos` con `fetch = FetchType.LAZY`, por lo que los productos solo se cargan cuando se piden. `Servicio` es el dueño de la relación porque es quien tiene el `@JoinTable`. No se puso cascada: guardar o borrar un servicio no debe crear ni borrar productos del catálogo.

**Tabla de unión `servicio_productos`.** Hibernate la crea con dos columnas: `id_servicio` (clave foránea a `servicios`) e `id_producto` (clave foránea a `productos`). La restricción `uk_servicio_producto` impide que el mismo producto quede asociado dos veces al mismo servicio. Al quitar una asociación solo se borra la fila de esta tabla; el producto sigue existiendo.

**Productos asociados vs. consumo real.** `servicio_productos` es solo el catálogo de productos que *normalmente* usa un servicio; no guarda cantidades ni lotes. Lo que realmente se gasta en una cita concreta (qué lote y cuántas unidades) se sigue registrando en `ConsumoInsumo`, que es lo que afecta el stock.

**Uso en la aplicación.** En la Agenda, el botón "Configurar productos del servicio" abre un diálogo con todos los productos. `ServicioDAO.buscarConProductos` usa `LEFT JOIN FETCH` para traer los productos antes de cerrar la sesión (si no, al recorrer la lista daría `LazyInitializationException`). `ServicioDAO.actualizarProductos` quita los repetidos, comprueba que todos los productos existan y reemplaza las asociaciones dentro de una sola transacción.

## Decisiones sobre cascadas

**Se conserva en `Venta.detalles`** (`CascadeType.ALL` y `orphanRemoval = true`) porque un detalle no tiene sentido sin su venta: se guarda junto con la venta y, si la venta se borra o se quita una línea, el detalle también debe desaparecer.

**Se retira de las relaciones de catálogo e historial:** `Categoria.productos`, `Marca.productos`, `Cliente.citas`, `Servicio.citas`, `Producto.lotes`, `Lote.consumos` y `Cita.consumos`. Con la cascada anterior, borrar una categoría borraba todos sus productos, con ellos sus lotes y con ellos los consumos; y borrar un cliente borraba todo su historial de citas. Eso es pérdida de información en cadena.

Ahora, si un registro tiene dependencias, la base de datos bloquea la eliminación por la clave foránea y la aplicación muestra el error al usuario. Para borrarlo primero hay que reasignar o eliminar a propósito sus dependencias. Esto también aplica a `servicio_productos`: un producto asociado a un servicio no se puede borrar hasta quitarle la asociación.

## Validación

> Completar la columna "Resultado observado" después de ejecutar cada prueba.

| # | Operación realizada | Resultado esperado | Resultado observado |
|---|---|---|---|
| 1 | Asociar dos productos a un servicio desde Agenda → "Configurar productos del servicio" y pulsar Guardar. | Mensaje "Productos actualizados"; aparecen dos filas en `servicio_productos`. | |
| 2 | Cerrar la aplicación y volver a abrirla. | La aplicación inicia sin errores. | |
| 3 | Abrir de nuevo el diálogo del mismo servicio. | Los dos productos aparecen seleccionados. | |
| 4 | Quitar uno de los productos y guardar. | Queda una sola fila en `servicio_productos`; el producto quitado sigue apareciendo en Inventario. | |
| 5 | Abrir el diálogo sin seleccionar servicio. | Mensaje de validación "Selecciona un servicio." | |
| 6 | Registrar una venta con dos productos en Ventas. | Se guardan la venta y sus dos detalles con `id_venta` correcto y el total coincide con la suma de subtotales. | |
| 7 | Intentar eliminar un cliente que tiene citas. | La eliminación se bloquea y se muestra el error; el cliente y sus citas siguen en la base. | |
| 8 | Intentar eliminar un producto que tiene lotes o está asociado a un servicio. | La eliminación se bloquea y se muestra el error; no se borra ningún lote ni asociación. | |

**Problemas encontrados y solución aplicada:**

- (Anotar aquí cualquier error que haya aparecido durante las pruebas y cómo se resolvió. Si no hubo ninguno, indicarlo.)