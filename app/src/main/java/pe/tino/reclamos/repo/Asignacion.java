package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Prototipo.Empleado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asignacion automatica de los eventos del reclamo. Ningun empleado elige
 * que atender: el tipo de problema decide el area, el tipo de operario que
 * piden los pasos del evento filtra quien puede hacerlo, la menor carga
 * decide el especialista y el parque de tickets decide la fecha.
 */
public final class Asignacion {

    /** El ultimo especialista que recibio un evento en cada area: desempata por turno. */
    private static final Map<String, Integer> TURNO = new HashMap<>();

    private Asignacion() {}

    /** Carga de un especialista: los eventos agendados que todavia tiene pendientes. */
    public static long carga(String especialista) {
        return Reclamos.todos().stream()
                .flatMap(t -> t.eventos().stream())
                .filter(e -> e.pendiente() && e.agendado() && e.especialista().equals(especialista))
                .count();
    }

    /**
     * Programa un evento del reclamo: elige especialista, busca su primer hueco
     * y reserva la cita.
     *
     * Primero prueba con {@code preferido} (quien ya viene atendiendo el
     * reclamo) si tiene hueco a tiempo; si no, con el de menor carga del area
     * (en empate, por turno); y si el elegido no llega antes del limite, con
     * el siguiente. Si nadie llega, toma el primer hueco que haya y el reclamo
     * queda marcado sin capacidad dentro del plazo.
     */
    public static EventoReclamo programar(Reclamo t, String evento, String protocolo,
                                          String preferido, LocalDateTime desde) {
        String area = Catalogos.areaDe(t.tipoProblema(), t.instancia());
        String requerido = Prototipo.operarioRequerido(protocolo, evento);
        List<Empleado> candidatos = ordenar(area).stream()
                .filter(e -> Prototipo.nivelDe(e.tipo()) >= Prototipo.nivelDe(requerido)).toList();
        boolean sinPerfil = candidatos.isEmpty();
        // si nadie del area tiene el perfil, se asigna igual y queda anotado
        if (sinPerfil) candidatos = ordenar(area);
        if (candidatos.isEmpty()) {
            throw new IllegalStateException("El área " + area + " no tiene especialistas habilitados");
        }
        int minutos = Math.max(Prototipo.duracionDe(protocolo, evento), ParqueTickets.PERIODO_MIN);

        List<Empleado> orden = new ArrayList<>(candidatos);
        Empleado continuidad = candidatos.stream()
                .filter(e -> e.id().equals(preferido)).findFirst().orElse(null);
        if (continuidad != null) {
            orden.remove(continuidad);
            orden.add(0, continuidad);
        }

        Empleado elegido = null;
        LocalDateTime inicio = null;
        Empleado masTemprano = null;
        LocalDateTime huecoMasTemprano = null;

        for (Empleado e : orden) {
            LocalDateTime hueco = ParqueTickets.primerHueco(e.id(), desde, minutos);
            if (!hueco.plusMinutes(minutos).isAfter(t.limiteAtencion())) {
                elegido = e;
                inicio = hueco;
                break;
            }
            if (huecoMasTemprano == null || hueco.isBefore(huecoMasTemprano)) {
                masTemprano = e;
                huecoMasTemprano = hueco;
            }
        }

        boolean aTiempo = elegido != null;
        if (!aTiempo) {
            elegido = masTemprano;
            inicio = huecoMasTemprano;
        }

        String motivo;
        if (!aTiempo) motivo = "sin capacidad dentro del plazo: primer hueco disponible";
        else if (elegido == continuidad) motivo = "continúa el reclamo que ya atiende";
        else if (elegido != candidatos.get(0)) motivo = "el de menor carga no tenía hueco a tiempo";
        else motivo = "menor carga: " + carga(elegido.id()) + (carga(elegido.id()) == 1 ? " evento pendiente" : " eventos pendientes");
        if (sinPerfil) motivo += "; nadie del área es " + requerido;

        TURNO.put(area, candidatosDe(area).indexOf(elegido));
        List<String> tickets = ParqueTickets.tomar(elegido.id(), inicio, minutos, t.numero(), evento);

        EventoReclamo e = new EventoReclamo(evento, protocolo, area, elegido.id(),
                inicio, inicio.plusMinutes(minutos), aTiempo);
        e.asignarTickets(tickets);
        t.programar(e, motivo);
        return e;
    }

