package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El grupo "Busqueda" de los formularios del prototipo: una o varias listas
 * desplegables y el boton Buscar debajo.
 */
public class PanelBusqueda extends Grupo {

    private final Map<String, JComboBox<String>> filtros = new LinkedHashMap<>();
    private final JButton buscar = Ui.boton("Buscar");

    /**
     * @param columnas cuantos filtros van por fila; el prototipo usa una o dos
     */
    public PanelBusqueda(int columnas, Map<String, List<String>> definicion) {
        super("Búsqueda");

        JPanel rejilla = Ui.panel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(Tema.ESP_XS, 0, Tema.ESP_XS, Tema.ESP_MD);
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int i = 0;
        for (Map.Entry<String, List<String>> e : definicion.entrySet()) {
            List<String> valores = new ArrayList<>();
            valores.add("");
            valores.addAll(e.getValue());
            JComboBox<String> combo = Ui.combo(valores);
            filtros.put(e.getKey(), combo);

            gc.gridx = (i % columnas) * 2;
            gc.gridy = i / columnas;
            gc.weightx = 0;
            rejilla.add(Ui.etiqueta(e.getKey() + ":"), gc);

            gc.gridx++;
            gc.weightx = 1;
            rejilla.add(combo, gc);
            i++;
        }

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_SM));
        pie.add(buscar);

        add(rejilla, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
    }

    /** Valor elegido en un filtro; cadena vacia si no se eligio ninguno. */
    public String valor(String etiqueta) {
        JComboBox<String> c = filtros.get(etiqueta);
        return c == null ? "" : String.valueOf(c.getSelectedItem());
    }

    public void alBuscar(Runnable accion) {
        buscar.addActionListener(e -> accion.run());
    }
}
