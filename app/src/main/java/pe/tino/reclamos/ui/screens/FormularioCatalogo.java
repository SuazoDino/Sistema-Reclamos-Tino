package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Armazon de los catalogos del prototipo: arriba la busqueda y sus
 * resultados, abajo el grupo "Agregar" con sus pestanias.
 */
public final class FormularioCatalogo {

    private FormularioCatalogo() {}

    /** Arma la mitad de arriba: busqueda a la izquierda, resultados a la derecha. */
    public static JComponent cabecera(PanelBusqueda busqueda, String tituloResultados,
                                      Tabla tabla, int anchoBusqueda, JComponent... botones) {
        Grupo resultados = new Grupo(tituloResultados);
        resultados.add(tabla.enScroll(), BorderLayout.CENTER);

        if (botones.length > 0) {
            JPanel columna = Ui.panel(new GridLayout(botones.length, 1, 0, Tema.ESP_SM));
            for (JComponent b : botones) columna.add(b);
            JPanel contenedor = Ui.panel(new GridBagLayout());
            contenedor.add(columna);
            contenedor.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));
            resultados.add(contenedor, BorderLayout.EAST);
        }

        busqueda.setPreferredSize(new Dimension(anchoBusqueda, 100));
        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        p.add(busqueda, BorderLayout.WEST);
        p.add(resultados, BorderLayout.CENTER);
        return p;
    }

    /** Grupo "Agregar" con las pestanias que reciba. */
    public static JComponent agregar(JTabbedPane pestanias) {
        pestanias.setFont(Tema.cuerpo());
        Grupo g = new Grupo("Agregar");
        g.add(pestanias, BorderLayout.CENTER);
        return g;
    }

    /** Une cabecera y grupo Agregar en el reparto que usa el prototipo. */
    public static JComponent armar(JComponent cabecera, JComponent agregar, int altoCabecera) {
        cabecera.setPreferredSize(new Dimension(100, altoCabecera));
        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(cabecera, BorderLayout.NORTH);
        p.add(agregar, BorderLayout.CENTER);
        return p;
    }

    /**
     * El bloque "Lista de X Existentes" que en vez de campo de texto usa una
     * lista desplegable, como "Agregar Producto" del catalogo de problemas.
     */
    public static JComponent listaConCombo(Component padre, String titulo, String etiqueta,
                                           java.util.List<String> opciones,
                                           java.util.List<String> valores) {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        valores.forEach(modelo::addElement);
        JList<String> lista = new JList<>(modelo);
        lista.setFont(Tema.cuerpo());

        JComboBox<String> combo = Ui.combo(opciones);
        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> {
            String v = String.valueOf(combo.getSelectedItem());
            if (modelo.contains(v)) {
                JOptionPane.showMessageDialog(padre, "El valor " + v + " ya esta en la lista.",
                        "Sistema de Reclamos", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            modelo.addElement(v);
        });

        JButton quitar = Ui.boton("Quitar");
        quitar.addActionListener(e -> {
            String v = lista.getSelectedValue();
            if (v == null) {
                JOptionPane.showMessageDialog(padre, "Seleccione un valor de la lista.",
                        "Sistema de Reclamos", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            modelo.removeElement(v);
        });

        JPanel alta = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        alta.add(Ui.etiqueta(etiqueta), BorderLayout.WEST);
        alta.add(combo, BorderLayout.CENTER);
        alta.add(agregar, BorderLayout.EAST);
        alta.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        JPanel botones = Ui.panel(new GridBagLayout());
        botones.add(quitar);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        Grupo g = new Grupo(titulo);
        g.add(alta, BorderLayout.NORTH);
        g.add(Ui.scroll(lista), BorderLayout.CENTER);
        g.add(botones, BorderLayout.EAST);
        return g;
    }
}
