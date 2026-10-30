package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Dominio.TipoObjeto;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Area - Data Entry: la agenda de trabajo del especialista.
 *
 * Nadie elige que atender: el sistema asigna cada evento al especialista de
 * menor carga y le fija la cita. Aqui el especialista ve solo sus eventos,
 * en el orden de su agenda, y al atender uno ejecuta las acciones del
 * protocolo. El siguiente evento se programa solo.
 */
public class AreaDataEntryPantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField idEmpleado = Ui.texto(8);
    private final JTextField tipoEmpleado = Ui.soloLectura();
    private final JTextField area = Ui.soloLectura();
    private final JTextField fecha = Ui.soloLectura();

    private final Tabla tabla = new Tabla(new String[]{
            "Fecha programada", "Nro Reclamo", "Evento", "Cliente", "Objeto", "Problema",
            "Prioridad", "Estado", "Límite", "Vencido"});
    private List<Reclamo> visibles = new ArrayList<>();
    private Empleado empleado;
    private boolean preguntando;

    public AreaDataEntryPantalla() {
        super("Atender Reclamo",
                "Los eventos que el sistema le asignó, en el orden de su agenda.");

        tabla.anchos(130, 110, 150, 180, 200, 160, 80, 100, 120, 70);
        fecha.setText(LocalDate.now().format(FECHA));

        JButton atender = Ui.boton("Inspeccionar");
        atender.addActionListener(e -> inspeccionar());
        JButton refrescar = Ui.boton("Refrescar");
        refrescar.addActionListener(e -> refrescar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 2, Tema.ESP_MD));
        pie.add(atender);
        pie.add(refrescar);

        Grupo g = new Grupo("Mis eventos asignados");
        g.add(datosEmpleado(), BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(g, BorderLayout.CENTER);

        refrescar();
        Reclamos.alCambiar(t -> refrescar());
        idEmpleado.setToolTipText("Escriba su ID y pulse Enter o Ingresar");
        idEmpleado.addActionListener(e -> ingresar(idEmpleado.getText()));
        // la pantalla se crea una sola vez: se pregunta cada vez que se muestra
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                SwingUtilities.invokeLater(this::pedirEmpleado);
            }
        });
    }

    private JComponent datosEmpleado() {
        JPanel p = Ui.panel(new GridLayout(2, 4, Tema.ESP_MD, Tema.ESP_SM));
        p.add(Ui.etiqueta("ID Empleado:"));
        JButton ingresar = Ui.boton("Ingresar");
        ingresar.addActionListener(e -> ingresar(idEmpleado.getText()));
        JPanel filaId = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        filaId.add(idEmpleado, BorderLayout.CENTER);
        filaId.add(ingresar, BorderLayout.EAST);
        p.add(filaId);
        p.add(Ui.etiqueta("Área:"));
        p.add(area);
        p.add(Ui.etiqueta("Tipo Empleado:"));
        p.add(tipoEmpleado);
        p.add(Ui.etiqueta("Fecha:"));
        p.add(fecha);
        p.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));
        return p;
    }

    /**
     * La ventana "Ingresar Datos" del prototipo. Se abre al entrar a la
     * pantalla mientras nadie se haya identificado; si se cancela, el ID
     * tambien se puede escribir en el campo de arriba.
     */
    private void pedirEmpleado() {
        if (preguntando || empleado != null || !isShowing()) return;
        preguntando = true;
        try {
            JTextField campo = Ui.texto(12);
            campo.setText("E01");
            campo.selectAll();

            Formulario f = new Formulario();
            f.campo("ID Empleado:", campo);
            f.setBorder(Ui.relleno(Tema.ESP_MD));

            int r = JOptionPane.showConfirmDialog(this, f, "Ingresar Datos",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r == JOptionPane.OK_OPTION) ingresar(campo.getText());
        } finally {
            preguntando = false;
        }
    }

    /** Identifica al empleado y carga su agenda. */
    private void ingresar(String texto) {
        Empleado e = Prototipo.EMPLEADOS.get(texto.trim().toUpperCase());
        if (e == null) {
            avisar("El ID de empleado no existe. Pruebe con "
                    + String.join(", ", Prototipo.EMPLEADOS.keySet()) + ".");
            return;
        }
        if (e.esSupervisor()) {
            avisar(e.id() + " es supervisor: revisa los reclamos en Área - Revisión de Calidad.");
            return;
        }
        empleado = e;
        idEmpleado.setText(e.id());
        tipoEmpleado.setText(e.tipo());
        area.setText(e.area());
        refrescar();
    }

    /**
     * La agenda del empleado: los reclamos cuyo evento pendiente es suyo,
     * ordenados por fecha programada y, a la misma hora, por prioridad.
     */
    private void refrescar() {
        visibles = empleado == null ? List.of() : Reclamos.todos().stream()
                .filter(t -> esSuyo(t, empleado.id()))
                .sorted(Comparator.comparing((Reclamo t) -> t.eventoActual().inicio())
                        .thenComparing(Reclamo::prioridad))
                .toList();
        tabla.limpiar();
        for (Reclamo t : visibles) {
            EventoReclamo e = t.eventoActual();
            tabla.agregar(e.inicio().format(RELOJ), t.numero(), e.evento(), t.nombreCliente(),
                    t.objeto(), t.problema(), t.prioridad().etiqueta(), t.estado().etiqueta(),
                    t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ),
                    t.vencido() ? "Sí" : "No");
        }
    }

    private static boolean esSuyo(Reclamo t, String empleado) {
        EventoReclamo e = t.eventoActual();
        return e != null && e.agendado() && empleado.equals(e.especialista())
                && (t.estado() == Estado.ASIGNADO || t.estado() == Estado.EN_ATENCION);
    }

    private Reclamo seleccionado() {
        int i = tabla.filaModelo();
        return i < 0 || i >= visibles.size() ? null : visibles.get(i);
    }

    /** La ventana de inspeccion: ejecuta las acciones del protocolo del evento. */
    private void inspeccionar() {
        Reclamo t = seleccionado();
        if (t == null) { avisar("Seleccione un evento de su agenda."); return; }
        if (empleado == null) { avisar("Primero ingrese su ID de empleado."); return; }
        EventoReclamo ev = t.eventoActual();
        List<String[]> acciones = Prototipo.pasosDe(ev.protocolo(), ev.evento());
        if (acciones.isEmpty()) {
            advertir("El protocolo " + ev.protocolo() + " no tiene acciones definidas "
                    + "en el Catálogo de Protocolos.");
            return;
        }
        boolean ultimo = Atencion.siguiente(t, ev) == null;
        boolean preguntaReparable = Atencion.PENDIENTE_INSPECCION.equals(t.solucionSugerida());

        JPanel datos = Ui.panel(new GridLayout(0, 4, Tema.ESP_MD, Tema.ESP_XS));
        dato(datos, "Nro Reclamo:", t.numero());
        dato(datos, "Prioridad:", t.prioridad().etiqueta());
        dato(datos, "Objeto:", t.objeto());
        dato(datos, "Instancia:", t.instancia().etiqueta());
        dato(datos, "Tipo Problema:", t.tipoProblema());
        dato(datos, "Problema:", t.problema());
        dato(datos, "Evento:", ev.evento());
        dato(datos, "Cita:", ev.inicio().format(RELOJ));
        dato(datos, "Garantía:", t.tipoObjeto() == TipoObjeto.PRODUCTO
                ? (t.fueraDeGarantia() ? "Fuera de garantía" : "Vigente") : "-");
        dato(datos, "Límite:", t.limiteAtencion() == null ? "-"
                : t.limiteAtencion().format(RELOJ));

        Grupo grupoDatos = new Grupo("Datos del Reclamo");
        grupoDatos.add(datos, BorderLayout.CENTER);

        JPanel pasos = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        List<JCheckBox> casillas = new ArrayList<>();
        for (String[] a : acciones) {
            JCheckBox c = new JCheckBox(a[0] + "   (" + a[1] + " min, " + a[2] + ", " + a[3] + ")");
            c.setFont(Tema.cuerpo());
            c.setOpaque(false);
            casillas.add(c);
            pasos.add(c);
        }

        Grupo grupoProtocolo = new Grupo(ev.evento() + " - protocolo " + ev.protocolo());
        grupoProtocolo.add(pasos, BorderLayout.NORTH);
        grupoProtocolo.add(Ui.suave("Las acciones críticas no pueden omitirse para "
                + "completar el evento."), BorderLayout.SOUTH);

        JCheckBox reparable = new JCheckBox("El bien admite reparación (lo lee el motor de reglas)");
        reparable.setFont(Tema.cuerpo());
        reparable.setOpaque(false);
        JComboBox<String> solucion = Ui.combo(MotorReglas.ACCIONES);
        if (MotorReglas.ACCIONES.contains(t.solucionSugerida())) solucion.setSelectedItem(t.solucionSugerida());

        JPanel resultado = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        if (preguntaReparable) resultado.add(reparable);
        if (ultimo) {
            resultado.add(Ui.fila(Ui.etiqueta("Solución aplicada:"), solucion));
            resultado.add(Ui.suave("Sugerida por el motor: " + (preguntaReparable
                    ? "se calcula con la inspección" : t.solucionSugerida())));
        } else {
            resultado.add(Ui.suave("Al completar, el sistema programa el siguiente evento: "
                    + Atencion.siguiente(t, ev)[0] + "."));
        }
        Grupo grupoResultado = new Grupo("Resultado");
        grupoResultado.add(resultado, BorderLayout.CENTER);

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(grupoDatos, BorderLayout.NORTH);
        cuerpo.add(grupoProtocolo, BorderLayout.CENTER);
        cuerpo.add(grupoResultado, BorderLayout.SOUTH);
        cuerpo.setPreferredSize(Ui.dim(680, 440));

        Object[] opciones = {"Completar", "Proponer rechazo", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, cuerpo, "Inspeccionar",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);

        if (r == 0) {
            List<String> hechas = new ArrayList<>();
            for (int i = 0; i < acciones.size(); i++) {
                if (casillas.get(i).isSelected()) hechas.add(acciones.get(i)[0]);
            }
            completar(t, hechas, preguntaReparable ? reparable.isSelected() : null,
                    ultimo ? (String) solucion.getSelectedItem() : null);
        } else if (r == 1) {
            rechazar(t);
        }
    }

    private void completar(Reclamo t, List<String> hechas, Boolean reparable, String solucion) {
        try {
            String mensaje = Atencion.completar(t, empleado.id(), hechas, reparable, solucion);
            refrescar();
            avisar("Reclamo " + t.numero() + ": " + mensaje);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            advertir(ex.getMessage() + " No se puede completar el evento.");
        }
    }

    /** El rechazo no se notifica al cliente: lo revisa antes el supervisor. */
    private void rechazar(Reclamo t) {
        JTextArea motivo = Ui.area(3);
        Formulario f = new Formulario();
        f.campo("Motivo del rechazo:", Ui.scroll(motivo));
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Proponer Rechazo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (motivo.getText().isBlank()) { avisar("Indique el motivo del rechazo."); return; }

        Atencion.proponerRechazo(t, empleado.id(), motivo.getText().trim());
        refrescar();
        avisar("Rechazo del reclamo " + t.numero() + " enviado al supervisor. "
                + "El cliente recién será notificado cuando lo apruebe.");
    }

    private static void dato(JPanel destino, String etiqueta, String valor) {
        destino.add(Ui.etiqueta(etiqueta));
        destino.add(Ui.fuerte(valor));
    }
}
