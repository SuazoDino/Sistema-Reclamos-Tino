package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.model.Modelo.Compra;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Validacion.Campo;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Los reclamos abiertos en la sesion y las reglas que los gobiernan: como se
 * numeran, que prioridad les toca y hasta cuando hay plazo para atenderlos.
 *
 * Todo eso antes estaba repartido entre las pantallas; aqui hay un solo
 * lugar, que es lo que permite explicar el sistema sin contradecirse.
 */
public final class Reclamos {

    private static final List<Reclamo> TICKETS = new ArrayList<>();
    private static final List<Consumer<List<Reclamo>>> OYENTES = new ArrayList<>();
    private static int correlativo = 0;

    private Reclamos() {}

    static { sembrar(); }

    public static List<Reclamo> todos() { return TICKETS; }

    public static void alCambiar(Consumer<List<Reclamo>> oyente) { OYENTES.add(oyente); }

    public static void notificar() {
        for (Consumer<List<Reclamo>> o : List.copyOf(OYENTES)) o.accept(TICKETS);
    }

    /** Numeracion del reclamo: TK mas correlativo del anio. */
    public static String siguienteNumero() {
        return String.format("RC-%d-%04d", LocalDateTime.now().getYear(), correlativo + 1);
    }

    /** Lo que devuelve registrar: el reclamo, o por que no se genero. */
    public record Resultado(Reclamo reclamo, Map<Campo, String> errores, List<String> rechazos) {
        public boolean generado() { return reclamo != null; }
    }

    /**
     * Registra el reclamo que lleno el cliente: valida el formulario, revisa
     * que el reclamo proceda y recien entonces abre el reclamo.
     */
    public static Resultado registrar(Solicitud s) {
        Map<Campo, String> errores = Validacion.formulario(s);
        if (!errores.isEmpty()) return new Resultado(null, errores, List.of());
        List<String> rechazos = Validacion.procedencia(s);
        if (!rechazos.isEmpty()) return new Resultado(null, Map.of(), rechazos);

        Cliente c = Datos.clientePorDocumento(s.documento());
        Compra compra = Datos.compra(c.documento(), s.tipoObjeto() == TipoObjeto.PRODUCTO, s.objeto());
        Reclamo t;
        try {
            t = abrir(s.canal(), s.canal() == Canal.PRESENCIAL ? s.local() : "-",
                    c.documento(), c.nombreCompleto(), c.tipo().etiqueta(),
                    s.tipoObjeto(), s.objeto(), s.tipoProblema(), s.problema(),
                    compra == null ? "" : compra.comprobante(),
                    compra == null ? "" : Validacion.fecha(compra.fecha()));
        } catch (RuntimeException e) {
            // abrir ya dejo todo como estaba: aqui solo se avisa al cliente y queda la huella
            Auditoria.registrar("Sistema", "Formulario Reclamo", "Registro de reclamo",
                    "No se pudo programar la atención de " + c.documento() + ": " + e.getMessage(),
                    Auditoria.RECHAZADA, null);
            return new Resultado(null, Map.of(), List.of(
                    "No pudimos programar la atención de su reclamo en este momento y no se registró. "
                            + "Inténtelo de nuevo en unos minutos."));
        }

        t.registrarSolicitud(Prototipo.naturalezaDe(s.tipoProblema()), s.detalle().trim(), s.pedido());
        if (compra == null) t.anotar("Sistema", "Sustento", "Sin compra: el reclamo es por atención");
        else t.anotar("Sistema", "Sustento", "Compra verificada: comprobante " + compra.comprobante());
        t.registrarValidacion(Validacion.fueraDeGarantia(compra, LocalDate.now()),
                compra == null ? 0 : compra.monto());
        Atencion.sugerir(t, null);
        notificar();
        return new Resultado(t, Map.of(), List.of());
    }

    /**
     * Abre un reclamo aplicando las reglas del sistema: la prioridad sale del
     * tipo de problema y del tipo de cliente, el plazo de atencion sale del
     * catalogo de tiempos, y el primer evento se asigna y se agenda solo.
     */
    public static Reclamo abrir(Canal canal, String local,
                               String documento, String nombre, String tipoCliente,
                               TipoObjeto tipoObjeto, String objeto,
                               String tipoProblema, String problema,
                               String comprobante, String fechaComprobante) {
        Prioridad prioridad = Catalogos.prioridadDe(tipoProblema, tipoCliente);
        long horas = Catalogos.horasDeAtencion(tipoCliente, prioridad);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = Catalogos.limiteAtencion(ahora, tipoCliente, prioridad);

        Reclamo t = new Reclamo(siguienteNumeroDe(correlativo + 1), ahora, canal, local,
                documento, nombre, tipoCliente, tipoObjeto, objeto, tipoProblema, problema,
                comprobante, fechaComprobante, prioridad, limite);

        t.anotar("Sistema", "Prioridad", prioridad.etiqueta()
                + " según " + tipoProblema + " y cliente " + tipoCliente);
        t.anotar("Sistema", "Plazo", "Atención dentro de " + horas + " horas laborables"
                + (limite.equals(Parametros.limiteLegal(ahora)) ? " (tope: plazo legal)" : ""));
        // es una transaccion: si no se puede programar, se devuelven los tickets y no se consume el numero
        try {
            Atencion.programarPrimero(t, ahora);
        } catch (RuntimeException e) {
            t.eventos().forEach(ev -> ParqueTickets.liberar(ev.tickets()));
            throw e;
        }
        correlativo++;

        TICKETS.add(0, t);
        notificar();
        return t;
    }

