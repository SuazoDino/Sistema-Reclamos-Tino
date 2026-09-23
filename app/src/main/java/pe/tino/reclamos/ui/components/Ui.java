package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** Fabrica de controles. Botones y campos estandar, sin adornos. */
public final class Ui {

    private Ui() {}

    /* ---------------- texto ---------------- */

    public static JLabel titulo(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.titulo());
        l.setForeground(Tema.TEXTO);
        return l;
    }

    public static JLabel etiqueta(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.cuerpo());
        l.setForeground(Tema.TEXTO);
        return l;
    }

    public static JLabel fuerte(String s) {
        JLabel l = etiqueta(s);
        l.setFont(Tema.fuerte());
        return l;
    }

    public static JLabel suave(String s) {
        JLabel l = etiqueta(s);
        l.setForeground(Tema.TEXTO_SUAVE);
        return l;
    }

    /* ---------------- botones ---------------- */

    public static JButton boton(String texto) {
        JButton b = new JButton(texto);
        b.setFont(Tema.cuerpo());
        b.setMargin(new Insets(4, 14, 4, 14));
        return b;
    }

    /* ---------------- campos ---------------- */

    public static JTextField texto() {
        JTextField t = new JTextField();
        t.setFont(Tema.cuerpo());
        return t;
    }

    public static JTextField texto(int columnas) {
        JTextField t = new JTextField(columnas);
        t.setFont(Tema.cuerpo());
        return t;
    }

    /** Campo de solo lectura, con el fondo del control para que se note. */
    public static JTextField soloLectura() {
        JTextField t = texto();
        t.setEditable(false);
        t.setBackground(Tema.FONDO);
        return t;
    }

    public static <T> JComboBox<T> combo(java.util.List<T> valores) {
        JComboBox<T> c = new JComboBox<>(new DefaultComboBoxModel<>(new java.util.Vector<>(valores)));
        c.setFont(Tema.cuerpo());
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(Math.min(d.width, dim(170, 0).width), d.height));
        c.setMinimumSize(new Dimension(90, d.height));
        return c;
    }

    public static JTextArea area(int filas) {
        JTextArea a = new JTextArea(filas, 20);
        a.setFont(Tema.cuerpo());
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        return a;
    }

    /* ---------------- contenedores ---------------- */

    /**
     * Dimension fija que acompania a la escala del texto. Un alto pensado
     * para 12 px deja la tabla sin filas cuando el texto va al 150 %.
     */
    public static Dimension dim(int ancho, int alto) {
        double e = Tema.escalaTexto();
        return new Dimension((int) Math.round(ancho * e), (int) Math.round(alto * e));
    }

    public static Border relleno(int todo) {
        return BorderFactory.createEmptyBorder(todo, todo, todo, todo);
    }

    public static Border relleno(int arriba, int der, int abajo, int izq) {
        return BorderFactory.createEmptyBorder(arriba, izq, abajo, der);
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JPanel fila(Component... hijos) {
        JPanel p = panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_SM, 0));
        for (Component c : hijos) p.add(c);
        return p;
    }

    public static JPanel filaDerecha(Component... hijos) {
        JPanel p = panel(new FlowLayout(FlowLayout.RIGHT, Tema.ESP_SM, 0));
        for (Component c : hijos) p.add(c);
        return p;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        s.getViewport().setBackground(Tema.SUPERFICIE);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    /** Scroll que adapta el contenido al ancho disponible, sin barra horizontal. */
    public static JScrollPane scrollVertical(Component c) {
        JScrollPane s = scroll(new PanelAncho(c));
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return s;
    }
}
