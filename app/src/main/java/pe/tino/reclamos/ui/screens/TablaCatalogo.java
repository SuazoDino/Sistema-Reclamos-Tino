package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.components.Formulario;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogo tabular con alta, modificacion y baja. Lo comparten los catalogos
 * cuyos datos son una tabla plana: problemas, politicas y reclamos.
 */
public class TablaCatalogo extends JPanel {

    /** Un campo del formulario; si trae valores es una lista cerrada. */
    public record Campo(String etiqueta, List<String> valores) {
        public static Campo libre(String etiqueta) { return new Campo(etiqueta, null); }
        public static Campo lista(String etiqueta, List<String> v) { return new Campo(etiqueta, v); }
        public boolean esCerrado() { return valores != null; }
    }

    private final List<Campo> campos;
    private final List<JComponent> controles = new ArrayList<>();
    private final List<String[]> filas;
    private final Tabla tabla;
    private final Component padre;

    public TablaCatalogo(Component padre, String titulo, List<Campo> campos,
                         List<String[]> filas, int[] anchos) {
        super(new BorderLayout(Tema.ESP_MD, Tema.ESP_MD));
        this.padre = padre;
        this.campos = campos;
        this.filas = filas;

        setOpaque(false);
        tabla = new Tabla(campos.stream().map(Campo::etiqueta).toArray(String[]::new));
        tabla.anchos(anchos);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Grupo listado = Grupo.ajustado(titulo);
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        add(listado, BorderLayout.CENTER);
        add(editor(), BorderLayout.SOUTH);
        refrescar();
    }

    private JComponent editor() {
        Grupo g = new Grupo("Registro");

        Formulario f = new Formulario();
        for (Campo c : campos) {
            JComponent control = c.esCerrado() ? Ui.combo(c.valores()) : Ui.texto();
            controles.add(control);
            f.campo(c.etiqueta(), control);
        }

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
        filas.forEach(fila -> tabla.agregar((Object[]) fila));
    }

    private void cargarSeleccion() {
        int i = tabla.filaModelo();
        if (i < 0) return;
        String[] fila = filas.get(i);
        for (int c = 0; c < controles.size() && c < fila.length; c++) {
            JComponent control = controles.get(c);
            if (control instanceof JTextField t) t.setText(fila[c]);
            else if (control instanceof JComboBox<?> combo) combo.setSelectedItem(fila[c]);
        }
    }

    private static String leer(JComponent control) {
        if (control instanceof JTextField t) return t.getText().trim();
        if (control instanceof JComboBox<?> c) return String.valueOf(c.getSelectedItem());
        return "";
    }

    private void guardar() {
        String[] fila = new String[campos.size()];
        for (int c = 0; c < controles.size(); c++) fila[c] = leer(controles.get(c));

        if (fila[0].isBlank()) {
            mensaje("El campo \"" + campos.get(0).etiqueta() + "\" es obligatorio.");
            return;
        }
        int i = tabla.filaModelo();
        if (i < 0) filas.add(fila); else filas.set(i, fila);
        refrescar();
        mensaje(i < 0 ? "Registro agregado." : "Registro actualizado.");
    }

    private void eliminar() {
        int i = tabla.filaModelo();
        if (i < 0) { mensaje("Seleccione el registro que desea eliminar."); return; }
        int r = JOptionPane.showConfirmDialog(padre, "Eliminar el registro seleccionado?",
                "Confirmar", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        filas.remove(i);
        refrescar();
        limpiar();
    }

    private void limpiar() {
        tabla.clearSelection();
        for (JComponent c : controles) {
            if (c instanceof JTextField t) t.setText("");
            else if (c instanceof JComboBox<?> combo) combo.setSelectedIndex(0);
        }
    }

    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(padre, texto, "Sistema de Reclamos",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
