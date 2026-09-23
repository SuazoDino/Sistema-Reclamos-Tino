package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Dominio;
import pe.tino.reclamos.repo.Tickets;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Consulta de Tickets: la vista completa de un ticket, con su ficha, sus
 * plazos y su bitacora.
 *
 * Es la pantalla que responde "que es un ticket y que lo compone": arriba el
 * listado, abajo todo lo que el sistema controla de el. El comprobante de
 * compra aparece como un dato mas del sustento, no como el ticket.
 */
public class TicketPantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Tabla tabla = new Tabla(new String[]{
            "Nro Ticket", "Apertura", "Cliente", "Objeto", "Problema",
            "Instancia", "Prioridad", "Estado", "Limite atencion"});
    private final JPanel ficha = Ui.panel(new BorderLayout());
    private final Tabla bitacora = new Tabla(new String[]{"Momento", "Usuario", "Accion", "Detalle"});

    public TicketPantalla() {
        super("Consulta de Tickets",
                "Un ticket por reclamo presentado: lo que el sistema numera, asigna y mide.");

        tabla.anchos(110, 120, 190, 220, 170, 90, 90, 110, 120);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarFicha();
        });
        bitacora.anchos(130, 90, 140, 400);

        Grupo listado = Grupo.ajustado("Tickets registrados");
        listado.add(tabla.enScroll(), BorderLayout.CENTER);
        listado.setPreferredSize(Ui.dim(100, 220));

        Grupo detalle = new Grupo("Ficha del ticket");
        detalle.add(ficha, BorderLayout.CENTER);

        Grupo log = Grupo.ajustado("Bitacora");
        log.add(bitacora.enScroll(), BorderLayout.CENTER);
        log.setPreferredSize(Ui.dim(420, 100));

        JPanel abajo = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        abajo.add(detalle, BorderLayout.CENTER);
        abajo.add(log, BorderLayout.EAST);

        contenido().add(listado, BorderLayout.NORTH);
        contenido().add(abajo, BorderLayout.CENTER);

        refrescar();
        Tickets.alCambiar(t -> refrescar());
        mostrarFicha();
    }

    private void refrescar() {
        int seleccion = tabla.getSelectedRow();
        tabla.limpiar();
        for (Ticket t : Tickets.todos()) {
            tabla.agregar(t.numero(), t.apertura().format(RELOJ), t.nombreCliente(),
                    t.objeto(), t.problema(), t.instancia().etiqueta(),
                    t.prioridad().etiqueta(), t.estado().etiqueta(),
                    t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ));
        }
        if (seleccion >= 0 && seleccion < tabla.getRowCount()) {
            tabla.setRowSelectionInterval(seleccion, seleccion);
        }
    }

    private Ticket seleccionado() {
        int i = tabla.filaModelo();
        List<Ticket> todos = Tickets.todos();
        return i < 0 || i >= todos.size() ? null : todos.get(i);
    }

    private void mostrarFicha() {
        ficha.removeAll();
        bitacora.limpiar();
        Ticket t = seleccionado();

        if (t == null) {
            ficha.add(Ui.suave("Seleccione un ticket del listado."), BorderLayout.NORTH);
            ficha.revalidate();
            ficha.repaint();
            return;
        }

        JPanel datos = Ui.panel(new GridLayout(0, 4, Tema.ESP_MD, Tema.ESP_XS));
        dato(datos, "Nro de ticket:", t.numero());
        dato(datos, "Apertura:", t.apertura().format(RELOJ));
        dato(datos, "Canal:", t.canal().etiqueta());
        dato(datos, "Local:", t.local());
        dato(datos, "Cliente:", t.nombreCliente());
        dato(datos, "Documento:", t.documentoCliente());
        dato(datos, "Tipo de cliente:", t.tipoCliente());
        dato(datos, "Instancia:", t.instancia().etiqueta());
        dato(datos, "Reclama sobre:", t.tipoObjeto().etiqueta());
        dato(datos, "Objeto:", t.objeto());
        dato(datos, "Tipo de problema:", t.tipoProblema());
        dato(datos, "Problema:", t.problema());
        dato(datos, "Prioridad:", t.prioridad().etiqueta());
        dato(datos, "Estado:", t.estado().etiqueta());
        dato(datos, "Area:", t.area());
        dato(datos, "Especialista:", t.especialista());
        dato(datos, "Protocolo:", t.protocolo());
        dato(datos, "Limite de atencion:",
                t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ));
        dato(datos, "Limite de impugnacion:",
                t.limiteImpugnacion() == null ? "-" : t.limiteImpugnacion().format(RELOJ));
        dato(datos, "Vencido:", t.vencido() ? "Si" : "No");

        Grupo sustento = new Grupo("Sustento (documento del cliente, no es el ticket)",
                new GridLayout(1, 4, Tema.ESP_MD, 0));
        sustento.add(Ui.etiqueta("Comprobante de compra:"));
        sustento.add(Ui.fuerte(t.comprobante().isBlank() ? "Sin comprobante" : t.comprobante()));
        sustento.add(Ui.etiqueta("Fecha del comprobante:"));
        sustento.add(Ui.fuerte(t.fechaComprobante().isBlank() ? "-" : t.fechaComprobante()));

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(datos, BorderLayout.NORTH);
        cuerpo.add(sustento, BorderLayout.CENTER);
        cuerpo.add(acciones(t), BorderLayout.SOUTH);
        ficha.add(cuerpo, BorderLayout.NORTH);

        t.bitacora().forEach(e -> bitacora.agregar(e.momento().format(RELOJ),
                e.usuario(), e.accion(), e.detalle()));

        ficha.revalidate();
        ficha.repaint();
    }

    /** Solo se ofrecen las transiciones que la maquina de estados permite. */
    private JComponent acciones(Ticket t) {
        JPanel p = Ui.panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_SM, 0));
        p.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        p.add(Ui.etiqueta("Puede pasar a:"));

        if (t.estado().siguientes().isEmpty()) {
            p.add(Ui.suave("ningun estado; el ticket esta cerrado"));
            return p;
        }
        for (Dominio.Estado destino : t.estado().siguientes()) {
            JButton b = Ui.boton(destino.etiqueta());
            b.addActionListener(e -> {
                if (destino == Dominio.Estado.IMPUGNADO) {
                    impugnar(t);
                    return;
                }
                t.cambiarEstado(destino, "Operador", "");
                Tickets.notificar();
                refrescar();
                mostrarFicha();
            });
            p.add(b);
        }
        return p;
    }

    private void impugnar(Ticket t) {
        JTextArea motivo = Ui.area(3);
        Formulario f = new Formulario();
        f.campo("Ticket:", Ui.fuerte(t.numero()))
         .campo("Instancia actual:", Ui.etiqueta(t.instancia().etiqueta()))
         .campo("Pasa a:", Ui.fuerte(t.instancia().siguiente().etiqueta()
                 + " - " + t.instancia().siguiente().resuelve()))
         .campo("Motivo:", Ui.scroll(motivo));
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Impugnar Ticket",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        if (motivo.getText().isBlank()) { avisar("Indique el motivo de la impugnacion."); return; }
        if (!t.impugnar(motivo.getText().trim(), "Cliente")) {
            advertir("El ticket no admite impugnacion: el plazo vencio o ya esta en la "
                    + "ultima instancia.");
            return;
        }
        Tickets.notificar();
        refrescar();
        mostrarFicha();
    }

    private static void dato(JPanel destino, String etiqueta, String valor) {
        destino.add(Ui.etiqueta(etiqueta));
        destino.add(Ui.fuerte(valor));
    }
}
