package pe.tino.reclamos.repo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Diario de Operaciones: la bitacora de todo lo que se hace sobre la
 * configuracion del sistema.
 *
 * Es el modulo de Auditoria del libro: "supervisar el normal desarrollo
 * operativo del sistema [...] dejando huella en cada evento". Cada reclamo
 * ya tiene su propia bitacora; esta registra lo que cambia las reglas con las
 * que se atienden todos: catalogos, parametros, especialistas y perfiles. Se
 * registran tambien los intentos rechazados, porque un intento de dejar al
 * sistema incoherente es justo lo que la supervision tiene que ver.
 */
public final class Auditoria {

    private Auditoria() {}

    /**
     * Una operacion del diario. {@code antes} es la foto de la configuracion
     * previa al cambio; con ella se puede revertir. Es null si la operacion se
     * rechazo, ya se revirtio o no se puede revertir.
     */
    public record Operacion(int numero, LocalDateTime momento, String usuario, String modulo,
                            String operacion, String detalle, String resultado, Foto antes) {
        public boolean revertible() { return antes != null; }
    }

    public static final String APLICADA = "Aplicada";
    public static final String RECHAZADA = "Rechazada";
    public static final String REVERTIDA = "Revertida";

    /** Mientras no haya inicio de sesion, el usuario es el puesto de gerencia. */
    public static final String SIN_SESION = "Gerencia (sin sesión)";

    private static final List<Operacion> DIARIO = new ArrayList<>();

    public static List<Operacion> diario() { return List.copyOf(DIARIO); }

    /** Deja huella de una operacion que no cambia la configuracion (por ejemplo, perfiles). */
    public static void anotar(String usuario, String modulo, String operacion, String detalle) {
        registrar(usuario, modulo, operacion, detalle, APLICADA, null);
    }

    static Operacion registrar(String usuario, String modulo, String operacion, String detalle,
                               String resultado, Foto antes) {
        Operacion o = new Operacion(DIARIO.size() + 1, LocalDateTime.now(), usuario, modulo,
                operacion, detalle, resultado, antes);
        DIARIO.add(o);
        return o;
    }

    /**
     * Solo se revierte la ultima operacion vigente: revertir una anterior
     * desharia tambien las de despues. Las reversas y lo ya revertido se
     * saltan, asi que se puede ir hacia atras una por una.
     */
    public static Operacion ultimaRevertible() {
        for (int i = DIARIO.size() - 1; i >= 0; i--) {
            Operacion o = DIARIO.get(i);
            // las que no tienen foto (reversas, perfiles) no tocan la configuracion: no estorban
            if (APLICADA.equals(o.resultado()) && o.revertible()) return o;
        }
        return null;
    }

    /**
     * Marcha atras: deja la configuracion como estaba antes de la operacion.
     * Los reclamos que ya se atendieron con la configuracion nueva no cambian.
     */
    public static String revertir(int numero, String usuario) {
        Operacion o = ultimaRevertible();
        if (o == null || o.numero() != numero) {
            throw new IllegalStateException("Solo se puede revertir la última operación aplicada.");
        }
        o.antes().restaurar();
        DIARIO.set(numero - 1, new Operacion(o.numero(), o.momento(), o.usuario(), o.modulo(),
                o.operacion(), o.detalle(), REVERTIDA, null));
        registrar(usuario, o.modulo(), "Reversa", "Se revirtió la operación " + numero + ": " + o.operacion(),
                APLICADA, null);
        return "Operación " + numero + " revertida.";
    }
}