    private static String siguienteNumeroDe(int n) {
        return String.format("RC-%d-%04d", LocalDateTime.now().getYear(), n);
    }

    public static List<Reclamo> abiertos() {
        return TICKETS.stream().filter(t -> t.estado().abierto()).toList();
    }

    public static List<Reclamo> de(String documento) {
        return TICKETS.stream().filter(t -> t.documentoCliente().equals(documento)).toList();
    }

    public static Reclamo porNumero(String numero) {
        return TICKETS.stream().filter(t -> t.numero().equals(numero)).findFirst().orElse(null);
    }

    /* ---------------- datos de arranque ---------------- */

    private static Reclamo sembrar(String documento, Canal canal, String local, TipoObjeto tipo,
                                  String objeto, String tipoProblema, String problema,
                                  String detalle, String pedido) {
        Resultado r = registrar(new Solicitud(documento, canal, local, tipo, objeto, tipoProblema, problema,
                detalle, pedido));
        if (!r.generado()) {
            throw new IllegalStateException("Reclamo de ejemplo inválido: " + r.errores() + r.rechazos());
        }
        return r.reclamo();
    }

    /** Todas las acciones del protocolo del evento actual: para dar un evento por hecho. */
    private static List<String> todas(Reclamo t) {
        return Prototipo.pasosDe(t.eventoActual().protocolo(), t.eventoActual().evento()).stream().map(a -> a[0]).toList();
    }

    /**
     * Reclamos de ejemplo para que las pantallas no arranquen vacias. Pasan
     * por el mismo flujo que uno real: validacion, asignacion y cita. El orden
     * importa: deja a E01 y E04 empatados para que el reclamo de la demo
     * (Alexander, la camara del Samsung) le toque a E01.
     */
    private static void sembrar() {
        Reclamo t1 = sembrar("10112387203", Canal.PRESENCIAL, "Loc01", TipoObjeto.PRODUCTO,
                "Computadores Notebook HP Omen 16", "Funcionamiento", "No enciende",
                "La laptop dejó de encender a las dos semanas; no carga ni prende el indicador.", "Reparación");
        Atencion.iniciar(t1, t1.especialista());

        // fuera de garantia, y se aplico otra solucion que la sugerida: queda observado
        Reclamo t2 = sembrar("20548712341", Canal.PRESENCIAL, "Loc02", TipoObjeto.PRODUCTO,
                "Computadores Notebook HP Omen", "Funcionamiento", "La cámara no funciona",
                "La cámara web muestra la pantalla en negro en todas las aplicaciones.", "Reparación");
        Atencion.completar(t2, t2.especialista(), todas(t2), false, null);
        Atencion.completar(t2, t2.especialista(), todas(t2), null, "Reparación Equipo");

        sembrar("70775031", Canal.WEB, null, TipoObjeto.PRODUCTO,
                "Teléfono Móvil Motorola Moto G84", "Funcionamiento", "Teléfono Sobrecalentado",
                "El teléfono se calienta mucho al cargar y se apaga solo.", "Cambio del producto");

        // rechazado con aprobacion del supervisor: el cliente puede impugnar
        Reclamo t4 = sembrar("72119474", Canal.WEB, null, TipoObjeto.SERVICIO,
                "Delivery", "Entrega", "Empaque abierto",
                "El pedido llegó con la caja abierta y sin el sello de seguridad.", "Nueva prestación del servicio");
        Atencion.proponerRechazo(t4, t4.especialista(), "El empaque fue abierto por el cliente al recibir");
        Calidad.aprobar(t4, "S01");

        sembrar("10112387203", Canal.TELEFONICO, null, TipoObjeto.PRODUCTO,
                "Computadores Notebook HP Omen 16", "Cobranza", "Facturación Doble",
                "Me cobraron dos veces la misma laptop en la tarjeta de crédito.", "Devolución del dinero");

        sembrar("10112387203", Canal.INTERNET, null, TipoObjeto.SERVICIO,
                "Instalación", "Atención", "Demora",
                "El técnico llegó tres horas tarde a la instalación sin avisar.", "Disculpa y corrección de la atención");
    }
}
