package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.model.Modelo.Compra;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las validaciones del reclamo, en dos niveles:
 *
 * 1. Formulario: que cada campo este lleno, con formato valido y que la
 *    combinacion producto o servicio con problema tenga sentido.
 * 2. Procedencia: que el reclamo tenga derecho a existir. Que el cliente haya
 *    comprado lo que reclama y que no tenga ya el mismo reclamo abierto.
 *
 * Esta fuera de la pantalla para que la misma regla sirva a cualquier
 * interfaz: la de escritorio hoy y la web despues.
 */
public final class Validacion {

    private Validacion() {}

    /** Los campos del formulario, para marcar en rojo los que fallan. */
    public enum Campo {
        DOCUMENTO("DNI/RUC"), CANAL("Canal de ingreso"), LOCAL("Local"),
        TIPO_OBJETO("Reclama sobre"), OBJETO("Producto o servicio"),
        TIPO_PROBLEMA("Tipo de problema"), PROBLEMA("Problema"),
        COMPROBANTE("Nro de comprobante"), FECHA_COMPROBANTE("Fecha del comprobante"),
        DETALLE("Detalle"), PEDIDO("Pedido del consumidor");

        private final String etiqueta;
        Campo(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
    }

    /**
     * Lo que el cliente lleno en el formulario. Un campo sin elegir es null.
     * El detalle y el pedido son los de la hoja del Libro de Reclamaciones:
     * que paso, con sus palabras, y que solucion pide.
     */
    public record Solicitud(String documento, Canal canal, String local, TipoObjeto tipoObjeto,
                            String objeto, String tipoProblema, String problema,
                            String detalle, String pedido) {

        /** Solo el documento: para validar al cliente antes de mostrarle el formulario. */
        public static Solicitud deDocumento(String documento) {
            return new Solicitud(documento, null, null, null, null, null, null, null, null);
        }
    }

    public static final int DETALLE_MIN = 10;
    public static final int DETALLE_MAX = 500;

    public static final String TIPO_SIN_COMPRA = "Atención";

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /* ---------------- listas dependientes del formulario ---------------- */

    /**
     * Lo que el cliente puede elegir en "Producto o servicio": los productos
     * que compro y todos los servicios habilitados (sobre un servicio que no
     * contrato solo puede reclamar por Atencion).
     */
    public static List<String> objetosPara(String documento, TipoObjeto tipo) {
        if (tipo == TipoObjeto.PRODUCTO) {
            return Datos.comprasDe(documento).stream()
                    .filter(Compra::esProducto).map(Compra::objeto).toList();
        }
        return Catalogos.serviciosHabilitados();
    }

    /** Los tipos de problema permitidos para lo que se reclama. */
    public static List<String> tiposPara(String documento, TipoObjeto tipo, String objeto) {
        if (objeto == null) return List.of();
        boolean esProducto = tipo == TipoObjeto.PRODUCTO;
        Compra compra = Datos.compra(documento, esProducto, objeto);
        String clave = clave(esProducto, objeto, compra);

        List<String> tipos = new ArrayList<>(Prototipo.tiposDeProblemaPara(esProducto, clave));
        if (!esProducto) tipos.retainAll(Catalogos.tiposDeProblemaDeServicio(objeto));
        if (compra == null) tipos.retainAll(List.of(TIPO_SIN_COMPRA));
        return tipos;
    }

    /** Los problemas del tipo elegido que aplican a lo que se reclama. */
    public static List<String> problemasPara(String documento, TipoObjeto tipo, String objeto,
                                             String tipoProblema) {
        if (objeto == null || tipoProblema == null) return List.of();
        boolean esProducto = tipo == TipoObjeto.PRODUCTO;
        String clave = clave(esProducto, objeto, Datos.compra(documento, esProducto, objeto));
        return Prototipo.problemasDe(tipoProblema, esProducto, clave);
    }

