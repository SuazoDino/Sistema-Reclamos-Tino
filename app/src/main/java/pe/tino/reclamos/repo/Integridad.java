package pe.tino.reclamos.repo;

import pe.tino.reclamos.repo.Dominio.Instancia;
import pe.tino.reclamos.repo.MotorReglas.Condicion;
import pe.tino.reclamos.repo.MotorReglas.TipoSalida;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Integridad de la configuracion: las reglas que ningun cambio de
 * mantenimiento puede romper.
 *
 * El libro pide para el modulo transaccional "asegurar que la base de datos
 * no pierda su coherencia en sus datos relacionados". Aqui eso es: todo tipo
 * de problema tiene quien lo atienda, todo evento cabe en una jornada y la
 * cadena de reglas no apunta a nada que no exista ni gira en circulos.
 *
 * Cada cambio corre como una transaccion: se toma la foto, se aplica, se
 * verifica todo y, si algo quedo incoherente, se vuelve a la foto. Asi el
 * sistema nunca queda a medio cambiar.
 */
public final class Integridad {

    private Integridad() {}

    /** Un cambio de configuracion: lo que se hace y el texto que lo describe en el diario. */
    public interface Cambio { String aplicar(); }

    /**
     * Aplica un cambio de configuracion como transaccion y lo registra en el
     * Diario de Operaciones. Si el cambio falla o deja la configuracion
     * incoherente, se revierte y se lanza IllegalArgumentException.
     *
     * @return el detalle que devolvio el cambio
     */
    public static String aplicar(String usuario, String modulo, String operacion, Cambio cambio) {
        Foto antes = Foto.tomar();
        String detalle;
        try {
            detalle = cambio.aplicar();
        } catch (RuntimeException e) {
            antes.restaurar();
            Auditoria.registrar(usuario, modulo, operacion, e.getMessage(), Auditoria.RECHAZADA, null);
            throw e;
        }
        List<String> fallas;
        try {
            fallas = verificar();
        } catch (RuntimeException e) {
            fallas = List.of("un valor no tiene el formato que el sistema necesita (" + e.getMessage() + ")");
        }
        if (!fallas.isEmpty()) {
            antes.restaurar();
            String motivo = "El cambio dejaría el sistema incoherente: " + String.join("; ", fallas) + ".";
            Auditoria.registrar(usuario, modulo, operacion, motivo, Auditoria.RECHAZADA, null);
            throw new IllegalArgumentException(motivo);
        }
        Auditoria.registrar(usuario, modulo, operacion, detalle, Auditoria.APLICADA, antes);
        return detalle;
    }

    /** Lo que esta incoherente en la configuracion actual; vacio si todo esta bien. */
    public static List<String> verificar() {
        List<String> fallas = new ArrayList<>();
        long jornada = Parametros.minutosJornada();

        // primero el tipo de cada valor: lo demas calcula con ellos
        tipos(fallas);
        if (!fallas.isEmpty()) return fallas;

        // cada tipo habilitado tiene un area habilitada con al menos un especialista habilitado
        for (String tipo : Prototipo.tiposDeProblema()) {
            String area = Catalogos.areaDe(tipo, Instancia.PRIMERA);
            revisarArea(area, "el tipo " + tipo, fallas);
            String protocolo = Prototipo.protocoloDe(tipo);
            if (Prototipo.eventosDelProtocolo(protocolo).isEmpty()) {
                fallas.add("el tipo " + tipo + " usa el protocolo " + protocolo + ", que no tiene eventos");
            }
        }
        for (Instancia i : Instancia.values()) {
            if (i != Instancia.PRIMERA) revisarArea(i.resuelve(), "la " + i.etiqueta().toLowerCase() + " instancia", fallas);
        }

        // cada evento tiene pasos y cabe en una jornada
        for (String[] e : Prototipo.eventosDeProtocolo()) {
            int dura = Prototipo.duracionDe(e[0], e[2]);
            if (dura == 0) fallas.add(e[0] + " " + e[2] + " no tiene pasos");
            else if (dura > jornada) {
                fallas.add(e[0] + " " + e[2] + " dura " + dura + " min y la jornada tiene " + jornada);
            }
        }

        // la cadena de reglas: referencias validas y sin ciclos desde cada condicion inicial
        fallas.addAll(MotorReglas.problemas());
        MotorReglas.asignadas().forEach((tipo, inicio) -> {
            if (MotorReglas.condicion(inicio) == null) {
                fallas.add("la regla de " + tipo + " empieza en " + inicio + ", que no existe");
            } else if (hayCiclo(inicio, new HashSet<>())) {
                fallas.add("la cadena que empieza en " + inicio + " vuelve sobre sí misma");
            }
        });

        // lo que referencian los catalogos existe
        List<String> tipos = Prototipo.tipos().stream().map(f -> f[0]).toList();
        Prototipo.problemas().stream().filter(f -> !tipos.contains(f[0]))
                .forEach(f -> fallas.add("el problema " + f[1] + " es de un tipo que no existe"));
        return fallas;
    }

