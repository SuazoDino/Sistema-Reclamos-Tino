package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Validacion.Campo;
import pe.tino.reclamos.repo.Validacion.Solicitud;
import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.model.Modelo.Compra;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Formulario Reclamo (Cliente - Data Entry). Registrar un reclamo abre un
 * reclamo, que es lo que el sistema numera y controla.
 *
 * Nada viene preseleccionado y cada lista depende de la anterior: el cliente
 * solo ve lo que compro, los tipos de problema que admite y los problemas
 * de ese tipo. El comprobante no se escribe: sale de la compra.
 */
public class ClienteDataEntryPantalla extends Pantalla {

    private static final String SELECCIONE = "— Seleccione —";
    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter CITA =
            DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy 'a las' HH:mm", Locale.of("es", "PE"));
    private static final Border BORDE_ERROR = BorderFactory.createLineBorder(new Color(0xC62828), 2);

    /* datos del cliente */
    private final JTextField documento = Ui.texto(16);
    private final JTextField nombre = Ui.soloLectura();
    private final JTextField apellido = Ui.soloLectura();
    private final JTextField correo = Ui.soloLectura();
    private final JTextField telefono = Ui.soloLectura();
    private final JTextField tipoCliente = Ui.soloLectura();

    /* datos del reclamo */
    private final JComboBox<Object> canal = combo();
    private final JComboBox<Object> local = combo();
    private final JRadioButton esProducto = new JRadioButton("Producto");
    private final JRadioButton esServicio = new JRadioButton("Servicio");
    private final ButtonGroup sobre = new ButtonGroup();
    private final JPanel filaSobre;
    private final JComboBox<Object> objeto = combo();
    private final JComboBox<Object> tipoProblema = combo();
    private final JComboBox<Object> problema = combo();

    /* sustento: sale de la compra, no se escribe */
    private final JTextField comprobante = Ui.soloLectura();
    private final JTextField fechaComprobante = Ui.soloLectura();
    private final JLabel notaSustento = Ui.suave(" ");
    private final JTextArea detalle = new JTextArea(3, 40);
    private final JComboBox<Object> pedido = combo();

    private final Map<Campo, JComponent> controles = new EnumMap<>(Campo.class);
    private final Map<JComponent, Border> bordes = new java.util.HashMap<>();

    private final JPanel reclamo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
    private Cliente cliente;
    private boolean recargando;

    public ClienteDataEntryPantalla() {
        super("Formulario Reclamo",
                "Registrar un reclamo abre un reclamo. El sistema lo valida, lo asigna y le da fecha.");

        sobre.add(esProducto);
        sobre.add(esServicio);
        for (JRadioButton r : List.of(esProducto, esServicio)) {
            r.setOpaque(false);
            r.setFont(Tema.cuerpo());
            r.addActionListener(e -> recargarObjetos());
        }
        filaSobre = Ui.fila(esProducto, esServicio);

        canal.addActionListener(e -> habilitarLocal());
        objeto.addActionListener(e -> { if (!recargando) { recargarTipos(); mostrarSustento(); } });
        tipoProblema.addActionListener(e -> { if (!recargando) recargarProblemas(); });

        controles.put(Campo.DOCUMENTO, documento);
        controles.put(Campo.CANAL, canal);
        controles.put(Campo.LOCAL, local);
        controles.put(Campo.TIPO_OBJETO, filaSobre);
        controles.put(Campo.OBJETO, objeto);
        controles.put(Campo.TIPO_PROBLEMA, tipoProblema);
        controles.put(Campo.PROBLEMA, problema);
        controles.put(Campo.COMPROBANTE, comprobante);
        controles.put(Campo.FECHA_COMPROBANTE, fechaComprobante);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setFont(Tema.cuerpo());
        detalle.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        controles.put(Campo.DETALLE, detalle);
        controles.put(Campo.PEDIDO, pedido);
        controles.values().forEach(c -> bordes.put(c, c.getBorder()));

        armarReclamo();
        reclamo.setVisible(false);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(datosPersonales(), BorderLayout.NORTH);
        raiz.add(reclamo, BorderLayout.CENTER);

        contenido().add(Ui.scrollVertical(raiz), BorderLayout.CENTER);
    }

    private static JComboBox<Object> combo() {
        JComboBox<Object> c = new JComboBox<>(new Object[]{SELECCIONE});
        c.setFont(Tema.cuerpo());
        return c;
    }

    private static void llenar(JComboBox<Object> c, List<?> valores) {
        List<Object> items = new ArrayList<>();
        items.add(SELECCIONE);
        items.addAll(valores);
        c.setModel(new DefaultComboBoxModel<>(items.toArray()));
    }

    /** Lo elegido en un combo, o null si sigue en "Seleccione". */
    private static Object elegido(JComboBox<Object> c) {
        Object v = c.getSelectedItem();
        return v == null || SELECCIONE.equals(v) ? null : v;
    }

    /* ---------------- datos del cliente ---------------- */

