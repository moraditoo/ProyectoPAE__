# Proyecto PAE

Aplicación JavaFX con JDK 21, Hibernate y PostgreSQL. Los reportes generales se
generan con **JasperReports 7.0.6** y se exportan en PDF y CSV, con apartados y
campos seleccionables mediante casillas.

La [guía de instalación del equipo](docs/CONFIGURACION_EQUIPO.md) contiene los pasos
para preparar las cuatro computadoras, configurar IntelliJ y editar las plantillas
con Jaspersoft Studio 7.0.6.

## Preparación inicial

1. Instalar JDK 21 y PostgreSQL (la guía usa PostgreSQL 18).
2. Crear una base vacía llamada `tienda_maquillaje_db` en PostgreSQL.
3. Copiar `config/pae-local.example.properties` como `config/pae-local.properties`
   y escribir las credenciales locales. El archivo personal está excluido de Git.

Desde la raíz del proyecto, en PowerShell:

```powershell
# Ejecutar solo la primera vez; después completar las credenciales del archivo.
Copy-Item config/pae-local.example.properties config/pae-local.properties
# Compilar, probar JasperReports y empaquetar (no requiere PostgreSQL encendido).
.\scripts\pae.ps1 -Accion verificar
# Comprobar JasperReports, las fuentes y la conexión real con PostgreSQL.
.\scripts\pae.ps1 -Accion diagnostico
# Abrir la aplicación.
.\scripts\pae.ps1 -Accion ejecutar
```

El script encuentra JDK 21, incluido el instalado por IntelliJ en `.jdks`.
Se puede elegir uno explícitamente con `-Jdk 'C:\ruta\al\jdk-21'`.
Maven Wrapper descarga las dependencias; no hace falta instalar Maven, JavaFX ni
los JAR de JasperReports por separado. La primera ejecución necesita Internet.

Con `JAVA_HOME` configurado también se puede usar directamente el wrapper:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd compile -Pdiagnostico javafx:run
.\mvnw.cmd compile javafx:run
```

## Reportes

En **Menú → Reportes**:

1. Marcar los apartados que se quieren incluir. Inicialmente están todos seleccionados.
2. Desplegar cada apartado con su flecha y marcar sus campos. Cada apartado incluido
   necesita al menos un campo. **Seleccionar todo** restaura todos los apartados y campos;
   **Ninguno**, junto a ese botón, desmarca los apartados conservando sus campos.
3. Elegir fechas y servicio si corresponde; dejarlos vacíos incluye todo el período.
4. Pulsar **Generar reporte** y revisar cada apartado en su pestaña.
5. Pulsar **Imprimir** para abrir la vista previa del PDF. Recorrer las páginas
   con **Anterior / Siguiente** y ajustar la ampliación si hace falta.
6. Desde esa ventana, pulsar **Imprimir** para elegir impresora, copias y páginas,
   o **Guardar PDF** para elegir la ubicación del documento.
   **Exportar PDF / CSV** sigue disponible en el generador para elegir cualquiera
   de los dos formatos.

La vista previa muestra las mismas páginas que se guardan en el PDF y conserva
los datos del reporte generado. La impresión se ajusta al papel elegido sin
recortar la página. Si no hay impresoras disponibles, se puede usar **Guardar PDF**.
Los controles de la ventana respetan el modo claro y oscuro; las hojas conservan
el fondo blanco del documento. Consulta la [guía de impresión](docs/IMPRESION_REPORTES.md).

Están disponibles resumen general, productos e inventario, lotes y caducidad,
clientes, servicios, agenda de citas, ventas, detalle de ventas, consumos de
insumos, categorías, marcas y usuarios con sus roles. No se incluyen contraseñas.

Las fechas filtran ventas y sus detalles, citas, consumos según su cita e ingreso
de lotes. Los catálogos y el stock corresponden al estado actual. El filtro de
servicio afecta servicios, citas, consumos y los indicadores de citas del resumen;
las ventas de productos son independientes del servicio.

El PDF incluye una sección por apartado, con tablas en A4 o A3 horizontal según
la cantidad de campos. El CSV reúne los apartados en cuatro columnas: **Apartado,
Registro, Campo y Valor**, con una fila por campo seleccionado. Registro es la
posición de la fila dentro del apartado; los identificadores de la base son campos
seleccionables. PDF y CSV conservan la selección y los datos de la vista previa.

Cambiar filtros o casillas limpia la vista previa y requiere generar de nuevo.
Los apartados sin resultados se pueden exportar con el aviso **Sin registros con
los filtros seleccionados**. **Limpiar filtros** conserva las casillas elegidas.

La plantilla activa está en `src/main/resources/reportes/general.jrxml`. El motor
construye sus columnas según la selección y la rellena con datos consultados por
Hibernate. `citas.jrxml` se conserva para el motor anterior y sus pruebas. El
diagnóstico guarda ejemplos ficticios de los 12 apartados en
`target/diagnostico-reportes/`; esos datos nunca se insertan en PostgreSQL.

## Base de datos y acceso

Se usa PostgreSQL, no H2. Hibernate crea o actualiza las tablas en la base existente.
Las variables `PAE_DB_URL`, `PAE_DB_USER` y `PAE_DB_PASSWORD` tienen prioridad sobre
el archivo personal. El directorio de trabajo debe ser la raíz del proyecto.

En una base sin usuarios, el inicio de sesión crea `admin` / `admin123`.
`PAE_ADMIN_USER` y `PAE_ADMIN_PASSWORD` permiten elegir esos valores antes del
primer inicio. Una base que ya tenga usuarios conserva sus cuentas.

Las pruebas del motor no requieren PostgreSQL; las de consultas usan una base H2
aislada y las de pantalla cargan el FXML en una sesión gráfica sin abrir ventanas.
El diagnóstico comprueba JasperReports y la consulta de los 12 apartados en la base local.
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