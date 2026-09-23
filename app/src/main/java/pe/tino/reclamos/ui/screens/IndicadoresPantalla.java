package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Consulta de Indicadores, como en la captura: la lista de indicadores y los
 * botones Detalle y Salir.
 */
public class IndicadoresPantalla extends Pantalla {

    private final DefaultListModel<String> modelo = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(modelo);

    public IndicadoresPantalla() {
        super("Consulta de Indicadores",
                "Indicadores estadisticos del sistema de reclamos.");

        Prototipo.INDICADORES.forEach(modelo::addElement);
        lista.setFont(Tema.cuerpo());
        lista.setSelectedIndex(0);

        JButton detalle = Ui.boton("Detalle");
        detalle.addActionListener(e -> mostrarDetalle());
        JButton salir = Ui.boton("Salir");
        salir.addActionListener(e -> Navegacion.volver());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 3, Tema.ESP_MD));
        pie.add(detalle);
        pie.add(salir);

        Grupo g = new Grupo("Indicadores");
        g.add(Ui.scroll(lista), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        g.setPreferredSize(new Dimension(560, 320));

        JPanel centro = Ui.panel(new GridBagLayout());
        centro.add(g);
        contenido().add(centro, BorderLayout.CENTER);
    }

    private void mostrarDetalle() {
        String indicador = lista.getSelectedValue();
        if (indicador == null) { avisar("Seleccione un indicador."); return; }
        avisar("Detalle del indicador: " + indicador + ".\n"
                + "El resultado se calcula con los reclamos del periodo.");
    }
}
