package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Superficie con esquinas redondeadas, borde fino y cabecera opcional.
 * Es el unico contenedor visible del sistema: todo el contenido vive dentro
 * de una tarjeta, de modo que la jerarquia se lee sola.
 */
public class Tarjeta extends JPanel {

    private static final int RADIO = 10;

    private final JPanel cuerpo = new JPanel(new BorderLayout());
    private JPanel cabecera;

    public Tarjeta() {
        super(new BorderLayout());
        setOpaque(false);
        cuerpo.setOpaque(false);
        cuerpo.setBorder(Ui.relleno(Tema.ESP_MD));
        super.add(cuerpo, BorderLayout.CENTER);
    }

    public Tarjeta(String titulo) { this(titulo, null); }

    public Tarjeta(String titulo, String descripcion) {
        this();
        cabecera = Ui.panel(new BorderLayout());
        cabecera.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.borde()),
                Ui.relleno(Tema.ESP_MD, Tema.ESP_MD, Tema.ESP_SM + 4, Tema.ESP_MD)));

        JPanel textos = Ui.panel(new GridLayout(0, 1, 0, 2));
        textos.add(Ui.subtitulo(titulo));
        if (descripcion != null) textos.add(Ui.micro(descripcion));
        cabecera.add(textos, BorderLayout.CENTER);

        super.add(cabecera, BorderLayout.NORTH);
    }

    /** Coloca controles alineados a la derecha en la cabecera (filtros, acciones). */
    public void acciones(Component... componentes) {
        if (cabecera == null) throw new IllegalStateException("La tarjeta no tiene cabecera");
        cabecera.add(Ui.filaDerecha(Tema.ESP_SM, componentes), BorderLayout.EAST);
    }

    /** Quita el relleno interno, util cuando el contenido es una tabla a sangre. */
    public Tarjeta sinRelleno() {
        cuerpo.setBorder(BorderFactory.createEmptyBorder());
        return this;
    }

    public Tarjeta relleno(int px) {
        cuerpo.setBorder(Ui.relleno(px));
        return this;
    }

    @Override public Component add(Component c) { return cuerpo.add(c); }

    public void add(Component c, Object restriccion) { cuerpo.add(c, restriccion); }

    public JPanel cuerpo() { return cuerpo; }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(Tema.superficie());
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, RADIO, RADIO));
        g2.setColor(Tema.borde());
        g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, RADIO, RADIO));
        g2.dispose();
        super.paintComponent(g);
    }
}
