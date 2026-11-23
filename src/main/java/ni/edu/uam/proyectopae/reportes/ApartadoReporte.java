package ni.edu.uam.proyectopae.reportes;

import java.util.List;

/** Catálogo de los apartados y campos que podrá ofrecer el checklist de reportes. */
public enum ApartadoReporte {
    RESUMEN("Resumen general", "Inventario actual y movimientos del período seleccionado.",
            c("indicador", "Indicador", 250), c("valor", "Valor", 150)),
    PRODUCTOS("Productos e inventario", "Catálogo y existencias actuales; no se filtran por fecha.",
            c("id", "ID producto", 55), c("nombre", "Producto", 160),
            c("idCategoria", "ID categoría", 55), c("categoria", "Categoría", 85),
            c("idMarca", "ID marca", 55), c("marca", "Marca", 100),
            c("compra", "Precio compra", 95), c("venta", "Precio venta", 95),
            c("ganancia", "Ganancia por unidad", 95), c("margen", "Margen sobre venta", 85),
            c("stock", "Stock vigente", 75), c("minimo", "Stock mínimo", 75),
            c("estado", "Estado de stock", 110)),
    LOTES("Lotes y caducidad", "Las fechas filtran el ingreso del lote; el stock y la caducidad son actuales.",
            c("id", "ID lote", 55), c("idProducto", "ID producto", 55), c("producto", "Producto", 170),
            c("codigo", "Código de lote", 120), c("ingreso", "Fecha ingreso", 95),
            c("caducidad", "Caducidad", 95), c("cantidad", "Unidades disponibles", 85),
            c("dias", "Días para vencer", 80), c("estado", "Estado de caducidad", 110)),
    CLIENTES("Clientes", "Directorio actual de clientes; no se filtra por fecha.",
            c("id", "ID cliente", 55), c("nombres", "Nombres", 110), c("apellidos", "Apellidos", 110),
            c("sexo", "Sexo", 75), c("telefono", "Teléfono", 100), c("correo", "Correo electrónico", 180),
            c("alergias", "Alergias / sensibilidad", 200)),
    SERVICIOS("Servicios", "Catálogo actual y productos asociados; admite el filtro de servicio.",
            c("id", "ID servicio", 60), c("nombre", "Servicio", 190),
            c("duracion", "Duración (min)", 90), c("tarifa", "Tarifa actual", 110),
            c("productos", "Productos asociados (ID y nombre)", 270)),
    CITAS("Agenda de citas", "Las fechas filtran la fecha y hora de la cita; admite el filtro de servicio.",
            c("id", "N.º de cita", 60), c("idCliente", "ID cliente", 60), c("cliente", "Cliente", 150),
            c("idServicio", "ID servicio", 60), c("servicio", "Servicio", 170),
            c("fecha", "Fecha y hora", 140), c("estado", "Estado", 100),
            c("tarifa", "Tarifa actual del servicio", 110)),
    VENTAS("Ventas", "Las fechas filtran la fecha de venta; importes guardados al registrar la operación.",
            c("id", "N.º de venta", 60), c("idCliente", "ID cliente", 60), c("cliente", "Cliente", 180),
            c("fecha", "Fecha y hora", 150), c("total", "Total venta", 115),
            c("costo", "Costo total", 115), c("ganancia", "Ganancia", 115)),
    DETALLE_VENTAS("Detalle de ventas", "Una fila por producto vendido; conserva precios y costos históricos.",
            c("id", "ID detalle", 55), c("idVenta", "N.º de venta", 60),
            c("fecha", "Fecha de venta", 135), c("cliente", "Cliente", 140),
            c("idProducto", "ID producto", 60), c("producto", "Producto", 165),
            c("cantidad", "Cantidad", 65), c("precio", "Precio al vender", 100),
            c("costo", "Costo al vender", 100), c("subtotal", "Subtotal", 105),
            c("ganancia", "Ganancia", 105)),
    CONSUMOS("Consumos de insumos", "Las fechas corresponden a la cita asociada; admite el filtro de servicio.",
            c("id", "ID consumo", 60), c("idCita", "N.º de cita", 60), c("fecha", "Fecha de cita", 140),
            c("cliente", "Cliente", 140), c("servicio", "Servicio", 160),
            c("idLote", "ID lote", 55), c("lote", "Código de lote", 115),
            c("idProducto", "ID producto", 60), c("producto", "Producto", 160),
            c("cantidad", "Cantidad descontada", 90)),
    CATEGORIAS("Categorías", "Catálogo actual de categorías; no se filtra por fecha.",
            c("id", "ID categoría", 65), c("nombre", "Categoría", 160), c("descripcion", "Descripción", 300)),
    MARCAS("Marcas", "Catálogo actual de marcas; no se filtra por fecha.",
            c("id", "ID marca", 65), c("nombre", "Marca", 250)),
    USUARIOS("Usuarios y roles", "Listado de identificadores, nombres de usuario y roles.",
            c("id", "ID usuario", 65), c("usuario", "Usuario", 180), c("rol", "Rol", 180));

    private final String titulo;
    private final String alcance;
    private final List<CampoReporte> campos;

    ApartadoReporte(String titulo, String alcance, CampoReporte... campos) {
        this.titulo = titulo;
        this.alcance = alcance;
        this.campos = List.of(campos);
    }

    public String titulo() { return titulo; }
    public String alcance() { return alcance; }
    public List<CampoReporte> campos() { return campos; }

    private static CampoReporte c(String clave, String titulo, int peso) {
        return new CampoReporte(clave, titulo, peso);
    }
}
