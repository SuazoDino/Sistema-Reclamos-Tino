package pe.tino.reclamos.model;

import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * El reclamo: la unidad de trabajo del sistema de reclamos.
 *
 * Un reclamo NO es la orden de compra ni el comprobante. El comprobante es un
 * documento externo que el cliente trae y que solo sirve de sustento para
 * probar la garantia; el reclamo es el documento interno que el sistema crea,
 * numera y controla desde que el reclamo entra hasta que se cierra.
 *
 * De ahi la diferencia practica: un comprobante puede originar varios reclamos
 * (un reclamo por cada producto o servicio), y un reclamo vive aunque el
 * comprobante no exista, porque tambien se reclama por atencion o por
 * servicios donde no hubo boleta.
 *
 * Lo indispensable de un reclamo es lo que permite rastrearlo y medirlo:
 * numero, apertura, objeto reclamado, prioridad, estado, responsable y plazo.
 *
 * El reclamo es el caso; lo que se ejecuta son sus eventos (Inspeccion,
 * Solucion, Seguimiento), cada uno con su protocolo, responsable y cita.
 */
public class Reclamo {

    /** Un movimiento de la bitacora: quien hizo que y cuando. */
    public record Registro(LocalDateTime momento, String usuario, String accion, String detalle) {}

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

    /* --- lo que cuenta y pide el cliente (hoja del Libro de Reclamaciones) --- */
    private String naturaleza = "Reclamo";
    private String detalle = "";
    private String pedido = "";

    /* --- sustento: el comprobante, que no es el reclamo --- */
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

    /* --- eventos del reclamo --- */
    private final List<EventoReclamo> eventos = new ArrayList<>();
    private final List<MovimientoSolucion> movimientos = new ArrayList<>();

    /* --- validacion del reclamo y motor de reglas --- */
    private boolean fueraDeGarantia;
    private double monto;
    private String solucionSugerida = "";
    private List<String> trazaMotor = List.of();
    private String solucionAplicada = "";
    private LocalDateTime resolucion;

    /* --- control de calidad --- */
    private Revision revision;
    private final List<String> observaciones = new ArrayList<>();
    private String motivoRechazo = "";
    private Boolean conforme;
    private int nota;
    private int devoluciones;

    private final List<Registro> bitacora = new ArrayList<>();

    public Reclamo(String numero, LocalDateTime apertura, Canal canal, String local,
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
        anotar("Sistema", "Registro", "Reclamo abierto por canal " + canal.etiqueta());
    }