    /**
     * Habilita o deshabilita a un especialista como operacion del diario. Al
     * deshabilitarlo, sus citas pendientes pasan a otro del area sin que nadie
     * las reparta a mano.
     */
    public static String cambiarEstado(String id, boolean habilitar, String usuario) {
        String detalle = Integridad.aplicar(usuario, "Parámetro General",
                (habilitar ? "Habilitar" : "Deshabilitar") + " especialista " + id, () -> {
                    Prototipo.habilitar(id, habilitar);
                    return id + (habilitar ? " habilitado." : " deshabilitado: deja de recibir reclamos.");
                });
        if (habilitar) return detalle;
        int n = reasignarPendientes(id);
        // lo que ya empezo no se mueve: los pasos hechos son suyos, pero la gerencia tiene que saberlo
        long enCurso = Reclamos.todos().stream().map(Reclamo::eventoActual)
                .filter(e -> e != null && id.equals(e.especialista()) && e.estado() == Dominio.EstadoEvento.EN_CURSO)
                .count();
        return detalle + (n == 0 ? "" : " Sus " + n + (n == 1 ? " cita pendiente pasó" : " citas pendientes pasaron")
                + " a otro especialista del área.")
                + (enCurso == 0 ? "" : " Ojo: tiene " + enCurso + (enCurso == 1 ? " atención en curso" : " atenciones en curso")
                + " que no se movieron porque ya empezaron.");
    }

    /**
     * Pasa a otro especialista las citas que {@code id} todavia no empezo.
     * Busca primero a alguien libre en la misma hora, para no mover la fecha
     * que ya se le dio al cliente; si nadie puede, toma el primer hueco.
     */
    public static int reasignarPendientes(String id) {
        int n = 0;
        for (Reclamo t : Reclamos.todos()) {
            EventoReclamo e = t.eventoActual();
            if (e == null || !e.agendado() || !id.equals(e.especialista())
                    || e.estado() != Dominio.EstadoEvento.PROGRAMADO) continue;
            e.anular("Reasignado: " + id + " no está disponible");
            ParqueTickets.liberar(e.tickets());
            LocalDateTime desde = e.inicio().isAfter(LocalDateTime.now()) ? e.inicio() : LocalDateTime.now();
            EventoReclamo nuevo = programar(t, e.evento(), e.protocolo(), null, desde);
            if (!nuevo.inicio().equals(e.inicio())) {
                t.anotar("Sistema", "Cambio de cita", "Nadie del área estaba libre a la misma hora: nueva cita "
                        + nuevo.inicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + ". Se informa al cliente.");
            }
            n++;
        }
        if (n > 0) Reclamos.notificar();
        return n;
    }

    /** Los especialistas del area por menor carga y, en empate, por turno. */
    static List<Empleado> ordenar(String area) {
        List<Empleado> todos = candidatosDe(area);
        int n = todos.size();
        int ultimo = TURNO.getOrDefault(area, -1);
        return todos.stream()
                .sorted(Comparator.comparingLong((Empleado e) -> carga(e.id()))
                        .thenComparingInt(e -> Math.floorMod(todos.indexOf(e) - ultimo - 1, n)))
                .toList();
    }

    /** Los del area que pueden recibir reclamos: los deshabilitados no entran. */
    private static List<Empleado> candidatosDe(String area) {
        return Prototipo.empleadosDe(area).stream().filter(e -> Prototipo.habilitado(e.id())).toList();
    }
}
