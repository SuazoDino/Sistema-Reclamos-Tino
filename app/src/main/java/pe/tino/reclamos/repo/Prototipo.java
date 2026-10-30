package pe.tino.reclamos.repo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Los catalogos del negocio que lee el flujo del reclamo: tipos de problema,
 * problemas, protocolos con sus pasos, politicas de garantia y empleados.
 *
 * Cada lista es la misma que editan las pantallas de mantenimiento, asi que
 * un cambio hecho alli llega al flujo sin reiniciar. Aqui no hay datos de
 * pantalla sueltos: si algo esta en esta clase es porque el sistema lo usa.
 */
public final class Prototipo {

    private Prototipo() {}

    public static final List<String> ESTADO = List.of("Habilitado", "Deshabilitado");

    /* ---------------- catalogo de tipos de reclamo ---------------- */

    /** Reclamo: disconformidad con el producto o servicio. Queja: malestar con la atencion. */
    public static final List<String> NATURALEZAS = List.of("Reclamo", "Queja");

    /**
     * Tipo de problema | Naturaleza | Estado. Es el unico catalogo de tipos de
     * problema del sistema: antes habia cuatro listas distintas (con
     * "Instalacion", "Funcionalidad", "Cobro de producto"...) y solo una se
     * usaba. Un tipo deshabilitado deja de ofrecerse en el formulario.
     */
    private static final List<String[]> TIPOS = new ArrayList<>(List.of(
            new String[]{"Funcionamiento", "Reclamo", "Habilitado"},
            new String[]{"Entrega", "Reclamo", "Habilitado"},
            new String[]{"Cobranza", "Reclamo", "Habilitado"},
            new String[]{"Atención", "Queja", "Habilitado"}));

    public static List<String[]> tipos() { return TIPOS; }

    /** Los tipos de problema habilitados, en orden. */
    public static List<String> tiposDeProblema() {
        return TIPOS.stream().filter(f -> "Habilitado".equals(f[2])).map(f -> f[0]).toList();
    }

    /** Reclamo o Queja, segun el tipo de problema. */
    public static String naturalezaDe(String tipoProblema) {
        return TIPOS.stream().filter(f -> f[0].equals(tipoProblema)).map(f -> f[1])
                .findFirst().orElse("Reclamo");
    }

    /** Lo que el consumidor pide, como en la hoja del Libro de Reclamaciones. */
    public static final List<String> PEDIDOS = List.of(
            "Reparación", "Cambio del producto", "Devolución del dinero",
            "Nueva prestación del servicio", "Disculpa y corrección de la atención");

    /* ---------------- catalogo de problemas ---------------- */

    /** A que aplica un problema: a todo producto, a todo servicio o a un bien o servicio puntual. */
    public static final String TODO_PRODUCTO = "Todo producto";
    public static final String TODO_SERVICIO = "Todo servicio";

    /**
     * Tipo de problema | Problema | Aplica a. Es la relacion problema ↔
     * producto o servicio que el formulario usa para no ofrecer combinaciones
     * sin sentido. "Aplica a" es un bien (Telefono Movil), un servicio
     * (Delivery) o todos los productos o servicios.
     */
    private static final List<String[]> PROBLEMAS = new ArrayList<>(List.of(
            new String[]{"Funcionamiento", "La cámara no funciona", "Teléfono Móvil, Computadores Notebook"},
            new String[]{"Funcionamiento", "Teléfono Sobrecalentado", "Teléfono Móvil"},
            new String[]{"Funcionamiento", "El Teléfono No Responde", "Teléfono Móvil"},
            new String[]{"Funcionamiento", "No enciende", TODO_PRODUCTO},
            new String[]{"Funcionamiento", "Mala instalación", "Instalación, Mantenimiento"},
            new String[]{"Funcionamiento", "El equipo falla después del servicio", "Instalación, Mantenimiento"},
            new String[]{"Entrega", "Empaque abierto", TODO_PRODUCTO + ", Delivery"},
            new String[]{"Entrega", "Producto equivocado", TODO_PRODUCTO + ", Delivery, Recojo en tienda"},
            new String[]{"Entrega", "Demora en la entrega", "Delivery, Recojo en tienda"},
            new String[]{"Cobranza", "Facturación Doble", TODO_PRODUCTO + ", " + TODO_SERVICIO},
            new String[]{"Cobranza", "Error de Facturación", TODO_PRODUCTO + ", " + TODO_SERVICIO},
            new String[]{"Atención", "Mal trato", TODO_PRODUCTO + ", " + TODO_SERVICIO},
            new String[]{"Atención", "Demora", TODO_PRODUCTO + ", " + TODO_SERVICIO}));