    private JComponent datosPersonales() {
        JButton validar = Ui.boton("Validar");
        validar.addActionListener(e -> validar());
        documento.addActionListener(e -> validar());

        Formulario f = new Formulario();
        f.campo("DNI/RUC:", Ui.fila(documento, validar))
         .campo("Nombre:", nombre)
         .campo("Apellido:", apellido)
         .campo("Correo electrónico:", correo)
         .campo("Teléfono:", telefono)
         .campo("Tipo de cliente:", tipoCliente);

        Grupo g = new Grupo("Datos del Cliente");
        g.add(f, BorderLayout.CENTER);
        return g;
    }

    private void validar() {
        limpiarMarcas();
        Map<Campo, String> errores = Validacion.formulario(
                Solicitud.deDocumento(documento.getText()));
        String error = errores.get(Campo.DOCUMENTO);
        if (error != null) {
            cliente = null;
            List.of(nombre, apellido, correo, telefono, tipoCliente).forEach(c -> c.setText(""));
            reclamo.setVisible(false);
            marcar(Campo.DOCUMENTO);
            JOptionPane.showMessageDialog(this, "DNI/RUC: " + error + ".",
                    "Validación de Usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cliente = Datos.clientePorDocumento(documento.getText());
        nombre.setText(cliente.nombres());
        apellido.setText(cliente.apellidos());
        correo.setText(cliente.correo());
        telefono.setText(cliente.telefono());
        tipoCliente.setText(cliente.tipo().etiqueta());
        reiniciarReclamo();
        reclamo.setVisible(true);
        reclamo.revalidate();
    }

    /* ---------------- datos del reclamo ---------------- */

    private void armarReclamo() {
        Formulario f = new Formulario();
        f.campo("Canal de ingreso:", canal)
         .campo("Local:", local)
         .campo("Reclama sobre:", filaSobre)
         .campo("Producto o servicio:", objeto)
         .campo("Tipo de problema:", tipoProblema)
         .campo("Problema:", problema)
         .campo("Detalle (qué pasó):", detalle)
         .campo("Pedido del consumidor:", pedido);

        Grupo gReclamo = new Grupo("Datos del Reclamo");
        gReclamo.add(f, BorderLayout.CENTER);

        JButton generar = Ui.boton("Generar Reclamo");
        generar.addActionListener(e -> generar());
        JButton cancelar = Ui.boton("Cancelar");
        cancelar.addActionListener(e -> limpiar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 3, Tema.ESP_MD));
        pie.add(generar);
        pie.add(cancelar);

        reclamo.add(gReclamo, BorderLayout.NORTH);
        reclamo.add(sustento(), BorderLayout.CENTER);
        reclamo.add(pie, BorderLayout.SOUTH);
    }

    private JComponent sustento() {
        Formulario f = new Formulario();
        f.campo("Nro de comprobante:", comprobante)
         .campo("Fecha del comprobante:", fechaComprobante);

        JPanel nota = Ui.panel(new BorderLayout(0, Tema.ESP_XS));
        nota.add(notaSustento, BorderLayout.NORTH);
        nota.add(Ui.suave("El comprobante sale de la compra registrada a su nombre. Solo los "
                + "reclamos de Atención (mal trato, demora) no necesitan compra."), BorderLayout.SOUTH);
        nota.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));

