package pe.tino.reclamos.model;

import pe.tino.reclamos.repo.Dominio.EstadoEvento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un evento del reclamo: una etapa real de su atencion (Inspeccion, Solucion,
 * Seguimiento) con su responsable y su fecha.
 *
 * Es lo que se ejecuta, se asigna y se agenda. El reclamo es el caso completo
 * y va pasando por sus eventos; el protocolo dice como se atiende cada evento
 * segun el tipo de problema. Asi un mismo evento (Inspeccion) usa protocolos
 * distintos para Funcionamiento y para Entrega.
 */
public class EventoReclamo {

    private final String evento;
    private final String protocolo;
    private final String area;
    private final String especialista;
    /** La cita: fija desde que se programa, no se recalcula. */
    private final LocalDateTime inicio;
    private final LocalDateTime fin;
    private final boolean dentroDelPlazo;

    private EstadoEvento estado = EstadoEvento.PROGRAMADO;
    private final List<String> accionesEjecutadas = new ArrayList<>();
    private LocalDateTime iniciadoEn;
    private String resultado = "";
    /** Los tickets (turnos) del parque que tomo este evento. */
    private List<String> tickets = List.of();

    public EventoReclamo(String evento, String protocolo, String area, String especialista,
                         LocalDateTime inicio, LocalDateTime fin, boolean dentroDelPlazo) {
        this.evento = evento;
        this.protocolo = protocolo;
        this.area = area;
        this.especialista = especialista;
        this.inicio = inicio;
        this.fin = fin;
        this.dentroDelPlazo = dentroDelPlazo;
    }

    public String evento()                   { return evento; }
    public String protocolo()                { return protocolo; }
    public String area()                     { return area; }
    public String especialista()             { return especialista; }
    public LocalDateTime inicio()            { return inicio; }
    public LocalDateTime fin()               { return fin; }
    public boolean dentroDelPlazo()          { return dentroDelPlazo; }
    public EstadoEvento estado()             { return estado; }
    public List<String> accionesEjecutadas() { return List.copyOf(accionesEjecutadas); }
    public String resultado()                { return resultado; }
    public boolean pendiente()               { return estado.pendiente(); }
    public List<String> tickets()            { return tickets; }

    public void asignarTickets(List<String> codigos) { this.tickets = List.copyOf(codigos); }

    /** Seguimiento no se agenda: lo atienden el control de calidad y el cliente. */
    public boolean agendado()                { return inicio != null; }

    public LocalDateTime iniciadoEn()        { return iniciadoEn; }

    /** Un paso del protocolo quedo hecho (su movimiento ya se registro). */
    public void marcarPaso(String accion) {
        if (!accionesEjecutadas.contains(accion)) accionesEjecutadas.add(accion);
    }

    public void iniciar() {
        if (iniciadoEn == null) iniciadoEn = LocalDateTime.now();
        if (estado == EstadoEvento.PROGRAMADO) estado = EstadoEvento.EN_CURSO;
    }

    public void completar(List<String> acciones, String resultado) {
        accionesEjecutadas.clear();
        accionesEjecutadas.addAll(acciones);
        this.resultado = resultado;
        estado = EstadoEvento.COMPLETADO;
    }

    public void anular(String motivo) {
        resultado = motivo;
        estado = EstadoEvento.ANULADO;
    }

    @Override public String toString() { return evento + " (" + protocolo + ")"; }
}
