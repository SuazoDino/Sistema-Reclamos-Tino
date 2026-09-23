package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Iconos;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** Fabrica de piezas de interfaz. Todo lo visual sale de {@link Tema}. */
public final class Ui {

    private Ui() {}

    /* ---------------- texto ---------------- */

    public static JLabel titulo(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.titulo());
        l.setForeground(Tema.texto());
        return l;
    }

    public static JLabel subtitulo(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.subtitulo());
        l.setForeground(Tema.texto());
        return l;
    }

    public static JLabel etiqueta(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.cuerpo());
        l.setForeground(Tema.texto());
        return l;
    }

    public static JLabel suave(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.cuerpo());
        l.setForeground(Tema.textoSuave());
        return l;
    }

    public static JLabel micro(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Tema.micro());
        l.setForeground(Tema.textoSuave());
        return l;
    }

    /* ---------------- botones ---------------- */

    public static JButton primario(String texto, String icono) {
        JButton b = base(texto, icono, Color.WHITE);
        b.setBackground(Tema.acento());
        b.setForeground(Color.WHITE);
        b.putClientProperty("JButton.buttonType", "default");
        return b;
    }

    public static JButton secundario(String texto, String icono) {
        JButton b = base(texto, icono, Tema.texto());
        b.setBackground(Tema.superficie());
        return b;
    }

    /** Boton sin relleno, para acciones terciarias dentro de una barra. */
    public static JButton plano(String texto, String icono) {
        JButton b = base(texto, icono, Tema.textoSuave());
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        return b;
    }

    /** Boton cuadrado de solo icono. */
    public static JButton icono(String icono, String tooltip) {
        JButton b = new JButton(Iconos.de(icono, 16, Tema.textoSuave()));
        b.setToolTipText(tooltip);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setMargin(new Insets(6, 6, 6, 6));
        return b;
    }

    private static JButton base(String texto, String icono, Color tinte) {
        JButton b = new JButton(texto);
        if (icono != null) {
            b.setIcon(Iconos.de(icono, 15, tinte));
            b.setIconTextGap(Tema.ESP_SM);
        }
        b.setFont(Tema.cuerpo());
        b.setFocusPainted(false);
        return b;
    }

    /* ---------------- campos ---------------- */

    public static JTextField texto(String marcador) {
        JTextField t = new JTextField();
        t.putClientProperty("JTextField.placeholderText", marcador);
        t.setFont(Tema.cuerpo());
        return t;
    }

    public static JTextField busqueda(String marcador) {
        JTextField t = texto(marcador);
        t.putClientProperty("JTextField.leadingIcon", Iconos.de("lupa", 15, Tema.textoSuave()));
        t.putClientProperty("JTextField.showClearButton", true);
        return t;
    }

    /**
     * Combo cuyo ancho preferido esta acotado: sin esto, una sola opcion larga
     * ("Area Tecnica de Reparacion e Inspeccion") estira la columna entera del
     * formulario y desborda el panel que lo contiene. Al rellenar la celda el
     * control igual ocupa todo el ancho disponible.
     */
    public static <T> JComboBox<T> combo(java.util.List<T> valores) {
        JComboBox<T> c = new JComboBox<>(new DefaultComboBoxModel<>(new java.util.Vector<>(valores)));
        c.setFont(Tema.cuerpo());
        c.setToolTipText(null);
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(Math.min(d.width, 170), d.height));
        c.setMinimumSize(new Dimension(90, d.height));
        c.addActionListener(e -> {
            Object v = c.getSelectedItem();
            c.setToolTipText(v == null ? null : String.valueOf(v));
        });
        return c;
    }

    public static JTextArea area(String marcador, int filas) {
        JTextArea a = new JTextArea(filas, 20);
        a.setFont(Tema.cuerpo());
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        a.putClientProperty("JTextArea.placeholderText", marcador);
        return a;
    }

    /* ---------------- contenedores ---------------- */

    public static Border relleno(int arriba, int der, int abajo, int izq) {
        return BorderFactory.createEmptyBorder(arriba, izq, abajo, der);
    }

    public static Border relleno(int todo) {
        return BorderFactory.createEmptyBorder(todo, todo, todo, todo);
    }

    /** Panel transparente con BorderLayout y separacion estandar. */
    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JPanel fila(int separacion, Component... hijos) {
        JPanel p = panel(new FlowLayout(FlowLayout.LEFT, separacion, 0));
        for (Component c : hijos) p.add(c);
        return p;
    }

    public static JPanel filaDerecha(int separacion, Component... hijos) {
        JPanel p = panel(new FlowLayout(FlowLayout.RIGHT, separacion, 0));
        for (Component c : hijos) p.add(c);
        return p;
    }

    public static JComponent separador() {
        JSeparator s = new JSeparator();
        s.setForeground(Tema.borde());
        return s;
    }

    /** Scroll sin borde propio, para meter dentro de una tarjeta. */
    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getViewport().setOpaque(false);
        s.setOpaque(false);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    /** Scroll que nunca crece a lo ancho: el contenido se acomoda al ancho disponible. */
    public static JScrollPane scrollVertical(Component c) {
        JScrollPane s = scroll(new PanelAncho(c));
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return s;
    }

    /** Scroll con borde visible, para areas de texto dentro de un formulario. */
    public static JScrollPane scrollConBorde(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(Tema.borde()));
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    /* ---------------- chips de estado ---------------- */

    /**
     * Distintivo de estado: punto de color + etiqueta. El color nunca viaja
     * solo; el texto siempre acompania, por accesibilidad.
     */
    public static JComponent chip(String texto, Color color) {
        JLabel l = new JLabel(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(mezcla(color, Tema.superficie(), Tema.esOscuro() ? 0.22f : 0.13f));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 999, 999);
                g2.setColor(color);
                g2.fillOval(9, getHeight() / 2 - 3, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Tema.fuente(11, Font.BOLD));
        l.setForeground(Tema.texto());
        l.setBorder(BorderFactory.createEmptyBorder(3, 22, 3, 10));
        l.setOpaque(false);
        return l;
    }

    public static Color mezcla(Color frente, Color fondo, float alfa) {
        return new Color(
                Math.round(frente.getRed()   * alfa + fondo.getRed()   * (1 - alfa)),
                Math.round(frente.getGreen() * alfa + fondo.getGreen() * (1 - alfa)),
                Math.round(frente.getBlue()  * alfa + fondo.getBlue()  * (1 - alfa)));
    }
}
