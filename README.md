## Módulo de servicios web: Catálogo del proveedor (API REST)

**Objetivo.** El estudio necesita consultar el catálogo de su proveedor de cosméticos
(precios, marcas y existencias) sin registrar los datos a mano. El módulo
**Menú → Catálogo proveedor** consume un servicio web REST desde JavaFX.

**API utilizada.** `api-catalogo/` es una API propia hecha con Spring Boot 3.5.16,
Spring Data JPA y H2 en memoria. **Los datos son simulados**: se cargan 13 productos
al iniciar la API y se reinician cada vez que se apaga.

### Endpoints

| Método | URL | Uso en JavaFX | Respuestas |
|---|---|---|---|
| GET | `/api/productos?buscar=&categoria=` | Buscar con filtros | 200 |
| GET | `/api/productos/categorias` | Llenar el filtro de categorías | 200 |
| GET | `/api/productos/{id}` | Detalle al seleccionar una fila o consultar por ID | 200 / 404 |
| POST | `/api/productos` | Registrar producto | 201 / 400 / 409 |
| PUT | `/api/productos/{id}` | Guardar cambios | 200 / 400 / 404 / 409 |
| DELETE | `/api/productos/{id}` | Eliminar producto | 204 / 404 |

Los errores siempre responden `{"mensaje": "..."}`, y JavaFX muestra ese texto.

### Relación entre el JSON y la interfaz

| Clave JSON | Atributo Java (`api.ProductoProveedor`) | Columna / campo JavaFX |
|---|---|---|
| `codigo` | `codigo` | Código |
| `nombre` | `nombre` | Nombre |
| `marca` | `marca` | Marca |
| `categoria` | `categoria` | Categoría |
| `precio` | `precio` (`BigDecimal`) | Precio (C$) |
| `existencias` | `existencias` | Existencias |

### Organización

- `api/CatalogoApiClient`: `HttpClient`, armado de URL, método HTTP, verificación del código de estado y Jackson.
- `api/ProductoProveedor`, `api/RespuestaApi`, `api/ApiException`: modelo del JSON, respuesta (método, URL, estado) y errores.
- `controllers/CatalogoProveedorController` + `catalogo-proveedor-view.fxml`: interfaz. Las solicitudes corren en un `Task` en segundo plano y la interfaz se actualiza en el hilo de JavaFX.

### Cómo ejecutar

1. Abrir `api-catalogo` en IntelliJ (**File → Open**, ventana nueva) y ejecutar `ApiCatalogoApplication`
   (▶ junto a `main`) o, en una terminal dentro de `api-catalogo`, `.\mvnw.cmd spring-boot:run`.
   Debe aparecer `Tomcat started on port 8080`.
2. Comprobar en el navegador: `http://localhost:8080/api/productos`.
3. Ejecutar la aplicación JavaFX como siempre (`.\scripts\pae.ps1 -Accion ejecutar`) y abrir
   **Menú → Catálogo proveedor**.

La URL de la API se puede cambiar con la variable `PAE_API_URL` o con `api.url` en
`config/pae-local.properties` (ver `config/pae-local.example.properties`). La API no usa credenciales.

**Dependencias agregadas:** `com.fasterxml.jackson.core:jackson-databind:2.18.2` en la aplicación
JavaFX (y `requires java.net.http; requires com.fasterxml.jackson.databind;` en `module-info.java`).

### Pruebas

`api-catalogo/postman/CatalogoProveedor.postman_collection.json` contiene 11 solicitudes
(casos correctos, lista vacía, 400, 404 y 409). Importarla en Postman con **Import**.

| Escenario en JavaFX | Resultado esperado |
|---|---|
| Abrir el módulo con la API encendida | Tabla con 13 productos y `GET .../api/productos → 200` |
| Buscar `labial` con categoría *Labiales* | 2 productos |
| Buscar `zzz` | «No se encontraron productos con esos filtros.» |
| Consultar ID `999` | Alerta «Error 404: Producto no encontrado: 999» |
| Registrar con precio `abc` | Validación local, no se envía nada |
| Registrar con precio `-5` | Alerta con el mensaje 400 de la API |
| Registrar un código repetido | Alerta 409 |
| Apagar la API y pulsar Buscar | «No se pudo conectar con la API...», la ventana no se congela |