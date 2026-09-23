package pe.tino.reclamos.model;

import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * El ticket: la unidad de trabajo del sistema de reclamos.
 *
 * Un ticket NO es la orden de compra ni el comprobante. El comprobante es un
 * documento externo que el cliente trae y que solo sirve de sustento para
 * probar la garantia; el ticket es el documento interno que el sistema crea,
 * numera y controla desde que el reclamo entra hasta que se cierra.
 *
 * De ahi la diferencia practica: un comprobante puede originar varios tickets
 * (un reclamo por cada producto o servicio), y un ticket vive aunque el
 * comprobante no exista, porque tambien se reclama por atencion o por
 * servicios donde no hubo boleta.
 *
 * Lo indispensable de un ticket es lo que permite rastrearlo y medirlo:
 * numero, apertura, objeto reclamado, prioridad, estado, responsable y plazo.
 */
public class Ticket {

    /** Un movimiento de la bitacora: quien hizo que y cuando. */
    public record Evento(LocalDateTime momento, String usuario, String accion, String detalle) {}

    /* --- identificacion --- */
    private final String numero;
    private final LocalDateTime apertura;
    private final Canal canal;
    private final String local;

    /* --- quien reclama --- */
    private final String documentoCliente;
    private final String nombreCliente;
    private final String tipoCliente;

    /* --- sobre que reclama --- */
    private final TipoObjeto tipoObjeto;
    private final String objeto;
    private final String tipoProblema;
    private final String problema;

    /* --- sustento: el comprobante, que no es el ticket --- */
    private final String comprobante;
    private final String fechaComprobante;

    /* --- como se atiende --- */
    private Instancia instancia;
    private Prioridad prioridad;
    private Estado estado;
    private String area;
    private String especialista;
    private String protocolo;

    /* --- plazos --- */
    private LocalDateTime limiteAtencion;
    private LocalDateTime limiteImpugnacion;

    private final List<Evento> bitacora = new ArrayList<>();

    public Ticket(String numero, LocalDateTime apertura, Canal canal, String local,
                  String documentoCliente, String nombreCliente, String tipoCliente,
                  TipoObjeto tipoObjeto, String objeto, String tipoProblema, String problema,
                  String comprobante, String fechaComprobante,
                  Prioridad prioridad, LocalDateTime limiteAtencion) {
        this.numero = numero;
        this.apertura = apertura;
        this.canal = canal;
        this.local = local;
        this.documentoCliente = documentoCliente;
        this.nombreCliente = nombreCliente;
        this.tipoCliente = tipoCliente;
        this.tipoObjeto = tipoObjeto;
        this.objeto = objeto;
        this.tipoProblema = tipoProblema;
        this.problema = problema;
        this.comprobante = comprobante;
        this.fechaComprobante = fechaComprobante;
        this.instancia = Instancia.PRIMERA;
        this.prioridad = prioridad;
        this.estado = Estado.REGISTRADO;
        this.area = "Sin asignar";
        this.especialista = "Sin asignar";
        this.protocolo = "Sin asignar";
        this.limiteAtencion = limiteAtencion;
        anotar("Sistema", "Registro", "Ticket abierto por canal " + canal.etiqueta());
    }

    /* ---------------- consultas ---------------- */

    public String numero()            { return numero; }
    public LocalDateTime apertura()   { return apertura; }
    public Canal canal()              { return canal; }
    public String local()             { return local; }
    public String documentoCliente()  { return documentoCliente; }
    public String nombreCliente()     { return nombreCliente; }
    public String tipoCliente()       { return tipoCliente; }
    public TipoObjeto tipoObjeto()    { return tipoObjeto; }
    public String objeto()            { return objeto; }
    public String tipoProblema()      { return tipoProblema; }
    public String problema()          { return problema; }
    public String comprobante()       { return comprobante; }
    public String fechaComprobante()  { return fechaComprobante; }
    public Instancia instancia()      { return instancia; }
    public Prioridad prioridad()      { return prioridad; }
    public Estado estado()            { return estado; }
    public String area()              { return area; }
    public String especialista()      { return especialista; }
    public String protocolo()         { return protocolo; }
    public LocalDateTime limiteAtencion()    { return limiteAtencion; }
    public LocalDateTime limiteImpugnacion() { return limiteImpugnacion; }
    public List<Evento> bitacora()    { return List.copyOf(bitacora); }

    /** Un ticket esta vencido si el plazo de atencion paso y sigue abierto. */
    public boolean vencido() {
        return estado.abierto() && limiteAtencion != null
                && LocalDateTime.now().isAfter(limiteAtencion);
    }

    /* ---------------- movimientos ---------------- */

    public void asignar(String area, String especialista, String protocolo, String usuario) {
        this.area = area;
        this.especialista = especialista;
        this.protocolo = protocolo;
        anotar(usuario, "Asignación", area + " / " + especialista + " / protocolo " + protocolo);
        if (estado == Estado.REGISTRADO || estado == Estado.IMPUGNADO) cambiarEstado(Estado.ASIGNADO, usuario, "");
    }

    /**
     * Cambia el estado respetando la maquina de estados; devuelve false si la
     * transicion no esta permitida.
     */
    public boolean cambiarEstado(Estado destino, String usuario, String detalle) {
        if (!estado.puedePasarA(destino)) return false;
        Estado anterior = estado;
        estado = destino;

        if (destino == Estado.RECHAZADO && instancia.admiteImpugnacion()) {
            limiteImpugnacion = LocalDateTime.now().plusDays(instancia.diasImpugnacion());
        }
        anotar(usuario, "Cambio de estado", anterior.etiqueta() + " -> " + destino.etiqueta()
                + (detalle.isBlank() ? "" : ": " + detalle));
        return true;
    }

    /** Sube el ticket a la siguiente instancia y reabre su atencion. */
    public boolean impugnar(String motivo, String usuario) {
        if (estado != Estado.RECHAZADO) return false;
        if (!instancia.admiteImpugnacion()) return false;
        if (limiteImpugnacion != null && LocalDateTime.now().isAfter(limiteImpugnacion)) return false;

        cambiarEstado(Estado.IMPUGNADO, usuario, motivo);
        instancia = instancia.siguiente();
        area = instancia.resuelve();
        especialista = "Sin asignar";
        limiteImpugnacion = null;
        anotar(usuario, "Impugnación", "Pasa a " + instancia.etiqueta()
                + " instancia, resuelve " + instancia.resuelve());
        return true;
    }

    public void reprogramar(LocalDateTime nuevoLimite, String usuario) {
        this.limiteAtencion = nuevoLimite;
        anotar(usuario, "Reprogramación", "Nuevo límite de atención");
    }

    public void anotar(String usuario, String accion, String detalle) {
        bitacora.add(new Evento(LocalDateTime.now(), usuario, accion, detalle));
    }

    @Override public String toString() { return numero + " - " + objeto + " (" + estado + ")"; }
}