    /** Lo que el cliente conto y pidio al registrar. */
    public void registrarSolicitud(String naturaleza, String detalle, String pedido) {
        this.naturaleza = naturaleza;
        this.detalle = detalle;
        this.pedido = pedido;
        anotar("Cliente", "Pedido", naturaleza + ": " + pedido);
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
    /** Reclamo (disconformidad con el producto o servicio) o Queja (malestar con la atencion). */
    public String naturaleza()        { return naturaleza; }
    public String detalle()           { return detalle; }
    public String pedido()            { return pedido; }
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
    public List<Registro> bitacora()    { return List.copyOf(bitacora); }
    public List<EventoReclamo> eventos() { return List.copyOf(eventos); }
    public List<MovimientoSolucion> movimientos() { return List.copyOf(movimientos); }

    public void registrarMovimiento(MovimientoSolucion m) { movimientos.add(m); }

    /** Pasos ejecutados, como en la Maestra Reclamos (PasosCompletados). */
    public int pasosCompletados() { return movimientos.size(); }

    /**
     * Pasos que tiene el reclamo (TotalPasos): los de sus eventos agendados mas
     * los de los eventos del protocolo que todavia no se programan.
     */
    public int totalPasos() {
        int total = 0;
        for (EventoReclamo e : eventos) {
            if (!e.agendado() || e.estado() == EstadoEvento.ANULADO) continue;
            total += pe.tino.reclamos.repo.Prototipo.pasosDe(e.protocolo(), e.evento()).size();
        }
        List<String[]> secuencia = pe.tino.reclamos.repo.Prototipo.eventosDe(tipoProblema);
        long distintos = eventos.stream().filter(e -> e.agendado() && e.estado() != EstadoEvento.ANULADO)
                .map(EventoReclamo::evento).distinct().count();
        for (int i = (int) distintos; i < secuencia.size(); i++) {
            total += pe.tino.reclamos.repo.Prototipo.pasosDe(secuencia.get(i)[1], secuencia.get(i)[0]).size();
        }
        return total;
    }
    public boolean fueraDeGarantia()    { return fueraDeGarantia; }
    public double monto()               { return monto; }
    public String solucionSugerida()    { return solucionSugerida; }
    public List<String> trazaMotor()    { return trazaMotor; }
    public String solucionAplicada()    { return solucionAplicada; }
    public LocalDateTime resolucion()   { return resolucion; }
    public Revision revision()          { return revision; }
    public List<String> observaciones() { return List.copyOf(observaciones); }
    public String motivoRechazo()       { return motivoRechazo; }
    public Boolean conforme()           { return conforme; }
    public int nota()                   { return nota; }
    public int devoluciones()           { return devoluciones; }

    /** El evento que se esta atendiendo o el proximo por atender. */
    public EventoReclamo eventoActual() {
        return eventos.stream().filter(EventoReclamo::pendiente).findFirst().orElse(null);
    }

    /** El ultimo evento agendado que se hizo o se va a hacer. */
    public EventoReclamo ultimoEventoAgendado() {
        for (int i = eventos.size() - 1; i >= 0; i--) {
            EventoReclamo e = eventos.get(i);
            if (e.agendado() && e.estado() != EstadoEvento.ANULADO) return e;
        }
        return null;
    }

    /**
     * La cita que se le dio al cliente: el inicio del evento pendiente o, si
     * ya no queda ninguno, el del ultimo que se atendio.
     */
    public LocalDateTime fechaProgramada() {
        EventoReclamo actual = eventoActual();
        if (actual != null && actual.agendado()) return actual.inicio();
        EventoReclamo ultimo = ultimoEventoAgendado();
        return ultimo == null ? null : ultimo.inicio();
    }

    /** Algun evento se programo despues del limite porque no habia agenda. */
    public boolean sinCapacidad() {
        return eventos.stream().anyMatch(e -> e.agendado() && !e.dentroDelPlazo()
                && e.estado() != EstadoEvento.ANULADO);
    }

    /** Un reclamo esta vencido si el plazo de atencion paso y sigue abierto. */
    public boolean vencido() {
        return estado.abierto() && limiteAtencion != null
                && LocalDateTime.now().isAfter(limiteAtencion);
    }

    /* ---------------- movimientos ---------------- */

    /**
     * Agrega un evento ya programado: el reclamo pasa a estar a cargo del
     * responsable de ese evento.
     */
    public void programar(EventoReclamo e, String motivo) {
        eventos.add(e);
        if (!e.agendado()) {
            anotar("Sistema", "Evento", e.evento() + " a cargo de " + e.area());
            return;
        }
        asignar(e.area(), e.especialista(), e.protocolo(), "Sistema");
        anotar("Sistema", "Asignación automática", e.evento() + " asignado a "
                + e.especialista() + " (" + motivo + ")");
        anotar("Sistema", "Cita", e.evento() + " programado el "
                + e.inicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + (e.dentroDelPlazo() ? "" : " - Sin capacidad dentro del plazo"));
    }

    public void registrarValidacion(boolean fueraDeGarantia, double monto) {
        this.fueraDeGarantia = fueraDeGarantia;
        this.monto = monto;
        if (fueraDeGarantia) anotar("Sistema", "Garantía", "Fuera de garantía");
    }

    public void registrarSugerencia(String solucion, List<String> traza) {
        this.solucionSugerida = solucion;
        this.trazaMotor = List.copyOf(traza);
        anotar("Motor de reglas", "Solución sugerida", solucion);
    }

    public void registrarSolucion(String solucion, String usuario) {
        this.solucionAplicada = solucion;
        this.resolucion = LocalDateTime.now();
        anotar(usuario, "Solución aplicada", solucion);
    }

    /** Lo que deja la revision automatica al pasar el reclamo a En revision. */
    public void registrarRevision(Revision revision, List<String> observaciones) {
        this.revision = revision;
        this.observaciones.clear();
        this.observaciones.addAll(observaciones);
        anotar("Control de calidad", "Revisión", revision.etiqueta()
                + (observaciones.isEmpty() ? "" : ": " + String.join("; ", observaciones)));
    }

    public void proponerRechazo(String motivo) { this.motivoRechazo = motivo; }

    public void aprobarRevision(String usuario) {
        this.revision = Revision.APROBADO;
        anotar(usuario, "Revisión", "Aprobado por el supervisor");
    }

    public void registrarConformidad(boolean conforme, int nota) {
        this.conforme = conforme;
        this.nota = nota;
        anotar("Cliente", "Conformidad", (conforme ? "Conforme" : "No conforme") + ", nota " + nota);
    }

    /** El reclamo vuelve a atencion: se borra la revision anterior. */
    public void devolver(String usuario, String motivo) {
        devoluciones++;
        revision = null;
        observaciones.clear();
        motivoRechazo = "";
        conforme = null;
        solucionAplicada = "";
        resolucion = null;
        eventos.stream().filter(e -> e.pendiente() && !e.agendado())
                .forEach(e -> e.anular("Devuelto a atención"));
        cambiarEstado(Estado.EN_ATENCION, usuario, motivo);
    }

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

    /** Sube el reclamo a la siguiente instancia y reabre su atencion. */
    public boolean impugnar(String motivo, String usuario) {
        if (estado != Estado.RECHAZADO) return false;
        if (!instancia.admiteImpugnacion()) return false;
        if (limiteImpugnacion != null && LocalDateTime.now().isAfter(limiteImpugnacion)) return false;

        cambiarEstado(Estado.IMPUGNADO, usuario, motivo);
        eventos.stream().filter(EventoReclamo::pendiente).forEach(e -> e.anular("Impugnación"));
        revision = null;
        motivoRechazo = "";
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
        bitacora.add(new Registro(LocalDateTime.now(), usuario, accion, detalle));
    }

    @Override public String toString() { return numero + " - " + objeto + " (" + estado + ")"; }
}
