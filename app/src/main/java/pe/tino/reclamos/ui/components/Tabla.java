package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/** Tabla con rejilla completa y cabecera gris, como cualquier grilla de escritorio. */
public class Tabla extends JTable {

    private final DefaultTableModel modelo;

    public Tabla(String[] columnas) {
        this.modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
        };
        setModel(modelo);
        configurar();
    }

    private void configurar() {
        setFont(Tema.cuerpo());
        setRowHeight((int) Math.round(22 * Tema.escalaTexto()));
        setShowGrid(true);
        setGridColor(Tema.BORDE_FINO);
        setIntercellSpacing(new Dimension(1, 1));
        setSelectionBackground(Tema.SELECCION);
        setSelectionForeground(Tema.TEXTO);
        setAutoCreateRowSorter(true);
        setFillsViewportHeight(true);

        JTableHeader cab = getTableHeader();
        cab.setReorderingAllowed(false);
        cab.setFont(Tema.fuerte());
        cab.setBackground(Tema.CABECERA);

        setDefaultRenderer(Object.class, new Render());
    }

    public DefaultTableModel modelo() { return modelo; }

    public void limpiar() { modelo.setRowCount(0); }

    public void agregar(Object... celdas) { modelo.addRow(celdas); }

    /** Fija anchos preferidos por columna. */
    public Tabla anchos(int... px) {
        for (int i = 0; i < px.length && i < getColumnCount(); i++) {
            TableColumn col = getColumnModel().getColumn(i);
            col.setPreferredWidth(px[i]);
            col.setMinWidth(Math.max(40, Math.round(px[i] * 0.8f)));
        }
        return this;
    }

    /** Centra el contenido de las columnas indicadas. */
    public Tabla centrar(int... indices) {
        for (int i : indices) {
            getColumnModel().getColumn(i).setCellRenderer(new Render() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, f, c);
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                    return l;
                }
            });
        }
        return this;
    }

    /** Renderer con filas alternadas suaves y texto completo en el tooltip. */
    private static class Render extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, f, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            l.setFont(Tema.cuerpo());
            if (!sel) {
                l.setBackground(f % 2 == 0 ? Tema.SUPERFICIE : Tema.FILA_ALTERNA);
                l.setForeground(Tema.TEXTO);
            }
            l.setToolTipText(v == null ? null : String.valueOf(v));
            return l;
        }
    }

    public JScrollPane enScroll() { return Ui.scroll(this); }

    /** Indice de la fila seleccionada en el modelo, o -1 si no hay seleccion. */
    public int filaModelo() {
        int f = getSelectedRow();
        return f < 0 ? -1 : convertRowIndexToModel(f);
    }

    public List<Object> filaSeleccionada() {
        int real = filaModelo();
        if (real < 0) return null;
        java.util.ArrayList<Object> vals = new java.util.ArrayList<>();
        for (int c = 0; c < modelo.getColumnCount(); c++) vals.add(modelo.getValueAt(real, c));
        return vals;
    }
}