    /** Para un producto la relacion con el problema es por su bien; para un servicio, por su nombre. */
    private static String clave(boolean esProducto, String objeto, Compra compra) {
        if (!esProducto) return objeto;
        if (compra != null) return compra.clave();
        return Datos.PRODUCTOS.stream().filter(p -> Datos.nombreProducto(p.id()).equals(objeto))
                .map(p -> p.bien()).findFirst().orElse(objeto);
    }

    /* ---------------- nivel 1: el formulario ---------------- */

    /** Los errores de cada campo; vacio si el formulario esta bien llenado. */
    public static Map<Campo, String> formulario(Solicitud s) {
        Map<Campo, String> errores = new LinkedHashMap<>();

        String doc = s.documento() == null ? "" : s.documento().trim();
        if (doc.isEmpty()) errores.put(Campo.DOCUMENTO, "es obligatorio");
        else if (!doc.matches("\\d+")) errores.put(Campo.DOCUMENTO, "solo admite dígitos");
        else if (doc.length() != 8 && doc.length() != 11)
            errores.put(Campo.DOCUMENTO, "debe tener 8 dígitos (DNI) u 11 (RUC)");
        else if (Datos.clientePorDocumento(doc) == null)
            errores.put(Campo.DOCUMENTO, "no corresponde a un cliente registrado");

        if (s.canal() == null) errores.put(Campo.CANAL, "es obligatorio");
        else if (!Catalogos.canalHabilitado(s.canal())) errores.put(Campo.CANAL, "no está habilitado");

        if (s.canal() == Canal.PRESENCIAL) {
            if (s.local() == null) errores.put(Campo.LOCAL, "es obligatorio en la atención presencial");
            else if (!Catalogos.localesHabilitados().contains(s.local()))
                errores.put(Campo.LOCAL, "no está habilitado");
        }

        if (s.tipoObjeto() == null) {
            errores.put(Campo.TIPO_OBJETO, "elija Producto o Servicio");
            if (s.objeto() == null) errores.put(Campo.OBJETO, "es obligatorio");
            if (s.tipoProblema() == null) errores.put(Campo.TIPO_PROBLEMA, "es obligatorio");
            if (s.problema() == null) errores.put(Campo.PROBLEMA, "es obligatorio");
            detalleYPedido(s, errores);
            return errores;
        }
        if (s.objeto() == null) errores.put(Campo.OBJETO, "es obligatorio");
        else if (!errores.containsKey(Campo.DOCUMENTO)
                && !objetosPara(doc, s.tipoObjeto()).contains(s.objeto()))
            errores.put(Campo.OBJETO, "no está entre lo que puede reclamar");

        if (s.tipoProblema() == null) errores.put(Campo.TIPO_PROBLEMA, "es obligatorio");
        else if (s.objeto() != null && !tiposPara(doc, s.tipoObjeto(), s.objeto()).contains(s.tipoProblema()))
            errores.put(Campo.TIPO_PROBLEMA, "no corresponde a " + s.objeto());

        if (s.problema() == null) errores.put(Campo.PROBLEMA, "es obligatorio");
        else if (s.tipoProblema() != null && !Prototipo.problemasDe(s.tipoProblema()).contains(s.problema()))
            errores.put(Campo.PROBLEMA, "no es del tipo " + s.tipoProblema());
        else if (s.objeto() != null && s.tipoProblema() != null
                && !problemasPara(doc, s.tipoObjeto(), s.objeto(), s.tipoProblema()).contains(s.problema()))
            errores.put(Campo.PROBLEMA, "no corresponde a " + s.objeto());

        Compra compra = s.objeto() == null ? null
                : Datos.compra(doc, s.tipoObjeto() == TipoObjeto.PRODUCTO, s.objeto());
        if (compra != null) errores.putAll(comprobante(compra.comprobante(), compra.fecha().format(FECHA)));
        detalleYPedido(s, errores);
        return errores;
    }