    public static List<String[]> problemas() { return PROBLEMAS; }

    public static List<String> problemasDe(String tipo) {
        return PROBLEMAS.stream().filter(f -> f[0].equals(tipo)).map(f -> f[1]).toList();
    }

    /**
     * Los problemas de un tipo que tienen sentido para lo reclamado. Para un
     * producto, {@code clave} es su bien (Telefono Movil); para un servicio,
     * su nombre (Delivery).
     */
    public static List<String> problemasDe(String tipo, boolean esProducto, String clave) {
        return PROBLEMAS.stream()
                .filter(f -> f[0].equals(tipo) && aplica(f[2], esProducto, clave))
                .map(f -> f[1]).toList();
    }

    /** Los tipos de problema habilitados que tienen al menos un problema para lo reclamado. */
    public static List<String> tiposDeProblemaPara(boolean esProducto, String clave) {
        return tiposDeProblema().stream()
                .filter(t -> !problemasDe(t, esProducto, clave).isEmpty())
                .toList();
    }

    private static boolean aplica(String aplicaA, boolean esProducto, String clave) {
        for (String a : aplicaA.split(",\\s*")) {
            if (esProducto && a.equals(TODO_PRODUCTO)) return true;
            if (!esProducto && a.equals(TODO_SERVICIO)) return true;
            if (a.equalsIgnoreCase(clave)) return true;
        }
        return false;
    }

    /* ---------------- catalogo de protocolos ---------------- */

    /** De menor a mayor: un operario puede hacer los pasos de su nivel y de los de abajo. */
    public static final List<String> TIPO_OPERARIO = List.of("Asistente", "Técnico", "Especialista");
    public static final List<String> CRITICIDAD_ACCION = List.of("No Crítico", "Crítico");

    /**
     * Protocolo | Tipo de problema. Un protocolo por tipo de reclamo: es la
     * respuesta planificada a ese reclamo y esta formado por eventos.
     */
    public static List<String[]> protocolos() {
        return new ArrayList<>(List.of(
                new String[]{"PR-FUNC", "Funcionamiento"},
                new String[]{"PR-ENTR", "Entrega"},
                new String[]{"PR-COBR", "Cobranza"},
                new String[]{"PR-ATEN", "Atención"}
        ));
    }

    /**
     * Protocolo | Orden | Evento. Los eventos de cada protocolo, en el orden
     * en que se atienden: Funcionamiento pasa por Inspeccion y luego por
     * Solucion; Cobranza va directo a Solucion.
     */
    public static List<String[]> eventosDeProtocolo() {
        return new ArrayList<>(List.of(
                new String[]{"PR-FUNC", "1", "Inspección"},
                new String[]{"PR-FUNC", "2", "Solución del Reclamo"},
                new String[]{"PR-ENTR", "1", "Inspección"},
                new String[]{"PR-COBR", "1", "Solución del Reclamo"},
                new String[]{"PR-ATEN", "1", "Seguimiento de Reclamo"}
        ));
    }

    /** El protocolo de un tipo de problema. */
    public static String protocoloDe(String tipoProblema) {
        return protocolos().stream()
                .filter(f -> f[1].equalsIgnoreCase(tipoProblema))
                .map(f -> f[0])
                .findFirst().orElse("PR-ATEN");
    }

    /** Los eventos de un protocolo, en orden. */
    public static List<String> eventosDelProtocolo(String protocolo) {
        return eventosDeProtocolo().stream()
                .filter(f -> f[0].equals(protocolo))
                .sorted(java.util.Comparator.comparingInt(f -> Integer.parseInt(f[1])))
                .map(f -> f[2]).toList();
    }

