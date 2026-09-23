package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Protocolo;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Mant-Param - Protocolos. Cada fila define como se atiende un tipo de
 * reclamo: inspeccion, solucion, reasignacion, entrega y seguimiento.
 */
public class ProtocolosPantalla extends Pantalla {

    private final Tabla tabla = new Tabla(new String[]{
            "Tipo de reclamo", "Inspeccion", "Solucion", "Reasignacion",
            "Entrega", "Seguimiento", "Estado"});

    private final JTextField tipoReclamo = Ui.texto("Segmento - Tipo de problema");
    private final JComboBox<String> inspeccion   = Ui.combo(Datos.INSPECCION);
    private final JComboBox<String> solucion     = Ui.combo(Datos.SOLUCION);
    private final JComboBox<String> reasignacion = Ui.combo(Datos.REASIGNACION);
    private final JComboBox<String> entrega      = Ui.combo(Datos.ENTREGA);
    private final JComboBox<String> seguimiento  = Ui.combo(Datos.SEGUIMIENTO);
    private final JCheckBox habilitado = new JCheckBox("Protocolo habilitado", true);

    public ProtocolosPantalla() {
        super("Mant-Param", "Protocolos de atencion",
                "Secuencia de pasos que el sistema exige para cada tipo de reclamo.");

        tabla.anchos(265, 100, 100, 115, 100, 110, 145);
        tabla.columnaEstado(6, Estados::color);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Tarjeta listado = new Tarjeta("Protocolos definidos",
                "Un protocolo deshabilitado queda documentado pero no se aplica.");
        listado.sinRelleno();
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(listado, BorderLayout.CENTER);
        contenido().add(editor(), BorderLayout.EAST);

        refrescar();
    }

    private JComponent editor() {
        Tarjeta t = new Tarjeta("Definicion del protocolo",
                "Alta de un protocolo nuevo o edicion del seleccionado.");
        t.setPreferredSize(new Dimension(370, 100));

        habilitado.setFont(Tema.cuerpo());
        habilitado.setOpaque(false);

        Formulario f = new Formulario();
        f.grupo("Alcance")
         .campo("Tipo de reclamo", tipoReclamo)
         .grupo("Pasos del protocolo")
         .campo("Inspeccion del problema", inspeccion)
         .campo("Solucion del problema", solucion)
         .campo("Reasignacion de area", reasignacion)
         .campo("Entrega del producto", entrega)
         .campo("Seguimiento de solucion", seguimiento)
         .ancho(habilitado)
         .finalizar();

        JButton guardar = Ui.primario("Guardar", "guardar");
        guardar.addActionListener(e -> guardar());
        JButton nuevo = Ui.secundario("Nuevo", "mas");
        nuevo.addActionListener(e -> limpiar());
        JButton eliminar = Ui.plano("Eliminar", "cruz");
        eliminar.addActionListener(e -> eliminar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        pie.add(eliminar, BorderLayout.WEST);
        pie.add(Ui.filaDerecha(Tema.ESP_SM, nuevo, guardar), BorderLayout.EAST);

        JPanel cuerpo = Ui.panel(new BorderLayout());
        cuerpo.add(f, BorderLayout.CENTER);
        cuerpo.add(pie, BorderLayout.SOUTH);
        t.add(Ui.scrollVertical(cuerpo), BorderLayout.CENTER);
        return t;
    }

    private void refrescar() {
        tabla.limpiar();
        for (Protocolo p : Estado.protocolos()) {
            tabla.agregar(p.tipoReclamo(), p.inspeccionProblema(), p.solucionProblema(),
                    p.reasignacionArea(), p.entregaProducto(), p.seguimientoSolucion(),
                    p.habilitado() ? "Habilitado" : "Deshabilitado");
        }
    }

    private int indiceSeleccionado() {
        int f = tabla.getSelectedRow();
        return f < 0 ? -1 : tabla.convertRowIndexToModel(f);
    }

    private void cargarSeleccion() {
        int i = indiceSeleccionado();
        if (i < 0) return;
        Protocolo p = Estado.protocolos().get(i);
        tipoReclamo.setText(p.tipoReclamo());
        inspeccion.setSelectedItem(p.inspeccionProblema());
        solucion.setSelectedItem(p.solucionProblema());
        reasignacion.setSelectedItem(p.reasignacionArea());
        entrega.setSelectedItem(p.entregaProducto());
        seguimiento.setSelectedItem(p.seguimientoSolucion());
        habilitado.setSelected(p.habilitado());
    }

    private void guardar() {
        if (tipoReclamo.getText().isBlank()) {
            avisar("Indique a que tipo de reclamo aplica el protocolo.");
            tipoReclamo.requestFocusInWindow();
            return;
        }
        Protocolo p = new Protocolo(tipoReclamo.getText().trim(),
                String.valueOf(inspeccion.getSelectedItem()),
                String.valueOf(solucion.getSelectedItem()),
                String.valueOf(reasignacion.getSelectedItem()),
                String.valueOf(entrega.getSelectedItem()),
                String.valueOf(seguimiento.getSelectedItem()),
                habilitado.isSelected());

        int i = indiceSeleccionado();
        if (i < 0) Estado.protocolos().add(p);
        else Estado.protocolos().set(i, p);
        refrescar();
        avisar(i < 0 ? "Protocolo agregado." : "Protocolo actualizado.");
    }

    private void eliminar() {
        int i = indiceSeleccionado();
        if (i < 0) { avisar("Seleccione el protocolo que desea eliminar."); return; }
        if (!confirmar("Eliminar el protocolo seleccionado?")) return;
        Estado.protocolos().remove(i);
        refrescar();
        limpiar();
    }

    private void limpiar() {
        tabla.clearSelection();
        tipoReclamo.setText("");
        inspeccion.setSelectedIndex(0);
        solucion.setSelectedIndex(0);
        reasignacion.setSelectedIndex(0);
        entrega.setSelectedIndex(0);
        seguimiento.setSelectedIndex(0);
        habilitado.setSelected(true);
        tipoReclamo.requestFocusInWindow();
    }
}
