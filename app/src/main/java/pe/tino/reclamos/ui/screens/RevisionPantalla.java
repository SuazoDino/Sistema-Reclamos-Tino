package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Calidad;
import pe.tino.reclamos.repo.Dominio.Revision;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.repo.Reclamos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Revision de Calidad (Area - Supervisor). Ningun reclamo se cierra sin
 * revision: la automatica revisa todos, y aqui el supervisor ve los que no
 * pasaron, una muestra al azar de los que si, y todos los rechazos antes de
 * que se notifiquen al cliente.
 */
public class RevisionPantalla extends Pantalla {

    private final JTextField idSupervisor = Ui.texto(8);
    private final JSpinner muestra = new JSpinner(new SpinnerNumberModel(Calidad.muestraCada(), 1, 100, 1));
    private final Tabla tabla = new Tabla(new String[]{
            "Nro Reclamo", "Motivo de revisión", "Cliente", "Objeto", "Problema",
            "Especialista", "Solución aplicada", "Sugerida", "Observaciones"});
    private List<Reclamo> visibles = List.of();
    private Empleado supervisor;

    public RevisionPantalla() {
        super("Revisión de Calidad",
                "Reclamos observados, la muestra al azar y los rechazos, antes de llegar al cliente.");

        tabla.anchos(110, 140, 170, 190, 150, 90, 130, 130, 320);
        muestra.setFont(Tema.cuerpo());
        muestra.addChangeListener(e -> Calidad.muestraCada((Integer) muestra.getValue()));

        JButton ingresar = Ui.boton("Ingresar");
        ingresar.addActionListener(e -> ingresar());
        idSupervisor.addActionListener(e -> ingresar());
        idSupervisor.setText("S01");

        JPanel cabecera = Ui.panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_SM, 0));
        cabecera.add(Ui.etiqueta("ID Supervisor:"));
        cabecera.add(idSupervisor);
        cabecera.add(ingresar);
        cabecera.add(Ui.suave("     Muestra al azar: 1 de cada"));
        cabecera.add(muestra);
        cabecera.add(Ui.suave("reclamos aprobados"));
        cabecera.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        JButton aprobar = Ui.boton("Aprobar");
        aprobar.addActionListener(e -> aprobar());
        JButton devolver = Ui.boton("Devolver");
        devolver.addActionListener(e -> devolver());
        JButton refrescar = Ui.boton("Refrescar");
        refrescar.addActionListener(e -> refrescar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 2, Tema.ESP_MD));
        pie.add(aprobar);
        pie.add(devolver);
        pie.add(refrescar);

        Grupo g = new Grupo("Reclamos por revisar");
        g.add(cabecera, BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        contenido().add(g, BorderLayout.CENTER);

        Reclamos.alCambiar(t -> refrescar());
    }

    private void ingresar() {
        Empleado e = Prototipo.EMPLEADOS.get(idSupervisor.getText().trim().toUpperCase());
        if (e == null || !e.esSupervisor()) {
            avisar("Solo un supervisor de calidad puede revisar reclamos (pruebe con S01).");
            supervisor = null;
        } else {
            supervisor = e;
        }
        refrescar();
    }

    private void refrescar() {
        visibles = supervisor == null ? List.of() : Calidad.paraSupervisor();
        tabla.limpiar();
        for (Reclamo t : visibles) {
            boolean rechazo = t.revision() == Revision.RECHAZO_PROPUESTO;
            tabla.agregar(t.numero(), t.revision().etiqueta(), t.nombreCliente(), t.objeto(),
                    t.problema(), t.especialista(),
                    rechazo ? "Rechazo" : t.solucionAplicada(),
                    t.solucionSugerida(),
                    rechazo ? t.motivoRechazo() : String.join("; ", t.observaciones()));
        }
    }

    private Reclamo seleccionado() {
        if (supervisor == null) { avisar("Primero ingrese su ID de supervisor."); return null; }
        int i = tabla.filaModelo();
        if (i < 0 || i >= visibles.size()) { avisar("Seleccione el reclamo que desea revisar."); return null; }
        return visibles.get(i);
    }

    private void aprobar() {
        Reclamo t = seleccionado();
        if (t == null) return;
        boolean rechazo = t.revision() == Revision.RECHAZO_PROPUESTO;
        if (!confirmar(rechazo ? "¿Aprobar el rechazo del reclamo " + t.numero() + "? Se notificará al cliente."
                : "¿Aprobar la solución del reclamo " + t.numero() + "?")) return;
        Calidad.aprobar(t, supervisor.id());
        avisar(rechazo ? "Rechazo aprobado. El cliente tiene " + t.instancia().diasImpugnacion()
                + " días para impugnar."
                : t.estado().abierto() ? "Solución aprobada. Falta la conformidad del cliente."
                : "Solución aprobada. El reclamo quedó cerrado.");
    }

    private void devolver() {
        Reclamo t = seleccionado();
        if (t == null) return;
        JTextArea motivo = Ui.area(3);
        Formulario f = new Formulario();
        f.campo("Motivo de la devolución:", Ui.scroll(motivo));
        f.setBorder(Ui.relleno(Tema.ESP_MD));
        int r = JOptionPane.showConfirmDialog(this, f, "Devolver a Atención",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (motivo.getText().isBlank()) { avisar("Indique el motivo de la devolución."); return; }

        Calidad.devolverPorSupervisor(t, supervisor.id(), motivo.getText().trim());
        avisar("Reclamo " + t.numero() + " devuelto a atención. Nueva cita para "
                + t.especialista() + ".");
    }
}
