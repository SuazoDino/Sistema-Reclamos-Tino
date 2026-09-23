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
            List.of("Entrega", "Atencion", "Funcionamiento", "Cobranza", "Instalacion");
    public static final List<String> INSTANCIA = List.of("primera", "segunda");
    public static final List<String> CRITICIDAD = List.of("Alta", "Media", "Baja");
    public static final List<String> ESTADO = List.of("Habilitado", "Deshabilitado");

    /** Tipo de Problema | Instancia | Criticidad | Estado. */
    public static List<String[]> reclamos() {
        return new ArrayList<>(List.of(
                new String[]{"Entrega", "primera", "Alta", "Habilitado"},
                new String[]{"Entrega", "primera", "Media", "Habilitado"},
                new String[]{"Entrega", "primera", "Baja", "Habilitado"},
                new String[]{"Entrega", "segunda", "Alta", "Deshabilitado"},
                new String[]{"Entrega", "segunda", "Media", "Deshabilitado"},
                new String[]{"Entrega", "segunda", "Baja", "Deshabilitado"},
                new String[]{"Entrega", "segunda", "Alta", "Deshabilitado"},
                new String[]{"Atencion", "primera", "Alta", "Habilitado"}
        ));
    }

    public static final List<String> EVENTOS_EXISTENTES = List.of("Inspeccion",
            "Solucion del Reclamo", "Reasignacion de Area", "Seguimiento de Reclamo", "Entrega");
    public static final List<String> EVENTOS_HABILITADOS = List.of("Inspeccion",
            "Solucion del Reclamo", "Reasignacion de Area", "Seguimiento de Reclamo");

    /** Evento | Estado. */
    public static List<String[]> eventos() {
        return new ArrayList<>(List.of(
                new String[]{"Inspeccion", "Habilitado"},
                new String[]{"Solucion del Reclamo", "Habilitado"},
                new String[]{"Reasignacion de Area", "Deshabilitado"},
                new String[]{"Seguimiento de Reclamo", "Habilitado"}
        ));
    }

    /* ---------------- catalogo de problemas ---------------- */

    public static final List<String> PROBLEMAS_EXISTENTES = List.of("La camara no funciona",
            "Telefono Sobrecalentado", "El Telefono No Responde", "La Tarjeta MicroSD No Funciona",
            "Puerto de Carga Daniado", "Reinicio automatico");
    public static final List<String> PROBLEMAS_HABILITADOS = List.of("La camara no funciona",
            "Telefono Sobrecalentado", "El Telefono No Responde", "La Tarjeta MicroSD No Funciona");

    /** Problema | Producto. */
    public static List<String[]> problemasPorProducto() {
        return new ArrayList<>(List.of(
                new String[]{"No funciona la camara", "Celular"},
                new String[]{"No Funciona la camara", "Laptop"},
                new String[]{"No funciona la camara", "Camara"}
        ));
    }

    public static final List<String> PRODUCTOS_SIMPLES = List.of("Celular", "Laptop", "Camara");

    /* ---------------- catalogo de protocolos ---------------- */

    public static final List<String> TIPO_OPERARIO = List.of("Tecnico", "Especialista", "Asistente");
    public static final List<String> CRITICIDAD_ACCION = List.of("No Critico", "Critico");

    /** Accion | Tiempo Maximo(s) | Tipo Operario | Criticidad. */
    public static List<String[]> accionesProtocolo() {
        return new ArrayList<>(List.of(
                new String[]{"Reiniciar celular", "120", "Tecnico", "No Critico"},
                new String[]{"Abrir aplicacion de camara", "60", "Tecnico", "No Critico"},
                new String[]{"Tomar foto", "20", "Tecnico", "No Critico"}
        ));
    }

    /* ---------------- catalogo de politicas ---------------- */

    public static final List<String> CATALOGOS = List.of("Producto", "Reclamo", "Garantia",
            "Canal", "Cliente", "Problema");

    /** Catalogo1 | Catalogo2: una politica cruza dos catalogos. */
    public static List<String[]> politicas() {
        return new ArrayList<>(List.of(
                new String[]{"Reclamo", "Canal"},
                new String[]{"Producto", "Garantia"}
        ));
    }

    /** Producto | Garantia | Estado: los tipos de la politica seleccionada. */
    public static List<String[]> tiposDePolitica() {
        return new ArrayList<>(List.of(
                new String[]{"Prod001", "Legal", "Habilitado"},
                new String[]{"Prod001", "Comercial", "Habilitado"},
                new String[]{"Prod002", "Legal", "Habilitado"},
                new String[]{"Prod002", "Comercial", "Deshabilitado"},
                new String[]{"Prod003", "Legal", "Habilitado"},
                new String[]{"Prod003", "Comercial", "Habilitado"},
                new String[]{"Prod004", "Legal", "Habilitado"},
                new String[]{"Prod004", "Comercial", "Habilitado"}
        ));
    }

    /* ---------------- parametros generales ---------------- */

    public static final List<String> MAGNITUD_EXISTENTE = List.of("Corporativa");
    public static final List<String> MAGNITUDES = List.of("Corporativa", "Nacional",
            "MultiNacional", "InterNacional");
    public static final List<String> IDIOMAS_EXISTENTES = List.of("Espaniol", "Ingles", "Quechua");
    public static final List<String> IDIOMAS_HABILITADOS = List.of("Espaniol");

    /* ---------------- area: atender reclamos ---------------- */

    public record Empleado(String id, String tipo, String area) {}

    public static final Map<String, Empleado> EMPLEADOS = new LinkedHashMap<>();
    static {
        EMPLEADOS.put("E01", new Empleado("E01", "Tecnico", "Tecnica de Reparacion e Inspeccion"));
        EMPLEADOS.put("E02", new Empleado("E02", "Tecnico", "Tecnica de Reparacion e Inspeccion"));
        EMPLEADOS.put("E03", new Empleado("E03", "Especialista", "Area Especializada"));
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
            case "R003" -> new DatosReclamo("R003", "Camara", "Canon", "Funcionamiento", "No enfoca");
            case "R004" -> new DatosReclamo("R004", "Celular", "Motorola", "Entrega", "Empaque abierto");
            case "R005" -> new DatosReclamo("R005", "Celular", "Huawei", "Funcionamiento", "No carga");
            default -> new DatosReclamo("R001", "Celular", "Samsung",
                    "Funcionamiento", "No funciona Camara");
        };
    }

    /* ---------------- cliente: registrar reclamo ---------------- */

    public record Persona(String documento, String nombres, String apellidos,
                          String correo, String direccion, String telefono) {}

    public static final Map<String, Persona> PERSONAS = new LinkedHashMap<>();
    static {
        PERSONAS.put("72119474", new Persona("72119474", "Alexander", "Rodriguez Camacho",
                "arodriguez@correo.pe", "Av. Los Alamos 123", "987654321"));
        PERSONAS.put("10112387203", new Persona("10112387203", "Michael Skinner",
                "Brito Calero", "michaelbc.141@gmail.com", "Jr. Union 456", "912345678"));
    }

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
        PROBLEMAS_POR_TIPO.put("Funcionamiento", List.of("La camara no funciona",
                "Telefono Sobrecalentado", "El Telefono No Responde", "No enciende"));
        PROBLEMAS_POR_TIPO.put("Entrega", List.of("Empaque abierto", "Producto equivocado", "Demora"));
        PROBLEMAS_POR_TIPO.put("Cobranza", List.of("Facturacion Doble", "Error de Facturacion"));
        PROBLEMAS_POR_TIPO.put("Atencion", List.of("Mal trato", "Demora"));
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
                    "No Funciona la camara", "02/12/2019", "8/12/2019", "13/12/2019",
                    "Su reclamo fue aceptado. El equipo pasa a reparacion.", "Aceptado");
            case 3 -> new Detalle("R004", "Celular", "Azumi", "Funcionamiento",
                    "No Funciona la camara", "05/12/2019", "-", "15/12/2019",
                    "Su reclamo esta en evaluacion por el area tecnica.", "Pendiente");
            case 1 -> new Detalle("R002", "Celular", "Motorola", "Funcionamiento",
                    "No Funciona la camara", "02/12/2019", "8/12/2019", "13/12/2019",
                    "Su equipo se encuentra daniado por lo que no cumple los requisitos "
                            + "de la garantia.", "Rechazado");
            default -> new Detalle("R001", "Celular", "Huawei", "Funcionamiento",
                    "No Funciona la camara", "02/12/2019", "8/12/2019", "13/12/2019",
                    "Su equipo se encuentra daniado por lo que no cumple los requisitos "
                            + "de la garantia.", "Rechazado");
        };
    }

    /* ---------------- consulta de indicadores ---------------- */

    public static final List<String> INDICADORES = List.of(
            "nivel de satisfaccion del cliente",
            "tiempo promedio de solucion por reclamo",
            "tipo de bienes mas reclamados",
            "areas con mayor demanda de reclamo",
            "numeros de reclamos por clase de producto",
            "costo por atencion de reclamo",
            "numero de rotacion de especialitas por reclamo",
            "% de reclamos en un mes",
            "% de reclamos criticos",
            "porcentaje de reclamos solucionados fuera de plazo",
            "eficiencia de reclamo por tipo de bien");
}
