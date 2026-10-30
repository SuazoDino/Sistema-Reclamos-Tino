package pe.tino.reclamos.repo;

import pe.tino.reclamos.repo.Dominio.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Los catalogos que el sistema necesitaba y no tenia. Cada uno existe para
 * cerrar un dato que antes aparecia en pantalla sin origen o que se decidia
 * a mano dentro del codigo.
 *
 * Los catalogos que se editan en pantalla (servicios, canales, locales) son
 * una sola lista compartida: si el gerente deshabilita un servicio, el
 * formulario del cliente lo deja de ofrecer sin reiniciar.
 */
public final class Catalogos {

    private Catalogos() {}

    /* ---------------- catalogo de servicios ---------------- */

    /**
     * Tipo de servicio | Servicio | Tipos de problema que admite | Estado.
     *
     * El sistema atendia solo productos, pero el Excel ya reclamaba sobre
     * servicios (delivery, cobro, limpieza, instalacion). Los tipos de problema
     * que admite cada servicio evitan combinaciones sin sentido, como "La
     * camara no funciona" sobre un recojo en tienda.
     */
    public static List<String[]> servicios() { return SERVICIOS; }

    private static final List<String[]> SERVICIOS = new ArrayList<>(List.of(
                new String[]{"Logístico", "Delivery", "Entrega, Atención", "Habilitado"},
                new String[]{"Logístico", "Recojo en tienda", "Entrega, Atención", "Habilitado"},
                new String[]{"Comercial", "Cobro", "Cobranza, Atención", "Habilitado"},
                new String[]{"Comercial", "Facturación", "Cobranza, Atención", "Habilitado"},
                new String[]{"Técnico", "Instalación", "Funcionamiento, Atención", "Habilitado"},
                new String[]{"Técnico", "Mantenimiento", "Funcionamiento, Atención", "Habilitado"},
                new String[]{"Técnico", "Limpieza", "Atención", "Habilitado"},
                new String[]{"Atención", "Soporte telefónico", "Atención", "Habilitado"},
                new String[]{"Atención", "Asesoría en tienda", "Atención", "Deshabilitado"}
        ));

    /** Los servicios que el formulario puede ofrecer. */
    public static List<String> serviciosHabilitados() {
        return SERVICIOS.stream().filter(s -> "Habilitado".equals(s[3])).map(s -> s[1]).toList();
    }

    /** Los tipos de problema que admite un servicio. Atencion vale para todos. */
    public static List<String> tiposDeProblemaDeServicio(String servicio) {
        return SERVICIOS.stream().filter(s -> s[1].equals(servicio))
                .map(s -> List.of(s[2].split(",\\s*"))).findFirst().orElse(List.of("Atención"));
    }

    public static final List<String> TIPOS_SERVICIO =
            List.of("Logístico", "Comercial", "Técnico", "Atención");

    /* ---------------- catalogo de canales ---------------- */

    /**
     * Canal | Horario | Requiere local | Estado.
     *
     * El canal se elegia en el Data Entry pero no estaba en ningun catalogo,
     * asi que no se podia habilitar ni deshabilitar.
     */
    public static List<String[]> canales() { return CANALES; }

    private static final List<String[]> CANALES = new ArrayList<>(List.of(
                new String[]{"Presencial", "Lunes a sábado 9:00 a 18:00", "Sí", "Habilitado"},
                new String[]{"Telefónico", "Lunes a viernes 8:00 a 20:00", "No", "Habilitado"},
                new String[]{"Internet", "24 horas", "No", "Habilitado"},
                new String[]{"Web", "24 horas", "No", "Habilitado"}
        ));

    public static boolean canalHabilitado(Canal canal) {
        return CANALES.stream().anyMatch(c -> c[0].equals(canal.etiqueta()) && "Habilitado".equals(c[3]));
    }

    /* ---------------- catalogo de locales ---------------- */

    /** Local | Direccion | Estado. Antes venian fijos del codigo. */
    public static List<String[]> locales() { return LOCALES; }

    private static final List<String[]> LOCALES = new ArrayList<>(List.of(
            new String[]{"Loc01", "Av. La Marina 2355, San Miguel", "Habilitado"},
            new String[]{"Loc02", "Av. Javier Prado Este 4200, Surco", "Habilitado"},
            new String[]{"Loc03", "Av. Alfredo Mendiola 3698, Independencia", "Habilitado"},
            new String[]{"Loc04", "Av. Primavera 1050, San Borja", "Deshabilitado"}
    ));

