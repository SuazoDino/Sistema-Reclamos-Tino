package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Modelo.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Repositorio en memoria. Los valores salen de los prototipos en
 * TinoPrototipos_menu_con_iconos.xlsm, hoja por hoja, para que las pantallas
 * muestren el mismo vocabulario que el diseno externo aprobado.
 */
public final class Datos {

    private Datos() {}

    /* ---------------- catalogos simples (hojas *Exis / *Hab) ---------------- */

    public record Catalogo(String titulo, String descripcion,
                           List<String> existentes, List<String> habilitados) {}

    private static final Map<String, Catalogo> CATALOGOS = new LinkedHashMap<>();

    private static void cat(String clave, String titulo, String desc, String[] existentes, String[] habilitados) {
        CATALOGOS.put(clave, new Catalogo(titulo, desc,
                new ArrayList<>(List.of(existentes)), new ArrayList<>(List.of(habilitados))));
    }

    static {
        cat("areas", "Areas de atencion",
                "Areas de la empresa que pueden recibir la asignacion de un reclamo.",
                new String[]{"Produccion", "Ventas", "Operaciones", "PostVenta", "Logistica",
                        "Area Tecnica de Reparacion e Inspeccion", "Area Especializada"},
                new String[]{"PostVenta", "Ventas", "Logistica",
                        "Area Tecnica de Reparacion e Inspeccion", "Area Especializada", "Operaciones"});

        cat("empleados", "Empleados especialistas",
                "Codigos de especialista disponibles para la asignacion de reclamos.",
                new String[]{"E01", "E02", "E03", "E04", "E05", "E06"},
                new String[]{"E01", "E02", "E03"});

        cat("variables", "Variables de regla",
                "Variables que pueden intervenir en las condiciones del motor de reglas.",
                new String[]{"Tgarantia", "Ttranscurrido", "Precio", "TiempoMin"},
                new String[]{"Tgarantia", "Ttranscurrido"});

        cat("clientes", "Categorizacion de cliente",
                "Segmentos de cliente reconocidos por el sistema.",
                new String[]{"Nuevo", "Ordinario", "Vip", "Estandar", "SuperVip"},
                new String[]{"Vip", "Estandar", "Ordinario"});

        cat("ambito", "Ambito de operacion",
                "Alcance geografico configurado para la organizacion.",
                new String[]{"Nacional", "MultiNacional", "InterNacional"},
                new String[]{"Nacional", "InterNacional"});

        cat("sectores", "Sectores",
                "Sectores geograficos habilitados para la atencion de reclamos.",
                new String[]{"Lima", "Lima y Provincias", "Lima y Callao", "Provincias"},
                new String[]{"Lima", "Lima y Provincias"});

        cat("locales", "Locales",
                "Locales fisicos donde se recibe y entrega producto.",
                new String[]{"Loc01", "Loc02", "Loc03", "Loc04", "Loc05"},
                new String[]{"Loc01", "Loc02", "Loc03"});

        cat("idiomas", "Idiomas",
                "Idiomas de atencion soportados por los canales.",
                new String[]{"Espanol", "Ingles", "Quechua"},
                new String[]{"Espanol", "Ingles"});

        cat("consideraciones", "Consideraciones de categorizacion",
                "Criterios que alimentan el calculo de categoria del cliente.",
                new String[]{"Zona de residencia", "Valor medio_compras",
                        "Segmento ingreso_prom", "Iteracion_compras"},
                new String[]{"Valor medio_compras", "Iteracion_compras"});

        cat("tiposProblema", "Tipos de problema",
                "Tipos de problema aceptados al registrar un reclamo.",
                new String[]{"Atencion", "Cobranza", "Entrega", "Funcionalidad", "Instalacion",
                        "Demora", "Danio", "Mal estado", "Instancia Duplicada", "Disenio Erroneo"},
                new String[]{"Funcionalidad", "Cobranza", "Entrega", "Atencion", "Danio"});

        cat("unidadTiempo", "Unidades de tiempo",
                "Unidades usadas para los plazos de garantia y de solucion.",
                new String[]{"Minutos", "Horas", "Dias", "Semanas", "Meses"},
                new String[]{"Horas", "Dias", "Semanas"});
    }

    public static Catalogo catalogo(String clave) { return CATALOGOS.get(clave); }
    public static Set<String> clavesCatalogo()    { return CATALOGOS.keySet(); }

    /* ---------------- jerarquia de producto (hoja Producto) ---------------- */

