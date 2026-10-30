package pe.tino.reclamos.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Un ticket: un turno de atencion de un operario, dentro de su horario.
 *
 * No es el reclamo. El parque de tickets se genera de antemano (proceso batch)
 * a partir del horario de cada operario; cuando un reclamo necesita atenderse,
 * cada evento suyo toma los tickets libres que necesita. Asi la fecha y el
 * responsable salen del parque, sin que nadie los elija.
 */
public class Ticket {

    private static final DateTimeFormatter CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final String codigo;
    private final String operario;
    private final LocalDateTime inicio;
    private final LocalDateTime fin;

    private boolean disponible = true;
    private String reclamo = "";
    private String evento = "";

    public Ticket(String operario, LocalDateTime inicio, LocalDateTime fin) {
        this.codigo = "TK-" + operario + "-" + inicio.format(CODIGO);
        this.operario = operario;
        this.inicio = inicio;
        this.fin = fin;
    }

    public String codigo()          { return codigo; }
    public String operario()        { return operario; }
    public LocalDateTime inicio()   { return inicio; }
    public LocalDateTime fin()      { return fin; }
    public boolean disponible()     { return disponible; }
    public String reclamo()         { return reclamo; }
    public String evento()          { return evento; }

    /** El reclamo toma el ticket para uno de sus eventos. */
    public void tomar(String reclamo, String evento) {
        if (!disponible) throw new IllegalStateException("El ticket " + codigo + " ya fue tomado");
        this.disponible = false;
        this.reclamo = reclamo;
        this.evento = evento;
    }

    public void liberar() {
        disponible = true;
        reclamo = "";
        evento = "";
    }

    @Override public String toString() { return codigo; }
}
