package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Los tickets abiertos en la sesion y las reglas que los gobiernan: como se
 * numeran, que prioridad les toca y hasta cuando hay plazo para atenderlos.
 *
 * Todo eso antes estaba repartido entre las pantallas; aqui hay un solo
 * lugar, que es lo que permite explicar el sistema sin contradecirse.
 */
public final class Tickets {

    private static final List<Ticket> TICKETS = new ArrayList<>();
    private static final List<Consumer<List<Ticket>>> OYENTES = new ArrayList<>();
    private static int correlativo = 0;

    private Tickets() {}

    static { sembrar(); }

    public static List<Ticket> todos() { return TICKETS; }

    public static void alCambiar(Consumer<List<Ticket>> oyente) { OYENTES.add(oyente); }

    public static void notificar() {
        for (Consumer<List<Ticket>> o : List.copyOf(OYENTES)) o.accept(TICKETS);
    }

    /** Numeracion del ticket: TK mas correlativo del anio. */
    public static String siguienteNumero() {
        return String.format("TK-%d-%04d", LocalDateTime.now().getYear(), correlativo + 1);
    }

    /**
     * Abre un ticket aplicando las reglas del sistema: la prioridad sale del
     * tipo de problema y del tipo de cliente, y el plazo de atencion sale del
     * catalogo de tiempos.
     */
    public static Ticket abrir(Canal canal, String local,
                               String documento, String nombre, String tipoCliente,
                               TipoObjeto tipoObjeto, String objeto,
                               String tipoProblema, String problema,
                               String comprobante, String fechaComprobante) {
        Prioridad prioridad = Catalogos.prioridadDe(tipoProblema, tipoCliente);
        long horas = Catalogos.horasDeAtencion(tipoCliente, prioridad);
        LocalDateTime ahora = LocalDateTime.now();

        correlativo++;
        Ticket t = new Ticket(siguienteNumeroDe(correlativo), ahora, canal, local,
                documento, nombre, tipoCliente, tipoObjeto, objeto, tipoProblema, problema,
                comprobante, fechaComprobante, prioridad, ahora.plusHours(horas));

        t.anotar("Sistema", "Prioridad", prioridad.etiqueta()
                + " según " + tipoProblema + " y cliente " + tipoCliente);
        t.anotar("Sistema", "Plazo", "Atención dentro de " + horas + " horas");

        TICKETS.add(0, t);
        notificar();
        return t;
    }

    private static String siguienteNumeroDe(int n) {
        return String.format("TK-%d-%04d", LocalDateTime.now().getYear(), n);
    }

    public static List<Ticket> abiertos() {
        return TICKETS.stream().filter(t -> t.estado().abierto()).toList();
    }

    public static List<Ticket> de(String documento) {
        return TICKETS.stream().filter(t -> t.documentoCliente().equals(documento)).toList();
    }

    public static Ticket porNumero(String numero) {
        return TICKETS.stream().filter(t -> t.numero().equals(numero)).findFirst().orElse(null);
    }

    /* ---------------- datos de arranque ---------------- */

    /** Tickets de ejemplo para que las pantallas no arranquen vacias. */
    private static void sembrar() {
        Ticket t1 = abrir(Canal.PRESENCIAL, "Loc01", "72119474", "Alexander Rodríguez Camacho",
                "Vip", TipoObjeto.PRODUCTO, "Teléfono Móvil Samsung Galaxy A54",
                "Funcionamiento", "La cámara no funciona", "25713", "12/01/2019");
        t1.asignar("Área Técnica de Reparación e Inspección", "E01", "PR-FUNC-01", "E01");
        t1.cambiarEstado(Dominio.Estado.EN_ATENCION, "E01", "Inspección iniciada");

        Ticket t2 = abrir(Canal.WEB, "Loc02", "72119474", "Alexander Rodríguez Camacho",
                "Vip", TipoObjeto.SERVICIO, "Delivery", "Entrega", "Empaque abierto",
                "32847", "17/01/2019");
        t2.asignar("Logística", "E02", "PR-ENTR-01", "E02");
        t2.cambiarEstado(Dominio.Estado.EN_ATENCION, "E02", "");
        t2.cambiarEstado(Dominio.Estado.RECHAZADO, "E02",
                "El empaque fue abierto por el cliente al recibir");

        Ticket t3 = abrir(Canal.TELEFONICO, "Loc01", "10112387203", "Michael Skinner Brito Calero",
                "Ordinario", TipoObjeto.PRODUCTO, "Computadores Notebook HP Omen 16",
                "Cobranza", "Facturación Doble", "25592", "27/02/2019");
        t3.asignar("Ventas", "E03", "PR-COBR-01", "E03");

        Ticket t4 = abrir(Canal.INTERNET, "Loc03", "10112387203", "Michael Skinner Brito Calero",
                "Ordinario", TipoObjeto.SERVICIO, "Instalación", "Atención", "Demora",
                "", "");
        t4.anotar("Sistema", "Sustento", "Reclamo sin comprobante: es por atención");
    }
}
