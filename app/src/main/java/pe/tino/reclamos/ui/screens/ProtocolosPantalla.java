package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Protocolo;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Catalogo de Protocolos.
 *
 * Entrada : datos de cada paso que se realiza en determinado tipo de protocolo.
 * Funcion : registrar y sincronizar los parametros del protocolo de solucion.
 * Salida  : protocolos registrados.
 */
public class ProtocolosPantalla extends Pantalla {

    private final Tabla tabla = new Tabla(new String[]{
            "Tipo de reclamo", "Inspeccion", "Solucion", "Reasignacion",
            "Entrega", "Seguimiento", "Estado"});

    private final JTextField tipoReclamo = Ui.texto();
    private final JComboBox<String> inspeccion   = Ui.combo(Datos.INSPECCION);
    private final JComboBox<String> solucion     = Ui.combo(Datos.SOLUCION);
    private final JComboBox<String> reasignacion = Ui.combo(Datos.REASIGNACION);
    private final JComboBox<String> entrega      = Ui.combo(Datos.ENTREGA);
    private final JComboBox<String> seguimiento  = Ui.combo(Datos.SEGUIMIENTO);
    private final JCheckBox habilitado = new JCheckBox("Habilitado", true);

    public ProtocolosPantalla() {
        super("Catalogo de Protocolos",
                "Parametros de cada evento del protocolo, segun el tipo de reclamo.");

        tabla.anchos(300, 105, 105, 120, 105, 115, 115);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Grupo listado = Grupo.ajustado("Protocolos definidos");
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(listado, BorderLayout.CENTER);
        contenido().add(editor(), BorderLayout.SOUTH);
        refrescar();
    }

    private JComponent editor() {
        Grupo g = new Grupo("Definicion del protocolo");
        habilitado.setFont(Tema.cuerpo());
        habilitado.setOpaque(false);

        Formulario f = new Formulario();
        f.campo("Tipo de reclamo", tipoReclamo)
         .campo("Inspeccion del problema", inspeccion)
         .campo("Solucion del problema", solucion)
         .campo("Reasignacion de area", reasignacion)
         .campo("Entrega del producto", entrega)
         .campo("Seguimiento de solucion", seguimiento)
         .campo("Estado", habilitado);

        JButton nuevo = Ui.boton("Nuevo");
        nuevo.addActionListener(e -> limpiar());
        JButton guardar = Ui.boton("Guardar");
        guardar.addActionListener(e -> guardar());
        JButton eliminar = Ui.boton("Eliminar");
        eliminar.addActionListener(e -> eliminar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(Ui.filaDerecha(nuevo, eliminar, guardar), BorderLayout.EAST);

        g.add(f, BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        return g;
    }

    private void refrescar() {
        tabla.limpiar();
        for (Protocolo p : Estado.protocolos()) {
            tabla.agregar(p.tipoReclamo(), p.inspeccionProblema(), p.solucionProblema(),
                    p.reasignacionArea(), p.entregaProducto(), p.seguimientoSolucion(),
                    p.habilitado() ? "Habilitado" : "Deshabilitado");
        }
    }

    private void cargarSeleccion() {
        int i = tabla.filaModelo();
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

        int i = tabla.filaModelo();
        if (i < 0) Estado.protocolos().add(p); else Estado.protocolos().set(i, p);
        refrescar();
        avisar(i < 0 ? "Protocolo agregado." : "Protocolo actualizado.");
    }

    private void eliminar() {
        int i = tabla.filaModelo();
        if (i < 0) { avisar("Seleccione el protocolo que desea eliminar."); return; }
        if (!confirmar("Eliminar el protocolo seleccionado?")) return;
        Estado.protocolos().remove(i);
        refrescar();
        limpiar();
    }

    private void limpiar() {
        tabla.clearSelection();
        tipoReclamo.setText("");
        List.of(inspeccion, solucion, reasignacion, entrega, seguimiento)
                .forEach(c -> c.setSelectedIndex(0));
        habilitado.setSelected(true);
        tipoReclamo.requestFocusInWindow();
    }
}
