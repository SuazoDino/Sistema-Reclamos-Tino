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
                        "Área Técnica de Reparación e Inspección", "Área Especializada",
                        "Gerencia de PostVenta"},
                new String[]{"PostVenta", "Ventas", "Logística",
                        "Área Técnica de Reparación e Inspección", "Área Especializada", "Operaciones",
                        "Gerencia de PostVenta"});

        cat("clientes", "Categorización de cliente",
                "Segmentos de cliente reconocidos por el sistema.",
                new String[]{"Nuevo", "Ordinario", "Vip", "SuperVip"},
                new String[]{"Nuevo", "Ordinario", "Vip", "SuperVip"});

        cat("consideraciones", "Consideraciones de categorización",
                "Criterios que alimentan el cálculo de categoría del cliente.",
                new String[]{"Zona de residencia", "Valor medio_compras",
                        "Segmento ingreso_prom", "Iteracion_compras"},
                new String[]{"Valor medio_compras", "Iteracion_compras"});

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

    public static final List<Producto> PRODUCTOS = new ArrayList<>(List.of(
            new Producto("PROD007", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Móvil", "LG", "G5"),
            new Producto("PROD012", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Computadores", "Computadores Notebook", "HP", "Omen"),
            new Producto("PROD019", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Monitores Y Pantallas De Computador", "Monitores De Pantalla Táctil (Touch)", "Samsung", "T27"),
            new Producto("PROD023", "Muebles, Mobiliario Y Decoración", "Muebles De Alojamiento", "Muebles", "Sofás", "Casa Bonita", "SF-3"),
            new Producto("PROD029", "Textiles", "Prendas de Vestir", "Ropa Superior", "Camisa", "Pima", "CM-102"),
            new Producto("PROD031", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Móvil", "Samsung", "Galaxy A54"),
            new Producto("PROD032", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Equipo Informático Y Accesorios", "Computadores", "Computadores Notebook", "HP", "Omen 16"),
            new Producto("PROD033", "Difusión De Tecnologías De Información Y Telecomunicaciones", "Dispositivos De Comunicaciones Y Accesorios", "Dispositivos De Comunicación Personal", "Teléfono Móvil", "Motorola", "Moto G84")
    ));

    /* ---------------- compras de los clientes ---------------- */

    private static Compra producto(String documento, String codigo, String comprobante,
                                   LocalDate fecha, double monto) {
        Producto p = PRODUCTOS.stream().filter(x -> x.id().equals(codigo)).findFirst().orElseThrow();
        return new Compra(documento, true, codigo, nombreProducto(codigo), p.bien(),
                comprobante, fecha, monto);
    }

    private static Compra servicio(String documento, String servicio, String comprobante,
                                   LocalDate fecha, double monto) {
        return new Compra(documento, false, servicio, servicio, servicio, comprobante, fecha, monto);
    }

    /**
     * Lo que cada cliente compro o contrato. Un reclamo solo procede sobre lo
     * que esta aqui (salvo los de Atencion), y de aqui salen el comprobante,
     * la fecha para la garantia y el precio que lee el motor de reglas.
     */
    public static final List<Compra> COMPRAS = List.of(
            producto("72119474", "PROD031", "B001-00025713", LocalDate.of(2026, 6, 15), 1299.00),
            servicio("72119474", "Delivery", "B001-00032847", LocalDate.of(2026, 9, 20), 25.00),
            producto("10112387203", "PROD032", "F001-00025592", LocalDate.of(2026, 3, 2), 6499.00),
            servicio("10112387203", "Instalación", "F001-00025593", LocalDate.of(2026, 3, 2), 150.00),
            producto("70775031", "PROD033", "B002-00004410", LocalDate.of(2026, 8, 30), 899.00),
            producto("20548712341", "PROD019", "F003-00000871", LocalDate.of(2025, 2, 10), 1850.00),
            producto("20548712341", "PROD012", "F003-00000872", LocalDate.of(2024, 5, 6), 4200.00)
    );

    public static List<Compra> comprasDe(String documento) {
        return COMPRAS.stream().filter(c -> c.documento().equals(documento)).toList();
    }

    /** La compra que sustenta reclamar sobre ese objeto, o null si no la hay. */
    public static Compra compra(String documento, boolean esProducto, String objeto) {
        return COMPRAS.stream()
                .filter(c -> c.documento().equals(documento) && c.esProducto() == esProducto
                        && c.objeto().equals(objeto))
                .findFirst().orElse(null);
    }

    /** Nombre legible de un producto del catalogo, o el codigo si no existe. */
    public static String nombreProducto(String id) {
        return PRODUCTOS.stream()
                .filter(p -> p.id().equals(id))
                .map(p -> p.bien() + " " + p.marca() + " " + p.modelo())
                .findFirst().orElse(id);
    }

    public static List<ReclamoPrototipo> reclamosDemo() {
        List<ReclamoPrototipo> l = new ArrayList<>();
        l.add(new ReclamoPrototipo("R001", LocalDate.of(2026, 8, 14), CLIENTES.get(0), "COMP007", PRODUCTOS.get(0),
                new Problema("Funcionamiento", "No enciende"), Canal.PRESENCIAL, EstadoReclamo.EN_COLA,
                "PostVenta", "E01", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Cliente indica que el equipo dejó de encender a los 3 días de la compra."));
        l.add(new ReclamoPrototipo("R002", LocalDate.of(2026, 7, 20), CLIENTES.get(1), "COMP003", PRODUCTOS.get(1),
                new Problema("Parte de producto", "Pantalla rayada"), Canal.INTERNET, EstadoReclamo.EN_ATENCION,
                "Área Técnica de Reparación e Inspección", "E02", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Rayadura visible en el borde inferior de la pantalla al desembalar."));
        l.add(new ReclamoPrototipo("R003", LocalDate.of(2026, 8, 17), CLIENTES.get(2), "COMP009", PRODUCTOS.get(2),
                new Problema("Cobro de producto", "Doble facturación"), Canal.TELEFONICO, EstadoReclamo.ENTREGADO,
                "Ventas", "E03", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Se emitieron dos comprobantes por la misma compra."));
        l.add(new ReclamoPrototipo("R004", LocalDate.of(2026, 6, 17), CLIENTES.get(3), "COMP011", PRODUCTOS.get(3),
                new Problema("Entrega de producto", "Empaque dañado"), Canal.WEB, EstadoReclamo.ENTREGADO,
                "Logística", "E01", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "Empaque llegó abierto y con golpes en la esquina superior."));
        l.add(new ReclamoPrototipo("R005", LocalDate.of(2026, 8, 24), CLIENTES.get(0), "COMP012", PRODUCTOS.get(4),
                new Problema("Instalación de producto", "Mala instalación"), Canal.PRESENCIAL, EstadoReclamo.ATENDIDO,
                "Operaciones", "E02", LocalDate.of(2026, 8, 27), LocalTime.of(17, 0),
                "El técnico dejó la instalación incompleta y sin probar el equipo."));
        l.add(new ReclamoPrototipo("R006", LocalDate.of(2026, 9, 2), CLIENTES.get(1), "COMP014", PRODUCTOS.get(0),
                new Problema("Funcionamiento", "No reconoce sim"), Canal.INTERNET, EstadoReclamo.EN_COLA,
                "Área Especializada", "E03", null, null,
                "El equipo no reconoce la tarjeta SIM de ningún operador."));
        l.add(new ReclamoPrototipo("R007", LocalDate.of(2026, 9, 9), CLIENTES.get(2), "COMP015", PRODUCTOS.get(1),
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
    public static final List<String> ACCIONES = List.of("Pago Adicional", "Intercambio", "Corrección",
            "Reembolso", "Reparación", "Volver a hacer", "Escalar a especialista");

    /* ---------------- seguridad (hoja SEGURIDAD) ---------------- */

    /** Los accesos que enumera la hoja, en su mismo orden. */
    public static final List<String> ACCESOS = List.of(
            "Parámetro General", "Catálogo de Reclamo", "Catálogo de Producto",
            "Catálogo de Servicios", "Catálogo de Problemas", "Catálogo de Cliente",
            "Catálogo de Protocolos", "Catálogo de Reglas", "Catálogo de Políticas",
            "Parámetros de Atención", "Consulta de Reclamos", "Consulta de Indicadores",
            "Área - Data Entry", "Área - Reportes", "Área - Revisión de Calidad", "Cliente - Data Entry",
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
                new Perfil("Supervisor de Calidad", List.of("Área - Revisión de Calidad",
                        "Consulta de Reclamos", "Consulta de Indicadores")),
                new Perfil("Gerente General", List.of("Parámetro General",
                        "Catálogo de Problemas", "Catálogo de Cliente",
                        "Consulta de Reclamos", "Consulta de Indicadores"))
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

}
