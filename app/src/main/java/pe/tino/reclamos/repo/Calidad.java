package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Control de calidad: ningun reclamo pasa a Cerrado sin revision.
 *
 * Todo reclamo resuelto pasa por la revision automatica. Los observados, una
 * muestra al azar y todos los rechazos pasan ademas por el supervisor. Y el
 * cliente confirma si su reclamo quedo solucionado.
 */
public final class Calidad {

    /** Uno de cada tantos reclamos aprobados va igual al supervisor. */
    private static int muestraCada = 10;
    private static Random azar = new Random();

    private Calidad() {}

    public static int muestraCada() { return muestraCada; }

    public static void muestraCada(int n) {
        if (n < 1) throw new IllegalArgumentException("La muestra debe ser 1 de cada 1 o más");
        muestraCada = n;
    }

    /** Para las pruebas: un azar repetible. */
    public static void azar(Random r) { azar = r; }

    /* ---------------- revision automatica ---------------- */

    /** Lo que la revision automatica encuentra mal; vacio si todo esta en orden. */
    public static List<String> observaciones(Reclamo t) {
        List<String> obs = new ArrayList<>();
        for (EventoReclamo e : t.eventos()) {
            if (!e.agendado() || e.estado() != EstadoEvento.COMPLETADO) continue;
            for (String[] a : Prototipo.pasosDe(e.protocolo(), e.evento())) {
                if ("Crítico".equalsIgnoreCase(a[3]) && !e.accionesEjecutadas().contains(a[0])) {
                    obs.add("Falta la acción crítica \"" + a[0] + "\" en " + e.evento());
                }
            }
        }
        if (t.resolucion() != null && t.limiteAtencion() != null
                && t.resolucion().isAfter(t.limiteAtencion())) {
            obs.add("Se resolvió fuera del límite de atención");
        }
        String sugerida = t.solucionSugerida();
        if (MotorReglas.ACCIONES.contains(sugerida) && !sugerida.equals(t.solucionAplicada())) {
            obs.add("La solución aplicada (" + t.solucionAplicada()
                    + ") no es la sugerida por el motor (" + sugerida + ")");
        }
        return obs;
    }

    /** Pasa el reclamo resuelto a En revision con el resultado de la revision automatica. */
    static void revisar(Reclamo t) {
        List<String> obs = observaciones(t);
        Revision r;
        if (!obs.isEmpty()) r = Revision.OBSERVADO;
        else if (azar.nextInt(muestraCada) == 0) r = Revision.MUESTRA;
        else r = Revision.APROBADO;

        t.cambiarEstado(Estado.EN_REVISION, "Control de calidad", "Revisión automática");
        t.registrarRevision(r, obs);
        Atencion.abrirSeguimiento(t);
    }

    /* ---------------- supervisor ---------------- */

    /** Lo que el supervisor tiene que revisar: observados, muestra y rechazos propuestos. */
    public static List<Reclamo> paraSupervisor() {
        return Reclamos.todos().stream()
                .filter(t -> t.estado() == Estado.EN_REVISION && t.revision() != null
                        && t.revision().esperaSupervisor())
                .toList();
    }

    /** Aprueba: un rechazo se notifica al cliente; una solucion espera su conformidad. */
    public static void aprobar(Reclamo t, String supervisor) {
        if (t.estado() != Estado.EN_REVISION) throw new IllegalStateException("No está en revisión");
        if (t.revision() == Revision.RECHAZO_PROPUESTO) {
            cerrarSeguimiento(t, "Rechazo aprobado");
            t.cambiarEstado(Estado.RECHAZADO, supervisor, t.motivoRechazo());
        } else {
            t.aprobarRevision(supervisor);
            if (Boolean.TRUE.equals(t.conforme())) cerrar(t, supervisor);
        }
        Reclamos.notificar();
    }

    /** Devuelve a atencion con motivo. */
    public static void devolverPorSupervisor(Reclamo t, String supervisor, String motivo) {
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("Indique el motivo");
        Atencion.devolver(t, supervisor, "Devuelto por calidad: " + motivo);
    }

    /* ---------------- conformidad del cliente ---------------- */

    /** El cliente puede responder mientras su solucion este en revision. */
    public static boolean esperaConformidad(Reclamo t) {
        return t.estado() == Estado.EN_REVISION && t.revision() != Revision.RECHAZO_PROPUESTO
                && t.conforme() == null;
    }

    /**
     * "Su reclamo quedo solucionado?" Si dice que no, vuelve a atencion. Si
     * dice que si, se cierra en cuanto la revision este aprobada.
     */
    public static void conformidad(Reclamo t, boolean conforme, int nota) {
        if (!esperaConformidad(t)) throw new IllegalStateException("No espera conformidad");
        if (nota < 1 || nota > 5) throw new IllegalArgumentException("La nota va del 1 al 5");
        t.registrarConformidad(conforme, nota);
        if (!conforme) {
            Atencion.devolver(t, "Cliente", "El cliente no quedó conforme");
            return;
        }
        if (t.revision() == Revision.APROBADO) cerrar(t, "Cliente");
        Reclamos.notificar();
    }

    private static void cerrar(Reclamo t, String usuario) {
        cerrarSeguimiento(t, "Conforme");
        t.cambiarEstado(Estado.CERRADO, usuario, "Revisado y conforme");
    }

    private static void cerrarSeguimiento(Reclamo t, String resultado) {
        EventoReclamo s = t.eventoActual();
        if (s != null && !s.agendado()) s.completar(List.of(), resultado);
    }

    /* ---------------- indicadores ---------------- */

    /** Promedio de las notas de conformidad, o -1 si nadie respondio. */
    public static double satisfaccion() {
        return Reclamos.todos().stream().filter(t -> t.nota() > 0)
                .mapToInt(Reclamo::nota).average().orElse(-1);
    }

    /** Porcentaje de reclamos revisados que alguna vez se devolvieron, o -1 si no hay revisados. */
    public static double porcentajeDevueltos() {
        List<Reclamo> revisados = Reclamos.todos().stream()
                .filter(t -> t.revision() != null || t.devoluciones() > 0
                        || t.estado() == Estado.CERRADO).toList();
        if (revisados.isEmpty()) return -1;
        long devueltos = revisados.stream().filter(t -> t.devoluciones() > 0).count();
        return 100.0 * devueltos / revisados.size();
    }
}
