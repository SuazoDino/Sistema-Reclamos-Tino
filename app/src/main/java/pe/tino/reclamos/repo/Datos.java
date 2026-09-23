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
        cat("areas", "Áreas de atención",
                "Áreas de la empresa que pueden recibir la asignación de un reclamo.",
                new String[]{"Producción", "Ventas", "Operaciones", "PostVenta", "Logística",
                        "Área Técnica de Reparación e Inspección", "Área Especializada"},
                new String[]{"PostVenta", "Ventas", "Logística",
                        "Área Técnica de Reparación e Inspección", "Área Especializada", "Operaciones"});

        cat("empleados", "Empleados especialistas",
                "Códigos de especialista disponibles para la asignación de reclamos.",
                new String[]{"E01", "E02", "E03", "E04", "E05", "E06"},
                new String[]{"E01", "E02", "E03"});

        cat("variables", "Variables de regla",
                "Variables que pueden intervenir en las condiciones del motor de reglas.",
                new String[]{"Tgarantia", "Ttranscurrido", "Precio", "TiempoMin"},
                new String[]{"Tgarantia", "Ttranscurrido"});

        cat("clientes", "Categorización de cliente",
                "Segmentos de cliente reconocidos por el sistema.",
                new String[]{"Nuevo", "Ordinario", "Vip", "Estándar", "SuperVip"},
                new String[]{"Vip", "Estándar", "Ordinario"});

        cat("ambito", "Ámbito de operación",
                "Alcance geográfico configurado para la organización.",
                new String[]{"Nacional", "MultiNacional", "InterNacional"},
                new String[]{"Nacional", "InterNacional"});

        cat("sectores", "Sectores",
                "Sectores geográficos habilitados para la atención de reclamos.",
                new String[]{"Lima", "Lima y Provincias", "Lima y Callao", "Provincias"},
                new String[]{"Lima", "Lima y Provincias"});

        cat("locales", "Locales",
                "Locales físicos donde se recibe y entrega producto.",
                new String[]{"Loc01", "Loc02", "Loc03", "Loc04", "Loc05"},
                new String[]{"Loc01", "Loc02", "Loc03"});

        cat("idiomas", "Idiomas",
                "Idiomas de atención soportados por los canales.",
                new String[]{"Español", "Inglés", "Quechua"},
                new String[]{"Español", "Inglés"});

        cat("consideraciones", "Consideraciones de categorización",
                "Criterios que alimentan el cálculo de categoría del cliente.",
                new String[]{"Zona de residencia", "Valor medio_compras",
                        "Segmento ingreso_prom", "Iteracion_compras"},
                new String[]{"Valor medio_compras", "Iteracion_compras"});

        cat("tiposProblema", "Tipos de problema",
                "Tipos de problema aceptados al registrar un reclamo.",
                new String[]{"Atención", "Cobranza", "Entrega", "Funcionalidad", "Instalación",
                        "Demora", "Daño", "Mal estado", "Instancia Duplicada", "Diseño Erróneo"},
                new String[]{"Funcionalidad", "Cobranza", "Entrega", "Atención", "Daño"});

        cat("unidadTiempo", "Unidades de tiempo",
                "Unidades usadas para los plazos de garantía y de solución.",
                new String[]{"Minutos", "Horas", "Días", "Semanas", "Meses"},
                new String[]{"Horas", "Días", "Semanas"});
    }

    public static Catalogo catalogo(String clave) { return CATALOGOS.get(clave); }
    public static Set<String> clavesCatalogo()    { return CATALOGOS.keySet(); }

    /* ---------------- jerarquia de producto (hoja Producto) ---------------- */

    /** Segmento > Familia > Clase > Bien, tal cual el catalogo UNSPSC del prototipo. */
    public static final List<String[]> JERARQUIA_PRODUCTO = List.of(
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Móvil"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Fijo"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Máquinas Contestadoras"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfonos Digitales"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Ip"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Computadores", "Servidores De Computador"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Computadores", "Computadores Notebook"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Computadores", "Computadores De Escritorio"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Accesorios De Computador", "Parlantes De Computador"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Monitores Y Pantallas De Computador", "Monitores De Pantalla Táctil (Touch)"},
            new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Impresoras De Computador", "Impresoras Laser"},
            new String[]{"Componentes Y Suministros Electrónicos", "Dispositivo Semiconductor Discreto", "Diodos", "Cinta De Ductos"},
            new String[]{"Componentes Y Suministros Electrónicos", "Dispositivo Semiconductor Discreto", "Diodos", "Cinta Aislante Eléctrica"},
            new String[]{"Muebles, Mobiliario Y Decoración", "Muebles De Alojamiento", "Muebles", "Stands"},
            new String[]{"Muebles, Mobiliario Y Decoración", "Muebles De Alojamiento", "Muebles", "Sofás"},
            new String[]{"Textiles", "Prendas de Vestir", "Ropa Superior", "Camisa"},
            new String[]{"Textiles", "Prendas de Vestir", "Ropa Superior", "Chaqueta"},
            new String[]{"Textiles", "Prendas de Vestir", "Ropa Inferior", "Pantalón"},
            new String[]{"Textiles", "Prendas de Vestir", "Accesorios", "Corbata"}
    );

    /* ---------------- problemas por tipo (hoja DATA_ENTRY_CLIENTE) ---------------- */

    private static final Map<String, List<String>> PROBLEMAS = new LinkedHashMap<>();
    static {
        PROBLEMAS.put("Atención", List.of("Mal trato", "Demora"));
        PROBLEMAS.put("Cobro de producto", List.of("Facturación de más", "Doble facturación"));
        PROBLEMAS.put("Entrega de producto", List.of("Mal trato del courier", "Demora",
                "Empaque no sellado", "Empaque dañado"));
        PROBLEMAS.put("Funcionamiento", List.of("No funciona cámara", "No capta señal",
                "No capta señal de red", "No enciende", "No reconoce sim", "El sensor no reconoce"));
        PROBLEMAS.put("Parte de producto", List.of("Pantalla rayada", "Carcasa con mal acabado",
                "Botón de volumen duro", "Entrada de cargador dañada"));
        PROBLEMAS.put("Instalación de producto", List.of("Mal trato", "Mala instalación",
                "Demora", "Material defectuoso", "Material equivocado", "Producto equivocado"));
    }

    public static List<String> tiposDeProblema()            { return new ArrayList<>(PROBLEMAS.keySet()); }
    public static List<String> problemasDe(String tipo)     { return PROBLEMAS.getOrDefault(tipo, List.of()); }

    /* ---------------- clientes, productos y reclamos de muestra ---------------- */

    public static final List<Cliente> CLIENTES = List.of(
            new Cliente("CLIEN001", "72119474", "Alexander", "Rodríguez Camacho",
                    "arodriguez@correo.pe", "Av. Los Álamos 123", "987654321",
                    TipoCliente.VIP, "Lima"),
            new Cliente("CLIEN002", "10112387203", "Michael Skinner", "Brito Calero",
                    "michaelbc.141@gmail.com", "Jr. Unión 456", "912345678",
                    TipoCliente.ORDINARIO, "Lima"),
            new Cliente("CLIEN003", "70775031", "Juan José", "Moreno Guadamur",
                    "jmoreno@correo.pe", "Av. Brasil 789", "956781234",
                    TipoCliente.NUEVO, "Lima y Provincias"),
            new Cliente("CLIEN004", "20548712341", "Corporación Andina", "SAC",
                    "contacto@andina.com.pe", "Av. Javier Prado 2020", "013456789",
                    TipoCliente.SUPER_VIP, "Lima y Provincias")
    );

    /** Busca al cliente por su documento; es el unico punto de validacion. */
    public static Cliente clientePorDocumento(String documento) {
        return CLIENTES.stream()
                .filter(c -> c.documento().equals(documento.trim()))
                .findFirst().orElse(null);
    }

    public static final List<Producto> PRODUCTOS = List.of(
            new Producto("PROD007", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Móvil", "LG", "G5"),
            new Producto("PROD012", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Computadores", "Computadores Notebook", "HP", "Omen"),
            new Producto("PROD019", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Monitores Y Pantallas De Computador", "Monitores De Pantalla Táctil (Touch)", "Samsung", "T27"),
            new Producto("PROD023", "Muebles, Mobiliario Y Decoración", "Muebles De Alojamiento", "Muebles", "Sofás", "Casa Bonita", "SF-3"),
            new Producto("PROD029", "Textiles", "Prendas de Vestir", "Ropa Superior", "Camisa", "Pima", "CM-102")
    );

    /** Nombre legible de un producto del catalogo, o el codigo si no existe. */
    public static String nombreProducto(String id) {
        return PRODUCTOS.stream()
                .filter(p -> p.id().equals(id))
                .map(p -> p.bien() + " " + p.marca() + " " + p.modelo())
                .findFirst().orElse(id);
    }

    public static List<Reclamo> reclamosDemo() {
        List<Reclamo> l = new ArrayList<>();
        l.add(new Reclamo("R001", LocalDate.of(2026, 8, 14), CLIENTES.get(0), "COMP007", PRODUCTOS.get(0),
                new Problema("Funcionamiento", "No enciende"), Canal.PRESENCIAL, EstadoReclamo.EN_COLA,
                "PostVenta", "E01", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Cliente indica que el equipo dejó de encender a los 3 días de la compra."));
        l.add(new Reclamo("R002", LocalDate.of(2026, 7, 20), CLIENTES.get(1), "COMP003", PRODUCTOS.get(1),
                new Problema("Parte de producto", "Pantalla rayada"), Canal.INTERNET, EstadoReclamo.EN_ATENCION,
                "Área Técnica de Reparación e Inspección", "E02", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Rayadura visible en el borde inferior de la pantalla al desembalar."));
        l.add(new Reclamo("R003", LocalDate.of(2026, 8, 17), CLIENTES.get(2), "COMP009", PRODUCTOS.get(2),
                new Problema("Cobro de producto", "Doble facturación"), Canal.TELEFONICO, EstadoReclamo.ENTREGADO,
                "Ventas", "E03", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Se emitieron dos comprobantes por la misma compra."));
        l.add(new Reclamo("R004", LocalDate.of(2026, 6, 17), CLIENTES.get(3), "COMP011", PRODUCTOS.get(3),
                new Problema("Entrega de producto", "Empaque dañado"), Canal.WEB, EstadoReclamo.ENTREGADO,
                "Logística", "E01", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Empaque llegó abierto y con golpes en la esquina superior."));
        l.add(new Reclamo("R005", LocalDate.of(2026, 8, 24), CLIENTES.get(0), "COMP012", PRODUCTOS.get(4),
                new Problema("Instalación de producto", "Mala instalación"), Canal.PRESENCIAL, EstadoReclamo.ATENDIDO,
                "Operaciones", "E02", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "El técnico dejó la instalación incompleta y sin probar el equipo."));
        l.add(new Reclamo("R006", LocalDate.of(2026, 9, 2), CLIENTES.get(1), "COMP014", PRODUCTOS.get(0),
                new Problema("Funcionamiento", "No reconoce sim"), Canal.INTERNET, EstadoReclamo.EN_COLA,
                "Área Especializada", "E03", null, null,
                "El equipo no reconoce la tarjeta SIM de ningún operador."));
        l.add(new Reclamo("R007", LocalDate.of(2026, 9, 9), CLIENTES.get(2), "COMP015", PRODUCTOS.get(1),
                new Problema("Atención", "Demora"), Canal.TELEFONICO, EstadoReclamo.RECHAZADO,
                "PostVenta", "E01", LocalDate.of(2026, 9, 12), LocalTime.of(10, 30),
                "Reclamo presentado fuera del plazo de garantía establecido."));
        return l;
    }

    /* ---------------- protocolos (hoja MantProtocolo) ---------------- */

    public static List<Protocolo> protocolos() {
        return new ArrayList<>(List.of(
                new Protocolo("EQUIPOS Y SUMINISTROS DE LABORATORIO Y DE MEDICIÓN - Funcionalidad", "Estándar", "Provisional", "Ninguna", "Presencial", "Personalizado", true),
                new Protocolo("EQUIPO MÉDICO, ACCESORIOS Y SUMINISTROS - Cobranza", "Exhaustiva", "Provisional", "Ninguna", "Presencial", "Estándar", true),
                new Protocolo("EQUIPOS Y SUMINISTROS DE LABORATORIO Y DE MEDICIÓN - Funcionalidad", "Ninguna", "Final", "Estándar", "Presencial", "Estándar", true),
                new Protocolo("EQUIPOS Y SUMINISTROS DE LABORATORIO Y DE MEDICIÓN - Funcionalidad", "Exhaustiva", "Provisional", "Estándar", "Delivery", "Estándar", false),
                new Protocolo("EQUIPO MÉDICO, ACCESORIOS Y SUMINISTROS - Cobranza", "Estándar", "Final", "Estándar", "Delivery", "Personalizado", false),
                new Protocolo("DIFUSIÓN DE TECNOLOGÍAS DE INFORMACIÓN Y TELECOMUNICACIONES - Cobranza", "Estándar", "Final", "Estándar", "Delivery", "Personalizado", true),
                new Protocolo("DIFUSIÓN DE TECNOLOGÍAS DE INFORMACIÓN Y TELECOMUNICACIONES - Funcionalidad", "Exhaustivo", "Final", "Frecuente", "Delivery", "Ninguna", true)
        ));
    }

    public static final List<String> INSPECCION   = List.of("Ninguna", "Estándar", "Exhaustiva");
    public static final List<String> SOLUCION     = List.of("Provisional", "Final");
    public static final List<String> REASIGNACION = List.of("Ninguna", "Única", "Frecuente", "Estándar");
    public static final List<String> ENTREGA      = List.of("Presencial", "Delivery", "Estándar", "Personalizado");
    public static final List<String> SEGUIMIENTO  = List.of("Ninguna", "Estándar", "Personalizado");

    /* ---------------- reglas (hoja MantReglas) ---------------- */

    public static List<Regla> reglas() {
        return new ArrayList<>(List.of(
                new Regla("CND1", "Tiempo_Garantia", "GT", "Entero", "Pago Adicional", "CND2", "Tgarantia > Ttranscurrido"),
                new Regla("CND2", "TiempoMin", "LT", "Entero", "Intercambio", "CND3", "Tiempo de atención bajo el mínimo pactado"),
                new Regla("CND3", "Reparable", "EQ", "Lógico", "Corrección", "CND4", "El bien admite reparación en taller"),
                new Regla("CND4", "Stock", "GT", "Entero", "Intercambio", "Reembolso", "Hay stock disponible para intercambio")
        ));
    }

    public static final List<String> OPERADORES = List.of("EQ", "NE", "GT", "GE", "LT", "LE");
    public static final List<String> TIPOS_VARIABLE = List.of("Entero", "Decimal", "Lógico", "Texto", "Fecha");
    public static final List<String> ACCIONES = List.of("Pago Adicional", "Intercambio", "Corrección",
            "Reembolso", "Reparación", "Volver a hacer", "Escalar a especialista");

    /* ---------------- catalogo de problemas (hojas CatProblemas / TipoProblema) ---------------- */

    /** Segmento | Familia | Tipo de problema | Problema. */
    public static List<String[]> catalogoProblemas() {
        return new ArrayList<>(List.of(
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Daño", "Abolladura"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Daño", "Rayadura"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Falta de Componentes", "Falta cables"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Falta de Componentes", "Falta Control"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Mal funcionamiento", "No detecta señal digital"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Daño", "Golpe"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Daño", "Rotura"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Mal funcionamiento", "No detecta internet"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Falta de Componentes", "Falta cargador"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Cobranza", "Facturación Doble"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Cobranza", "Error de Facturación"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Cobranza", "Fraude"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Funcionalidad", "No funciona Cámara"},
                new String[]{"Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Funcionalidad", "Batería no Carga"},
                new String[]{"Muebles, Mobiliario Y Decoración", "Muebles De Alojamiento", "Decoloración", "Color asimétrico"}
        ));
    }

    public static final List<String> SEGMENTOS = List.of(
            "Difusión De Tecnologías De Información Y Telecomunicaciones",
            "Componentes Y Suministros Electrónicos",
            "Muebles, Mobiliario Y Decoración",
            "Equipo Médico, Accesorios Y Suministros",
            "Equipos De Oficina, Accesorios Y Suministros",
            "Textiles");

    public static final List<String> FAMILIAS = List.of(
            "Dispositivos De Comunicaciones Y Accesorios",
            "Dispositivo Semiconductor Discreto",
            "Equipo Informático Y Accesorios",
            "Muebles De Alojamiento",
            "Prendas de Vestir");

    public static final List<String> TIPOS_PROBLEMA_CAT = List.of("Daño", "Falta de Componentes",
            "Mal funcionamiento", "Cobranza", "Funcionalidad", "Decoloración", "Demora", "Entrega");

    /* ---------------- catalogo de bienes (hoja MANT-PARAM, bloque tipo de bien) ---------------- */

    /** Tipo de bien | Tipo de problema que admite | Estado. */
    public static List<String[]> catalogoBienes() {
        return new ArrayList<>(List.of(
                new String[]{"Electrodomésticos", "Demora", "Habilitado"},
                new String[]{"Servicios Tangibles", "Instancia Duplicada", "Habilitado"},
                new String[]{"Comestibles", "Diseño Erróneo", "Habilitado"},
                new String[]{"Textiles", "Daño", "Habilitado"},
                new String[]{"Papel, Cartón e Impresos", "Mal estado", "Habilitado"},
                new String[]{"Químicos y Conexos", "Daño", "Habilitado"}
        ));
    }

    public static final List<String> TIPOS_BIEN = List.of("Electrodomésticos",
            "Servicios Tangibles", "Comestibles", "Textiles",
            "Papel, Cartón e Impresos", "Químicos y Conexos");

    public static final List<String> TIPOS_PROBLEMA_BIEN = List.of("Demora",
            "Instancia Duplicada", "Diseño Erróneo", "Daño", "Mal estado");

    /* ---------------- seguridad (hoja SEGURIDAD) ---------------- */

    /** Los accesos que enumera la hoja, en su mismo orden. */
    public static final List<String> ACCESOS = List.of(
            "Parámetro General", "Catálogo de Reclamo", "Catálogo de Producto",
            "Catálogo de Servicios", "Catálogo de Problemas", "Catálogo de Cliente",
            "Catálogo de Protocolos", "Catálogo de Reglas", "Catálogo de Políticas",
            "Parámetros de Atención", "Consulta de Tickets", "Consulta de Indicadores",
            "Área - Data Entry", "Área - Reportes", "Cliente - Data Entry",
            "Cliente - Reportes", "Seguridad", "Módulo BATCH");

    /** Los permisos que enumera la hoja. */
    public static final List<String> PERMISOS = List.of(
            "Agregar líneas para los bienes",
            "Agregar productos a las líneas");

    /**
     * Los perfiles de la hoja. Solo el perfil gerencial trae asignacion,
     * porque es el unico que la hoja documenta (columna MOD-PERFIL GERENCIAL);
     * los demas se asignan desde la pantalla.
     */
    public static List<Perfil> perfiles() {
        return new ArrayList<>(List.of(
                new Perfil("Administrador de BD", List.of()),
                new Perfil("Administrador de Seguridad", List.of()),
                new Perfil("Cliente", List.of()),
                new Perfil("Especialista", List.of()),
                new Perfil("Gerente General", List.of("Parámetro General",
                        "Catálogo de Problemas", "Catálogo de Cliente",
                        "Consulta de Tickets", "Consulta de Indicadores"))
        ));
    }

    /* ---------------- catalogo de politicas / garantias (hoja Hoja1) ---------------- */

    /** Codigo | Tipo de garantia | Politica | Plazo | Estado. */
    public static List<String[]> catalogoPoliticas() {
        return new ArrayList<>(List.of(
                new String[]{"G001", "Legal", "Identificarse al pagar con tarjeta", "12 Meses", "Habilitado"},
                new String[]{"G002", "Explícito", "Sistema hidráulico de frenos", "24 Meses", "Habilitado"},
                new String[]{"G003", "Explícito", "Contiene módulo a elección", "6 Meses", "Habilitado"},
                new String[]{"G004", "Explícito", "No posee garantía", "0 Días", "Deshabilitado"},
                new String[]{"G005", "Implícito", "Ante daño de producto", "30 Días", "Habilitado"},
                new String[]{"G006", "Implícito", "Ante fallo de funcionamiento", "90 Días", "Habilitado"},
                new String[]{"G007", "Implícito", "Ante fallo de componente", "60 Días", "Habilitado"},
                new String[]{"G008", "Explícito", "Posee reembolso", "15 Días", "Habilitado"}
        ));
    }

    public static final List<String> TIPOS_GARANTIA = List.of("Legal", "Explícito", "Implícito");
    public static final List<String> PLAZOS = List.of("0 Días", "15 Días", "30 Días", "60 Días",
            "90 Días", "6 Meses", "12 Meses", "24 Meses");

    /* ---------------- categorizacion de cliente (hoja MANT-PARAM) ---------------- */

    /** Condicion | Parametro | Operador | Variable | Accion V | Accion F. */
    public static List<String[]> condicionesCliente() {
        return new ArrayList<>(List.of(
                new String[]{"CND1", "RangMin", "<=", "NroVeces", "CND2", "No aplica"},
                new String[]{"CND2", "RangMax", ">", "NroVeces", "met1", "CND3"},
                new String[]{"CND3", "IngresoProm", ">=", "Monto", "met1", "No aplica"}
        ));
    }

    /** Codigo | Descripcion de la formula. */
    public static List<String[]> metodosCliente() {
        return new ArrayList<>(List.of(
                new String[]{"met1", "Iteraccion_compras = ValorItercCompra * PesoConsider"},
                new String[]{"met2", "Valor_medio = SumaCompras / NroVeces"}
        ));
    }

    /** Metodo | Variable | Secuencia | Operador. */
    public static List<String[]> formulasCliente() {
        return new ArrayList<>(List.of(
                new String[]{"met1", "ValorItercCompra", "1", "Inicializa"},
                new String[]{"met1", "PesoConsider", "2", "Multiplica"},
                new String[]{"met1", "Iteraccion_compras", "3", "Asigna"},
                new String[]{"met2", "SumaCompras", "1", "Inicializa"},
                new String[]{"met2", "NroVeces", "2", "Divide"},
                new String[]{"met2", "Valor_medio", "3", "Asigna"}
        ));
    }

    /* ---------------- indicadores (hoja MANT-PARAM) ---------------- */

    /** Los indicadores que enumera el prototipo, en su mismo orden. */
    public static final List<String> INDICADORES = List.of(
            "Nivel de satisfacción del cliente",
            "Tiempo promedio de solución por reclamo",
            "Tipo de bienes más reclamados",
            "Áreas con mayor demanda de reclamo",
            "Número de reclamos por clase de producto",
            "Costo por atención de reclamo",
            "Número de rotación de especialistas por reclamo",
            "Porcentaje de reclamos en un mes",
            "Porcentaje de reclamos críticos",
            "Porcentaje de reclamos solucionados fuera de plazo",
            "Eficiencia de reclamo por tipo de bien");

    /* ---------------- catalogo de reclamos y eventos ---------------- */

    /** Segmento | Tipo de problema | Estado (hojas ReclamosHab y ReclamosDes). */
    public static List<String[]> catalogoReclamos() {
        return new ArrayList<>(List.of(
                new String[]{"Equipos y suministros de Laboratorio y de Medición", "Funcionalidad", "Habilitado"},
                new String[]{"Equipo Médico, accesorios y suministros", "Funcionalidad", "Habilitado"},
                new String[]{"Difusión de tecnología de información y Telecomunicaciones", "Cobranza", "Habilitado"},
                new String[]{"Difusión de tecnología de información y Telecomunicaciones", "Funcionalidad", "Habilitado"},
                new String[]{"Equipo Médico, accesorios y suministros", "Cobranza", "Deshabilitado"},
                new String[]{"Equipos de oficina, accesorios y Suministros", "Entrega", "Deshabilitado"},
                new String[]{"Difusión de tecnología de información y Telecomunicaciones", "Atención", "Deshabilitado"}
        ));
    }

    /** Los eventos del protocolo, con los valores que admite cada uno (hoja Protocolo). */
    public static List<String[]> catalogoEventos() {
        return new ArrayList<>(List.of(
                new String[]{"Inspección del problema", String.join(", ", INSPECCION)},
                new String[]{"Solución del problema", String.join(", ", SOLUCION)},
                new String[]{"Reasignación de área", String.join(", ", REASIGNACION)},
                new String[]{"Entrega del producto", String.join(", ", ENTREGA)},
                new String[]{"Seguimiento de la solución", String.join(", ", SEGUIMIENTO)}
        ));
    }

    public static final List<String> SEGMENTOS_RECLAMO = List.of(
            "Equipos y suministros de Laboratorio y de Medición",
            "Equipo Médico, accesorios y suministros",
            "Difusión de tecnología de información y Telecomunicaciones",
            "Equipos de oficina, accesorios y Suministros");

    public static final List<String> TIPOS_PROBLEMA_RECLAMO = List.of(
            "Atención", "Cobranza", "Entrega", "Funcionalidad", "Instalación");
}