    public static List<String> localesHabilitados() {
        return LOCALES.stream().filter(l -> "Habilitado".equals(l[2])).map(l -> l[0]).toList();
    }

    /* ---------------- area responsable ---------------- */

    /**
     * Tipo de problema | Area responsable. Es lo que permite asignar sin que
     * nadie elija: el tipo de problema decide el area.
     */
    private static final List<String[]> AREAS_POR_TIPO = new ArrayList<>(List.of(
            new String[]{"Funcionamiento", "Área Técnica de Reparación e Inspección"},
            new String[]{"Entrega", "Logística"},
            new String[]{"Cobranza", "Ventas"},
            new String[]{"Atención", "PostVenta"}
    ));

    public static List<String[]> areasPorTipo() { return AREAS_POR_TIPO; }

    /** Quien usa un area: un tipo de problema, una instancia o sus especialistas; "-" si nadie. */
    public static String usoDeArea(String area) {
        List<String> usos = new ArrayList<>();
        AREAS_POR_TIPO.stream().filter(f -> f[1].equals(area)).forEach(f -> usos.add("el tipo " + f[0]));
        for (Instancia i : Instancia.values()) {
            if (i != Instancia.PRIMERA && i.resuelve().equals(area)) usos.add("la " + i.etiqueta().toLowerCase() + " instancia");
        }
        long empleados = Prototipo.empleadosDe(area).size();
        if (empleados > 0) usos.add(empleados + (empleados == 1 ? " especialista" : " especialistas"));
        return usos.isEmpty() ? "-" : String.join(", ", usos);
    }

    /**
     * Habilita o deshabilita un area. Un area en uso no se deshabilita: dejaria
     * reclamos sin quien los atienda.
     */
    public static void habilitarArea(String area, boolean si) {
        var cat = Datos.catalogo("areas");
        if (!cat.existentes().contains(area)) throw new IllegalArgumentException("No existe el área " + area + ".");
        if (si) { if (!cat.habilitados().contains(area)) cat.habilitados().add(area); return; }
        String uso = usoDeArea(area);
        if (!uso.equals("-")) throw new IllegalArgumentException("No se puede deshabilitar " + area + ": la usa " + uso + ".");
        cat.habilitados().remove(area);
    }

    /**
     * El area que atiende un reclamo: en primera instancia la del tipo de
     * problema; despues, la que resuelve la instancia.
     */
    public static String areaDe(String tipoProblema, Instancia instancia) {
        if (instancia != Instancia.PRIMERA) return instancia.resuelve();
        return AREAS_POR_TIPO.stream().filter(f -> f[0].equalsIgnoreCase(tipoProblema))
                .map(f -> f[1]).findFirst().orElse("PostVenta");
    }

    /* ---------------- horario laboral y feriados ---------------- */

    /** Lunes a sabado; la hora de inicio y fin es un parametro (Generalidades del sistema). */
    public static LocalTime inicioJornada() { return Parametros.inicioJornada(); }
    public static LocalTime finJornada()    { return Parametros.finJornada(); }

    /** Feriados nacionales que caen en la misma fecha cada anio. */
    private static final List<MonthDay> FERIADOS_FIJOS = List.of(
            MonthDay.of(1, 1), MonthDay.of(5, 1), MonthDay.of(6, 7), MonthDay.of(6, 29),
            MonthDay.of(7, 23), MonthDay.of(7, 28), MonthDay.of(7, 29), MonthDay.of(8, 6),
            MonthDay.of(8, 30), MonthDay.of(10, 8), MonthDay.of(11, 1), MonthDay.of(12, 8),
            MonthDay.of(12, 9), MonthDay.of(12, 25));

