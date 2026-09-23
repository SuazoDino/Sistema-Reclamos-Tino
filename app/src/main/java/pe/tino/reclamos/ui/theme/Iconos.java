package pe.tino.reclamos.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Iconos vectoriales dibujados con Java2D. No hay archivos de imagen en el
 * proyecto: cada icono es un trazo sobre una grilla de 24x24 que se escala
 * al tamano pedido y toma el color que se le indique.
 */
public final class Iconos {

    private Iconos() {}

    public static Icon de(String nombre, int tam, Color color) {
        return new IconoTrazo(nombre, tam, color);
    }

    public static Icon de(String nombre, int tam) {
        return new IconoTrazo(nombre, tam, Tema.texto());
    }

    private static final class IconoTrazo implements Icon {
        private final String nombre;
        private final int tam;
        private final Color color;

        IconoTrazo(String nombre, int tam, Color color) {
            this.nombre = nombre; this.tam = tam; this.color = color;
        }

        @Override public int getIconWidth()  { return tam; }
        @Override public int getIconHeight() { return tam; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            double e = tam / 24.0;
            g2.scale(e, e);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            dibujar(g2, nombre);
            g2.dispose();
        }
    }

    /* ---------------- catalogo de glifos (grilla 24x24) ---------------- */

    private static void dibujar(Graphics2D g, String n) {
        switch (n) {
            case "tablero" -> {                       // dashboard
                rect(g, 3, 3, 8, 8); rect(g, 13, 3, 8, 5);
                rect(g, 13, 10, 8, 11); rect(g, 3, 13, 8, 8);
            }
            case "formulario" -> {                    // data entry
                rect(g, 4, 3, 16, 18);
                linea(g, 8, 8, 16, 8); linea(g, 8, 12, 16, 12); linea(g, 8, 16, 13, 16);
            }
            case "bandeja" -> {                       // atencion on-line
                g.draw(new Path2D.Float() {{
                    moveTo(3, 13); lineTo(7, 13); lineTo(9, 16); lineTo(15, 16);
                    lineTo(17, 13); lineTo(21, 13);
                }});
                poli(g, 3, 13, 6, 4, 18, 4, 21, 13, 21, 19, 3, 19);
            }
            case "lupa" -> {
                circulo(g, 10.5, 10.5, 6.5);
                linea(g, 15.5, 15.5, 21, 21);
            }
            case "ajustes" -> {                       // sliders / mant-param
                linea(g, 4, 7, 20, 7); linea(g, 4, 12, 20, 12); linea(g, 4, 17, 20, 17);
                circulo(g, 9, 7, 2.3); circulo(g, 15, 12, 2.3); circulo(g, 7.5, 17, 2.3);
            }
            case "flujo" -> {                         // protocolos
                rect(g, 3, 3, 6, 5); rect(g, 15, 9, 6, 5); rect(g, 3, 16, 6, 5);
                linea(g, 9, 5.5, 12, 5.5); linea(g, 12, 5.5, 12, 11.5); linea(g, 12, 11.5, 15, 11.5);
                linea(g, 12, 11.5, 12, 18.5); linea(g, 12, 18.5, 9, 18.5);
            }
            case "reglas" -> {                        // balanza / reglas de negocio
                linea(g, 12, 3, 12, 21); linea(g, 6, 6, 18, 6);
                g.draw(new Arc2D.Double(3, 8, 8, 8, 180, 180, Arc2D.OPEN));
                linea(g, 7, 6, 3.6, 12.2); linea(g, 7, 6, 10.4, 12.2);
                g.draw(new Arc2D.Double(13, 8, 8, 8, 180, 180, Arc2D.OPEN));
                linea(g, 17, 6, 13.6, 12.2); linea(g, 17, 6, 20.4, 12.2);
                linea(g, 8, 21, 16, 21);
            }
            case "jerarquia" -> {                     // arbol producto
                rect(g, 9, 2, 6, 5); rect(g, 2, 15, 6, 5); rect(g, 16, 15, 6, 5);
                linea(g, 12, 7, 12, 11); linea(g, 5, 11, 19, 11);
                linea(g, 5, 11, 5, 15); linea(g, 19, 11, 19, 15);
            }
            case "escudo" -> {
                g.draw(new Path2D.Float() {{
                    moveTo(12, 2); lineTo(20, 5.5f); lineTo(20, 11);
                    curveTo(20, 16.5f, 16.5f, 20.5f, 12, 22);
                    curveTo(7.5f, 20.5f, 4, 16.5f, 4, 11);
                    lineTo(4, 5.5f); closePath();
                }});
                g.draw(new Path2D.Float() {{ moveTo(8.5f, 12); lineTo(11, 14.5f); lineTo(15.5f, 9.5f); }});
            }
            case "grafico" -> {
                linea(g, 3, 21, 21, 21); linea(g, 3, 3, 3, 21);
                rect(g, 6, 13, 3.4, 8); rect(g, 11.5, 8, 3.4, 13); rect(g, 17, 4.5, 3.4, 16.5);
            }
            case "reporte" -> {
                g.draw(new Path2D.Float() {{
                    moveTo(5, 2); lineTo(14, 2); lineTo(19, 7); lineTo(19, 22); lineTo(5, 22); closePath();
                }});
                g.draw(new Path2D.Float() {{ moveTo(14, 2); lineTo(14, 7); lineTo(19, 7); }});
                linea(g, 8, 12, 16, 12); linea(g, 8, 16, 16, 16);
            }
            case "personas" -> {
                circulo(g, 9, 8, 3.6); g.draw(new Arc2D.Double(2.5, 13, 13, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(12.5, 5, 7, 7, 250, 190, Arc2D.OPEN));
                g.draw(new Arc2D.Double(13, 13, 9, 12, 0, 95, Arc2D.OPEN));
            }
            case "base" -> {                          // base de datos
                g.draw(new Ellipse2D.Double(4, 3, 16, 5.5));
                linea(g, 4, 5.75, 4, 18.25); linea(g, 20, 5.75, 20, 18.25);
                g.draw(new Arc2D.Double(4, 9, 16, 5.5, 180, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(4, 15.5, 16, 5.5, 180, 180, Arc2D.OPEN));
            }
            case "reloj" -> {
                circulo(g, 12, 12, 9); linea(g, 12, 7, 12, 12); linea(g, 12, 12, 16, 14);
            }
            case "mas" -> { linea(g, 12, 5, 12, 19); linea(g, 5, 12, 19, 12); }
            case "check" -> { g.draw(new Path2D.Float() {{ moveTo(4, 12.5f); lineTo(9.5f, 18); lineTo(20, 6); }}); }
            case "cruz" -> { linea(g, 6, 6, 18, 18); linea(g, 18, 6, 6, 18); }
            case "flechaDer" -> { linea(g, 4, 12, 19, 12); g.draw(new Path2D.Float() {{ moveTo(13, 6); lineTo(19, 12); lineTo(13, 18); }}); }
            case "flechaIzq" -> { linea(g, 20, 12, 5, 12); g.draw(new Path2D.Float() {{ moveTo(11, 6); lineTo(5, 12); lineTo(11, 18); }}); }
            case "guardar" -> {
                g.draw(new Path2D.Float() {{
                    moveTo(4, 4); lineTo(16, 4); lineTo(20, 8); lineTo(20, 20); lineTo(4, 20); closePath();
                }});
                rect(g, 8, 4, 8, 5); rect(g, 7, 13, 10, 7);
            }
            case "luna" -> {
                g.draw(new Path2D.Float() {{
                    moveTo(20, 15.2f); curveTo(13.5f, 17.5f, 7, 13, 8.2f, 6.2f);
                    curveTo(3, 9, 2.8f, 16.5f, 8, 19.6f); curveTo(12.5f, 22.3f, 18.6f, 20.2f, 20, 15.2f);
                    closePath();
                }});
            }
            default -> rect(g, 4, 4, 16, 16);
        }
    }

    /* ---------------- primitivas ---------------- */

    private static void rect(Graphics2D g, double x, double y, double w, double h) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 3, 3));
    }
    private static void linea(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
    private static void circulo(Graphics2D g, double cx, double cy, double r) {
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }
    private static void poli(Graphics2D g, double... p) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.closePath();
        g.draw(path);
    }
}