    /** Segmento > Familia > Clase > Bien, tal cual el catalogo UNSPSC del prototipo. */
    public static final List<String[]> JERARQUIA_PRODUCTO = List.of(
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicacion Personal", "Telefono Movil"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicacion Personal", "Telefono Fijo"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicacion Personal", "Maquinas Contestadoras"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicacion Personal", "Telefonos Digitales"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicacion Personal", "Telefono Ip"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Computadores", "Servidores De Computador"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Computadores", "Computadores Notebook"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Computadores", "Computadores De Escritorio"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Accesorios De Computador", "Parlantes De Computador"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Monitores Y Pantallas De Computador", "Monitores De Pantalla Tactil (Touch)"},
            new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Impresoras De Computador", "Impresoras Laser"},
            new String[]{"Componentes Y Suministros Electronicos", "Dispositivo Semiconductor Discreto", "Diodos", "Cinta De Ductos"},
            new String[]{"Componentes Y Suministros Electronicos", "Dispositivo Semiconductor Discreto", "Diodos", "Cinta Aislante Electrica"},
            new String[]{"Muebles, Mobiliario Y Decoracion", "Muebles De Alojamiento", "Muebles", "Stands"},
            new String[]{"Muebles, Mobiliario Y Decoracion", "Muebles De Alojamiento", "Muebles", "Sofas"},
            new String[]{"Textiles", "Prendas de Vestir", "Ropa Superior", "Camisa"},
            new String[]{"Textiles", "Prendas de Vestir", "Ropa Superior", "Chaqueta"},
            new String[]{"Textiles", "Prendas de Vestir", "Ropa Inferior", "Pantalon"},
            new String[]{"Textiles", "Prendas de Vestir", "Accesorios", "Corbata"}
    );

    /* ---------------- problemas por tipo (hoja DATA_ENTRY_CLIENTE) ---------------- */

    private static final Map<String, List<String>> PROBLEMAS = new LinkedHashMap<>();
    static {
        PROBLEMAS.put("Atencion", List.of("Mal trato", "Demora"));
        PROBLEMAS.put("Cobro de producto", List.of("Facturacion demas", "Doble facturacion"));
        PROBLEMAS.put("Entrega de producto", List.of("Mal trato del curier", "Demora",
                "Empaque no sellado", "Empaque daniado"));
        PROBLEMAS.put("Funcionamiento", List.of("No funciona camara", "No capta senial",
                "No capta senial de red", "No enciende", "No reconoce sim", "El sensor no reconoce"));
        PROBLEMAS.put("Parte de producto", List.of("Pantalla rayada", "Carcasa con mal acabado",
                "Boton de volumen duro", "Entrada de cargador daniada"));
        PROBLEMAS.put("Instalacion de producto", List.of("Mal trato", "Mala instalacion",
                "Demora", "Material defectuoso", "Material equivocado", "Producto equivocado"));
    }

    public static List<String> tiposDeProblema()            { return new ArrayList<>(PROBLEMAS.keySet()); }
    public static List<String> problemasDe(String tipo)     { return PROBLEMAS.getOrDefault(tipo, List.of()); }

    /* ---------------- clientes, productos y reclamos de muestra ---------------- */

    public static final List<Cliente> CLIENTES = List.of(
            new Cliente("CLIEN001", "10112387203", "Michael Skinner Brito Calero", "michaelbc.141@gmail.com", TipoCliente.VIP, "Lima"),
            new Cliente("CLIEN002", "70775031", "Juan Jose Moreno Guadamur", "jmoreno@correo.pe", TipoCliente.ORDINARIO, "Lima y Provincias"),
            new Cliente("CLIEN003", "76729288", "Ana Lucia Ramirez Soto", "aramirez@correo.pe", TipoCliente.NUEVO, "Lima"),
            new Cliente("CLIEN004", "20548712341", "Corporacion Andina SAC", "contacto@andina.com.pe", TipoCliente.SUPER_VIP, "Lima y Provincias")
    );

    public static final List<Producto> PRODUCTOS = List.of(
            new Producto("PROD007", "Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicacion Personal", "Telefono Movil", "LG", "G5"),
            new Producto("PROD012", "Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Computadores", "Computadores Notebook", "HP", "Omen"),
            new Producto("PROD019", "Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Monitores Y Pantallas De Computador", "Monitores De Pantalla Tactil (Touch)", "Samsung", "T27"),
            new Producto("PROD023", "Muebles, Mobiliario Y Decoracion", "Muebles De Alojamiento", "Muebles", "Sofas", "Casa Bonita", "SF-3"),
            new Producto("PROD029", "Textiles", "Prendas de Vestir", "Ropa Superior", "Camisa", "Pima", "CM-102")
    );

    public static List<Reclamo> reclamosDemo() {
        List<Reclamo> l = new ArrayList<>();
        l.add(new Reclamo("R001", LocalDate.of(2026, 8, 14), CLIENTES.get(0), "COMP007", PRODUCTOS.get(0),
                new Problema("Funcionamiento", "No enciende"), Canal.PRESENCIAL, EstadoReclamo.EN_COLA,
                "PostVenta", "E01", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Cliente indica que el equipo dejo de encender a los 3 dias de la compra."));
        l.add(new Reclamo("R002", LocalDate.of(2026, 7, 20), CLIENTES.get(1), "COMP003", PRODUCTOS.get(1),
                new Problema("Parte de producto", "Pantalla rayada"), Canal.INTERNET, EstadoReclamo.EN_ATENCION,
                "Area Tecnica de Reparacion e Inspeccion", "E02", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Rayadura visible en el borde inferior de la pantalla al desembalar."));
        l.add(new Reclamo("R003", LocalDate.of(2026, 8, 17), CLIENTES.get(2), "COMP009", PRODUCTOS.get(2),
                new Problema("Cobro de producto", "Doble facturacion"), Canal.TELEFONICO, EstadoReclamo.ENTREGADO,
                "Ventas", "E03", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Se emitieron dos comprobantes por la misma compra."));
        l.add(new Reclamo("R004", LocalDate.of(2026, 6, 17), CLIENTES.get(3), "COMP011", PRODUCTOS.get(3),
                new Problema("Entrega de producto", "Empaque daniado"), Canal.WEB, EstadoReclamo.ENTREGADO,
                "Logistica", "E01", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Empaque llego abierto y con golpes en la esquina superior."));
        l.add(new Reclamo("R005", LocalDate.of(2026, 8, 24), CLIENTES.get(0), "COMP012", PRODUCTOS.get(4),
                new Problema("Instalacion de producto", "Mala instalacion"), Canal.PRESENCIAL, EstadoReclamo.ATENDIDO,
                "Operaciones", "E02", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "El tecnico dejo la instalacion incompleta y sin probar el equipo."));
        l.add(new Reclamo("R006", LocalDate.of(2026, 9, 2), CLIENTES.get(1), "COMP014", PRODUCTOS.get(0),
                new Problema("Funcionamiento", "No reconoce sim"), Canal.INTERNET, EstadoReclamo.EN_COLA,
                "Area Especializada", "E03", null, null,
                "El equipo no reconoce la tarjeta SIM de ningun operador."));
        l.add(new Reclamo("R007", LocalDate.of(2026, 9, 9), CLIENTES.get(2), "COMP015", PRODUCTOS.get(1),
                new Problema("Atencion", "Demora"), Canal.TELEFONICO, EstadoReclamo.RECHAZADO,
                "PostVenta", "E01", LocalDate.of(2026, 9, 12), LocalTime.of(10, 30),
                "Reclamo presentado fuera del plazo de garantia establecido."));
        return l;
    }

    /* ---------------- protocolos (hoja MantProtocolo) ---------------- */

    public static List<Protocolo> protocolos() {
        return new ArrayList<>(List.of(
                new Protocolo("EQUIPOS Y SUMINISTROS DE LABORATORIO Y DE MEDICION - Funcionalidad", "Estandar", "Provisional", "Ninguna", "Presencial", "Personalizado", true),
                new Protocolo("EQUIPO MEDICO, ACCESORIOS Y SUMINISTROS - Cobranza", "Exhaustiva", "Provisional", "Ninguna", "Presencial", "Estandar", true),
                new Protocolo("EQUIPOS Y SUMINISTROS DE LABORATORIO Y DE MEDICION - Funcionalidad", "Ninguna", "Final", "Estandar", "Presencial", "Estandar", true),
                new Protocolo("EQUIPOS Y SUMINISTROS DE LABORATORIO Y DE MEDICION - Funcionalidad", "Exhaustiva", "Provisional", "Estandar", "Delivery", "Estandar", false),
                new Protocolo("EQUIPO MEDICO, ACCESORIOS Y SUMINISTROS - Cobranza", "Estandar", "Final", "Estandar", "Delivery", "Personalizado", false),
                new Protocolo("DIFUSION DE TECNOLOGIAS DE INFORMACION Y TELECOMUNICACIONES - Cobranza", "Estandar", "Final", "Estandar", "Delivery", "Personalizado", true),
                new Protocolo("DIFUSION DE TECNOLOGIAS DE INFORMACION Y TELECOMUNICACIONES - Funcionalidad", "Exhaustivo", "Final", "Frecuente", "Delivery", "Ninguna", true)
        ));
    }

    public static final List<String> INSPECCION   = List.of("Ninguna", "Estandar", "Exhaustiva");
    public static final List<String> SOLUCION     = List.of("Provisional", "Final");
    public static final List<String> REASIGNACION = List.of("Ninguna", "Unica", "Frecuente", "Estandar");
    public static final List<String> ENTREGA      = List.of("Presencial", "Delivery", "Estandar", "Personalizado");
    public static final List<String> SEGUIMIENTO  = List.of("Ninguna", "Estandar", "Personalizado");

    /* ---------------- reglas (hoja MantReglas) ---------------- */

    public static List<Regla> reglas() {
        return new ArrayList<>(List.of(
                new Regla("CND1", "Tiempo_Garantia", "GT", "Entero", "Pago Adicional", "CND2", "Tgarantia > Ttranscurrido"),
                new Regla("CND2", "TiempoMin", "LT", "Entero", "Intercambio", "CND3", "Tiempo de atencion bajo el minimo pactado"),
                new Regla("CND3", "Reparable", "EQ", "Logico", "Correccion", "CND4", "El bien admite reparacion en taller"),
                new Regla("CND4", "Stock", "GT", "Entero", "Intercambio", "Reembolso", "Hay stock disponible para intercambio")
        ));
    }

    public static final List<String> OPERADORES = List.of("EQ", "NE", "GT", "GE", "LT", "LE");
    public static final List<String> TIPOS_VARIABLE = List.of("Entero", "Decimal", "Logico", "Texto", "Fecha");
    public static final List<String> ACCIONES = List.of("Pago Adicional", "Intercambio", "Correccion",
            "Reembolso", "Reparacion", "Volver a hacer", "Escalar a especialista");

    /* ---------------- seguridad (hoja SEGURIDAD) ---------------- */

    public static final List<String> ACCESOS = List.of(
            "Catalogo de Productos", "Catalogo de Bienes", "Catalogo de Politicas",
            "Catalogo de Problemas", "Catalogo de Protocolos", "Catalogo de Reglas",
            "Seguridad", "Estadisticas", "Reportes", "General", "Categorizacion Cliente");

    public static List<Perfil> perfiles() {
        return new ArrayList<>(List.of(
                new Perfil("Administrador de BD", List.of("General", "Seguridad", "Reportes",
                        "Catalogo de Productos", "Catalogo de Bienes", "Catalogo de Problemas",
                        "Catalogo de Protocolos", "Catalogo de Reglas", "Catalogo de Politicas",
                        "Estadisticas", "Categorizacion Cliente")),
                new Perfil("Administrador de Seguridad", List.of("Seguridad", "General", "Reportes")),
                new Perfil("Gerente General", List.of("General", "Estadisticas", "Reportes",
                        "Categorizacion Cliente")),
                new Perfil("Especialista", List.of("General", "Catalogo de Problemas",
                        "Catalogo de Protocolos", "Reportes")),
                new Perfil("Cliente", List.of("General"))
        ));
    }

    /* ---------------- indicadores (hoja MANT-PARAM) ---------------- */

    public static List<Indicador> indicadores() {
        return List.of(
                new Indicador("Nivel de satisfaccion del cliente", "87.4", "%", 2.1, true),
                new Indicador("Tiempo promedio de solucion por reclamo", "3.6", "dias", -0.4, false),
                new Indicador("Reclamos en el mes", "142", "reclamos", 11.0, false),
                new Indicador("Reclamos criticos", "9.2", "%", -1.3, false),
                new Indicador("Solucionados fuera de plazo", "6.8", "%", 0.9, false),
                new Indicador("Costo por atencion de reclamo", "S/ 42.10", "por reclamo", -3.2, false),
                new Indicador("Rotacion de especialistas por reclamo", "1.4", "asignaciones", 0.2, false),
                new Indicador("Eficiencia de reclamo por tipo de bien", "78.5", "%", 1.6, true)
        );
    }

    /** Reclamos agrupados por area, para el grafico del tablero. */
    public static Map<String, Integer> reclamosPorArea() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("PostVenta", 46);
        m.put("Area Tecnica", 33);
        m.put("Logistica", 24);
        m.put("Ventas", 19);
        m.put("Operaciones", 12);
        m.put("Area Especializada", 8);
        return m;
    }

    /** Reclamos por tipo de bien, para el tablero. */
    public static Map<String, Integer> reclamosPorTipoBien() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Equipo informatico", 51);
        m.put("Dispositivos de comunicacion", 38);
        m.put("Muebles y decoracion", 22);
        m.put("Textiles", 17);
        m.put("Componentes electronicos", 14);
        return m;
    }
}
