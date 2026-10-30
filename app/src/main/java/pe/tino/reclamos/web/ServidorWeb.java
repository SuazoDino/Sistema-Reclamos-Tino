package pe.tino.reclamos.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.MovimientoSolucion;
import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.model.Modelo.Compra;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.repo.Validacion.Campo;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * La version web del Sistema de Reclamos: sirve las paginas (HTML, CSS y
 * JavaScript) y una API JSON sobre la misma logica de repo/ que usa la app de
 * escritorio. El navegador no decide nada: valida, asigna y agenda el
 * servidor.
 *
 * Arranque: mvn compile exec:java -Dmain.class=pe.tino.reclamos.web.ServidorWeb
 * y abrir http://127.0.0.1:8080
 */
public final class ServidorWeb {

    private static final int PUERTO = 8080;

    private ServidorWeb() {}

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : PUERTO;
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        servidor.createContext("/api/", ServidorWeb::api);
        servidor.createContext("/", ServidorWeb::archivo);
        // un solo hilo: los repositorios en memoria no son concurrentes
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Sistema de Reclamos web en http://127.0.0.1:" + puerto);
    }

    /* ---------------- paginas ---------------- */

    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8");

    private static void archivo(HttpExchange x) throws IOException {
        String ruta = x.getRequestURI().getPath();
        if (ruta.equals("/")) ruta = "/index.html";
        if (ruta.contains("..")) { responder(x, 404, "text/plain", "No existe"); return; }
        try (InputStream in = ServidorWeb.class.getResourceAsStream("/web" + ruta)) {
            if (in == null) { responder(x, 404, "text/plain; charset=utf-8", "No existe " + ruta); return; }
            String ext = ruta.substring(ruta.lastIndexOf('.') + 1);
            responder(x, 200, TIPOS.getOrDefault(ext, "application/octet-stream"), in.readAllBytes());
        }
    }

    /* ---------------- api ---------------- */

    private static void api(HttpExchange x) throws IOException {
        try {
            Object r = rutear(x);
            if (r == null) responderJson(x, 404, Map.of("error", "Ruta no encontrada"));
            else responderJson(x, 200, r);
        } catch (NoEncontrado e) {
            responderJson(x, 404, Map.of("error", e.getMessage()));
        } catch (Conflicto e) {
            Map<String, Object> cuerpo = new LinkedHashMap<>(e.datos);
            cuerpo.put("error", e.getMessage());
            responderJson(x, 409, cuerpo);
        } catch (Demasiado e) {
            responderJson(x, 413, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            responderJson(x, 400, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            responderJson(x, 500, Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    private static final class NoEncontrado extends RuntimeException {
        NoEncontrado(String m) { super(m); }
    }

    /** Lo que el usuario veia ya cambio: se responde con los datos actuales para que se ponga al dia. */
    private static final class Conflicto extends RuntimeException {
        final Map<String, Object> datos;
        Conflicto(String m, Map<String, Object> datos) { super(m); this.datos = datos; }
    }

    private static final class Demasiado extends RuntimeException {
        Demasiado() { super("La solicitud es demasiado grande."); }
    }

    /** Ningun formulario del sistema pasa de unos pocos KB: lo que exceda se rechaza sin leerlo entero. */
    private static final int MAX_CUERPO = 64 * 1024;

    private static String cuerpo(HttpExchange x) throws IOException {
        byte[] b = x.getRequestBody().readNBytes(MAX_CUERPO + 1);
        if (b.length > MAX_CUERPO) throw new Demasiado();
        return new String(b, StandardCharsets.UTF_8);
    }

    private static Object rutear(HttpExchange x) throws IOException {
        String metodo = x.getRequestMethod();
        String[] p = Arrays.stream(x.getRequestURI().getPath().split("/"))
                .filter(s -> !s.isEmpty()).skip(1).toArray(String[]::new);
        Map<String, List<String>> q = parametros(x.getRequestURI().getRawQuery());
        Map<String, List<String>> f = metodo.equals("POST")
                ? parametros(cuerpo(x))
                : Map.of();
        if (p.length == 0) return null;

        if (metodo.equals("GET")) {
            switch (p[0]) {
                case "clientes":
                    if (p.length == 2) return cliente(p[1]);
                    if (p.length == 3 && p[2].equals("reclamos")) return reclamosDe(p[1]);
                    break;
                case "opciones": return opciones(q);
                case "empleados":
                    if (p.length == 3 && p[2].equals("agenda")) return agenda(p[1]);
                    if (p.length == 3 && p[2].equals("reporte")) return reporte(p[1]);
                    break;
                case "seguridad": return Mantenimiento.seguridad();
                case "auditoria": return Mantenimiento.diario();
                case "mantenimiento":
                    if (p.length == 2) return Mantenimiento.pantalla(p[1]);
                    break;
                case "revision": return revision(uno(q, "supervisor"));
                case "reclamos":
                    if (p.length == 1) return Reclamos.todos().stream().map(ServidorWeb::reclamo).toList();
                    break;
                case "indicadores": return indicadores();
                case "parque": return parque(uno(q, "operario"), uno(q, "dia"));
                case "operarios": return Prototipo.EMPLEADOS.values().stream().filter(e -> !e.esSupervisor())
                        .map(e -> mapa("id", e.id(), "tipo", e.tipo(), "area", e.area())).toList();
                default: break;
            }
        } else if (metodo.equals("POST")) {
            if (p[0].equals("reclamos") && p.length == 1) return registrar(f);
            if (p[0].equals("catalogos") && p.length == 3) {
                try {
                    return Mantenimiento.editar(p[1], p[2], f);
                } catch (Mantenimiento.CatalogoCambiado e) {
                    throw new Conflicto(e.getMessage(), Map.of("filas", Mantenimiento.filasDe(p[1])));
                }
            }
            if (p[0].equals("auditoria") && p.length == 3 && p[2].equals("revertir")) {
                return Mantenimiento.revertir(Integer.parseInt(p[1]));
            }
            if (p[0].equals("parametros") && p.length == 1) {
                return Mantenimiento.cambiarParametro(obligatorio(f, "parametro", "Indique el parámetro."), uno(f, "valor"));
            }
            if (p[0].equals("reglas") && p.length == 2 && p[1].equals("evaluar")) return Mantenimiento.evaluar(f);
            if (p[0].equals("seguridad") && p.length == 2 && p[1].equals("perfiles")) {
                return Mantenimiento.agregarPerfil(uno(f, "nombre"));
            }
            if (p[0].equals("seguridad") && p.length == 2 && p[1].equals("accesos")) {
                return Mantenimiento.guardarAccesos(obligatorio(f, "perfil", "Seleccione un perfil."),
                        f.getOrDefault("acceso", List.of()));
            }
            if (p[0].equals("calidad") && p.length == 2 && p[1].equals("muestra")) {
                supervisor(uno(f, "supervisor"));
                Calidad.muestraCada(Integer.parseInt(obligatorio(f, "valor", "Indique el valor.")));
                return mapa("mensaje", "Muestra actualizada: 1 de cada " + Calidad.muestraCada() + ".");
            }
            if (p[0].equals("reclamos") && p.length == 3) return accion(buscar(p[1]), p[2], f);
            if (p[0].equals("parque") && p.length == 2 && p[1].equals("generar")) {
                int dias = Integer.parseInt(Optional.ofNullable(uno(f, "dias")).orElse("30"));
                ParqueTickets.Cuadre c = ParqueTickets.generar(LocalDate.now(), dias);
                return mapa("mensaje", "Batch de generación del parque terminado.",
                        "operarios", c.operarios(), "dias", c.dias(), "ticketsGenerados", c.ticketsGenerados());
            }
        }
        return null;
    }

    /* ---------------- cliente ---------------- */

    private static Object cliente(String documento) {
        Map<Campo, String> e = Validacion.formulario(Solicitud.deDocumento(documento));
        if (e.containsKey(Campo.DOCUMENTO)) throw new IllegalArgumentException("DNI/RUC: " + e.get(Campo.DOCUMENTO));
        Cliente c = Datos.clientePorDocumento(documento);
        return mapa("documento", c.documento(), "nombres", c.nombres(), "apellidos", c.apellidos(),
                "correo", c.correo(), "telefono", c.telefono(), "tipo", c.tipo().etiqueta());
    }

    /** Las listas dependientes del formulario, segun lo que ya se eligio. */
    private static Object opciones(Map<String, List<String>> q) {
        String doc = uno(q, "documento");
        TipoObjeto tipo = tipoObjeto(uno(q, "tipoObjeto"));
        String objeto = uno(q, "objeto");
        String tipoProblema = uno(q, "tipoProblema");

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("canales", Arrays.stream(Canal.values()).filter(Catalogos::canalHabilitado)
                .map(c -> mapa("valor", c.name(), "texto", c.etiqueta())).toList());
        r.put("locales", Catalogos.localesHabilitados());
        r.put("objetos", doc == null || tipo == null ? List.of() : Validacion.objetosPara(doc, tipo));
        r.put("tipos", doc == null || tipo == null ? List.of() : Validacion.tiposPara(doc, tipo, objeto));
        r.put("problemas", doc == null || tipo == null ? List.of()
                : Validacion.problemasPara(doc, tipo, objeto, tipoProblema));
        Compra c = doc == null || tipo == null || objeto == null ? null
                : Datos.compra(doc, tipo == TipoObjeto.PRODUCTO, objeto);
        r.put("pedidos", Prototipo.PEDIDOS);
        r.put("naturaleza", tipoProblema == null ? null : Prototipo.naturalezaDe(tipoProblema));
        r.put("detalleMax", Validacion.DETALLE_MAX);
        r.put("compra", c == null ? null : mapa("comprobante", c.comprobante(),
                "fecha", Validacion.fecha(c.fecha()), "monto", c.monto()));
        return r;
    }

    private static Object registrar(Map<String, List<String>> f) {
        String canal = uno(f, "canal");
        Solicitud s = new Solicitud(uno(f, "documento"), canal == null ? null : Canal.valueOf(canal),
                uno(f, "local"), tipoObjeto(uno(f, "tipoObjeto")), uno(f, "objeto"),
                uno(f, "tipoProblema"), uno(f, "problema"), uno(f, "detalle"), uno(f, "pedido"));
        Reclamos.Resultado r = Reclamos.registrar(s);
        Map<String, Object> errores = new LinkedHashMap<>();
        r.errores().forEach((campo, error) -> errores.put(campo.name(),
                mapa("etiqueta", campo.etiqueta(), "error", error)));
        return mapa("reclamo", r.generado() ? reclamo(r.reclamo()) : null,
                "errores", errores, "rechazos", r.rechazos());
    }

    private static Object reclamosDe(String documento) {
        cliente(documento);
        return Reclamos.de(documento).stream().map(ServidorWeb::reclamo).toList();
    }

    /* ---------------- empleado ---------------- */

    private static Empleado empleado(String id) {
        Empleado e = id == null ? null : Prototipo.EMPLEADOS.get(id.trim().toUpperCase());
        if (e == null) {
            throw new NoEncontrado("El ID de empleado no existe. Pruebe con "
                    + String.join(", ", Prototipo.EMPLEADOS.keySet()) + ".");
        }
        return e;
    }

    /** Los eventos del empleado, en el orden de su agenda. */
    private static Object agenda(String id) {
        Empleado e = empleado(id);
        if (e.esSupervisor()) throw new IllegalArgumentException(e.id() + " es supervisor: use Revisión de Calidad.");
        List<Map<String, Object>> filas = Reclamos.todos().stream()
                .filter(t -> {
                    EventoReclamo ev = t.eventoActual();
                    return ev != null && ev.agendado() && ev.especialista().equals(e.id())
                            && (t.estado() == Estado.ASIGNADO || t.estado() == Estado.EN_ATENCION);
                })
                .sorted(Comparator.comparing((Reclamo t) -> t.eventoActual().inicio())
                        .thenComparing(Reclamo::prioridad))
                .map(t -> {
                    EventoReclamo ev = t.eventoActual();
                    String[] sig = Atencion.siguiente(t, ev);
                    Map<String, Object> m = new LinkedHashMap<>(reclamo(t));
                    List<Map<String, Object>> pasos = new ArrayList<>();
                    int acumulado = 0;
                    for (String[] a : Prototipo.pasosDe(ev.protocolo(), ev.evento())) {
                        int min = Integer.parseInt(a[1]);
                        MovimientoSolucion hecho = t.movimientos().stream()
                                .filter(mv -> mv.evento().equals(ev.evento()) && mv.accion().equals(a[0])
                                        && ev.accionesEjecutadas().contains(a[0]))
                                .reduce((x, y) -> y).orElse(null);
                        pasos.add(mapa("nombre", a[0], "minutos", min, "operario", a[2],
                                "critica", "Crítico".equalsIgnoreCase(a[3]),
                                "estimado", ev.inicio().plusMinutes(acumulado),
                                "hecho", hecho != null, "real", hecho == null ? null : hecho.fecFinPaso()));
                        acumulado += min;
                    }
                    m.put("acciones", pasos);
                    m.put("tickets", ev.tickets());
                    m.put("ultimo", sig == null);
                    m.put("siguienteEvento", sig == null ? null : sig[0]);
                    m.put("preguntaReparable", Atencion.PENDIENTE_INSPECCION.equals(t.solucionSugerida()));
                    return m;
                }).toList();
        return mapa("empleado", mapa("id", e.id(), "tipo", e.tipo(), "area", e.area()),
                "eventos", filas, "soluciones", MotorReglas.ACCIONES);
    }

    /** Reporte de Atencion: todos los reclamos que el sistema le asigno al empleado. */
    private static Object reporte(String id) {
        Empleado e = empleado(id);
        List<Reclamo> suyos = Reclamos.todos().stream().filter(t -> e.id().equals(t.especialista())).toList();
        long abiertos = suyos.stream().filter(t -> t.estado().abierto()).count();
        return mapa("empleado", mapa("id", e.id(), "tipo", e.tipo(), "area", e.area()),
                "total", suyos.size(), "abiertos", abiertos, "cerrados", suyos.size() - abiertos,
                "vencidos", suyos.stream().filter(Reclamo::vencido).count(),
                "reclamos", suyos.stream().map(ServidorWeb::reclamo).toList());
    }

    private static Object revision(String id) {
        Empleado e = empleado(id);
        if (!e.esSupervisor()) throw new IllegalArgumentException("Solo un supervisor de calidad puede revisar (pruebe con S01).");
        return mapa("muestraCada", Calidad.muestraCada(),
                "reclamos", Calidad.paraSupervisor().stream().map(ServidorWeb::reclamo).toList());
    }

    /* ---------------- acciones sobre un reclamo ---------------- */

    private static Reclamo buscar(String numero) {
        Reclamo t = Reclamos.porNumero(numero);
        if (t == null) throw new NoEncontrado("No existe el reclamo " + numero);
        return t;
    }

    private static Object accion(Reclamo t, String accion, Map<String, List<String>> f) {
        String mensaje;
        switch (accion) {
            case "completar" -> {
                Empleado e = empleado(uno(f, "empleado"));
                EventoReclamo ev = t.eventoActual();
                if (ev == null || !e.id().equals(ev.especialista())) {
                    throw new IllegalStateException("El evento no está asignado a " + e.id());
                }
                String rep = uno(f, "reparable");
                mensaje = Atencion.completar(t, e.id(), f.getOrDefault("acciones", List.of()),
                        rep == null ? null : Boolean.valueOf(rep), uno(f, "solucion"));
            }
            case "paso" -> {
                Empleado e = empleado(uno(f, "empleado"));
                MovimientoSolucion mv = Atencion.ejecutarPaso(t, e.id(),
                        obligatorio(f, "accion", "Indique el paso."));
                mensaje = "Paso " + mv.paso() + " registrado: " + mv.accion() + ".";
            }
            case "rechazar" -> {
                Empleado e = empleado(uno(f, "empleado"));
                String motivo = obligatorio(f, "motivo", "Indique el motivo del rechazo.");
                Atencion.proponerRechazo(t, e.id(), motivo);
                mensaje = "Rechazo enviado al supervisor. El cliente será notificado cuando lo apruebe.";
            }
            case "aprobar" -> {
                supervisor(uno(f, "supervisor"));
                boolean rechazo = t.revision() == Revision.RECHAZO_PROPUESTO;
                Calidad.aprobar(t, uno(f, "supervisor").toUpperCase());
                mensaje = rechazo ? "Rechazo aprobado: se notificó al cliente."
                        : t.estado() == Estado.CERRADO ? "Solución aprobada: el reclamo quedó cerrado."
                        : "Solución aprobada: falta la conformidad del cliente.";
            }
            case "devolver" -> {
                supervisor(uno(f, "supervisor"));
                Calidad.devolverPorSupervisor(t, uno(f, "supervisor").toUpperCase(),
                        obligatorio(f, "motivo", "Indique el motivo de la devolución."));
                mensaje = "Devuelto a atención. Nueva cita para " + t.especialista() + ".";
            }
            case "conformidad" -> {
                boolean conforme = Boolean.parseBoolean(uno(f, "conforme"));
                String nota = obligatorio(f, "nota", "Indique una nota del 1 al 5.");
                Calidad.conformidad(t, conforme, Integer.parseInt(nota));
                mensaje = !conforme ? "Su reclamo vuelve a atención."
                        : t.estado() == Estado.CERRADO ? "Gracias. Su reclamo quedó cerrado."
                        : "Gracias. Se cerrará cuando termine la revisión de calidad.";
            }
            case "impugnar" -> {
                String motivo = obligatorio(f, "motivo", "Indique el motivo de la impugnación.");
                if (!Atencion.impugnar(t, motivo)) throw new IllegalStateException("El reclamo ya no admite impugnación.");
                mensaje = "Impugnación enviada: pasa a " + t.instancia().etiqueta() + " instancia.";
            }
            default -> { return null; }
        }
        return mapa("mensaje", mensaje, "reclamo", reclamo(t));
    }

    private static void supervisor(String id) {
        if (!empleado(id).esSupervisor()) throw new IllegalArgumentException("Solo un supervisor puede revisar.");
    }

    /* ---------------- parque de tickets ---------------- */

    private static Object parque(String operario, String dia) {
        Empleado e = empleado(operario);
        LocalDate d = dia == null ? LocalDate.now() : LocalDate.parse(dia);
        List<Ticket> tickets = ParqueTickets.de(e.id(), d);
        return mapa("operario", mapa("id", e.id(), "tipo", e.tipo(), "area", e.area()),
                "dia", d, "laborable", Catalogos.esLaborable(d),
                "disponibles", tickets.stream().filter(Ticket::disponible).count(),
                "tickets", tickets.stream().map(k -> mapa("codigo", k.codigo(), "inicio", k.inicio(),
                        "fin", k.fin(), "disponible", k.disponible(), "reclamo", k.reclamo(),
                        "evento", k.evento())).toList());
    }

    /* ---------------- indicadores ---------------- */

    private static Object indicadores() {
        List<Reclamo> todos = Reclamos.todos();
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (Estado e : Estado.values()) {
            porEstado.put(e.etiqueta(), todos.stream().filter(t -> t.estado() == e).count());
        }
        Map<String, Long> porArea = new TreeMap<>();
        todos.forEach(t -> porArea.merge(t.area(), 1L, Long::sum));
        Map<String, Object> carga = new LinkedHashMap<>();
        Prototipo.EMPLEADOS.values().stream().filter(e -> !e.esSupervisor())
                .forEach(e -> carga.put(e.id(), Asignacion.carga(e.id())));
        double sat = Calidad.satisfaccion();
        double dev = Calidad.porcentajeDevueltos();
        return mapa("total", todos.size(),
                "satisfaccion", sat < 0 ? null : sat,
                "devueltos", dev < 0 ? null : dev,
                "sinCapacidad", todos.stream().filter(Reclamo::sinCapacidad).count(),
                "vencidos", todos.stream().filter(Reclamo::vencido).count(),
                "porEstado", porEstado, "porArea", porArea, "carga", carga,
                "pasosEjecutados", todos.stream().mapToInt(Reclamo::pasosCompletados).sum(),
                "desvioPromedio", todos.stream().flatMap(t -> t.movimientos().stream())
                        .mapToLong(MovimientoSolucion::desvioMinutos).average().orElse(Double.NaN));
    }

    /* ---------------- reclamo a JSON ---------------- */

    private static Map<String, Object> reclamo(Reclamo t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("numero", t.numero());
        m.put("apertura", t.apertura());
        m.put("canal", t.canal().etiqueta());
        m.put("local", t.local());
        m.put("cliente", t.nombreCliente());
        m.put("documento", t.documentoCliente());
        m.put("tipoCliente", t.tipoCliente());
        m.put("tipoObjeto", t.tipoObjeto().etiqueta());
        m.put("objeto", t.objeto());
        m.put("tipoProblema", t.tipoProblema());
        m.put("problema", t.problema());
        m.put("naturaleza", t.naturaleza());
        m.put("detalle", t.detalle());
        m.put("pedido", t.pedido());
        m.put("comprobante", t.comprobante());
        m.put("fechaComprobante", t.fechaComprobante());
        m.put("fueraDeGarantia", t.fueraDeGarantia());
        m.put("instancia", t.instancia().etiqueta());
        m.put("siguienteInstancia", t.instancia().siguiente().etiqueta() + " - " + t.instancia().siguiente().resuelve());
        m.put("diasImpugnacion", t.instancia().diasImpugnacion());
        m.put("prioridad", t.prioridad().etiqueta());
        m.put("estado", t.estado().etiqueta());
        m.put("estadoCodigo", t.estado().name());
        m.put("area", t.area());
        m.put("especialista", t.especialista());
        m.put("protocolo", t.protocolo());
        m.put("fechaProgramada", t.fechaProgramada());
        m.put("limiteAtencion", t.limiteAtencion());
        m.put("limiteImpugnacion", t.limiteImpugnacion());
        m.put("vencido", t.vencido());
        m.put("sinCapacidad", t.sinCapacidad());
        m.put("solucionSugerida", t.solucionSugerida());
        m.put("solucionAplicada", t.solucionAplicada());
        m.put("trazaMotor", t.trazaMotor());
        m.put("revision", t.revision() == null ? null : t.revision().etiqueta());
        m.put("observaciones", t.observaciones());
        m.put("motivoRechazo", t.motivoRechazo());
        m.put("conforme", t.conforme());
        m.put("nota", t.nota());
        m.put("esperaConformidad", Calidad.esperaConformidad(t));
        m.put("puedeImpugnar", t.estado() == Estado.RECHAZADO && t.instancia().admiteImpugnacion()
                && t.limiteImpugnacion() != null && !LocalDateTime.now().isAfter(t.limiteImpugnacion()));
        m.put("siguientes", t.estado().siguientes().stream().map(Estado::etiqueta).toList());
        EventoReclamo actual = t.eventoActual();
        m.put("eventoActual", actual == null ? null : actual.evento());
        m.put("totalPasos", t.totalPasos());
        m.put("pasosCompletados", t.pasosCompletados());
        m.put("movimientos", t.movimientos().stream().map(mv -> mapa(
                "numero", mv.numero(), "ticket", mv.ticket(), "operario", mv.operario(),
                "protocolo", mv.protocolo(), "evento", mv.evento(), "paso", mv.paso(), "accion", mv.accion(),
                "fecPasoEstimado", mv.fecPasoEstimado(), "fecFinPasoEstimado", mv.fecFinPasoEstimado(),
                "fecPaso", mv.fecPaso(), "fecFinPaso", mv.fecFinPaso(),
                "desvio", mv.desvioMinutos())).toList());
        m.put("eventos", t.eventos().stream().map(e -> mapa(
                "evento", e.evento(), "protocolo", e.protocolo(), "area", e.area(),
                "especialista", e.especialista(), "inicio", e.inicio(), "fin", e.fin(),
                "estado", e.estado().etiqueta(), "dentroDelPlazo", e.dentroDelPlazo(),
                "resultado", e.resultado(), "tickets", e.tickets())).toList());
        m.put("bitacora", t.bitacora().stream().map(b -> mapa(
                "momento", b.momento(), "usuario", b.usuario(), "accion", b.accion(),
                "detalle", b.detalle())).toList());
        return m;
    }

    /* ---------------- utilidades ---------------- */

    private static TipoObjeto tipoObjeto(String v) {
        if (v == null) return null;
        return v.equalsIgnoreCase("PRODUCTO") ? TipoObjeto.PRODUCTO
                : v.equalsIgnoreCase("SERVICIO") ? TipoObjeto.SERVICIO : null;
    }

    private static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, List<String>> parametros(String crudo) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        if (crudo == null || crudo.isEmpty()) return m;
        for (String par : crudo.split("&")) {
            int i = par.indexOf('=');
            String k = URLDecoder.decode(i < 0 ? par : par.substring(0, i), StandardCharsets.UTF_8);
            String v = i < 0 ? "" : URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8);
            m.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
        }
        return m;
    }

    /** El valor de un parametro, o null si falta o esta vacio. */
    private static String uno(Map<String, List<String>> m, String clave) {
        List<String> v = m.get(clave);
        return v == null || v.isEmpty() || v.get(0).isBlank() ? null : v.get(0);
    }

    private static String obligatorio(Map<String, List<String>> m, String clave, String error) {
        String v = uno(m, clave);
        if (v == null) throw new IllegalArgumentException(error);
        return v.trim();
    }

    private static void responderJson(HttpExchange x, int codigo, Object cuerpo) throws IOException {
        responder(x, codigo, "application/json; charset=utf-8", Json.de(cuerpo));
    }

    private static void responder(HttpExchange x, int codigo, String tipo, String cuerpo) throws IOException {
        responder(x, codigo, tipo, cuerpo.getBytes(StandardCharsets.UTF_8));
    }

    private static void responder(HttpExchange x, int codigo, String tipo, byte[] cuerpo) throws IOException {
        x.getResponseHeaders().set("Content-Type", tipo);
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream out = x.getResponseBody()) { out.write(cuerpo); }
    }
}
