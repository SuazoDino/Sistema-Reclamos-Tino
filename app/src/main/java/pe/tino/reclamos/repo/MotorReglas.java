package pe.tino.reclamos.repo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de reglas del sistema.
 *
 * Antes una condicion decia "AccionV = Pago Adicional" y "AccionF = CND2" en
 * las mismas dos columnas, asi que el motor tenia que adivinar por el nombre
 * si la salida era otra condicion o una solucion. Aqui cada salida declara su
 * tipo, y las variables que se comparan salen de un catalogo en vez de ser
 * texto libre que no coincidia con nada.
 */
public final class MotorReglas {

    private MotorReglas() {}

    /* ---------------- catalogo de variables ---------------- */

    /** Tipo de dato de una variable; decide como se compara. */
    public enum TipoDato { ENTERO, DECIMAL, LOGICO, TEXTO }

    /**
     * Una variable es un dato del ticket que el motor puede leer. Antes se
     * declaraban unas (Tgarantia, Precio) y se usaban otras (Tiempo_Garantia,
     * Reparable), asi que ninguna regla podia resolverse sola.
     */
    public record Variable(String nombre, TipoDato tipo, String origen, String unidad) {}

    private static final List<Variable> VARIABLES = new ArrayList<>(List.of(
            new Variable("Tiempo_Garantia", TipoDato.ENTERO,
                    "Catalogo de Politicas: plazo de la garantia del producto", "Dias"),
            new Variable("Tiempo_Transcurrido", TipoDato.ENTERO,
                    "Ticket: dias entre la compra y la apertura del ticket", "Dias"),
            new Variable("Tiempo_Atencion", TipoDato.ENTERO,
                    "Parametros de Atencion: plazo comprometido segun el SLA", "Horas"),
            new Variable("Reparable", TipoDato.LOGICO,
                    "Inspeccion del area: si el bien admite reparacion", ""),
            new Variable("Stock", TipoDato.ENTERO,
                    "Almacen: unidades disponibles para intercambio", "Unidades"),
            new Variable("Tiene_Comprobante", TipoDato.LOGICO,
                    "Ticket: si el cliente presento comprobante", ""),
            new Variable("Tipo_Cliente", TipoDato.TEXTO,
                    "Catalogo de Cliente: categoria del cliente", ""),
            new Variable("Instancia", TipoDato.ENTERO,
                    "Ticket: numero de instancia en la que esta", ""),
            new Variable("Precio", TipoDato.DECIMAL,
                    "Comprobante: monto pagado por el bien o servicio", "Soles")
    ));

    public static List<Variable> variables() { return VARIABLES; }

    public static List<String> nombresDeVariable() {
        return VARIABLES.stream().map(Variable::nombre).toList();
    }

    public static Variable variable(String nombre) {
        return VARIABLES.stream().filter(v -> v.nombre().equals(nombre))
                .findFirst().orElse(null);
    }

    /* ---------------- catalogo de acciones ---------------- */

    /** Las soluciones con las que puede terminar la cadena de condiciones. */
    public static final List<String> ACCIONES = List.of(
            "Reembolso Total", "Reembolso Parcial", "Intercambio Equipo",
            "Intercambio Componente", "Reparacion Equipo", "Reparacion Componente",
            "Pago Adicional", "Correccion", "Rechazo");

    /* ---------------- condiciones ---------------- */

    public static final List<String> OPERADORES = List.of("EQ", "NE", "GT", "GE", "LT", "LE");

    /** A donde va una salida: a otra condicion o a una solucion final. */
    public enum TipoSalida { CONDICION, ACCION }

    public record Salida(TipoSalida tipo, String destino) {
        @Override public String toString() {
            return (tipo == TipoSalida.CONDICION ? "Ir a " : "Aplicar ") + destino;
        }
    }

    /** Una condicion del motor: compara una variable con un valor. */
    public record Condicion(String codigo, String variable, String operador, String valor,
                            Salida siVerdadero, Salida siFalso, String descripcion) {}

    private static final List<Condicion> CONDICIONES = new ArrayList<>(List.of(
            new Condicion("CND1", "Tiene_Comprobante", "EQ", "true",
                    new Salida(TipoSalida.CONDICION, "CND2"),
                    new Salida(TipoSalida.ACCION, "Rechazo"),
                    "Sin comprobante no hay garantia que invocar"),
            new Condicion("CND2", "Tiempo_Transcurrido", "LE", "Tiempo_Garantia",
                    new Salida(TipoSalida.CONDICION, "CND3"),
                    new Salida(TipoSalida.ACCION, "Pago Adicional"),
                    "Dentro de garantia continua; fuera de garantia el cliente asume el costo"),
            new Condicion("CND3", "Reparable", "EQ", "true",
                    new Salida(TipoSalida.ACCION, "Reparacion Equipo"),
                    new Salida(TipoSalida.CONDICION, "CND4"),
                    "Si el bien se puede reparar, se repara"),
            new Condicion("CND4", "Stock", "GT", "0",
                    new Salida(TipoSalida.ACCION, "Intercambio Equipo"),
                    new Salida(TipoSalida.ACCION, "Reembolso Total"),
                    "Si no es reparable, se cambia; si no hay stock, se devuelve el dinero")
    ));

    public static List<Condicion> condiciones() { return CONDICIONES; }

    public static Condicion condicion(String codigo) {
        return CONDICIONES.stream().filter(c -> c.codigo().equals(codigo))
                .findFirst().orElse(null);
    }

