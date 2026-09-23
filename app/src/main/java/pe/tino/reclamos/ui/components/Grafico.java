package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Grafico de una sola serie, dibujado a mano para no sumar dependencias.
 *
 * Dos formas: barras horizontales para comparar categorias (que area, que
 * bien) y tendencia para ver un valor mes a mes con su proyeccion, que es lo
 * que el libro pide a la consulta gerencial. Al pasar el mouse sobre una
 * barra o un punto se ve su detalle.
 */
public class Grafico extends JComponent {

    /** Un valor del grafico. El detalle es lo que muestra el tooltip. */
    public record Punto(String etiqueta, double valor, String detalle) {}

    private static final Color SERIE = new Color(0x2A78D6);
    private static final Color GRILLA = new Color(0xE2E2E2);

    private enum Forma { VACIO, BARRAS, TENDENCIA }

    private Forma forma = Forma.VACIO;
    private String mensaje = "";
    private String unidad = "";
    private List<Punto> puntos = List.of();
    private Punto enCurso;
    private Punto proyeccion;

    /** Zonas sensibles al mouse del ultimo pintado, con su tooltip. */
    private final List<Shape> zonas = new ArrayList<>();
    private final List<String> textos = new ArrayList<>();

    public Grafico() {
        setOpaque(true);
        setBackground(Tema.SUPERFICIE);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /** Sin grafico: solo un mensaje centrado. */
    public void vacio(String mensaje) {
        this.forma = Forma.VACIO;
        this.mensaje = mensaje;
        repaint();
    }

    /** Barras horizontales, de mayor a menor como vienen. */
    public void barras(List<Punto> puntos, String unidad) {
        this.forma = Forma.BARRAS;
        this.puntos = List.copyOf(puntos);
        this.unidad = unidad;
        repaint();
    }

    /**
     * Tendencia: los meses cerrados como linea continua, la proyeccion del mes
     * actual como linea punteada y lo que va del mes como punto hueco.
     * enCurso puede ser null si el mes todavia no tiene dato.
     */
    public void tendencia(List<Punto> meses, Punto enCurso, Punto proyeccion, String unidad) {
        this.forma = Forma.TENDENCIA;
        this.puntos = List.copyOf(meses);
        this.enCurso = enCurso;
        this.proyeccion = proyeccion;
        this.unidad = unidad;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = zonas.size() - 1; i >= 0; i--) {
            if (zonas.get(i).contains(e.getPoint())) return textos.get(i);
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(Tema.cuerpo());
        zonas.clear();
        textos.clear();

        switch (forma) {
            case VACIO -> pintarMensaje(g);
            case BARRAS -> pintarBarras(g);
            case TENDENCIA -> pintarTendencia(g);
        }
        g.dispose();
    }

    private void pintarMensaje(Graphics2D g) {
        g.setColor(Tema.TEXTO_SUAVE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(mensaje, (getWidth() - fm.stringWidth(mensaje)) / 2,
                (getHeight() + fm.getAscent()) / 2);
    }

    /* ---------------- barras ---------------- */

    private void pintarBarras(Graphics2D g) {
        if (puntos.isEmpty()) { vacioSinPintar(g, "Sin datos para graficar"); return; }

        FontMetrics fm = g.getFontMetrics();
        int m = escala(Tema.ESP_LG);
        int anchoEtiquetas = 0;
        for (Punto p : puntos) anchoEtiquetas = Math.max(anchoEtiquetas, fm.stringWidth(p.etiqueta()));
        anchoEtiquetas = Math.min(anchoEtiquetas, getWidth() * 2 / 5);
        int anchoValor = 0;
        for (Punto p : puntos) anchoValor = Math.max(anchoValor, fm.stringWidth(valor(p.valor())));

        int x0 = m + anchoEtiquetas + escala(Tema.ESP_SM);
        int ancho = Math.max(1, getWidth() - x0 - anchoValor - m - escala(Tema.ESP_SM));
        int alto = getHeight() - 2 * m;
        double banda = (double) alto / puntos.size();
        double grosor = Math.min(escala(24), banda * 0.6);
        double maximo = puntos.stream().mapToDouble(Punto::valor).max().orElse(1);
        if (maximo <= 0) maximo = 1;

        for (int i = 0; i < puntos.size(); i++) {
            Punto p = puntos.get(i);
            double centro = m + banda * i + banda / 2;
            double largo = ancho * p.valor() / maximo;
            double y = centro - grosor / 2;

            g.setColor(SERIE);
            g.fill(barra(x0, y, largo, grosor));

            g.setColor(Tema.TEXTO);
            String etiqueta = recortar(p.etiqueta(), fm, anchoEtiquetas);
            g.drawString(etiqueta, x0 - escala(Tema.ESP_SM) - fm.stringWidth(etiqueta),
                    (int) (centro + fm.getAscent() / 2.0 - 1));
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString(valor(p.valor()), (int) (x0 + largo + escala(Tema.ESP_SM)),
                    (int) (centro + fm.getAscent() / 2.0 - 1));

            zonas.add(new Rectangle2D.Double(0, m + banda * i, getWidth(), banda));
            textos.add(p.detalle());
        }

        g.setColor(Tema.BORDE);
        g.drawLine(x0, m, x0, m + alto);
    }

    /** Barra con el extremo de datos redondeado y el pie recto sobre la base. */
    private Shape barra(double x, double y, double largo, double grosor) {
        double r = Math.min(escala(4), largo);
        Area forma = new Area(new RoundRectangle2D.Double(x, y, largo, grosor, r * 2, r * 2));
        forma.add(new Area(new Rectangle2D.Double(x, y, Math.max(0, largo - r), grosor)));
        return forma;
    }

    /* ---------------- tendencia ---------------- */

    private void pintarTendencia(Graphics2D g) {
        if (puntos.isEmpty()) { vacioSinPintar(g, "Sin histórico para graficar"); return; }

        FontMetrics fm = g.getFontMetrics();
        int m = escala(Tema.ESP_LG);
        int alturaTexto = fm.getHeight();

        double maximo = puntos.stream().mapToDouble(Punto::valor).max().orElse(1);
        if (enCurso != null) maximo = Math.max(maximo, enCurso.valor());
        if (proyeccion != null) maximo = Math.max(maximo, proyeccion.valor());
        maximo = techo(maximo * 1.1);

        int anchoEje = fm.stringWidth(valor(maximo)) + escala(Tema.ESP_SM);
        int x0 = m + anchoEje;
        int arriba = m + alturaTexto + escala(Tema.ESP_SM);   // espacio para la leyenda
        int abajo = getHeight() - m - alturaTexto - escala(Tema.ESP_XS);
        int derecha = getWidth() - m - fm.stringWidth("Proyección 000.0") / 2;
        int columnas = puntos.size() + 1;                        // meses cerrados + el actual
        double paso = (double) (derecha - x0) / Math.max(1, columnas - 1);

        // grilla horizontal y valores del eje
        for (int i = 0; i <= 4; i++) {
            double v = maximo * i / 4;
            int y = (int) Math.round(abajo - (abajo - arriba) * v / maximo);
            g.setColor(i == 0 ? Tema.BORDE : GRILLA);
            g.drawLine(x0, y, derecha, y);
            g.setColor(Tema.TEXTO_SUAVE);
            String t = valor(v);
            g.drawString(t, x0 - escala(Tema.ESP_SM) - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
        }

        // meses en el eje x
        List<String> meses = new ArrayList<>();
        puntos.forEach(p -> meses.add(p.etiqueta()));
        meses.add(proyeccion != null ? proyeccion.etiqueta()
                : enCurso != null ? enCurso.etiqueta() : "");
        g.setColor(Tema.TEXTO_SUAVE);
        for (int i = 0; i < meses.size(); i++) {
            String t = meses.get(i);
            int x = (int) Math.round(x0 + paso * i);
            g.drawString(t, x - fm.stringWidth(t) / 2, abajo + fm.getAscent() + escala(Tema.ESP_XS));
        }

        double escalaY = (abajo - arriba) / maximo;
        double diametro = escala(8);

        // linea real
        Path2D linea = new Path2D.Double();
        for (int i = 0; i < puntos.size(); i++) {
            double x = x0 + paso * i, y = abajo - puntos.get(i).valor() * escalaY;
            if (i == 0) linea.moveTo(x, y); else linea.lineTo(x, y);
        }
        g.setColor(SERIE);
        g.setStroke(new BasicStroke(escala(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linea);

        double xUlt = x0 + paso * (puntos.size() - 1);
        double yUlt = abajo - puntos.get(puntos.size() - 1).valor() * escalaY;
        double xAct = x0 + paso * puntos.size();

        // proyeccion punteada hacia el mes actual
        if (proyeccion != null) {
            double yP = abajo - proyeccion.valor() * escalaY;
            g.setStroke(new BasicStroke(escala(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{escala(5), escala(5)}, 0f));
            g.draw(new Line2D.Double(xUlt, yUlt, xAct, yP));
            marcador(g, xAct, yP, diametro, false);
            etiquetaPunto(g, fm, "Proyección " + valor(proyeccion.valor()), xAct, yP, arriba);
            zona(xAct, yP, diametro, proyeccion.detalle());
        }

        // puntos reales
        for (int i = 0; i < puntos.size(); i++) {
            double x = x0 + paso * i, y = abajo - puntos.get(i).valor() * escalaY;
            marcador(g, x, y, diametro, true);
            zona(x, y, diametro, puntos.get(i).detalle());
        }
        etiquetaPunto(g, fm, valor(puntos.get(puntos.size() - 1).valor()), xUlt, yUlt, arriba);

        // lo que va del mes: un punto hueco, sin linea, porque el mes no cerro
        if (enCurso != null) {
            double y = abajo - enCurso.valor() * escalaY;
            marcador(g, xAct, y, diametro, false);
            g.setColor(Tema.TEXTO_SUAVE);
            String t = "En curso " + valor(enCurso.valor());
            g.drawString(t, (int) (xAct - fm.stringWidth(t) - diametro),
                    (int) (y + fm.getAscent() / 2.0 - 1));
            zona(xAct, y, diametro, enCurso.detalle());
        }

        leyenda(g, fm, x0, m);
    }

    private void marcador(Graphics2D g, double x, double y, double d, boolean lleno) {
        Ellipse2D c = new Ellipse2D.Double(x - d / 2, y - d / 2, d, d);
        g.setStroke(new BasicStroke(escala(2)));
        g.setColor(Tema.SUPERFICIE);
        g.fill(c);
        g.setColor(SERIE);
        if (lleno) g.fill(c); else g.draw(c);
    }

    private void etiquetaPunto(Graphics2D g, FontMetrics fm, String t, double x, double y, int tope) {
        g.setColor(Tema.TEXTO);
        int ty = (int) Math.max(tope + fm.getAscent(), y - escala(Tema.ESP_SM));
        g.drawString(t, (int) (x - fm.stringWidth(t) / 2.0), ty);
    }

    private void leyenda(Graphics2D g, FontMetrics fm, int x, int y) {
        int base = y + fm.getAscent();
        int medio = y + fm.getAscent() / 2;
        int largo = escala(20), sep = escala(Tema.ESP_SM), bloque = escala(Tema.ESP_LG);

        g.setColor(SERIE);
        g.setStroke(new BasicStroke(escala(2)));
        g.drawLine(x, medio, x + largo, medio);
        x += largo + sep;
        g.setColor(Tema.TEXTO_SUAVE);
        g.drawString("Meses cerrados " + unidad, x, base);
        x += fm.stringWidth("Meses cerrados " + unidad) + bloque;

        if (proyeccion != null) {
            g.setColor(SERIE);
            g.setStroke(new BasicStroke(escala(2), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[]{escala(5), escala(5)}, 0f));
            g.drawLine(x, medio, x + largo, medio);
            x += largo + sep;
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString("Proyección del mes", x, base);
            x += fm.stringWidth("Proyección del mes") + bloque;
        }
        if (enCurso != null) {
            marcador(g, x + escala(4), medio, escala(8), false);
            x += escala(8) + sep;
            g.setColor(Tema.TEXTO_SUAVE);
            g.drawString("Lo que va del mes", x, base);
        }
    }

    private void zona(double x, double y, double d, String texto) {
        double r = Math.max(d, escala(16));
        zonas.add(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        textos.add(texto);
    }

    /* ---------------- utilitarios ---------------- */

    private void vacioSinPintar(Graphics2D g, String texto) {
        mensaje = texto;
        pintarMensaje(g);
    }

    private static int escala(int px) { return (int) Math.round(px * Tema.escalaTexto()); }

    /** Redondea el maximo del eje a un numero redondo para que la grilla se lea. */
    private static double techo(double v) {
        if (v <= 0) return 1;
        double base = Math.pow(10, Math.floor(Math.log10(v)));
        for (double f : new double[]{1, 1.2, 1.6, 2, 2.4, 3, 4, 6, 8, 10}) {
            if (f * base >= v) return f * base;
        }
        return 10 * base;
    }

    private static String valor(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    private static String recortar(String t, FontMetrics fm, int ancho) {
        if (fm.stringWidth(t) <= ancho) return t;
        while (t.length() > 1 && fm.stringWidth(t + "...") > ancho) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