        Grupo g = new Grupo("Sustento de la garantía");
        g.add(nota, BorderLayout.NORTH);
        g.add(f, BorderLayout.CENTER);
        return g;
    }

    /** Los canales y locales se leen cada vez: si la gerencia deshabilita uno, ya no se ofrece. */
    private void reiniciarReclamo() {
        llenar(canal, java.util.Arrays.stream(Canal.values()).filter(Catalogos::canalHabilitado).toList());
        llenar(local, Catalogos.localesHabilitados());
        llenar(pedido, Prototipo.PEDIDOS);
        detalle.setText("");
        sobre.clearSelection();
        habilitarLocal();
        recargarObjetos();
    }

    private void habilitarLocal() {
        boolean presencial = elegido(canal) == Canal.PRESENCIAL;
        local.setEnabled(presencial);
        if (!presencial) local.setSelectedIndex(0);
    }

    private TipoObjeto tipoObjeto() {
        if (esProducto.isSelected()) return TipoObjeto.PRODUCTO;
        if (esServicio.isSelected()) return TipoObjeto.SERVICIO;
        return null;
    }

    private void recargarObjetos() {
        recargando = true;
        TipoObjeto tipo = tipoObjeto();
        llenar(objeto, cliente == null || tipo == null ? List.of()
                : Validacion.objetosPara(cliente.documento(), tipo));
        recargando = false;
        recargarTipos();
        mostrarSustento();
    }

    /** Al cambiar el producto o servicio se recarga el tipo y se limpia el problema. */
    private void recargarTipos() {
        recargando = true;
        Object o = elegido(objeto);
        llenar(tipoProblema, o == null ? List.of()
                : Validacion.tiposPara(cliente.documento(), tipoObjeto(), (String) o));
        recargando = false;
        recargarProblemas();
    }

    private void recargarProblemas() {
        Object o = elegido(objeto);
        Object t = elegido(tipoProblema);
        llenar(problema, o == null || t == null ? List.of()
                : Validacion.problemasPara(cliente.documento(), tipoObjeto(), (String) o, (String) t));
    }

    private void mostrarSustento() {
        Object o = elegido(objeto);
        Compra c = o == null || cliente == null ? null
                : Datos.compra(cliente.documento(), tipoObjeto() == TipoObjeto.PRODUCTO, (String) o);
        comprobante.setText(c == null ? "" : c.comprobante());
        fechaComprobante.setText(c == null ? "" : Validacion.fecha(c.fecha()));
        if (o == null) notaSustento.setText(" ");
        else if (c == null) notaSustento.setText("Sin compra registrada: solo puede reclamar por Atención.");
        else notaSustento.setText("Compra verificada por S/ " + String.format("%,.2f", c.monto()) + ".");
    }

    /* ---------------- generacion del reclamo ---------------- */

    private void generar() {
        limpiarMarcas();
        if (cliente == null) { avisar("Primero valide el documento del cliente."); return; }

        Solicitud s = new Solicitud(cliente.documento(), (Canal) elegido(canal), (String) elegido(local),
                tipoObjeto(), (String) elegido(objeto), (String) elegido(tipoProblema),
                (String) elegido(problema), detalle.getText(), (String) elegido(pedido));

        Map<Campo, String> errores = Validacion.formulario(s);
        if (!errores.isEmpty()) {
            StringBuilder m = new StringBuilder("Corrija los campos marcados en rojo:\n");
            errores.forEach((campo, error) -> {
                marcar(campo);
                m.append("\n• ").append(campo.etiqueta()).append(": ").append(error);
            });
            advertir(m.toString());
            return;
        }
        if (!confirmar("¿Confirma el registro del reclamo?")) return;

        Reclamos.Resultado r = Reclamos.registrar(s);
        if (!r.generado()) {
            JOptionPane.showMessageDialog(this, String.join("\n", r.rechazos()),
                    "El reclamo no procede", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mostrarTicket(r.reclamo());
        limpiar();
    }

    private void marcar(Campo campo) {
        JComponent c = controles.get(campo);
        if (c != null) c.setBorder(BORDE_ERROR);
    }

    private void limpiarMarcas() {
        bordes.forEach(JComponent::setBorder);
    }

    /** El comprobante de apertura que se le entrega al cliente: con la fecha en que se le atiende. */
    private void mostrarTicket(Reclamo t) {
        EventoReclamo e = t.eventoActual();

        Tabla resumen = new Tabla(new String[]{"Campo", "Valor"});
        resumen.anchos(220, 420);
        resumen.agregar("Nro de reclamo", t.numero());
        resumen.agregar("Apertura", t.apertura().format(RELOJ));
        resumen.agregar("Canal", t.canal().etiqueta() + ("-".equals(t.local()) ? "" : " - " + t.local()));
        resumen.agregar("Cliente", t.nombreCliente() + " (" + t.tipoCliente() + ")");
        resumen.agregar("Reclama sobre", t.tipoObjeto().etiqueta() + ": " + t.objeto());
        resumen.agregar("Problema", t.tipoProblema() + " - " + t.problema());
        resumen.agregar("Naturaleza", t.naturaleza());
        resumen.agregar("Pedido", t.pedido());
        resumen.agregar("Prioridad", t.prioridad().etiqueta());
        resumen.agregar("Estado", t.estado().etiqueta());
        if (e != null) {
            resumen.agregar("Primer evento", e.evento() + " (protocolo " + e.protocolo() + ")");
            resumen.agregar("Fecha programada", e.inicio().format(RELOJ));
            resumen.agregar("Responsable", e.area() + " - " + e.especialista());
        }
        resumen.agregar("Sustento", t.comprobante().isBlank() ? "Sin comprobante (Atención)"
                : "Comprobante " + t.comprobante() + (t.fueraDeGarantia() ? " - Fuera de garantía" : ""));

        String cita = e == null ? "" : "Su reclamo será atendido el " + e.inicio().format(CITA)
                + " por " + e.area() + ".";
        if (t.sinCapacidad()) cita += " (Sin capacidad dentro del plazo: la gerencia fue notificada.)";

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_SM));
        JPanel cabecera = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        cabecera.add(Ui.fuerte("Reclamo " + t.numero() + " generado."));
        cabecera.add(new JLabel("<html><body style='width:560px'>" + cita + "</body></html>"));
        p.add(cabecera, BorderLayout.NORTH);
        p.add(resumen.enScroll(), BorderLayout.CENTER);
        p.setPreferredSize(Ui.dim(680, 380));

        JOptionPane.showMessageDialog(this, p, "Reclamo Generado", JOptionPane.PLAIN_MESSAGE);
    }

    private void limpiar() {
        limpiarMarcas();
        documento.setText("");
        List.of(nombre, apellido, correo, telefono, tipoCliente).forEach(c -> c.setText(""));
        cliente = null;
        reiniciarReclamo();
        reclamo.setVisible(false);
        documento.requestFocusInWindow();
    }
}
