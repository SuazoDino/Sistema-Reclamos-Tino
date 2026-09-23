package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario Reclamo (Cliente - Data Entry). Registrar un reclamo abre un
 * ticket, que es lo que el sistema numera y controla.
 *
 * El comprobante de compra va aparte, en su propio grupo y como dato
 * opcional, porque es el sustento de la garantia y no el ticket: hay
 * reclamos por atencion o por servicio donde no hay comprobante, y una misma
 * compra puede originar varios tickets.
 */
public class ClienteDataEntryPantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /* datos del cliente */
    private final JTextField documento = Ui.texto(16);
    private final JTextField nombre = Ui.soloLectura();
    private final JTextField apellido = Ui.soloLectura();
    private final JTextField correo = Ui.soloLectura();
    private final JTextField telefono = Ui.soloLectura();
    private final JTextField tipoCliente = Ui.soloLectura();

    /* datos del reclamo */
    private final JComboBox<Canal> canal = Ui.combo(List.of(Canal.values()));
    private final JComboBox<String> local;
    private final JRadioButton esProducto = new JRadioButton("Producto", true);
    private final JRadioButton esServicio = new JRadioButton("Servicio");
    private final JComboBox<String> objeto = new JComboBox<>();
    private final JComboBox<String> tipoProblema = Ui.combo(Prototipo.tiposDeProblema());
    private final JComboBox<String> problema = new JComboBox<>();

    /* sustento */
    private final JCheckBox tieneComprobante = new JCheckBox("El cliente presenta comprobante de compra");
    private final JTextField comprobante = Ui.texto(14);
    private final JTextField fechaComprobante = Ui.texto(12);

    private final JPanel reclamo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
    private Cliente cliente;

    public ClienteDataEntryPantalla() {
        super("Formulario Reclamo",
                "Registrar un reclamo abre un ticket. El comprobante es el sustento, no el ticket.");

        local = Ui.combo(Datos.catalogo("locales").habilitados());

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(esProducto);
        grupo.add(esServicio);
        esProducto.setOpaque(false);
        esServicio.setOpaque(false);
        esProducto.setFont(Tema.cuerpo());
        esServicio.setFont(Tema.cuerpo());
        esProducto.addActionListener(e -> recargarObjetos());
        esServicio.addActionListener(e -> recargarObjetos());

        tipoProblema.addActionListener(e -> recargarProblemas());
        recargarObjetos();
        recargarProblemas();

        tieneComprobante.setOpaque(false);
        tieneComprobante.setFont(Tema.cuerpo());
        tieneComprobante.addActionListener(e -> habilitarComprobante());
        habilitarComprobante();

        armarReclamo();
        reclamo.setVisible(false);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(datosPersonales(), BorderLayout.NORTH);
        raiz.add(reclamo, BorderLayout.CENTER);

        contenido().add(Ui.scrollVertical(raiz), BorderLayout.CENTER);
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
        cliente = Datos.clientePorDocumento(documento.getText());
        if (cliente == null) {
            List.of(nombre, apellido, correo, telefono, tipoCliente)
                    .forEach(c -> c.setText(""));
            reclamo.setVisible(false);
            JOptionPane.showMessageDialog(this, "Su usuario no es válido.",
                    "Validación de Usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        nombre.setText(cliente.nombres());
        apellido.setText(cliente.apellidos());
        correo.setText(cliente.correo());
        telefono.setText(cliente.telefono());
        tipoCliente.setText(cliente.tipo().etiqueta());
        reclamo.setVisible(true);
        reclamo.revalidate();
    }

    /* ---------------- datos del reclamo ---------------- */

    private void armarReclamo() {
        Formulario f = new Formulario();
        f.campo("Canal de ingreso:", canal)
         .campo("Local:", local)
         .campo("Reclama sobre:", Ui.fila(esProducto, esServicio))
         .campo("Producto o servicio:", objeto)
         .campo("Tipo de problema:", tipoProblema)
         .campo("Problema:", problema);

        Grupo gReclamo = new Grupo("Datos del Reclamo");
        gReclamo.add(f, BorderLayout.CENTER);

        JButton generar = Ui.boton("Generar Ticket");
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
        nota.add(tieneComprobante, BorderLayout.NORTH);
        nota.add(Ui.suave("El comprobante solo prueba la garantía. No es el ticket: una misma "
                + "compra puede originar varios tickets, y hay reclamos de atención o de "
                + "servicio que no tienen comprobante."), BorderLayout.SOUTH);
        nota.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));

        Grupo g = new Grupo("Sustento de la garantía");
        g.add(nota, BorderLayout.NORTH);
        g.add(f, BorderLayout.CENTER);
        return g;
    }

    private void habilitarComprobante() {
        comprobante.setEnabled(tieneComprobante.isSelected());
        fechaComprobante.setEnabled(tieneComprobante.isSelected());
    }

    private void recargarObjetos() {
        List<String> valores = new ArrayList<>();
        if (esProducto.isSelected()) {
            Catalogos.marcas().forEach(m -> valores.add(m[0] + " " + m[1] + " " + m[2]));
        } else {
            Catalogos.servicios().stream()
                    .filter(s -> "Habilitado".equals(s[3]))
                    .forEach(s -> valores.add(s[1]));
        }
        objeto.setModel(new DefaultComboBoxModel<>(valores.toArray(new String[0])));
        objeto.setFont(Tema.cuerpo());
    }

    private void recargarProblemas() {
        problema.setModel(new DefaultComboBoxModel<>(
                Prototipo.problemasDe((String) tipoProblema.getSelectedItem()).toArray(new String[0])));
        problema.setFont(Tema.cuerpo());
    }

    /* ---------------- generacion del ticket ---------------- */

    private void generar() {
        if (cliente == null) { avisar("Primero valide el documento del cliente."); return; }
        if (tieneComprobante.isSelected() && comprobante.getText().isBlank()) {
            avisar("Indique el número de comprobante o desmarque la casilla.");
            return;
        }
        if (!confirmar("¿Confirma el registro del reclamo?")) return;

        Ticket t = Tickets.abrir(
                (Canal) canal.getSelectedItem(),
                String.valueOf(local.getSelectedItem()),
                cliente.documento(),
                cliente.nombreCompleto(),
                tipoCliente.getText(),
                esProducto.isSelected() ? TipoObjeto.PRODUCTO : TipoObjeto.SERVICIO,
                String.valueOf(objeto.getSelectedItem()),
                String.valueOf(tipoProblema.getSelectedItem()),
                String.valueOf(problema.getSelectedItem()),
                tieneComprobante.isSelected() ? comprobante.getText().trim() : "",
                tieneComprobante.isSelected() ? fechaComprobante.getText().trim() : "");

        mostrarTicket(t);
        limpiar();
    }

    /** El comprobante de apertura que se le entrega al cliente. */
    private void mostrarTicket(Ticket t) {
        Tabla resumen = new Tabla(new String[]{"Campo", "Valor"});
        resumen.anchos(220, 420);
        resumen.agregar("Nro de ticket", t.numero());
        resumen.agregar("Apertura", t.apertura().format(RELOJ));
        resumen.agregar("Canal", t.canal().etiqueta());
        resumen.agregar("Local", t.local());
        resumen.agregar("Cliente", t.nombreCliente());
        resumen.agregar("Tipo de cliente", t.tipoCliente());
        resumen.agregar("Reclama sobre", t.tipoObjeto().etiqueta() + ": " + t.objeto());
        resumen.agregar("Problema", t.tipoProblema() + " - " + t.problema());
        resumen.agregar("Instancia", t.instancia().etiqueta());
        resumen.agregar("Prioridad", t.prioridad().etiqueta());
        resumen.agregar("Estado", t.estado().etiqueta());
        resumen.agregar("Límite de atención", t.limiteAtencion().format(RELOJ));
        resumen.agregar("Sustento",
                t.comprobante().isBlank() ? "Sin comprobante" : "Comprobante " + t.comprobante());

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_SM));
        p.add(Ui.fuerte("Ticket " + t.numero() + " generado."), BorderLayout.NORTH);
        p.add(resumen.enScroll(), BorderLayout.CENTER);
        p.setPreferredSize(Ui.dim(680, 330));

        JOptionPane.showMessageDialog(this, p, "Ticket Generado", JOptionPane.PLAIN_MESSAGE);
    }

    private void limpiar() {
        documento.setText("");
        List.of(nombre, apellido, correo, telefono, tipoCliente).forEach(c -> c.setText(""));
        comprobante.setText("");
        fechaComprobante.setText("");
        tieneComprobante.setSelected(false);
        habilitarComprobante();
        cliente = null;
        reclamo.setVisible(false);
        documento.requestFocusInWindow();
    }
}