    /**
     * Los eventos por los que pasa un reclamo de ese tipo, en orden:
     * Evento | Protocolo. Todos son del mismo protocolo, el del tipo.
     */
    public static List<String[]> eventosDe(String tipoProblema) {
        String protocolo = protocoloDe(tipoProblema);
        return eventosDelProtocolo(protocolo).stream()
                .map(e -> new String[]{e, protocolo})
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** Lo que dura un evento: la suma de los tiempos maximos de sus pasos. */
    public static int duracionDe(String protocolo, String evento) {
        return pasosDe(protocolo, evento).stream().mapToInt(a -> Integer.parseInt(a[1])).sum();
    }

    /** El evento que cierra todo reclamo: control de calidad y conformidad. */
    public static final String EVENTO_SEGUIMIENTO = "Seguimiento de Reclamo";

    /**
     * Protocolo | Evento | Paso | Accion | Tiempo Maximo (min) | Tipo Operario | Criticidad.
     *
     * La inspeccion de Funcionamiento vale para cualquier equipo: antes pedia
     * "abrir la aplicacion de camara" aunque el reclamo fuera una laptop que
     * no enciende.
     */
    private static final List<String[]> PASOS = new ArrayList<>(List.of(
            new String[]{"PR-FUNC", "Inspección", "1", "Revisar estado físico del equipo", "10", "Técnico", "No Crítico"},
            new String[]{"PR-FUNC", "Inspección", "2", "Reproducir la falla reportada", "5", "Técnico", "No Crítico"},
            new String[]{"PR-FUNC", "Inspección", "3", "Registrar diagnóstico", "5", "Técnico", "Crítico"},
            new String[]{"PR-FUNC", "Solución del Reclamo", "1", "Aplicar la solución y probar el equipo", "120", "Especialista", "Crítico"},
            new String[]{"PR-ENTR", "Inspección", "1", "Verificar empaque", "15", "Asistente", "Crítico"},
            new String[]{"PR-ENTR", "Inspección", "2", "Contrastar guía de remisión", "20", "Asistente", "No Crítico"},
            new String[]{"PR-COBR", "Solución del Reclamo", "1", "Revisar comprobantes emitidos", "30", "Asistente", "Crítico"},
            new String[]{"PR-COBR", "Solución del Reclamo", "2", "Solicitar nota de crédito", "60", "Especialista", "No Crítico"},
            new String[]{"PR-ATEN", "Seguimiento de Reclamo", "1", "Registrar descargo del área", "30", "Asistente", "Crítico"}
    ));

    public static List<String[]> pasosDeProtocolo() { return PASOS; }

    /**
     * Los pasos de un evento del protocolo, en el orden en que se ejecutan:
     * Accion | Tiempo Maximo (min) | Tipo Operario | Criticidad.
     */
    public static List<String[]> pasosDe(String protocolo, String evento) {
        return PASOS.stream()
                .filter(f -> f[0].equals(protocolo) && f[1].equals(evento))
                .sorted(java.util.Comparator.comparingInt(f -> Integer.parseInt(f[2])))
                .map(f -> new String[]{f[3], f[4], f[5], f[6]})
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** 0 Asistente, 1 Tecnico, 2 Especialista; -1 si no atiende eventos. */
    public static int nivelDe(String tipoOperario) {
        return TIPO_OPERARIO.indexOf(tipoOperario);
    }

    /** El tipo de operario que pide un evento: el mas alto de sus pasos. */
    public static String operarioRequerido(String protocolo, String evento) {
        return pasosDe(protocolo, evento).stream().map(a -> a[2])
                .max(java.util.Comparator.comparingInt(Prototipo::nivelDe))
                .orElse(TIPO_OPERARIO.get(0));
    }

    /* ---------------- catalogo de politicas (garantia) ---------------- */

    /**
     * Codigo de producto | Garantia | Plazo (dias) | Estado. El codigo es el
     * del Catalogo de Producto; la pantalla muestra su nombre.
     *
     * El plazo es el que el motor de reglas lee como Tiempo_Garantia: sin el,
     * la condicion CND2 no tenia contra que comparar.
     */
    private static final List<String[]> GARANTIAS = new ArrayList<>(List.of(
            new String[]{"PROD007", "Legal", "365", "Habilitado"},
            new String[]{"PROD007", "Comercial", "180", "Habilitado"},
            new String[]{"PROD012", "Legal", "365", "Habilitado"},
            new String[]{"PROD012", "Comercial", "90", "Deshabilitado"},
            new String[]{"PROD019", "Legal", "730", "Habilitado"},
            new String[]{"PROD019", "Comercial", "365", "Habilitado"},
            new String[]{"PROD023", "Legal", "365", "Habilitado"},
            new String[]{"PROD023", "Comercial", "180", "Habilitado"},
            new String[]{"PROD031", "Legal", "365", "Habilitado"},
            new String[]{"PROD032", "Legal", "365", "Habilitado"},
            new String[]{"PROD033", "Legal", "365", "Habilitado"}
    ));

    public static final List<String> TIPOS_GARANTIA = List.of("Legal", "Comercial");

    public static List<String[]> tiposDePolitica() { return GARANTIAS; }

    /**
     * Plazo de garantia vigente de un producto, en dias: la mayor de sus
     * garantias habilitadas o, si no tiene ninguna, la garantia por defecto.
     */
    public static int plazoDeGarantia(String producto) {
        return GARANTIAS.stream()
                .filter(f -> f[0].equals(producto) && "Habilitado".equals(f[3]))
                .mapToInt(f -> Integer.parseInt(f[2]))
                .max().orElse(Parametros.garantiaPorDefectoDias());
    }

    /**
     * Unidades en almacen para intercambio, por codigo de producto. Es la
     * variable Stock del motor: se lee al terminar la inspeccion.
     */
    private static final Map<String, Integer> STOCK = Map.of(
            "PROD031", 2, "PROD032", 0, "PROD033", 5, "PROD012", 1, "PROD019", 0);

    public static int stockDe(String codigo) { return STOCK.getOrDefault(codigo, 0); }

    /* ---------------- empleados ---------------- */

    public record Empleado(String id, String tipo, String area) {
        public boolean esSupervisor() { return AREA_CALIDAD.equals(area); }
    }

    /** El area del supervisor: revisa, no atiende eventos. */
    public static final String AREA_CALIDAD = "Control de Calidad";

    /**
     * Cada area tiene al menos un especialista, y la tecnica dos (un tecnico
     * y un especialista), para que la asignacion por menor carga tenga entre
     * quienes elegir y la solucion, que pide especialista, tenga quien la haga.
     */
    public static final Map<String, Empleado> EMPLEADOS = new LinkedHashMap<>();
    static {
        EMPLEADOS.put("E01", new Empleado("E01", "Técnico", "Área Técnica de Reparación e Inspección"));
        EMPLEADOS.put("E02", new Empleado("E02", "Asistente", "Logística"));
        EMPLEADOS.put("E03", new Empleado("E03", "Especialista", "Ventas"));
        EMPLEADOS.put("E04", new Empleado("E04", "Especialista", "Área Técnica de Reparación e Inspección"));
        EMPLEADOS.put("E05", new Empleado("E05", "Asistente", "PostVenta"));
        EMPLEADOS.put("E06", new Empleado("E06", "Especialista", "Área Especializada"));
        EMPLEADOS.put("E07", new Empleado("E07", "Especialista", "Gerencia de PostVenta"));
        EMPLEADOS.put("S01", new Empleado("S01", "Supervisor", AREA_CALIDAD));
    }

    /** Los que no reciben reclamos (vacaciones, descanso medico). */
    private static final Set<String> DESHABILITADOS = new HashSet<>();

    public static boolean habilitado(String id) { return !DESHABILITADOS.contains(id); }

    static Set<String> deshabilitados() { return Set.copyOf(DESHABILITADOS); }

    static void restaurarDeshabilitados(Set<String> ids) {
        DESHABILITADOS.clear();
        DESHABILITADOS.addAll(ids);
    }

    public static void habilitar(String id, boolean si) {
        if (!EMPLEADOS.containsKey(id)) throw new IllegalArgumentException("No existe el empleado " + id + ".");
        if (si) DESHABILITADOS.remove(id);
        else DESHABILITADOS.add(id);
    }

    /** Los especialistas del area, en orden: el orden decide los empates. */
    public static List<Empleado> empleadosDe(String area) {
        return EMPLEADOS.values().stream().filter(e -> e.area().equals(area)).toList();
    }

    /* ---------------- consulta de indicadores ---------------- */

    public static final List<String> INDICADORES = List.of(
            "Nivel de satisfacción del cliente",
            "Tiempo promedio de solución por reclamo",
            "Tipo de bienes más reclamados",
            "Áreas con mayor demanda de reclamo",
            "Número de reclamos por clase de producto",
            "Costo por atención de reclamo",
            "Número de rotación de especialistas por reclamo",
            "% de reclamos en un mes",
            "% de reclamos críticos",
            "Porcentaje de reclamos solucionados fuera de plazo",
            "Eficiencia de reclamo por tipo de bien",
            "% devueltos por calidad");
}
