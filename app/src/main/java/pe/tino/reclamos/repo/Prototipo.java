package pe.tino.reclamos.repo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Los datos que muestran las capturas del prototipo en el informe. Se
 * mantienen aparte de {@link Datos} para que se vea de donde sale cada cosa:
 * aqui esta lo que aparece en pantalla en las capturas, tal cual.
 */
public final class Prototipo {

    private Prototipo() {}

    /* ---------------- catalogo de reclamos y eventos ---------------- */

    public static final List<String> TIPO_PROBLEMA =
            List.of("Entrega", "Atención", "Funcionamiento", "Cobranza", "Instalación");
    public static final List<String> INSTANCIA = List.of("Primera", "Segunda");
    public static final List<String> CRITICIDAD = List.of("Alta", "Media", "Baja");
    public static final List<String> ESTADO = List.of("Habilitado", "Deshabilitado");

    /** Tipo de Problema | Instancia | Criticidad | Estado. */
    public static List<String[]> reclamos() {
        return new ArrayList<>(List.of(
                new String[]{"Entrega", "Primera", "Alta", "Habilitado"},
                new String[]{"Entrega", "Primera", "Media", "Habilitado"},
                new String[]{"Entrega", "Primera", "Baja", "Habilitado"},
                new String[]{"Entrega", "Segunda", "Alta", "Deshabilitado"},
                new String[]{"Entrega", "Segunda", "Media", "Deshabilitado"},
                new String[]{"Entrega", "Segunda", "Baja", "Deshabilitado"},
                new String[]{"Entrega", "Segunda", "Alta", "Deshabilitado"},
                new String[]{"Atención", "Primera", "Alta", "Habilitado"}
        ));
    }

    public static final List<String> EVENTOS_EXISTENTES = List.of("Inspección",
            "Solución del Reclamo", "Reasignación de Área", "Seguimiento de Reclamo", "Entrega");
    public static final List<String> EVENTOS_HABILITADOS = List.of("Inspección",
            "Solución del Reclamo", "Reasignación de Área", "Seguimiento de Reclamo");

    /** Evento | Estado. */
    public static List<String[]> eventos() {
        return new ArrayList<>(List.of(
                new String[]{"Inspección", "Habilitado"},
                new String[]{"Solución del Reclamo", "Habilitado"},
                new String[]{"Reasignación de Área", "Deshabilitado"},
                new String[]{"Seguimiento de Reclamo", "Habilitado"}
        ));
    }

    /* ---------------- catalogo de problemas ---------------- */

    public static final List<String> PROBLEMAS_EXISTENTES = List.of("La cámara no funciona",
            "Teléfono Sobrecalentado", "El Teléfono No Responde", "La Tarjeta MicroSD No Funciona",
            "Puerto de Carga Dañado", "Reinicio automático");
    public static final List<String> PROBLEMAS_HABILITADOS = List.of("La cámara no funciona",
            "Teléfono Sobrecalentado", "El Teléfono No Responde", "La Tarjeta MicroSD No Funciona");

    /** Problema | Producto. */
    public static List<String[]> problemasPorProducto() {
        return new ArrayList<>(List.of(
                new String[]{"No funciona la cámara", "Celular"},
                new String[]{"No Funciona la cámara", "Laptop"},
                new String[]{"No funciona la cámara", "Cámara"}
        ));
    }

    public static final List<String> PRODUCTOS_SIMPLES = List.of("Celular", "Laptop", "Cámara");

    /* ---------------- catalogo de protocolos ---------------- */

    public static final List<String> TIPO_OPERARIO = List.of("Técnico", "Especialista", "Asistente");
    public static final List<String> CRITICIDAD_ACCION = List.of("No Crítico", "Crítico");

    /**
     * Protocolo | Tipo de problema | Evento. El ticket guarda este codigo, asi
     * que el protocolo que se le asigna existe de verdad en el catalogo.
     */
    public static List<String[]> protocolos() {
        return new ArrayList<>(List.of(
                new String[]{"PR-FUNC-01", "Funcionamiento", "Inspección"},
                new String[]{"PR-FUNC-02", "Funcionamiento", "Solución del Reclamo"},
                new String[]{"PR-ENTR-01", "Entrega", "Inspección"},
                new String[]{"PR-COBR-01", "Cobranza", "Solución del Reclamo"},
                new String[]{"PR-ATEN-01", "Atención", "Seguimiento de Reclamo"}
        ));
    }

    /** El protocolo que corresponde a un tipo de problema. */
    public static String protocoloDe(String tipoProblema) {
        return protocolos().stream()
                .filter(f -> f[1].equalsIgnoreCase(tipoProblema))
                .map(f -> f[0])
                .findFirst().orElse("PR-ATEN-01");
    }

