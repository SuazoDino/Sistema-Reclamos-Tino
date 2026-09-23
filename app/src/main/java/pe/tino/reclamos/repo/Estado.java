package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Modelo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Estado vivo de la aplicacion durante la sesion. El prototipo no persiste:
 * al cerrar, todo vuelve a los datos sembrados. Las pantallas se suscriben
 * para refrescarse cuando otra pantalla modifica los reclamos.
 */
public final class Estado {

    private static final List<Reclamo> RECLAMOS = new ArrayList<>(Datos.reclamosDemo());
    private static final List<Protocolo> PROTOCOLOS = Datos.protocolos();
    private static final List<Regla> REGLAS = Datos.reglas();
    private static final List<Consumer<List<Reclamo>>> OYENTES = new ArrayList<>();

    private Estado() {}

    public static List<Reclamo> reclamos()   { return RECLAMOS; }
    public static List<Protocolo> protocolos() { return PROTOCOLOS; }
    public static List<Regla> reglas()       { return REGLAS; }

    public static void alCambiarReclamos(Consumer<List<Reclamo>> oyente) {
        OYENTES.add(oyente);
    }

    public static void agregar(Reclamo r) {
        RECLAMOS.add(0, r);
        notificar();
    }

    public static void reemplazar(Reclamo viejo, Reclamo nuevo) {
        int i = RECLAMOS.indexOf(viejo);
        if (i >= 0) RECLAMOS.set(i, nuevo); else RECLAMOS.add(0, nuevo);
        notificar();
    }

    private static void notificar() {
        for (Consumer<List<Reclamo>> o : List.copyOf(OYENTES)) o.accept(RECLAMOS);
    }

    /** Siguiente codigo correlativo de reclamo (R008, R009, ...). */
    public static String siguienteCodigo() {
        int max = 0;
        for (Reclamo r : RECLAMOS) {
            try { max = Math.max(max, Integer.parseInt(r.id().substring(1))); }
            catch (NumberFormatException ignorado) { /* codigos no correlativos */ }
        }
        return String.format("R%03d", max + 1);
    }
}
