package pe.tino.reclamos.ui;

import pe.tino.reclamos.ui.theme.Iconos;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Menu lateral con los modulos del diseno arquitectonico. Los grupos
 * (Aplicativo, Mant-Param, Seguridad, Gerencial) replican el diagrama de
 * modulos tipicos del prototipo.
 */
public class BarraLateral extends JPanel {

    /** Una entrada del menu: clave de pantalla, texto e icono. */
    public record Item(String clave, String texto, String icono) {}

    private final List<Boton> botones = new ArrayList<>();
    private final Consumer<String> alElegir;
    private String activo;

    public BarraLateral(Consumer<String> alElegir) {
        super();
        this.alElegir = alElegir;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Tema.barraLateral());
        setPreferredSize(new Dimension(272, 100));
        setBorder(BorderFactory.createEmptyBorder(0, 0, Tema.ESP_MD, 0));

        add(alinear(marca()));
        grupo("Aplicativo", List.of(
                new Item("tablero", "Tablero de control", "tablero"),
                new Item("registro", "Registro de reclamo", "formulario"),
                new Item("atencion", "Atencion de reclamos", "bandeja"),
                new Item("consulta", "Consulta de reclamos", "lupa")));
        grupo("Mant-Param", List.of(
                new Item("catalogos", "Catalogos", "ajustes"),
                new Item("productos", "Catalogo de productos", "jerarquia"),
                new Item("protocolos", "Protocolos", "flujo"),
                new Item("reglas", "Reglas de negocio", "reglas")));
        grupo("Gerencial", List.of(
                new Item("estadisticas", "Indicadores", "grafico")));
        grupo("Seguridad", List.of(
                new Item("seguridad", "Perfiles y accesos", "escudo")));

        add(alinear((JComponent) Box.createVerticalGlue()));
        add(alinear(pie()));
    }

    /**
     * BoxLayout alinea los hijos entre si por su alignmentX: basta con que uno
     * quede en 0.5 para que toda la columna se descoloque. Aqui se fuerza que
     * todos compartan el borde izquierdo.
     */
    private static JComponent alinear(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private JComponent marca() {
        JPanel p = new JPanel(new BorderLayout(Tema.ESP_SM + 2, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(Tema.ESP_LG, Tema.ESP_MD, Tema.ESP_LG, Tema.ESP_MD));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JLabel icono = new JLabel(Iconos.de("bandeja", 26, Color.WHITE));
        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 1));
        textos.setOpaque(false);

        JLabel nombre = new JLabel("Sistema de Reclamos");
        nombre.setFont(Tema.fuente(15, Font.BOLD));
        nombre.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Diseno externo · prototipo");
        sub.setFont(Tema.micro());
        sub.setForeground(Tema.barraTexto());

        textos.add(nombre);
        textos.add(sub);
        p.add(icono, BorderLayout.WEST);
        p.add(textos, BorderLayout.CENTER);
        return p;
    }

    private void grupo(String titulo, List<Item> items) {
        JLabel l = new JLabel(titulo.toUpperCase());
        l.setFont(Tema.fuente(10, Font.BOLD));
        l.setForeground(new Color(0x7C8798));
        l.setBorder(BorderFactory.createEmptyBorder(Tema.ESP_MD, Tema.ESP_MD + 2, Tema.ESP_SM, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        add(l);

        for (Item i : items) {
            Boton b = new Boton(i);
            botones.add(b);
            add(alinear(b));
        }
    }

    private JComponent pie() {
        JLabel l = new JLabel("Java 21 · Swing · FlatLaf");
        l.setFont(Tema.micro());
        l.setForeground(new Color(0x6B7686));
        l.setBorder(BorderFactory.createEmptyBorder(Tema.ESP_MD, Tema.ESP_MD + 2, 0, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return l;
    }

    public void seleccionar(String clave) {
        activo = clave;
        botones.forEach(Boton::repaint);
        alElegir.accept(clave);
    }

    /** Entrada del menu: barra de acento a la izquierda cuando esta activa. */
    private class Boton extends JPanel {
        private final Item item;
        private boolean encima;

        Boton(Item item) {
            this.item = item;
            setOpaque(false);
            setLayout(new BorderLayout(Tema.ESP_SM + 2, 0));
            setBorder(BorderFactory.createEmptyBorder(9, Tema.ESP_MD + 2, 9, Tema.ESP_MD));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(item.texto());

            JLabel texto = new JLabel(item.texto()) {
                @Override public Color getForeground() {
                    return esActivo() ? Color.WHITE : Tema.barraTexto();
                }
            };
            texto.setFont(Tema.cuerpo());
            add(new JLabel() {
                @Override public Icon getIcon() {
                    return Iconos.de(item.icono(), 17, esActivo() ? Color.WHITE : Tema.barraTexto());
                }
            }, BorderLayout.WEST);
            add(texto, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { seleccionar(item.clave()); }
                @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { encima = false; repaint(); }
            });
        }

        private boolean esActivo() { return item.clave().equals(activo); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (esActivo()) {
                g2.setColor(new Color(255, 255, 255, 26));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Tema.serie());
                g2.fillRect(0, 0, 3, getHeight());
            } else if (encima) {
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
