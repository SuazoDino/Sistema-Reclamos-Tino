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

    /* ---------------- catalogo de problemas (hojas CatProblemas / TipoProblema) ---------------- */

    /** Segmento | Familia | Tipo de problema | Problema. */
    public static List<String[]> catalogoProblemas() {
        return new ArrayList<>(List.of(
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Danio", "Abolladura"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Danio", "Rayadura"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Falta de Componentes", "Falta cables"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Falta de Componentes", "Falta Control"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Mal funcionamiento", "No detecta senial digital"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Danio", "Golpe"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Danio", "Rotura"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Mal funcionamiento", "No detecta internet"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Falta de Componentes", "Falta cargador"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Cobranza", "Facturacion Doble"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Cobranza", "Error de Facturacion"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Cobranza", "Fraude"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Funcionalidad", "No funciona Camara"},
                new String[]{"Difusion De Tecnologias De Informacion Y Telecomunicaciones", "Equipo Informatico Y Accesorios", "Funcionalidad", "Bateria no Carga"},
                new String[]{"Muebles, Mobiliario Y Decoracion", "Muebles De Alojamiento", "Decoloracion", "Color asimetrico"}
        ));
    }

    public static final List<String> SEGMENTOS = List.of(
            "Difusion De Tecnologias De Informacion Y Telecomunicaciones",
            "Componentes Y Suministros Electronicos",
            "Muebles, Mobiliario Y Decoracion",
            "Equipo Medico, Accesorios Y Suministros",
            "Equipos De Oficina, Accesorios Y Suministros",
            "Textiles");

    public static final List<String> FAMILIAS = List.of(
            "Dispositivos De Comunicaciones Y Accesorios",
            "Dispositivo Semiconductor Discreto",
            "Equipo Informatico Y Accesorios",
            "Muebles De Alojamiento",
            "Prendas de Vestir");

    public static final List<String> TIPOS_PROBLEMA_CAT = List.of("Danio", "Falta de Componentes",
            "Mal funcionamiento", "Cobranza", "Funcionalidad", "Decoloracion", "Demora", "Entrega");

    /* ---------------- catalogo de politicas / garantias (hoja Hoja1) ---------------- */

    /** Codigo | Tipo de garantia | Politica | Plazo | Estado. */
    public static List<String[]> catalogoPoliticas() {
        return new ArrayList<>(List.of(
                new String[]{"G001", "Legal", "Identificarse al pagar con tarjeta", "12 Meses", "Habilitado"},
                new String[]{"G002", "Explicito", "Sistema hidraulico de frenos", "24 Meses", "Habilitado"},
                new String[]{"G003", "Explicito", "Contiene modulo a eleccion", "6 Meses", "Habilitado"},
                new String[]{"G004", "Explicito", "No posee garantia", "0 Dias", "Deshabilitado"},
                new String[]{"G005", "Implicito", "Ante danio de producto", "30 Dias", "Habilitado"},
                new String[]{"G006", "Implicito", "Ante fallo de funcionamiento", "90 Dias", "Habilitado"},
                new String[]{"G007", "Implicito", "Ante fallo de componente", "60 Dias", "Habilitado"},
                new String[]{"G008", "Explicito", "Posee reembolso", "15 Dias", "Habilitado"}
        ));
    }

    public static final List<String> TIPOS_GARANTIA = List.of("Legal", "Explicito", "Implicito");
    public static final List<String> PLAZOS = List.of("0 Dias", "15 Dias", "30 Dias", "60 Dias",
            "90 Dias", "6 Meses", "12 Meses", "24 Meses");

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
            "Nivel de satisfaccion del cliente",
            "Tiempo promedio de solucion por reclamo",
            "Tipo de bienes mas reclamados",
            "Areas con mayor demanda de reclamo",
            "Numero de reclamos por clase de producto",
            "Costo por atencion de reclamo",
            "Numero de rotacion de especialistas por reclamo",
            "Porcentaje de reclamos en un mes",
            "Porcentaje de reclamos criticos",
            "Porcentaje de reclamos solucionados fuera de plazo",
            "Eficiencia de reclamo por tipo de bien");

    /* ---------------- catalogo de reclamos y eventos ---------------- */

    /** Segmento | Tipo de problema | Estado (hojas ReclamosHab y ReclamosDes). */
    public static List<String[]> catalogoReclamos() {
        return new ArrayList<>(List.of(
                new String[]{"Equipos y suministros de Laboratorio y de Medicion", "Funcionalidad", "Habilitado"},
                new String[]{"Equipo Medico, accesorios y suministros", "Funcionalidad", "Habilitado"},
                new String[]{"Difusion de tecnologia de informacion y Telecomunicaciones", "Cobranza", "Habilitado"},
                new String[]{"Difusion de tecnologia de informacion y Telecomunicaciones", "Funcionalidad", "Habilitado"},
                new String[]{"Equipo Medico, accesorios y suministros", "Cobranza", "Deshabilitado"},
                new String[]{"Equipos de oficina, accesorios y Suministros", "Entrega", "Deshabilitado"},
                new String[]{"Difusion de tecnologia de informacion y Telecomunicaciones", "Atencion", "Deshabilitado"}
        ));
    }

    /** Los eventos del protocolo, con los valores que admite cada uno (hoja Protocolo). */
    public static List<String[]> catalogoEventos() {
        return new ArrayList<>(List.of(
                new String[]{"Inspeccion del problema", String.join(", ", INSPECCION)},
                new String[]{"Solucion del problema", String.join(", ", SOLUCION)},
                new String[]{"Reasignacion de area", String.join(", ", REASIGNACION)},
                new String[]{"Entrega del producto", String.join(", ", ENTREGA)},
                new String[]{"Seguimiento de la solucion", String.join(", ", SEGUIMIENTO)}
        ));
    }

    public static final List<String> SEGMENTOS_RECLAMO = List.of(
            "Equipos y suministros de Laboratorio y de Medicion",
            "Equipo Medico, accesorios y suministros",
            "Difusion de tecnologia de informacion y Telecomunicaciones",
            "Equipos de oficina, accesorios y Suministros");

    public static final List<String> TIPOS_PROBLEMA_RECLAMO = List.of(
            "Atencion", "Cobranza", "Entrega", "Funcionalidad", "Instalacion");
}
