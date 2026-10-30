package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Ticket;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * El parque de tickets: los turnos de atencion de cada operario.
 *
 * Lo genera un proceso batch a partir del horario laboral y los feriados:
 * cada dia laborable, de la hora de entrada a la de salida, en periodos de
 * {@link #PERIODO_MIN} minutos. Un evento del reclamo toma tantos tickets
 * seguidos como dure su protocolo (Inspeccion de 20 minutos toma 2).
 *
 * El limite de atencion (SLA) es un plazo maximo; la fecha que se le da al
 * cliente es la del primer ticket que toma. Un ticket tomado no se mueve, asi
 * que una cita nueva nunca desplaza a las anteriores.
 */
public final class ParqueTickets {

    /** Lo que dura un ticket. */
    public static final int PERIODO_MIN = 10;

    /** Cuantos dias hacia adelante deja generados el batch. */
    public static final int HORIZONTE_DIAS = 30;

    /** Operario -> sus tickets ordenados por hora de inicio. */
    private static final Map<String, TreeMap<LocalDateTime, Ticket>> PARQUE = new HashMap<>();
    /** Hasta que dia esta generado el parque de cada operario. */
    private static final Map<String, LocalDate> GENERADO_HASTA = new HashMap<>();

    private ParqueTickets() {}

    /* ---------------- batch: generacion del parque ---------------- */

    /** Resumen del batch: lo que el cuadre de control reporta. */
    public record Cuadre(int operarios, int dias, int ticketsGenerados) {}

    /** Genera el parque de todos los operarios para los proximos dias. */
    public static Cuadre generar(LocalDate desde, int dias) {
        int generados = 0;
        List<String> operarios = Prototipo.EMPLEADOS.values().stream()
                .filter(e -> !e.esSupervisor()).map(Prototipo.Empleado::id).toList();
        for (String o : operarios) generados += generar(o, desde, desde.plusDays(dias - 1));
        return new Cuadre(operarios.size(), dias, generados);
    }

    /** Genera los tickets de un operario entre dos fechas; devuelve cuantos creo. */
    public static int generar(String operario, LocalDate desde, LocalDate hasta) {
        TreeMap<LocalDateTime, Ticket> suyos = PARQUE.computeIfAbsent(operario, k -> new TreeMap<>());
        int generados = 0;
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            if (!Catalogos.esLaborable(d)) continue;
            for (LocalDateTime t = d.atTime(Catalogos.inicioJornada());
                 t.isBefore(d.atTime(Catalogos.finJornada())); t = t.plusMinutes(PERIODO_MIN)) {
                if (suyos.containsKey(t)) continue;
                suyos.put(t, new Ticket(operario, t, t.plusMinutes(PERIODO_MIN)));
                generados++;
            }
        }
        GENERADO_HASTA.merge(operario, hasta, (a, b) -> a.isAfter(b) ? a : b);
        return generados;
    }

    /**
     * Cuando cambia la jornada: los tickets libres que quedan fuera del nuevo
     * horario se retiran y se generan los que faltan dentro. Los tickets ya
     * tomados no se tocan, porque una cita dada es fija.
     */
    public static void ajustarJornada() {
        LocalDate hoy = LocalDate.now();
        for (Map.Entry<String, TreeMap<LocalDateTime, Ticket>> e : PARQUE.entrySet()) {
            e.getValue().entrySet().removeIf(x -> !x.getKey().toLocalDate().isBefore(hoy)
                    && x.getValue().disponible() && fueraDeJornada(x.getKey()));
            LocalDate hasta = GENERADO_HASTA.get(e.getKey());
            if (hasta != null && !hasta.isBefore(hoy)) generar(e.getKey(), hoy, hasta);
        }
    }

    private static boolean fueraDeJornada(LocalDateTime t) {
        return t.toLocalTime().isBefore(Catalogos.inicioJornada())
                || t.plusMinutes(PERIODO_MIN).toLocalTime().isAfter(Catalogos.finJornada())
                || t.plusMinutes(PERIODO_MIN).toLocalDate().isAfter(t.toLocalDate());
    }

    /** Si se busca mas alla de lo generado, se corre el batch para ese tramo. */
    private static void asegurar(String operario, LocalDate dia) {
        LocalDate hasta = GENERADO_HASTA.get(operario);
        if (hasta == null) generar(operario, dia.minusDays(1), dia.plusDays(HORIZONTE_DIAS));
        else if (dia.isAfter(hasta)) generar(operario, hasta.plusDays(1), dia.plusDays(HORIZONTE_DIAS));
    }

    /* ---------------- consultas ---------------- */

    public static List<Ticket> de(String operario) {
        return List.copyOf(PARQUE.getOrDefault(operario, new TreeMap<>()).values());
    }

    public static List<Ticket> de(String operario, LocalDate dia) {
        asegurar(operario, dia);
        return List.copyOf(PARQUE.get(operario)
                .subMap(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()).values());
    }

    public static Ticket porCodigo(String codigo) {
        return PARQUE.values().stream().map(m -> m.values().stream()
                        .filter(t -> t.codigo().equals(codigo)).findFirst().orElse(null))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /* ---------------- tomar tickets ---------------- */

    private static int periodos(int minutos) {
        long jornada = Duration.between(Catalogos.inicioJornada(), Catalogos.finJornada()).toMinutes();
        if (minutos <= 0 || minutos > jornada) {
            throw new IllegalArgumentException("Un evento debe durar entre 1 y " + jornada + " minutos");
        }
        return (minutos + PERIODO_MIN - 1) / PERIODO_MIN;
    }

    /**
     * El inicio del primer tramo de tickets libres y seguidos, desde
     * {@code desde}, que alcanza para {@code minutos}. Nunca parte un evento
     * entre dos dias.
     */
    public static LocalDateTime primerHueco(String operario, LocalDateTime desde, int minutos) {
        int k = periodos(minutos);
        LocalDate dia = desde.toLocalDate();
        for (int i = 0; i < 366; i++, dia = dia.plusDays(1)) {
            List<Ticket> delDia = de(operario, dia);
            int seguidos = 0;
            LocalDateTime inicioTramo = null;
            Ticket anterior = null;
            for (Ticket t : delDia) {
                boolean sirve = t.disponible() && !t.inicio().isBefore(desde);
                boolean contiguo = anterior != null && anterior.fin().equals(t.inicio());
                if (!sirve) { seguidos = 0; anterior = null; continue; }
                if (seguidos == 0 || !contiguo) { seguidos = 0; inicioTramo = t.inicio(); }
                seguidos++;
                anterior = t;
                if (seguidos == k) return inicioTramo;
            }
        }
        throw new IllegalStateException("El operario " + operario + " no tiene tickets libres en un año");
    }

    /**
     * El evento toma los tickets seguidos desde {@code inicio}. Si alguno ya
     * esta tomado, no toma ninguno: una cita nueva no desplaza a otra.
     */
    public static List<String> tomar(String operario, LocalDateTime inicio, int minutos,
                                     String reclamo, String evento) {
        int k = periodos(minutos);
        asegurar(operario, inicio.toLocalDate());
        TreeMap<LocalDateTime, Ticket> suyos = PARQUE.get(operario);
        List<Ticket> tramo = new ArrayList<>();
        LocalDateTime t = inicio;
        for (int i = 0; i < k; i++, t = t.plusMinutes(PERIODO_MIN)) {
            Ticket ticket = suyos.get(t);
            if (ticket == null || !ticket.disponible()) {
                throw new IllegalStateException("La cita se cruza con otra de " + operario
                        + " o cae fuera de su horario");
            }
            tramo.add(ticket);
        }
        tramo.forEach(x -> x.tomar(reclamo, evento));
        return tramo.stream().map(Ticket::codigo).toList();
    }

    /** Devuelve al parque los tickets de una cita que ya no se va a dar. */
    public static void liberar(List<String> codigos) {
        codigos.forEach(c -> {
            Ticket t = porCodigo(c);
            if (t != null) t.liberar();
        });
    }

    /** Sube al siguiente multiplo del periodo. */
    static LocalDateTime redondear(LocalDateTime t) {
        LocalDateTime base = t.truncatedTo(ChronoUnit.MINUTES);
        if (base.isBefore(t)) base = base.plusMinutes(1);
        int resto = base.getMinute() % PERIODO_MIN;
        return resto == 0 ? base : base.plusMinutes(PERIODO_MIN - resto);
    }

    static {
        generar(LocalDate.now(), HORIZONTE_DIAS);
    }
}