    /** Que los numeros sean numeros y los estados, estados: los calculos del flujo dependen de eso. */
    private static void tipos(List<String> fallas) {
        for (String[] f : Prototipo.pasosDeProtocolo()) {
            entero(f[2], 1, 20, "el paso " + f[3] + " (número de paso)", fallas);
            entero(f[4], 1, 480, "el paso " + f[3] + " (tiempo máximo)", fallas);
            if (!Prototipo.TIPO_OPERARIO.contains(f[5])) fallas.add("el paso " + f[3] + " pide un operario que no existe");
            if (!Prototipo.CRITICIDAD_ACCION.contains(f[6])) fallas.add("el paso " + f[3] + " tiene una criticidad inválida");
        }
        for (String[] f : Prototipo.tiposDePolitica()) {
            entero(f[2], 0, 3650, "la garantía de " + f[0], fallas);
            estado(f[3], "la garantía de " + f[0], fallas);
        }
        for (String[] f : Catalogos.tiemposAtencion()) {
            entero(f[2], 1, 90, "el tiempo de " + f[0] + " " + f[1], fallas);
            if (!List.of("Horas", "Días").contains(f[3])) fallas.add("el tiempo de " + f[0] + " " + f[1] + " tiene una unidad inválida");
        }
        for (String[] f : Catalogos.reglasPrioridad()) {
            if (!List.of("Alta", "Media", "Baja").contains(f[2])) fallas.add("la prioridad de " + f[0] + " / " + f[1] + " es inválida");
        }
        Catalogos.canales().forEach(f -> estado(f[3], "el canal " + f[0], fallas));
        Catalogos.locales().forEach(f -> estado(f[2], "el local " + f[0], fallas));
        Catalogos.servicios().forEach(f -> estado(f[3], "el servicio " + f[1], fallas));
        Prototipo.tipos().forEach(f -> {
            estado(f[2], "el tipo " + f[0], fallas);
            if (!Prototipo.NATURALEZAS.contains(f[1])) fallas.add("el tipo " + f[0] + " tiene una naturaleza inválida");
        });
    }

    private static void entero(String v, int min, int max, String que, List<String> fallas) {
        try {
            int n = Integer.parseInt(v.trim());
            if (n < min || n > max) fallas.add(que + " debe estar entre " + min + " y " + max);
        } catch (RuntimeException e) {
            fallas.add(que + " debe ser un número entero");
        }
    }

    private static void estado(String v, String que, List<String> fallas) {
        if (!Prototipo.ESTADO.contains(v)) fallas.add(que + " tiene un estado inválido");
    }

    private static void revisarArea(String area, String quien, List<String> fallas) {
        if (!Datos.catalogo("areas").habilitados().contains(area)) {
            fallas.add(quien + " se atiende en " + area + ", que no está habilitada");
        } else if (Prototipo.empleadosDe(area).stream().noneMatch(e -> Prototipo.habilitado(e.id()))) {
            fallas.add(quien + " se atiende en " + area + ", que se quedaría sin especialistas habilitados");
        }
    }

    private static boolean hayCiclo(String codigo, Set<String> camino) {
        if (!camino.add(codigo)) return true;
        Condicion c = MotorReglas.condicion(codigo);
        if (c != null) {
            for (var s : List.of(c.siVerdadero(), c.siFalso())) {
                if (s.tipo() == TipoSalida.CONDICION && hayCiclo(s.destino(), new HashSet<>(camino))) return true;
            }
        }
        return false;
    }
}
