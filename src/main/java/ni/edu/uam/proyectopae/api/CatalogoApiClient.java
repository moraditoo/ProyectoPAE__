package ni.edu.uam.proyectopae.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Cliente HTTP del catálogo del proveedor (proyecto api-catalogo).
 * Aquí vive toda la lógica de red: los controladores JavaFX solo llaman a estos métodos.
 *
 * La URL base se toma, en este orden, de:
 *   1. la variable de entorno PAE_API_URL
 *   2. la clave api.url de config/pae-local.properties
 *   3. http://localhost:8080/api/productos
 */
public class CatalogoApiClient {

    private static final String URL_PREDETERMINADA = "http://localhost:8080/api/productos";

    private final String baseUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public CatalogoApiClient() {
        this(urlConfigurada());
    }

    public CatalogoApiClient(String baseUrl) {
        String url = baseUrl == null || baseUrl.isBlank() ? URL_PREDETERMINADA : baseUrl.trim();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // ------------------------------------------------------------ operaciones

    /** GET /api/productos?buscar=...&categoria=...  (consulta con filtros) */
    public RespuestaApi<List<ProductoProveedor>> listar(String buscar, String categoria) throws ApiException {
        List<String> parametros = new ArrayList<>();
        if (buscar != null && !buscar.isBlank()) {
            parametros.add("buscar=" + codificar(buscar.trim()));
        }
        if (categoria != null && !categoria.isBlank()) {
            parametros.add("categoria=" + codificar(categoria.trim()));
        }
        String url = parametros.isEmpty() ? baseUrl : baseUrl + "?" + String.join("&", parametros);

        HttpResponse<String> respuesta = enviar(peticion(url).GET());
        ProductoProveedor[] productos = leer(respuesta.body(), ProductoProveedor[].class);
        return new RespuestaApi<>(List.of(productos), "GET", url, respuesta.statusCode());
    }

    /** GET /api/productos/{id}  (detalle de un producto) */
    public RespuestaApi<ProductoProveedor> obtener(long id) throws ApiException {
        String url = baseUrl + "/" + id;
        HttpResponse<String> respuesta = enviar(peticion(url).GET());
        return new RespuestaApi<>(leer(respuesta.body(), ProductoProveedor.class), "GET", url, respuesta.statusCode());
    }

    /** GET /api/productos/categorias */
    public RespuestaApi<List<String>> categorias() throws ApiException {
        String url = baseUrl + "/categorias";
        HttpResponse<String> respuesta = enviar(peticion(url).GET());
        String[] categorias = leer(respuesta.body(), String[].class);
        return new RespuestaApi<>(List.of(categorias), "GET", url, respuesta.statusCode());
    }

    /** POST /api/productos */
    public RespuestaApi<ProductoProveedor> crear(ProductoProveedor producto) throws ApiException {
        HttpResponse<String> respuesta = enviar(peticion(baseUrl)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(aJson(producto), StandardCharsets.UTF_8)));
        return new RespuestaApi<>(leer(respuesta.body(), ProductoProveedor.class), "POST", baseUrl, respuesta.statusCode());
    }

    /** PUT /api/productos/{id} */
    public RespuestaApi<ProductoProveedor> actualizar(long id, ProductoProveedor producto) throws ApiException {
        String url = baseUrl + "/" + id;
        HttpResponse<String> respuesta = enviar(peticion(url)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(aJson(producto), StandardCharsets.UTF_8)));
        return new RespuestaApi<>(leer(respuesta.body(), ProductoProveedor.class), "PUT", url, respuesta.statusCode());
    }

    /** DELETE /api/productos/{id} */
    public RespuestaApi<Void> eliminar(long id) throws ApiException {
        String url = baseUrl + "/" + id;
        HttpResponse<String> respuesta = enviar(peticion(url).DELETE());
        return new RespuestaApi<>(null, "DELETE", url, respuesta.statusCode());
    }

    // ------------------------------------------------------------ apoyo

    private HttpRequest.Builder peticion(String url) throws ApiException {
        try {
            return HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json");
        } catch (IllegalArgumentException e) {
            throw new ApiException(0, "La URL de la API no es válida: " + url, e);
        }
    }

    /** Envía la solicitud y verifica el código de estado HTTP. */
    private HttpResponse<String> enviar(HttpRequest.Builder builder) throws ApiException {
        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new ApiException(0, "No se pudo conectar con la API en " + baseUrl
                    + ".\nVerifique que el proyecto api-catalogo esté en ejecución.", e);
        } catch (HttpTimeoutException e) {
            throw new ApiException(0, "La API tardó demasiado en responder. Intente de nuevo.", e);
        } catch (IOException e) {
            throw new ApiException(0, "Error de comunicación con la API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "La consulta a la API fue interrumpida.", e);
        }
        verificar(respuesta);
        return respuesta;
    }

    /** Si el código HTTP no es 2xx, lanza ApiException con el mensaje que envió la API. */
    private void verificar(HttpResponse<String> respuesta) throws ApiException {
        int estado = respuesta.statusCode();
        if (estado >= 200 && estado < 300) {
            return;
        }
        String mensaje = respuesta.body();
        try {
            JsonNode json = mapper.readTree(respuesta.body());
            if (json != null && json.hasNonNull("mensaje")) {
                mensaje = json.get("mensaje").asText();
            } else if (json != null && json.hasNonNull("message")) {
                mensaje = json.get("message").asText();
            }
        } catch (JsonProcessingException ignorada) {
            // El cuerpo no era JSON: se usa el texto tal cual
        }
        if (mensaje == null || mensaje.isBlank()) {
            mensaje = "respuesta sin detalle";
        }
        throw new ApiException(estado, "Error " + estado + ": " + mensaje);
    }

    private <T> T leer(String json, Class<T> tipo) throws ApiException {
        try {
            return mapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new ApiException(0, "La respuesta de la API no tiene el formato JSON esperado.", e);
        }
    }

    private String aJson(ProductoProveedor producto) throws ApiException {
        try {
            return mapper.writeValueAsString(producto);
        } catch (JsonProcessingException e) {
            throw new ApiException(0, "No se pudo convertir el producto a JSON.", e);
        }
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String urlConfigurada() {
        String entorno = System.getenv("PAE_API_URL");
        if (entorno != null && !entorno.isBlank()) {
            return entorno;
        }
        Path archivo = Path.of("config", "pae-local.properties");
        if (Files.exists(archivo)) {
            Properties local = new Properties();
            try (Reader entrada = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                local.load(entrada);
                String valor = local.getProperty("api.url");
                if (valor != null && !valor.isBlank()) {
                    return valor;
                }
            } catch (IOException ignorada) {
                // Si no se puede leer el archivo se usa la URL predeterminada
            }
        }
        return URL_PREDETERMINADA;
    }
}