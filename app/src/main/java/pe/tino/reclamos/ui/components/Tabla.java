package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * JTable con el estilo del sistema: cabecera en mayusculas finas, filas
 * alternadas, seleccion con el acento y ordenamiento por columna activado.
 */
public class Tabla extends JTable {

    private final DefaultTableModel modelo;

    public Tabla(String[] columnas) {
        this.modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
        };
        setModel(modelo);
        configurar();
    }

    public Tabla(DefaultTableModel modelo) {
        this.modelo = modelo;
        setModel(modelo);
        configurar();
    }

    private void configurar() {
        setFont(Tema.cuerpo());
        setRowHeight(30);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(Tema.borde());
        setIntercellSpacing(new Dimension(0, 1));
        setSelectionBackground(Tema.acentoSuave());
        setSelectionForeground(Tema.texto());
        setAutoCreateRowSorter(true);
        setFillsViewportHeight(true);
        putClientProperty("JTable.rowHeightMode", "custom");

        JTableHeader cab = getTableHeader();
        cab.setReorderingAllowed(false);
        cab.setFont(Tema.fuente(11, Font.BOLD));
        cab.setForeground(Tema.textoSuave());
        cab.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.borde()));

        // el encabezado se alinea con el contenido de la celda, no centrado
        TableCellRenderer base = cab.getDefaultRenderer();
        cab.setDefaultRenderer((t, v, sel, foco, f, c) -> {
            Component comp = base.getTableCellRendererComponent(t, v, sel, foco, f, c);
            if (comp instanceof JLabel l) {
                l.setHorizontalAlignment(SwingConstants.LEADING);
                l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                l.setFont(Tema.fuente(11, Font.BOLD));
                l.setForeground(Tema.textoSuave());
            }
            return comp;
        });

        setDefaultRenderer(Object.class, new RenderFila());
    }

    public DefaultTableModel modelo() { return modelo; }

    public void limpiar() { modelo.setRowCount(0); }

    public void agregar(Object... celdas) { modelo.addRow(celdas); }

    /**
     * Deja de repartir el ancho sobrante entre columnas: cada una conserva el
     * ancho pedido y, si no entran todas, aparece scroll horizontal. Evita que
     * una tabla ancha comprima los codigos hasta volverlos ilegibles.
     */
    public Tabla anchoFijo() {
        setAutoResizeMode(AUTO_RESIZE_OFF);
        return this;
    }

    /** Fija anchos preferidos por columna; -1 deja la columna elastica. */
    public void anchos(int... px) {
        for (int i = 0; i < px.length && i < getColumnCount(); i++) {
            if (px[i] < 0) continue;
            TableColumn col = getColumnModel().getColumn(i);
            col.setPreferredWidth(px[i]);
            col.setMinWidth(Math.max(40, Math.round(px[i] * 0.8f)));
            if (getAutoResizeMode() == AUTO_RESIZE_OFF) col.setWidth(px[i]);
        }
    }

    /** Centra el contenido de las columnas indicadas. */
    public void centrar(int... indices) {
        for (int i : indices) {
            getColumnModel().getColumn(i).setCellRenderer(new RenderFila() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, f, c);
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                    return l;
                }
            });
        }
    }

    /** Pinta una columna como distintivo de estado (punto de color + texto). */
    public void columnaEstado(int indice, java.util.function.Function<String, Color> colorDe) {
        getColumnModel().getColumn(indice).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                String texto = String.valueOf(v);
                JComponent chip = (JComponent) Ui.chip(texto, colorDe.apply(texto));
                JPanel p = Ui.panel(new FlowLayout(FlowLayout.LEFT, 0, 4));
                p.setOpaque(true);
                p.setBackground(sel ? t.getSelectionBackground() : fondoFila(f));
                p.add(chip);
                return p;
            }
        });
    }

    private static Color fondoFila(int fila) {
        return fila % 2 == 0 ? Tema.superficie()
                             : Ui.mezcla(Tema.borde(), Tema.superficie(), 0.28f);
    }

    /** Renderer base con filas alternadas y elipsis en textos largos. */
    private static class RenderFila extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foco, f, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            l.setFont(Tema.cuerpo());
            if (!sel) {
                l.setBackground(fondoFila(f));
                l.setForeground(Tema.texto());
            }
            l.setToolTipText(v == null ? null : String.valueOf(v));
            return l;
        }
    }

    /** Envuelve la tabla en un scroll listo para meter en una {@link Tarjeta}. */
    public JScrollPane enScroll() {
        JScrollPane s = Ui.scroll(this);
        s.getViewport().setBackground(Tema.superficie());
        s.setOpaque(true);
        s.setBackground(Tema.superficie());
        return s;
    }

    /** Devuelve los valores de la fila seleccionada, o null si no hay seleccion. */
    public List<Object> filaSeleccionada() {
        int f = getSelectedRow();
        if (f < 0) return null;
        int real = convertRowIndexToModel(f);
        java.util.ArrayList<Object> vals = new java.util.ArrayList<>();
        for (int c = 0; c < modelo.getColumnCount(); c++) vals.add(modelo.getValueAt(real, c));
        return vals;
    }
}