    /** Semana Santa cambia de fecha: jueves y viernes santo. */
    private static final List<LocalDate> FERIADOS_MOVILES = List.of(
            LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3),
            LocalDate.of(2027, 3, 25), LocalDate.of(2027, 3, 26));

    public static boolean esFeriado(LocalDate dia) {
        return FERIADOS_FIJOS.contains(MonthDay.from(dia)) || FERIADOS_MOVILES.contains(dia);
    }

    public static boolean esLaborable(LocalDate dia) {
        return dia.getDayOfWeek() != DayOfWeek.SUNDAY && !esFeriado(dia);
    }

    /** Fecha | Feriado, para mostrarlo. */
    public static List<String[]> feriados() {
        List<String[]> filas = new ArrayList<>();
        FERIADOS_FIJOS.forEach(m -> filas.add(new String[]{
                String.format("%02d/%02d", m.getDayOfMonth(), m.getMonthValue()), "Todos los años"}));
        FERIADOS_MOVILES.forEach(d -> filas.add(new String[]{
                d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), "Semana Santa"}));
        return filas;
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
                    i.admiteImpugnacion() ? "Sí" : "No"});
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
    public static List<String[]> tiemposAtencion() { return TIEMPOS; }

    private static final List<String[]> TIEMPOS = new ArrayList<>(List.of(
                new String[]{"SuperVip", "Alta", "4", "Horas"},
                new String[]{"SuperVip", "Media", "1", "Días"},
                new String[]{"SuperVip", "Baja", "3", "Días"},
                new String[]{"Vip", "Alta", "8", "Horas"},
                new String[]{"Vip", "Media", "2", "Días"},
                new String[]{"Vip", "Baja", "5", "Días"},
                new String[]{"Ordinario", "Alta", "1", "Días"},
                new String[]{"Ordinario", "Media", "3", "Días"},
                new String[]{"Ordinario", "Baja", "7", "Días"},
                new String[]{"Nuevo", "Alta", "1", "Días"},
                new String[]{"Nuevo", "Media", "4", "Días"},
                new String[]{"Nuevo", "Baja", "10", "Días"}
    ));

    /**
     * Horas laborables de atencion que corresponden a un cliente con una
     * prioridad. Un dia es una jornada, no 24 horas: el area no atiende de
     * noche, asi que el plazo tampoco corre de noche.
     */
    public static long horasDeAtencion(String tipoCliente, Prioridad prioridad) {
        long jornada = Parametros.minutosJornada() / 60;
        for (String[] f : tiemposAtencion()) {
            if (f[0].equalsIgnoreCase(tipoCliente) && f[1].equalsIgnoreCase(prioridad.etiqueta())) {
                long valor = Long.parseLong(f[2]);
                return switch (f[3]) {
                    case "Horas" -> valor;
                    case "Días" -> valor * jornada;
                    default -> valor;
                };
            }
        }
        // cliente sin categoria: el plazo de un cliente Nuevo
        return horasDeAtencion("Nuevo", prioridad);
    }

    /**
     * El limite de atencion: las horas laborables del SLA contadas desde
     * {@code desde}, sin pasar nunca el plazo legal de respuesta.
     */
    public static LocalDateTime limiteAtencion(LocalDateTime desde, String tipoCliente, Prioridad prioridad) {
        LocalDateTime sla = Parametros.sumarMinutosLaborables(desde, horasDeAtencion(tipoCliente, prioridad) * 60);
        LocalDateTime legal = Parametros.limiteLegal(desde);
        return sla.isAfter(legal) ? legal : sla;
    }

    /* ---------------- catalogo de prioridad ---------------- */

    /**
     * Tipo de problema | Tipo de cliente | Prioridad.
     *
     * Es la regla que decide la prioridad del reclamo. Antes la criticidad se
     * cargaba a mano en el catalogo de reclamos y no se derivaba de nada.
     */
    public static List<String[]> reglasPrioridad() { return PRIORIDAD; }

    private static final List<String[]> PRIORIDAD = new ArrayList<>(List.of(
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
                new String[]{"Atención", "SuperVip", "Media"},
                new String[]{"Atención", "Vip", "Baja"},
                new String[]{"Atención", "Ordinario", "Baja"},
                new String[]{"Atención", "Nuevo", "Baja"}
    ));

    /** Aplica la regla de prioridad; si no hay fila, el reclamo queda en Media. */
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
                new String[]{"Teléfono Móvil", "Samsung", "Galaxy A54"},
                new String[]{"Teléfono Móvil", "Huawei", "Nova 11"},
                new String[]{"Teléfono Móvil", "Motorola", "Moto G84"},
                new String[]{"Teléfono Móvil", "Azumi", "Nitro 5"},
                new String[]{"Computadores Notebook", "HP", "Omen 16"},
                new String[]{"Computadores Notebook", "Lenovo", "IdeaPad 3"},
                new String[]{"Monitores De Pantalla Táctil (Touch)", "Samsung", "T27"},
                new String[]{"Impresoras Laser", "HP", "LaserJet M111"}
        ));
    }
}