    public static List<String> codigos() {
        return CONDICIONES.stream().map(Condicion::codigo).toList();
    }

    /* ---------------- evaluacion ---------------- */

    /** Un paso de la traza: que condicion se evaluo, con que datos y que salio. */
    public record Paso(String codigo, String comparacion, boolean resultado, String salida) {}

    /** Resultado de correr la cadena: la solucion y como se llego a ella. */
    public record Resultado(String accion, List<Paso> traza, String error) {
        public boolean valido() { return error == null; }
    }

    /**
     * Corre la cadena desde la primera condicion con los valores dados.
     * Corta si una condicion se repite, para que un encadenamiento mal
     * configurado no deje al motor girando en circulos.
     */
    public static Resultado evaluar(Map<String, String> valores) {
        List<Paso> traza = new ArrayList<>();
        if (CONDICIONES.isEmpty()) return new Resultado(null, traza, "No hay condiciones definidas");

        List<String> visitadas = new ArrayList<>();
        String actual = CONDICIONES.get(0).codigo();

        while (true) {
            if (visitadas.contains(actual)) {
                return new Resultado(null, traza,
                        "La cadena vuelve sobre " + actual + ": hay un ciclo en el encadenamiento");
            }
            visitadas.add(actual);

            Condicion c = condicion(actual);
            if (c == null) {
                return new Resultado(null, traza, "La condicion " + actual + " no existe");
            }

            String izquierda = valores.getOrDefault(c.variable(), "");
            // el valor de comparacion puede ser otra variable, no solo un literal
            String derecha = valores.containsKey(c.valor()) ? valores.get(c.valor()) : c.valor();

            if (izquierda.isBlank()) {
                return new Resultado(null, traza,
                        "Falta el valor de " + c.variable() + " para evaluar " + actual);
            }

            Boolean resultado = comparar(izquierda, c.operador(), derecha, c.variable());
            if (resultado == null) {
                return new Resultado(null, traza,
                        "No se puede comparar " + c.variable() + " con " + derecha);
            }

            Salida salida = resultado ? c.siVerdadero() : c.siFalso();
            traza.add(new Paso(c.codigo(),
                    c.variable() + " (" + izquierda + ") " + c.operador() + " " + derecha,
                    resultado, salida.toString()));

            if (salida.tipo() == TipoSalida.ACCION) {
                return new Resultado(salida.destino(), traza, null);
            }
            actual = salida.destino();
        }
    }

    /** Compara segun el tipo de la variable; devuelve null si no aplica. */
    private static Boolean comparar(String izquierda, String operador,
                                    String derecha, String nombreVariable) {
        Variable v = variable(nombreVariable);
        TipoDato tipo = v == null ? TipoDato.TEXTO : v.tipo();

        if (tipo == TipoDato.ENTERO || tipo == TipoDato.DECIMAL) {
            try {
                double a = Double.parseDouble(izquierda.trim());
                double b = Double.parseDouble(derecha.trim());
                return switch (operador) {
                    case "EQ" -> a == b;
                    case "NE" -> a != b;
                    case "GT" -> a > b;
                    case "GE" -> a >= b;
                    case "LT" -> a < b;
                    case "LE" -> a <= b;
                    default -> null;
                };
            } catch (NumberFormatException e) {
                return null;
            }
        }
        boolean iguales = izquierda.trim().equalsIgnoreCase(derecha.trim());
        return switch (operador) {
            case "EQ" -> iguales;
            case "NE" -> !iguales;
            default -> null;   // mayor o menor no tienen sentido sobre logico ni texto
        };
    }

    /* ---------------- verificacion del encadenamiento ---------------- */

    /**
     * Revisa que la cadena este bien armada: que cada salida exista y que
     * toda condicion termine alguna vez en una accion.
     */
    public static List<String> problemas() {
        List<String> fallas = new ArrayList<>();

        for (Condicion c : CONDICIONES) {
            if (variable(c.variable()) == null) {
                fallas.add(c.codigo() + ": la variable " + c.variable()
                        + " no esta en el catalogo de variables");
            }
            revisarSalida(c, c.siVerdadero(), "si es verdadero", fallas);
            revisarSalida(c, c.siFalso(), "si es falso", fallas);
        }
        return fallas;
    }

    private static void revisarSalida(Condicion c, Salida s, String rama, List<String> fallas) {
        if (s.tipo() == TipoSalida.CONDICION && condicion(s.destino()) == null) {
            fallas.add(c.codigo() + " " + rama + ": apunta a " + s.destino()
                    + ", que no existe");
        }
        if (s.tipo() == TipoSalida.ACCION && !ACCIONES.contains(s.destino())) {
            fallas.add(c.codigo() + " " + rama + ": la accion " + s.destino()
                    + " no esta en el catalogo de acciones");
        }
    }

    /** Valores de ejemplo para probar la cadena desde la pantalla. */
    public static Map<String, String> valoresDeEjemplo() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Tiene_Comprobante", "true");
        m.put("Tiempo_Transcurrido", "120");
        m.put("Tiempo_Garantia", "365");
        m.put("Tiempo_Atencion", "8");
        m.put("Reparable", "false");
        m.put("Stock", "3");
        m.put("Tipo_Cliente", "Vip");
        m.put("Instancia", "1");
        m.put("Precio", "1200");
        return m;
    }
}
