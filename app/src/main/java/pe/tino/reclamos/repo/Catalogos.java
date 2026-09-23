package pe.tino.reclamos.repo;

import pe.tino.reclamos.repo.Dominio.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Los catalogos que el sistema necesitaba y no tenia. Cada uno existe para
 * cerrar un dato que antes aparecia en pantalla sin origen o que se decidia
 * a mano dentro del codigo.
 */
public final class Catalogos {

    private Catalogos() {}

    /* ---------------- catalogo de servicios ---------------- */

    /**
     * Tipo de servicio | Servicio | Unidad de atencion | Estado.
     *
     * El sistema atendia solo productos, pero el Excel ya reclamaba sobre
     * servicios (delivery, cobro, limpieza, instalacion). Sin este catalogo
     * esos reclamos no tenian donde clasificarse.
     */
    public static List<String[]> servicios() {
        return new ArrayList<>(List.of(
                new String[]{"Logistico", "Delivery", "Entrega", "Habilitado"},
                new String[]{"Logistico", "Recojo en tienda", "Entrega", "Habilitado"},
                new String[]{"Comercial", "Cobro", "Transaccion", "Habilitado"},
                new String[]{"Comercial", "Facturacion", "Transaccion", "Habilitado"},
                new String[]{"Tecnico", "Instalacion", "Visita", "Habilitado"},
                new String[]{"Tecnico", "Mantenimiento", "Visita", "Habilitado"},
                new String[]{"Tecnico", "Limpieza", "Visita", "Habilitado"},
                new String[]{"Atencion", "Soporte telefonico", "Llamada", "Habilitado"},
                new String[]{"Atencion", "Asesoria en tienda", "Visita", "Deshabilitado"}
        ));
    }

    public static final List<String> TIPOS_SERVICIO =
            List.of("Logistico", "Comercial", "Tecnico", "Atencion");
    public static final List<String> UNIDADES_ATENCION =
            List.of("Entrega", "Transaccion", "Visita", "Llamada");

    /* ---------------- catalogo de canales ---------------- */

    /**
     * Canal | Horario | Requiere local | Estado.
     *
     * El canal se elegia en el Data Entry pero no estaba en ningun catalogo,
     * asi que no se podia habilitar ni deshabilitar.
     */
    public static List<String[]> canales() {
        return new ArrayList<>(List.of(
                new String[]{"Presencial", "Lunes a sabado 9:00 a 18:00", "Si", "Habilitado"},
                new String[]{"Telefonico", "Lunes a viernes 8:00 a 20:00", "No", "Habilitado"},
                new String[]{"Internet", "24 horas", "No", "Habilitado"},
                new String[]{"Web", "24 horas", "No", "Habilitado"}
        ));
    }

    /* ---------------- catalogo de instancias ---------------- */

    /**
     * Instancia | Quien resuelve | Dias de impugnacion | Admite impugnacion.
     *
     * De aqui sale la fecha limite de impugnacion que el prototipo mostraba
     * al cliente sin decir de donde salia.
     */
    public static List<String[]> instancias() {
        List<String[]> filas = new ArrayList<>();
        for (Instancia i : Instancia.values()) {
            filas.add(new String[]{i.etiqueta(), i.resuelve(),
                    String.valueOf(i.diasImpugnacion()),
                    i.admiteImpugnacion() ? "Si" : "No"});
        }
        return filas;
    }

    /* ---------------- catalogo de tiempos de atencion ---------------- */

    /**
     * Tipo de cliente | Prioridad | Tiempo | Unidad.
     *
     * Es el SLA: cuanto tiempo tiene el area para atender segun quien reclama
     * y que tan urgente es. Conecta la categorizacion del cliente con el
     * flujo, que antes era un modulo que no afectaba a nada.
     */
    public static List<String[]> tiemposAtencion() {
        return new ArrayList<>(List.of(
                new String[]{"SuperVip", "Alta", "4", "Horas"},
                new String[]{"SuperVip", "Media", "1", "Dias"},
                new String[]{"SuperVip", "Baja", "3", "Dias"},
                new String[]{"Vip", "Alta", "8", "Horas"},
                new String[]{"Vip", "Media", "2", "Dias"},
                new String[]{"Vip", "Baja", "5", "Dias"},
                new String[]{"Ordinario", "Alta", "1", "Dias"},
                new String[]{"Ordinario", "Media", "3", "Dias"},
                new String[]{"Ordinario", "Baja", "7", "Dias"},
                new String[]{"Nuevo", "Alta", "1", "Dias"},
                new String[]{"Nuevo", "Media", "4", "Dias"},
                new String[]{"Nuevo", "Baja", "10", "Dias"}
        ));
    }