    /** Protocolo | Accion | Tiempo Maximo (min) | Tipo Operario | Criticidad. */
    public static List<String[]> accionesDeProtocolo() {
        return new ArrayList<>(List.of(
                new String[]{"PR-FUNC-01", "Reiniciar equipo", "10", "Técnico", "No Crítico"},
                new String[]{"PR-FUNC-01", "Abrir aplicación de cámara", "5", "Técnico", "No Crítico"},
                new String[]{"PR-FUNC-01", "Tomar foto de prueba", "5", "Técnico", "Crítico"},
                new String[]{"PR-FUNC-02", "Reemplazar componente", "120", "Especialista", "Crítico"},
                new String[]{"PR-ENTR-01", "Verificar empaque", "15", "Asistente", "Crítico"},
                new String[]{"PR-ENTR-01", "Contrastar guía de remisión", "20", "Asistente", "No Crítico"},
                new String[]{"PR-COBR-01", "Revisar comprobantes emitidos", "30", "Asistente", "Crítico"},
                new String[]{"PR-COBR-01", "Solicitar nota de crédito", "60", "Especialista", "No Crítico"},
                new String[]{"PR-ATEN-01", "Registrar descargo del área", "30", "Asistente", "Crítico"}
        ));
    }

    /** Las acciones de un protocolo, en el orden en que se ejecutan. */
    public static List<String[]> accionesDe(String protocolo) {
        return accionesDeProtocolo().stream()
                .filter(f -> f[0].equals(protocolo))
                .map(f -> new String[]{f[1], f[2], f[3], f[4]})
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** Compatibilidad: las acciones del protocolo de funcionamiento. */
    public static List<String[]> accionesProtocolo() { return accionesDe("PR-FUNC-01"); }

    /* ---------------- catalogo de politicas ---------------- */

    public static final List<String> CATALOGOS = List.of("Producto", "Reclamo", "Garantía",
            "Canal", "Cliente", "Problema");

    /** Catalogo1 | Catalogo2: una politica cruza dos catalogos. */
    public static List<String[]> politicas() {
        return new ArrayList<>(List.of(
                new String[]{"Reclamo", "Canal"},
                new String[]{"Producto", "Garantía"}
        ));
    }

    /**
     * Codigo de producto | Garantia | Plazo (dias) | Estado. El codigo es el
     * del Catalogo de Producto; la pantalla muestra su nombre.
     *
     * El plazo es el que el motor de reglas lee como Tiempo_Garantia: sin el,
     * la condicion CND2 no tenia contra que comparar.
     */
    public static List<String[]> tiposDePolitica() {
        return new ArrayList<>(List.of(
                new String[]{"PROD007", "Legal", "365", "Habilitado"},
                new String[]{"PROD007", "Comercial", "180", "Habilitado"},
                new String[]{"PROD012", "Legal", "365", "Habilitado"},
                new String[]{"PROD012", "Comercial", "90", "Deshabilitado"},
                new String[]{"PROD019", "Legal", "730", "Habilitado"},
                new String[]{"PROD019", "Comercial", "365", "Habilitado"},
                new String[]{"PROD023", "Legal", "365", "Habilitado"},
                new String[]{"PROD023", "Comercial", "180", "Habilitado"}
        ));
    }

    /** Plazo de garantia vigente de un producto, en dias. */
    public static int plazoDeGarantia(String producto) {
        return tiposDePolitica().stream()
                .filter(f -> f[0].equals(producto) && "Habilitado".equals(f[3]))
                .mapToInt(f -> Integer.parseInt(f[2]))
                .max().orElse(0);
    }

    /* ---------------- parametros generales ---------------- */

    public static final List<String> MAGNITUD_EXISTENTE = List.of("Corporativa");
    public static final List<String> MAGNITUDES = List.of("Corporativa", "Nacional",
            "MultiNacional", "InterNacional");
    public static final List<String> IDIOMAS_EXISTENTES = List.of("Español", "Inglés", "Quechua");
    public static final List<String> IDIOMAS_HABILITADOS = List.of("Español");

    /* ---------------- area: atender reclamos ---------------- */

    public record Empleado(String id, String tipo, String area) {}

    public static final Map<String, Empleado> EMPLEADOS = new LinkedHashMap<>();
    static {
        EMPLEADOS.put("E01", new Empleado("E01", "Técnico", "Área Técnica de Reparación e Inspección"));
        EMPLEADOS.put("E02", new Empleado("E02", "Asistente", "Logística"));
        EMPLEADOS.put("E03", new Empleado("E03", "Especialista", "Ventas"));
    }

    /** IdReclamo | Fecha de emision | Fecha de Atencion | Hora de Atencion | Estado. */
    public static List<String[]> reclamosDelArea() {
        return new ArrayList<>(List.of(
                new String[]{"R001", "5/09/2019", "18/09/2019", "5:00 p. m.", "Atendido"},
                new String[]{"R002", "11/08/2019", "18/09/2019", "5:00 p. m.", "Pendiente"},
                new String[]{"R003", "8/09/2019", "18/09/2019", "5:00 p. m.", "Pendiente"},
                new String[]{"R004", "9/07/2019", "18/09/2019", "5:00 p. m.", "Pendiente"},
                new String[]{"R005", "15/09/2019", "18/09/2019", "5:00 p. m.", "Atendido"}
        ));
    }

    /** Datos del reclamo que muestra la ventana de inspeccion. */
    public record DatosReclamo(String id, String producto, String marca,
                               String tipoProblema, String problema) {}

    public static DatosReclamo datosDe(String idReclamo) {
        return switch (idReclamo) {
            case "R002" -> new DatosReclamo("R002", "Laptop", "HP", "Funcionamiento", "No enciende");
            case "R003" -> new DatosReclamo("R003", "Cámara", "Canon", "Funcionamiento", "No enfoca");
            case "R004" -> new DatosReclamo("R004", "Celular", "Motorola", "Entrega", "Empaque abierto");
            case "R005" -> new DatosReclamo("R005", "Celular", "Huawei", "Funcionamiento", "No carga");
            default -> new DatosReclamo("R001", "Celular", "Samsung",
                    "Funcionamiento", "No funciona Cámara");
        };
    }

    /* ---------------- cliente: registrar reclamo ---------------- */

    /** Cod. Compra | Fecha | Monto. */
    public static List<String[]> comprasDe(String documento) {
        return new ArrayList<>(List.of(
                new String[]{"25713", "12/01/2019", "S/900"},
                new String[]{"32847", "17/01/2019", "S/1,050"},
                new String[]{"25592", "27/02/2019", "S/1,200"},
                new String[]{"27554", "16/03/2019", "S/1,350"},
                new String[]{"47624", "23/03/2019", "S/50"},
                new String[]{"39085", "18/05/2019", "S/200"},
                new String[]{"22324", "23/06/2019", "S/350"}
        ));
    }

    public static final List<String> PRODUCTOS_COMPRA = List.of("Celular Huawei",
            "Celular Motorola", "Celular Samsung", "Celular Azumi", "Laptop HP");

    private static final Map<String, List<String>> PROBLEMAS_POR_TIPO = new LinkedHashMap<>();
    static {
        PROBLEMAS_POR_TIPO.put("Funcionamiento", List.of("La cámara no funciona",
                "Teléfono Sobrecalentado", "El Teléfono No Responde", "No enciende"));
        PROBLEMAS_POR_TIPO.put("Entrega", List.of("Empaque abierto", "Producto equivocado", "Demora"));
        PROBLEMAS_POR_TIPO.put("Cobranza", List.of("Facturación Doble", "Error de Facturación"));
        PROBLEMAS_POR_TIPO.put("Atención", List.of("Mal trato", "Demora"));
    }

    public static List<String> tiposDeProblema() { return List.copyOf(PROBLEMAS_POR_TIPO.keySet()); }

    public static List<String> problemasDe(String tipo) {
        return PROBLEMAS_POR_TIPO.getOrDefault(tipo, List.of());
    }

    /* ---------------- cliente: estado del reclamo ---------------- */

    /** Productos | Tipo Problema | Fecha Limite | Estado Reclamo. */
    public static List<String[]> estadoDeReclamos() {
        return new ArrayList<>(List.of(
                new String[]{"Celular Huawei", "Funcionamiento", "12/12/2019", "Rechazado"},
                new String[]{"Celular Motorola", "Funcionamiento", "12/12/2019", "Rechazado"},
                new String[]{"Celular Samsung", "Funcionamiento", "12/12/2019", "Aceptado"},
                new String[]{"Celular Azumi", "Funcionamiento", "15/12/2019", "Pendiente"}
        ));
    }

    /** El detalle que muestra la ventana "Detalle del Reclamo". */
    public record Detalle(String id, String producto, String marca, String tipoProblema,
                          String problema, String emision, String respuesta, String limite,
                          String descripcion, String estado) {}

    public static Detalle detalleDe(int fila) {
        return switch (fila) {
            case 2 -> new Detalle("R003", "Celular", "Samsung", "Funcionamiento",
                    "No Funciona la cámara", "02/12/2019", "8/12/2019", "13/12/2019",
                    "Su reclamo fue aceptado. El equipo pasa a reparación.", "Aceptado");
            case 3 -> new Detalle("R004", "Celular", "Azumi", "Funcionamiento",
                    "No Funciona la cámara", "05/12/2019", "-", "15/12/2019",
                    "Su reclamo está en evaluación por el área técnica.", "Pendiente");
            case 1 -> new Detalle("R002", "Celular", "Motorola", "Funcionamiento",
                    "No Funciona la cámara", "02/12/2019", "8/12/2019", "13/12/2019",
                    "Su equipo se encuentra dañado por lo que no cumple los requisitos "
                            + "de la garantía.", "Rechazado");
            default -> new Detalle("R001", "Celular", "Huawei", "Funcionamiento",
                    "No Funciona la cámara", "02/12/2019", "8/12/2019", "13/12/2019",
                    "Su equipo se encuentra dañado por lo que no cumple los requisitos "
                            + "de la garantía.", "Rechazado");
        };
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
            "Eficiencia de reclamo por tipo de bien");
}
