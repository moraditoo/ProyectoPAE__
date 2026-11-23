package ni.edu.uam.proyectopae.reportes;

import ni.edu.uam.proyectopae.models.Cita;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** Identificadores compartidos por la tabla, los datos y la plantilla JRXML. */
public enum ColumnaCita {
    ID("id", 70, cita -> cita.getIdCita()),
    FECHA("fecha", 150, cita -> cita.getFechaHora() == null ? "" :
            cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.US))),
    CLIENTE("cliente", 190, Cita::getNombreCliente),
    SERVICIO("servicio", 210, Cita::getNombreServicio),
    ESTADO("estado", 120, Cita::getEstado);

    private final String campo;
    private final int peso;
    private final Function<Cita, Object> valor;

    ColumnaCita(String campo, int peso, Function<Cita, Object> valor) {
        this.campo = campo;
        this.peso = peso;
        this.valor = valor;
    }

    public String campo() { return campo; }
    public int peso() { return peso; }
    public String valor(Cita cita) {
        Object dato = valor.apply(cita);
        return dato == null ? "" : dato.toString();
    }
}
