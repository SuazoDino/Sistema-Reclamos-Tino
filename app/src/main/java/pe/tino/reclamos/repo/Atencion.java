package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.MovimientoSolucion;
import pe.tino.reclamos.model.Modelo.Compra;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La atencion del reclamo evento por evento. Cuando el especialista termina
 * un evento, el sistema programa solo el siguiente; cuando termina el ultimo,
 * el reclamo queda resuelto y pasa a control de calidad.
 */
public final class Atencion {

    /** Lo que guarda el reclamo cuando el motor necesita la inspeccion para decidir. */
    public static final String PENDIENTE_INSPECCION = "Se define en la inspección";
    public static final String NO_APLICA = "No aplica";

    private Atencion() {}

    /* ---------------- motor de reglas ---------------- */

    /** Pasa por el motor el reclamo sobre un producto cuyo tipo tiene una regla asignada. */
    public static boolean usaMotor(Reclamo t) {
        return t.tipoObjeto() == TipoObjeto.PRODUCTO && MotorReglas.condicionInicial(t.tipoProblema()) != null;
    }

    /**
     * Corre la cadena del motor con los datos reales del reclamo. Reparable y
     * Stock solo se conocen tras la inspeccion: si {@code reparable} es null,
     * la cadena se detiene ahi y la solucion queda pendiente.
     */
    public static void sugerir(Reclamo t, Boolean reparable) {
        if (!usaMotor(t)) {
            if (t.solucionSugerida().isEmpty()) t.registrarSugerencia(NO_APLICA, List.of());
            return;
        }
        Compra c = compraDe(t);
        Map<String, String> v = new LinkedHashMap<>();
        v.put("Tiene_Comprobante", String.valueOf(c != null));
        v.put("Tiempo_Transcurrido", c == null ? "0"
                : String.valueOf(ChronoUnit.DAYS.between(c.fecha(), LocalDate.now())));
        v.put("Tiempo_Garantia", String.valueOf(Validacion.garantia(c)));
        v.put("Tiempo_Atencion", String.valueOf(
                Catalogos.horasDeAtencion(t.tipoCliente(), t.prioridad())));
        v.put("Tipo_Cliente", t.tipoCliente());
        v.put("Instancia", String.valueOf(t.instancia().ordinal() + 1));
        v.put("Precio", String.valueOf(c == null ? 0 : c.monto()));
        if (reparable != null) {
            v.put("Reparable", String.valueOf(reparable));
            v.put("Stock", String.valueOf(Prototipo.stockDe(c == null ? "" : c.codigo())));
        }

        MotorReglas.Resultado r = MotorReglas.evaluar(v, MotorReglas.condicionInicial(t.tipoProblema()));
        List<String> traza = r.traza().stream()
                .map(p -> p.codigo() + ": " + p.comparacion() + " -> " + p.salida()).toList();
        t.registrarSugerencia(r.valido() ? r.accion() : PENDIENTE_INSPECCION, traza);
    }

    public static Compra compraDe(Reclamo t) {
        return Datos.compra(t.documentoCliente(), t.tipoObjeto() == TipoObjeto.PRODUCTO, t.objeto());
    }

    /* ---------------- eventos ---------------- */

    /** Programa el primer evento del reclamo segun su tipo de problema. */
    public static EventoReclamo programarPrimero(Reclamo t, LocalDateTime desde) {
        String[] primero = Prototipo.eventosDe(t.tipoProblema()).get(0);
        return Asignacion.programar(t, primero[0], primero[1], null, desde);
    }

    /** El evento que sigue a uno, o null si era el ultimo de la secuencia. */
    public static String[] siguiente(Reclamo t, EventoReclamo e) {
        List<String[]> secuencia = Prototipo.eventosDe(t.tipoProblema());
        for (int i = 0; i < secuencia.size() - 1; i++) {
            if (secuencia.get(i)[0].equals(e.evento())) return secuencia.get(i + 1);
        }
        return null;
    }

