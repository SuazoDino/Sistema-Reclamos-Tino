package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Prototipo.DatosReclamo;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Area - Data Entry, como en las capturas: primero se pide el ID del
 * empleado, despues se listan sus reclamos ("Atender Reclamo") y al
 * inspeccionar se abre la ventana de inspeccion del producto.
 */
public class AreaDataEntryPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField idEmpleado = Ui.soloLectura();
    private final JTextField tipoEmpleado = Ui.soloLectura();
    private final JTextField area = Ui.soloLectura();
    private final JTextField fecha = Ui.soloLectura();
    private final Tabla tabla = new Tabla(new String[]{
            "IdReclamo", "Fecha de emision", "Fecha de Atencion", "Hora de Atencion", "Estado"});
    private final List<String[]> reclamos = Prototipo.reclamosDelArea();

    public AreaDataEntryPantalla() {
        super("Atender Reclamo", "Reclamos registrados del usuario del area.");

        tabla.anchos(110, 160, 160, 160, 140);
        reclamos.forEach(f -> tabla.agregar((Object[]) f));
        fecha.setText(LocalDate.now().format(FECHA));

        JButton inspeccionar = Ui.boton("Inspeccionar");
        inspeccionar.addActionListener(e -> inspeccionar());
        JButton cancelar = Ui.boton("Cancelar");
        cancelar.addActionListener(e -> tabla.clearSelection());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 3, Tema.ESP_MD));
        pie.add(inspeccionar);
        pie.add(cancelar);

        Grupo g = new Grupo("Reclamos Registrados");
        g.add(datosEmpleado(), BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(g, BorderLayout.CENTER);

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
    }

    /** La ventana "Inspeccionar Producto": datos del reclamo y pasos del protocolo. */
    private void inspeccionar() {
        int i = tabla.filaModelo();
        if (i < 0) { avisar("Seleccione un reclamo de la lista."); return; }
        if (idEmpleado.getText().isBlank()) { avisar("Primero ingrese su ID de empleado."); return; }

        String id = String.valueOf(tabla.modelo().getValueAt(i, 0));
        DatosReclamo d = Prototipo.datosDe(id);

        JPanel datos = Ui.panel(new GridLayout(3, 4, Tema.ESP_MD, Tema.ESP_SM));
        datos.add(Ui.etiqueta("ID Reclamo:"));
        datos.add(Ui.fuerte(d.id()));
        datos.add(Ui.etiqueta(""));
        datos.add(Ui.etiqueta(""));
        datos.add(Ui.etiqueta("Producto:"));
        datos.add(Ui.fuerte(d.producto()));
        datos.add(Ui.etiqueta("Marca:"));
        datos.add(Ui.fuerte(d.marca()));
        datos.add(Ui.etiqueta("Tipo Problema:"));
        datos.add(Ui.fuerte(d.tipoProblema()));
        datos.add(Ui.etiqueta("Problema:"));
        datos.add(Ui.fuerte(d.problema()));

        Grupo grupoDatos = new Grupo("Datos Reclamo");
        grupoDatos.add(datos, BorderLayout.CENTER);

        JPanel pasos = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        List<JCheckBox> casillas = new java.util.ArrayList<>();
        for (String[] accion : Prototipo.accionesProtocolo()) {
            JCheckBox c = new JCheckBox(accion[0]);
            c.setFont(Tema.cuerpo());
            c.setOpaque(false);
            casillas.add(c);
            pasos.add(c);
        }

        JPanel pregunta = Ui.panel(new BorderLayout(0, Tema.ESP_SM));
        pregunta.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        pregunta.add(Ui.etiqueta("Funciona el producto?"), BorderLayout.NORTH);

        JRadioButton si = new JRadioButton("Si");
        JRadioButton no = new JRadioButton("No", true);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(si);
        grupo.add(no);
        si.setOpaque(false);
        no.setOpaque(false);
        si.setFont(Tema.cuerpo());
        no.setFont(Tema.cuerpo());
        pregunta.add(Ui.fila(si, no), BorderLayout.CENTER);

        Grupo grupoInspeccion = new Grupo("Inspeccion Producto");
        grupoInspeccion.add(pasos, BorderLayout.NORTH);
        grupoInspeccion.add(pregunta, BorderLayout.CENTER);

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(grupoDatos, BorderLayout.NORTH);
        cuerpo.add(grupoInspeccion, BorderLayout.CENTER);
        cuerpo.setPreferredSize(new Dimension(520, 330));

        int r = JOptionPane.showConfirmDialog(this, cuerpo, "Inspeccionar Producto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (!confirmar("Desea finalizar inspeccion?")) return;

        long hechos = casillas.stream().filter(AbstractButton::isSelected).count();
        reclamos.get(i)[4] = si.isSelected() ? "Atendido" : "Pendiente";
        tabla.modelo().setValueAt(reclamos.get(i)[4], i, 4);
        avisar("Inspeccion registrada: " + hechos + " de " + casillas.size()
                + " acciones del protocolo.");
    }
}
