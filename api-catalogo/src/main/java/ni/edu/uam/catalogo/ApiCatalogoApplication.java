package ni.edu.uam.catalogo;

import ni.edu.uam.catalogo.model.ProductoProveedor;
import ni.edu.uam.catalogo.repository.ProductoProveedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;

@SpringBootApplication
public class ApiCatalogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiCatalogoApplication.class, args);
	}

	/** Catálogo SIMULADO del proveedor: se carga cada vez que la API inicia. */
	@Bean
	CommandLineRunner datosIniciales(ProductoProveedorRepository repo) {
		return args -> {
			if (repo.count() == 0) {
				repo.saveAll(List.of(
						producto("PRV-001", "Base líquida Fit Me 128", "Maybelline", "Bases", "520.00", 24,
								"Base de cobertura media con acabado natural."),
						producto("PRV-002", "Base Infallible 24H 120", "L'Oréal", "Bases", "690.00", 15,
								"Base de larga duración resistente al sudor."),
						producto("PRV-003", "Corrector Instant Age Rewind", "Maybelline", "Correctores", "430.00", 30,
								"Corrector con aplicador de esponja."),
						producto("PRV-004", "Labial Super Stay Matte Ink", "Maybelline", "Labiales", "410.00", 40,
								"Labial líquido mate de hasta 16 horas."),
						producto("PRV-005", "Labial Soft Matte Lip Cream", "NYX", "Labiales", "300.00", 35,
								"Crema labial mate de textura ligera."),
						producto("PRV-006", "Paleta de sombras Ultimate", "NYX", "Sombras", "780.00", 12,
								"Paleta de 16 tonos mates y metálicos."),
						producto("PRV-007", "Máscara Lash Sensational", "Maybelline", "Pestañas", "380.00", 28,
								"Máscara de pestañas con efecto abanico."),
						producto("PRV-008", "Pestañas postizas Jumbo Lash", "Ardell", "Pestañas", "230.00", 50,
								"Pestañas reutilizables de efecto dramático."),
						producto("PRV-009", "Set de brochas 12 piezas", "Real Techniques", "Brochas", "950.00", 8,
								"Set de brochas sintéticas para rostro y ojos."),
						producto("PRV-010", "Esponja de maquillaje", "Real Techniques", "Brochas", "170.00", 60,
								"Esponja para base líquida y corrector."),
						producto("PRV-011", "Agua micelar desmaquillante 400 ml", "Garnier", "Cuidado de la piel", "380.00", 22,
								"Limpia y desmaquilla sin enjuague."),
						producto("PRV-012", "Protector solar Ultra Sheer FPS 55", "Neutrogena", "Cuidado de la piel", "620.00", 18,
								"Protector solar de toque seco."),
						producto("PRV-013", "Spray fijador de maquillaje", "NYX", "Fijadores", "450.00", 0,
								"Fijador mate. Producto agotado para probar existencias en cero.")
				));
			}
		};
	}

	private static ProductoProveedor producto(String codigo, String nombre, String marca, String categoria,
											  String precio, int existencias, String descripcion) {
		return new ProductoProveedor(codigo, nombre, marca, categoria, new BigDecimal(precio), existencias, descripcion);
	}
}