    /** El especialista empieza el evento que tiene a su cargo. */
    public static void iniciar(Reclamo t, String usuario) {
        EventoReclamo e = t.eventoActual();
        if (e == null) return;
        e.iniciar();
        if (t.estado() == Estado.ASIGNADO) {
            t.cambiarEstado(Estado.EN_ATENCION, usuario, e.evento() + " iniciado");
        }
    }

    private static int correlativoMovimientos = 0;

    /**
     * El especialista ejecuta un paso del protocolo: queda un Movimiento de
     * Solucion con la hora estimada (la cita mas los tiempos de los pasos
     * anteriores) y la hora real.
     */
    public static MovimientoSolucion ejecutarPaso(Reclamo t, String usuario, String accion) {
        EventoReclamo e = t.eventoActual();
        if (e == null || !e.agendado()) throw new IllegalStateException("No hay evento por atender");
        if (!e.especialista().equals(usuario)) {
            throw new IllegalStateException("El evento no está asignado a " + usuario);
        }
        List<String[]> pasos = Prototipo.pasosDe(e.protocolo(), e.evento());
        int indice = -1;
        for (int i = 0; i < pasos.size(); i++) if (pasos.get(i)[0].equals(accion)) indice = i;
        if (indice < 0) throw new IllegalArgumentException("\"" + accion + "\" no es un paso de " + e.evento());
        if (e.accionesEjecutadas().contains(accion)) {
            throw new IllegalStateException("El paso \"" + accion + "\" ya se registró");
        }

        iniciar(t, usuario);
        int antes = 0;
        for (int i = 0; i < indice; i++) antes += Integer.parseInt(pasos.get(i)[1]);
        int dura = Integer.parseInt(pasos.get(indice)[1]);
        LocalDateTime estimado = e.inicio().plusMinutes(antes);

        LocalDateTime realInicio = t.movimientos().stream()
                .filter(m -> m.evento().equals(e.evento()) && e.accionesEjecutadas().contains(m.accion()))
                .map(MovimientoSolucion::fecFinPaso).max(LocalDateTime::compareTo)
                .orElse(e.iniciadoEn());
        String ticket = e.tickets().isEmpty() ? "" : e.tickets().get(
                Math.min(antes / ParqueTickets.PERIODO_MIN, e.tickets().size() - 1));

        MovimientoSolucion m = new MovimientoSolucion(++correlativoMovimientos, t.numero(), ticket,
                usuario, e.protocolo(), e.evento(), indice + 1, accion,
                estimado, estimado.plusMinutes(dura), realInicio, LocalDateTime.now());
        t.registrarMovimiento(m);
        e.marcarPaso(accion);
        t.anotar(usuario, "Paso", e.evento() + " - paso " + (indice + 1) + ": " + accion);
        Reclamos.notificar();
        return m;
    }

    /**
     * Termina el evento actual. Los pasos marcados en {@code acciones} que
     * todavia no se registraron se registran ahora. Las acciones criticas del
     * protocolo no pueden faltar. Si hay otro evento en la secuencia, se programa solo; si era el
     * ultimo, el reclamo queda resuelto con {@code solucion} y pasa a calidad.
     *
     * @return el mensaje para el especialista, o lanza si falta algo
     */
    public static String completar(Reclamo t, String usuario, List<String> acciones,
                                   Boolean reparable, String solucion) {
        EventoReclamo e = t.eventoActual();
        if (e == null || !e.agendado()) throw new IllegalStateException("No hay evento por atender");
        List<String> hechas = new java.util.ArrayList<>(e.accionesEjecutadas());
        acciones.stream().filter(a -> !hechas.contains(a)).forEach(hechas::add);
        for (String[] a : Prototipo.pasosDe(e.protocolo(), e.evento())) {
            if ("Crítico".equalsIgnoreCase(a[3]) && !hechas.contains(a[0])) {
                throw new IllegalArgumentException("Falta la acción crítica \"" + a[0] + "\".");
            }
        }
        String[] sig = siguiente(t, e);
        if (sig == null && (solucion == null || solucion.isBlank())) {
            throw new IllegalArgumentException("Indique la solución aplicada.");
        }

        iniciar(t, usuario);
        for (String[] a : Prototipo.pasosDe(e.protocolo(), e.evento())) {
            if (hechas.contains(a[0]) && !e.accionesEjecutadas().contains(a[0])) ejecutarPaso(t, e.especialista(), a[0]);
        }
        e.completar(e.accionesEjecutadas(), sig == null ? solucion : "Completado");
        t.anotar(usuario, "Evento", e.evento() + " completado: " + e.accionesEjecutadas().size() + " de "
                + Prototipo.pasosDe(e.protocolo(), e.evento()).size() + " acciones");
        if (reparable != null && PENDIENTE_INSPECCION.equals(t.solucionSugerida())) sugerir(t, reparable);

        if (sig != null) {
            EventoReclamo nuevo = Asignacion.programar(t, sig[0], sig[1], e.especialista(),
                    LocalDateTime.now());
            Reclamos.notificar();
            return e.evento() + " completado. " + nuevo.evento() + " programado para "
                    + nuevo.especialista() + ".";
        }
        t.registrarSolucion(solucion, usuario);
        t.cambiarEstado(Estado.RESUELTO, usuario, solucion);
        Calidad.revisar(t);
        Reclamos.notificar();
        return "Reclamo resuelto. Pasa a control de calidad (" + t.revision().etiqueta() + ").";
    }