    /** Detalle: texto libre de 10 a 500 caracteres. Pedido: uno del catalogo. */
    private static void detalleYPedido(Solicitud s, Map<Campo, String> errores) {
        String detalle = s.detalle() == null ? "" : s.detalle().trim();
        if (detalle.isEmpty()) errores.put(Campo.DETALLE, "es obligatorio: cuente qué pasó");
        else if (detalle.length() < DETALLE_MIN) errores.put(Campo.DETALLE, "debe tener al menos " + DETALLE_MIN + " caracteres");
        else if (detalle.length() > DETALLE_MAX) errores.put(Campo.DETALLE, "admite hasta " + DETALLE_MAX + " caracteres");

        if (s.pedido() == null) errores.put(Campo.PEDIDO, "es obligatorio");
        else if (!Prototipo.PEDIDOS.contains(s.pedido())) errores.put(Campo.PEDIDO, "no es un pedido válido");
    }

    /** El comprobante: B o F, serie de 3 digitos, correlativo de hasta 8; fecha no futura. */
    public static Map<Campo, String> comprobante(String numero, String fecha) {
        Map<Campo, String> errores = new LinkedHashMap<>();
        if (numero == null || numero.isBlank()) errores.put(Campo.COMPROBANTE, "es obligatorio");
        else if (!numero.trim().matches("[BF]\\d{3}-\\d{1,8}"))
            errores.put(Campo.COMPROBANTE, "debe tener el formato B001-00012345");

        if (fecha == null || fecha.isBlank()) {
            errores.put(Campo.FECHA_COMPROBANTE, "es obligatoria");
        } else {
            try {
                if (LocalDate.parse(fecha.trim(), FECHA).isAfter(LocalDate.now()))
                    errores.put(Campo.FECHA_COMPROBANTE, "no puede ser futura");
            } catch (DateTimeParseException e) {
                errores.put(Campo.FECHA_COMPROBANTE, "debe tener el formato dd/MM/yyyy");
            }
        }
        return errores;
    }

    /* ---------------- nivel 2: que el reclamo proceda ---------------- */

    /**
     * Por que el reclamo no procede; vacio si procede. Se corre con el
     * formulario ya valido.
     */
    public static List<String> procedencia(Solicitud s) {
        List<String> motivos = new ArrayList<>();
        boolean esProducto = s.tipoObjeto() == TipoObjeto.PRODUCTO;
        Compra compra = Datos.compra(s.documento().trim(), esProducto, s.objeto());

        if (compra == null && !TIPO_SIN_COMPRA.equals(s.tipoProblema())) {
            motivos.add("No registramos esa compra a su nombre.");
        }
        if (!problemasPara(s.documento().trim(), s.tipoObjeto(), s.objeto(), s.tipoProblema())
                .contains(s.problema())) {
            motivos.add("El problema \"" + s.problema() + "\" no corresponde a " + s.objeto() + ".");
        }
        Reclamo abierto = duplicado(s);
        if (abierto != null) {
            motivos.add("Ya tiene el reclamo " + abierto.numero() + " en atención por el mismo problema.");
        }
        return motivos;
    }

    /** Otro reclamo abierto del mismo cliente, sobre lo mismo y por el mismo problema. */
    public static Reclamo duplicado(Solicitud s) {
        return Reclamos.todos().stream()
                .filter(t -> t.estado().abierto()
                        && t.documentoCliente().equals(s.documento().trim())
                        && t.objeto().equals(s.objeto())
                        && t.problema().equals(s.problema()))
                .findFirst().orElse(null);
    }

    /** Dias de garantia de lo que se compro; los servicios no tienen garantia de producto. */
    public static int garantia(Compra c) {
        return c != null && c.esProducto() ? Prototipo.plazoDeGarantia(c.codigo()) : 0;
    }

    /** Un producto esta fuera de garantia si paso mas tiempo que su plazo desde la compra. */
    public static boolean fueraDeGarantia(Compra c, LocalDate hoy) {
        if (c == null || !c.esProducto()) return false;
        return c.fecha().plusDays(garantia(c)).isBefore(hoy);
    }

    public static Cliente cliente(String documento) {
        return documento == null ? null : Datos.clientePorDocumento(documento);
    }

    public static String fecha(LocalDate d) { return d.format(FECHA); }
}