    /** Horas de atencion que corresponden a un cliente con una prioridad. */
    public static long horasDeAtencion(String tipoCliente, Prioridad prioridad) {
        for (String[] f : tiemposAtencion()) {
            if (f[0].equalsIgnoreCase(tipoCliente) && f[1].equalsIgnoreCase(prioridad.etiqueta())) {
                long valor = Long.parseLong(f[2]);
                return switch (f[3]) {
                    case "Horas" -> valor;
                    case "Dias" -> valor * 24;
                    case "Semanas" -> valor * 24 * 7;
                    default -> valor;
                };
            }
        }
        return 72;   // si el cliente no esta categorizado, el plazo por defecto
    }

    /* ---------------- catalogo de prioridad ---------------- */

    /**
     * Tipo de problema | Tipo de cliente | Prioridad.
     *
     * Es la regla que decide la prioridad del ticket. Antes la criticidad se
     * cargaba a mano en el catalogo de reclamos y no se derivaba de nada.
     */
    public static List<String[]> reglasPrioridad() {
        return new ArrayList<>(List.of(
                new String[]{"Funcionamiento", "SuperVip", "Alta"},
                new String[]{"Funcionamiento", "Vip", "Alta"},
                new String[]{"Funcionamiento", "Ordinario", "Media"},
                new String[]{"Funcionamiento", "Nuevo", "Media"},
                new String[]{"Cobranza", "SuperVip", "Alta"},
                new String[]{"Cobranza", "Vip", "Alta"},
                new String[]{"Cobranza", "Ordinario", "Alta"},
                new String[]{"Cobranza", "Nuevo", "Alta"},
                new String[]{"Entrega", "SuperVip", "Alta"},
                new String[]{"Entrega", "Vip", "Media"},
                new String[]{"Entrega", "Ordinario", "Media"},
                new String[]{"Entrega", "Nuevo", "Baja"},
                new String[]{"Atencion", "SuperVip", "Media"},
                new String[]{"Atencion", "Vip", "Baja"},
                new String[]{"Atencion", "Ordinario", "Baja"},
                new String[]{"Atencion", "Nuevo", "Baja"}
        ));
    }

    /** Aplica la regla de prioridad; si no hay fila, el ticket queda en Media. */
    public static Prioridad prioridadDe(String tipoProblema, String tipoCliente) {
        for (String[] f : reglasPrioridad()) {
            if (f[0].equalsIgnoreCase(tipoProblema) && f[1].equalsIgnoreCase(tipoCliente)) {
                return switch (f[2]) {
                    case "Alta" -> Prioridad.ALTA;
                    case "Baja" -> Prioridad.BAJA;
                    default -> Prioridad.MEDIA;
                };
            }
        }
        return Prioridad.MEDIA;
    }

    /* ---------------- catalogo de estados ---------------- */

    /** Estado | Que significa | A que estados puede pasar. */
    public static List<String[]> estados() {
        List<String[]> filas = new ArrayList<>();
        for (Dominio.Estado e : Dominio.Estado.values()) {
            String destinos = e.siguientes().isEmpty() ? "(estado final)"
                    : String.join(", ", e.siguientes().stream()
                            .map(Dominio.Estado::etiqueta).toList());
            filas.add(new String[]{e.etiqueta(), e.descripcion(), destinos});
        }
        return filas;
    }

    /* ---------------- catalogo de marcas ---------------- */

    /**
     * Bien | Marca | Modelo.
     *
     * La marca se mostraba en la inspeccion y en el detalle del cliente pero
     * no existia en la jerarquia Segmento-Familia-Clase-Bien.
     */
    public static List<String[]> marcas() {
        return new ArrayList<>(List.of(
                new String[]{"Telefono Movil", "Samsung", "Galaxy A54"},
                new String[]{"Telefono Movil", "Huawei", "Nova 11"},
                new String[]{"Telefono Movil", "Motorola", "Moto G84"},
                new String[]{"Telefono Movil", "Azumi", "Nitro 5"},
                new String[]{"Computadores Notebook", "HP", "Omen 16"},
                new String[]{"Computadores Notebook", "Lenovo", "IdeaPad 3"},
                new String[]{"Monitores De Pantalla Tactil (Touch)", "Samsung", "T27"},
                new String[]{"Impresoras Laser", "HP", "LaserJet M111"}
        ));
    }
}