    /** El especialista propone rechazar: no se notifica al cliente hasta que lo apruebe el supervisor. */
    public static void proponerRechazo(Reclamo t, String usuario, String motivo) {
        EventoReclamo e = t.eventoActual();
        iniciar(t, usuario);
        if (e != null) e.completar(e.accionesEjecutadas(), "Rechazo propuesto: " + motivo);
        t.proponerRechazo(motivo);
        t.cambiarEstado(Estado.EN_REVISION, usuario, "Rechazo propuesto: " + motivo);
        t.registrarRevision(Revision.RECHAZO_PROPUESTO, List.of(motivo));
        abrirSeguimiento(t);
        Reclamos.notificar();
    }

    /** El evento de Seguimiento acompana al reclamo mientras esta en revision. */
    static void abrirSeguimiento(Reclamo t) {
        t.programar(new EventoReclamo(Prototipo.EVENTO_SEGUIMIENTO, "Control de calidad",
                Prototipo.AREA_CALIDAD, "Supervisor y cliente", null, null, true), "");
    }

    /**
     * Vuelve a atencion (lo devuelve el supervisor o el cliente no quedo
     * conforme): se repite el ultimo evento, con nueva cita.
     */
    public static void devolver(Reclamo t, String usuario, String motivo) {
        EventoReclamo ultimo = t.ultimoEventoAgendado();
        liberarPendientes(t);
        t.devolver(usuario, motivo);
        String evento = ultimo == null ? Prototipo.eventosDe(t.tipoProblema()).get(0)[0] : ultimo.evento();
        String protocolo = ultimo == null ? Prototipo.protocoloDe(t.tipoProblema()) : ultimo.protocolo();
        Asignacion.programar(t, evento, protocolo, ultimo == null ? null : ultimo.especialista(),
                LocalDateTime.now());
        Reclamos.notificar();
    }

    /**
     * Las citas que se anulan devuelven sus tickets al parque. Sin esto, la
     * agenda del especialista se llenaba de citas fantasma.
     */
    private static void liberarPendientes(Reclamo t) {
        ParqueTickets.liberar(ticketsPendientes(t));
    }

    private static List<String> ticketsPendientes(Reclamo t) {
        return t.eventos().stream().filter(e -> e.pendiente() && e.agendado())
                .flatMap(e -> e.tickets().stream()).toList();
    }

    /** El cliente impugna: sube de instancia y el sistema reasigna solo, con nuevo plazo. */
    public static boolean impugnar(Reclamo t, String motivo) {
        List<String> tickets = ticketsPendientes(t);
        if (!t.impugnar(motivo, "Cliente")) return false;
        ParqueTickets.liberar(tickets);
        t.reprogramar(Catalogos.limiteAtencion(LocalDateTime.now(), t.tipoCliente(), t.prioridad()), "Sistema");
        programarPrimero(t, LocalDateTime.now());
        Reclamos.notificar();
        return true;
    }
}
