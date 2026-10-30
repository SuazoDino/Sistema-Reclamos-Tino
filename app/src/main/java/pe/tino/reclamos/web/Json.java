package pe.tino.reclamos.web;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;

/**
 * Convierte mapas, listas y valores simples a JSON. Es lo unico que necesita
 * la API, asi que no hace falta una libreria aparte.
 */
final class Json {

    private Json() {}

    static String de(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(sb, valor);
        return sb.toString();
    }

    private static void escribir(StringBuilder sb, Object v) {
        if (v == null || v instanceof Double d && (d.isNaN() || d.isInfinite())) {
            sb.append("null");
        } else if (v instanceof Boolean || v instanceof Number) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                texto(sb, String.valueOf(e.getKey()));
                sb.append(':');
                escribir(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primero = true;
            for (Object o : c) {
                if (!primero) sb.append(',');
                primero = false;
                escribir(sb, o);
            }
            sb.append(']');
        } else if (v instanceof TemporalAccessor) {
            // LocalDateTime y LocalDate en ISO: el navegador les da formato
            texto(sb, v.toString());
        } else {
            texto(sb, v.toString());
        }
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
