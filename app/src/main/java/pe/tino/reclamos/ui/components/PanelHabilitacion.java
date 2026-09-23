package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * El bloque que se repite en casi todos los catalogos del prototipo: a la
 * izquierda la lista de valores existentes con su campo para agregar, a la
 * derecha la lista de valores habilitados.
 *
 * <pre>
 *  ┌ Lista de X Existentes ─────────┐   ┌ X habilitados ──────────┐
 *  │ Agregue X: [______]            │   │ lista                    │
 *  │ lista           [Agregar]      │   │           [Deshabilitar] │
 *  │                 [Habilitar]    │   └──────────────────────────┘
 *  │                 [Eliminar]     │
 *  └────────────────────────────────┘
 * </pre>
 */
public class PanelHabilitacion extends JPanel {

    private final DefaultListModel<String> existentes = new DefaultListModel<>();
    private final DefaultListModel<String> habilitados = new DefaultListModel<>();
    private final JList<String> listaExistentes = new JList<>(existentes);
    private final JList<String> listaHabilitados = new JList<>(habilitados);
    private final JTextField campo = Ui.texto(18);
    private final String singular;
    private final Component padre;
    private String sufijo = "";
    private boolean conEliminar = true;

    /**
     * @param titulo    como se llama el valor en plural ("Criticidades", "Eventos")
     * @param singular  como se llama en singular, para el campo de alta
     * @param quitar    rotulo del boton de la derecha ("Deshabilitar" o "Quitar")
     */
    public PanelHabilitacion(Component padre, String titulo, String singular, String quitar,
                             List<String> valoresExistentes, List<String> valoresHabilitados) {
        this(padre, titulo, singular, quitar, valoresExistentes, valoresHabilitados, "", true);
    }

    /**
     * @param sufijo      lo que el prototipo agrega a los botones ("Familia" en
     *                    "Agregar Familia"); vacio para botones a secas
     * @param conEliminar si el bloque ofrece el boton Eliminar
     */
    public PanelHabilitacion(Component padre, String titulo, String singular, String quitar,
                             List<String> valoresExistentes, List<String> valoresHabilitados,
                             String sufijo, boolean conEliminar) {
        super(new GridLayout(1, 2, Tema.ESP_MD, 0));
        this.padre = padre;
        this.singular = singular;
        this.sufijo = sufijo.isBlank() ? "" : " " + sufijo;
        this.conEliminar = conEliminar;
        setOpaque(false);

        valoresExistentes.forEach(existentes::addElement);
        valoresHabilitados.forEach(habilitados::addElement);
        listaExistentes.setFont(Tema.cuerpo());
        listaHabilitados.setFont(Tema.cuerpo());

        add(panelExistentes(titulo));
        add(panelHabilitados(titulo, quitar));
    }

    private JComponent panelExistentes(String titulo) {
        Grupo g = new Grupo("Lista de " + titulo + " Existentes");

        JPanel alta = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        alta.add(Ui.etiqueta("Agregue " + singular + ":"), BorderLayout.WEST);
        alta.add(campo, BorderLayout.CENTER);
        alta.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));

        JButton agregar = Ui.boton("Agregar" + sufijo);
        agregar.addActionListener(e -> agregar());
        campo.addActionListener(e -> agregar());
        JButton habilitar = Ui.boton("Habilitar" + sufijo);
        habilitar.addActionListener(e -> habilitar());

        JPanel botones = Ui.panel(new GridBagLayout());
        JPanel columna = Ui.panel(new GridLayout(conEliminar ? 3 : 2, 1, 0, Tema.ESP_SM));
        columna.add(agregar);
        columna.add(habilitar);
        if (conEliminar) {
            JButton eliminar = Ui.boton("Eliminar");
            eliminar.addActionListener(e -> eliminar());
            columna.add(eliminar);
        }
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        JPanel centro = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        centro.add(Ui.scroll(listaExistentes), BorderLayout.CENTER);
        centro.add(botones, BorderLayout.EAST);

        g.add(alta, BorderLayout.NORTH);
        g.add(centro, BorderLayout.CENTER);
        return g;
    }

    private JComponent panelHabilitados(String titulo, String quitar) {
        Grupo g = new Grupo(titulo + " habilitados");

        JButton boton = Ui.boton(quitar + sufijo);
        boton.addActionListener(e -> quitar());

        JPanel botones = Ui.panel(new GridBagLayout());
        botones.add(boton);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        g.add(Ui.scroll(listaHabilitados), BorderLayout.CENTER);
        g.add(botones, BorderLayout.EAST);
        return g;
    }

    private void agregar() {
        String v = campo.getText().trim();
        if (v.isEmpty()) { avisar("Escriba el valor que desea agregar."); return; }
        if (existentes.contains(v)) { avisar("\"" + v + "\" ya existe."); return; }
        existentes.addElement(v);
        campo.setText("");
        listaExistentes.setSelectedValue(v, true);
    }

    private void habilitar() {
        String v = listaExistentes.getSelectedValue();
        if (v == null) { avisar("Seleccione un valor de la lista de existentes."); return; }
        if (habilitados.contains(v)) { avisar("\"" + v + "\" ya está habilitado."); return; }
        habilitados.addElement(v);
    }

    private void eliminar() {
        String v = listaExistentes.getSelectedValue();
        if (v == null) { avisar("Seleccione el valor que desea eliminar."); return; }
        int r = JOptionPane.showConfirmDialog(padre, "Eliminar \"" + v + "\"?",
                "Confirmar", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        existentes.removeElement(v);
        habilitados.removeElement(v);
    }

    private void quitar() {
        String v = listaHabilitados.getSelectedValue();
        if (v == null) { avisar("Seleccione un valor habilitado."); return; }
        habilitados.removeElement(v);
    }

    private void avisar(String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Sistema de Reclamos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public List<String> habilitados() {
        return java.util.Collections.list(habilitados.elements());
    }
}
