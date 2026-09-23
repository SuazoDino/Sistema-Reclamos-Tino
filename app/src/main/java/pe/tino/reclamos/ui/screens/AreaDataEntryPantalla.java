package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Area - Data Entry: la cola de trabajo del especialista.
 *
 * Trabaja sobre tickets, no sobre reclamos sueltos: el usuario se identifica,
 * ve los tickets abiertos de su area y al inspeccionar ejecuta las acciones
 * del protocolo, que es lo que hace avanzar el estado.
 */
public class AreaDataEntryPantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField idEmpleado = Ui.soloLectura();
    private final JTextField tipoEmpleado = Ui.soloLectura();
    private final JTextField area = Ui.soloLectura();
    private final JTextField fecha = Ui.soloLectura();

    private final Tabla tabla = new Tabla(new String[]{
            "Nro Ticket", "Apertura", "Cliente", "Objeto", "Problema",
            "Prioridad", "Estado", "Limite", "Vencido"});
    private List<Ticket> visibles = new ArrayList<>();

    public AreaDataEntryPantalla() {
        super("Atender Ticket", "Tickets abiertos asignados al usuario del area.");

        tabla.anchos(110, 120, 180, 200, 160, 90, 110, 120, 80);
        fecha.setText(LocalDate.now().format(FECHA));

        JButton asignarme = Ui.boton("Asignarme");
        asignarme.addActionListener(e -> asignarme());
        JButton inspeccionar = Ui.boton("Inspeccionar");
        inspeccionar.addActionListener(e -> inspeccionar());
        JButton refrescar = Ui.boton("Refrescar");
        refrescar.addActionListener(e -> refrescar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 2, Tema.ESP_MD));
        pie.add(asignarme);
        pie.add(inspeccionar);
        pie.add(refrescar);

        Grupo g = new Grupo("Tickets Registrados");
        g.add(datosEmpleado(), BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(g, BorderLayout.CENTER);

        refrescar();
        Tickets.alCambiar(t -> refrescar());
        SwingUtilities.invokeLater(this::pedirEmpleado);
    }

    private JComponent datosEmpleado() {
        JPanel p = Ui.panel(new GridLayout(2, 4, Tema.ESP_MD, Tema.ESP_SM));
        p.add(Ui.etiqueta("ID Empleado:"));
        p.add(idEmpleado);
        p.add(Ui.etiqueta("Area:"));
        p.add(area);
        p.add(Ui.etiqueta("Tipo Empleado:"));
        p.add(tipoEmpleado);
        p.add(Ui.etiqueta("Fecha:"));
        p.add(fecha);
        p.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));
        return p;
    }

    /** La ventana "Ingresar Datos" con la que arranca el prototipo. */
    private void pedirEmpleado() {
        JTextField campo = Ui.texto(12);
        campo.setText("E01");

        Formulario f = new Formulario();
        f.campo("ID Empleado:", campo);
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Ingresar Datos",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        Empleado e = Prototipo.EMPLEADOS.get(campo.getText().trim().toUpperCase());
        if (e == null) {
            avisar("El ID de empleado no existe. Pruebe con E01, E02 o E03.");
            pedirEmpleado();
            return;
        }
        idEmpleado.setText(e.id());
        tipoEmpleado.setText(e.tipo());
        area.setText(e.area());
        refrescar();
    }

    private void refrescar() {
        visibles = Tickets.abiertos();
        tabla.limpiar();
        for (Ticket t : visibles) {
            tabla.agregar(t.numero(), t.apertura().format(RELOJ), t.nombreCliente(),
                    t.objeto(), t.problema(), t.prioridad().etiqueta(), t.estado().etiqueta(),
                    t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ),
                    t.vencido() ? "Si" : "No");
        }
    }

    private Ticket seleccionado() {
        int i = tabla.filaModelo();
        return i < 0 || i >= visibles.size() ? null : visibles.get(i);
    }

    /** Tomar el ticket: le pone area, especialista y protocolo. */
    private void asignarme() {
        Ticket t = seleccionado();
        if (t == null) { avisar("Seleccione un ticket de la cola."); return; }
        if (idEmpleado.getText().isBlank()) { avisar("Primero ingrese su ID de empleado."); return; }

        t.asignar(area.getText(), idEmpleado.getText(),
                Prototipo.protocoloDe(t.tipoProblema()), idEmpleado.getText());
        Tickets.notificar();
        refrescar();
        avisar("Ticket " + t.numero() + " asignado a " + idEmpleado.getText() + ".");
    }

    /** La ventana de inspeccion: ejecuta las acciones del protocolo. */
    private void inspeccionar() {
        Ticket t = seleccionado();
        if (t == null) { avisar("Seleccione un ticket de la cola."); return; }
        if (idEmpleado.getText().isBlank()) { avisar("Primero ingrese su ID de empleado."); return; }
        if (t.estado() == Estado.REGISTRADO) {
            avisar("El ticket todavia no esta asignado. Use Asignarme primero.");
            return;
        }
        if (Prototipo.accionesDe(t.protocolo()).isEmpty()) {
            advertir("El protocolo " + t.protocolo() + " no tiene acciones definidas "
                    + "en el Catalogo de Protocolos.");
            return;
        }

        JPanel datos = Ui.panel(new GridLayout(0, 4, Tema.ESP_MD, Tema.ESP_XS));
        dato(datos, "Nro Ticket:", t.numero());
        dato(datos, "Prioridad:", t.prioridad().etiqueta());
        dato(datos, "Objeto:", t.objeto());
        dato(datos, "Instancia:", t.instancia().etiqueta());
        dato(datos, "Tipo Problema:", t.tipoProblema());
        dato(datos, "Problema:", t.problema());
        dato(datos, "Protocolo:", t.protocolo());
        dato(datos, "Limite:", t.limiteAtencion() == null ? "-"
                : t.limiteAtencion().format(RELOJ));

        Grupo grupoDatos = new Grupo("Datos del Ticket");
        grupoDatos.add(datos, BorderLayout.CENTER);

        JPanel pasos = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        List<JCheckBox> casillas = new ArrayList<>();
        List<String[]> acciones = Prototipo.accionesDe(t.protocolo());
        for (String[] a : acciones) {
            JCheckBox c = new JCheckBox(a[0] + "   (" + a[1] + " s, " + a[2] + ", " + a[3] + ")");
            c.setFont(Tema.cuerpo());
            c.setOpaque(false);
            casillas.add(c);
            pasos.add(c);
        }

        Grupo grupoProtocolo = new Grupo("Acciones del protocolo " + t.protocolo());
        grupoProtocolo.add(pasos, BorderLayout.NORTH);
        grupoProtocolo.add(Ui.suave("Las acciones criticas no pueden omitirse para "
                + "dar el ticket por resuelto."), BorderLayout.SOUTH);

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(grupoDatos, BorderLayout.NORTH);
        cuerpo.add(grupoProtocolo, BorderLayout.CENTER);
        cuerpo.setPreferredSize(Ui.dim(620, 320));

        Object[] opciones = {"Resolver", "Rechazar", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, cuerpo, "Inspeccionar",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);

        if (r == 0) resolver(t, acciones, casillas);
        else if (r == 1) rechazar(t);
    }

    private void resolver(Ticket t, List<String[]> acciones, List<JCheckBox> casillas) {
        for (int i = 0; i < acciones.size(); i++) {
            if ("Critico".equalsIgnoreCase(acciones.get(i)[3]) && !casillas.get(i).isSelected()) {
                advertir("Falta la accion critica \"" + acciones.get(i)[0]
                        + "\". No se puede resolver el ticket.");
                return;
            }
        }
        long hechas = casillas.stream().filter(AbstractButton::isSelected).count();
        if (t.estado() == Estado.ASIGNADO) t.cambiarEstado(Estado.EN_ATENCION, idEmpleado.getText(), "");
        t.cambiarEstado(Estado.RESUELTO, idEmpleado.getText(),
                hechas + " de " + acciones.size() + " acciones ejecutadas");
        Tickets.notificar();
        refrescar();
        avisar("Ticket " + t.numero() + " resuelto.");
    }

    private void rechazar(Ticket t) {
        JTextArea motivo = Ui.area(3);
        Formulario f = new Formulario();
        f.campo("Motivo del rechazo:", Ui.scroll(motivo));
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Rechazar Ticket",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (motivo.getText().isBlank()) { avisar("Indique el motivo del rechazo."); return; }

        t.cambiarEstado(Estado.RECHAZADO, idEmpleado.getText(), motivo.getText().trim());
        Tickets.notificar();
        refrescar();
        avisar("Ticket " + t.numero() + " rechazado. El cliente tiene "
                + t.instancia().diasImpugnacion() + " dias para impugnar.");
    }

    private static void dato(JPanel destino, String etiqueta, String valor) {
        destino.add(Ui.etiqueta(etiqueta));
        destino.add(Ui.fuerte(valor));
    }
}
