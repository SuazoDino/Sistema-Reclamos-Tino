package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Barras horizontales de serie unica: una magnitud por categoria. Se eligio
 * horizontal porque las etiquetas (areas, tipos de bien) son textos largos.
 *
 * Decisiones de lectura, deliberadas:
 *  - una sola serie, un solo tono, sin leyenda: el titulo de la tarjeta la nombra;
 *  - valor rotulado al final de cada barra, asi el dato no depende del eje;
 *  - extremo redondeado solo del lado del valor, anclado a la linea base;
 *  - grilla y eje recesivos, por detras de las barras;
 *  - tooltip por barra al pasar el mouse.
 */
public class GraficoBarras extends JPanel {

    private static final int ALTO_BARRA = 18;
    private static final int SEP_BARRA = 14;     // separacion vertical entre barras
    private static final int RADIO = 4;          // extremo del dato
    private static final int ANCHO_ETIQUETA = 168;
    private static final int MARGEN_VALOR = 44;

    private final Map<String, Integer> datos = new LinkedHashMap<>();
    private final String unidad;
    private Rectangle[] zonas = new Rectangle[0];

    public GraficoBarras(Map<String, Integer> datos, String unidad) {
        this.datos.putAll(datos);
        this.unidad = unidad;
        setOpaque(false);
        setToolTipText("");   // habilita getToolTipText(MouseEvent)
    }

    @Override public Dimension getPreferredSize() {
        int alto = datos.size() * (ALTO_BARRA + SEP_BARRA) + 24;
        return new Dimension(420, alto);
    }

    @Override public String getToolTipText(MouseEvent e) {
        int i = 0;
        for (Map.Entry<String, Integer> en : datos.entrySet()) {
            if (i < zonas.length && zonas[i] != null && zonas[i].contains(e.getPoint())) {
                return en.getKey() + ": " + en.getValue() + " " + unidad;
            }
            i++;
        }
        return null;
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (datos.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int max = datos.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int x0 = ANCHO_ETIQUETA;
        int anchoUtil = Math.max(40, getWidth() - x0 - MARGEN_VALOR);
        int y = 12;
        zonas = new Rectangle[datos.size()];

        // grilla recesiva: cuatro divisiones, por detras de las barras
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Tema.grilla());
        int altoTotal = datos.size() * (ALTO_BARRA + SEP_BARRA) - SEP_BARRA;
        for (int d = 1; d <= 4; d++) {
            int gx = x0 + anchoUtil * d / 4;
            g2.drawLine(gx, y - 4, gx, y + altoTotal + 4);
        }

        int i = 0;
        for (Map.Entry<String, Integer> en : datos.entrySet()) {
            int ancho = Math.max(3, (int) Math.round(anchoUtil * (en.getValue() / (double) max)));

            // etiqueta de categoria, recortada con elipsis
            g2.setFont(Tema.cuerpo());
            g2.setColor(Tema.texto());
            String etiqueta = recortar(g2, en.getKey(), ANCHO_ETIQUETA - Tema.ESP_MD);
            int lineaBase = y + ALTO_BARRA - (ALTO_BARRA - g2.getFontMetrics().getAscent()) / 2 - 2;
            g2.drawString(etiqueta, 0, lineaBase);

            // barra: extremo redondeado solo del lado del valor
            g2.setColor(Tema.serie());
            g2.fill(extremoRedondeado(x0, y, ancho, ALTO_BARRA));

            // rotulo directo del valor
            g2.setFont(Tema.fuente(12, Font.BOLD));
            g2.setColor(Tema.texto());
            g2.drawString(String.valueOf(en.getValue()), x0 + ancho + Tema.ESP_SM, lineaBase);

            zonas[i] = new Rectangle(0, y - SEP_BARRA / 2,
                    getWidth(), ALTO_BARRA + SEP_BARRA);

            y += ALTO_BARRA + SEP_BARRA;
            i++;
        }

        // linea base del eje, por delante de la grilla pero discreta
        g2.setColor(Tema.ejeBase());
        g2.drawLine(x0, 8, x0, y - SEP_BARRA + 4);

        g2.dispose();
    }

    /** Rectangulo con las dos esquinas del extremo derecho redondeadas. */
    private static Shape extremoRedondeado(int x, int y, int ancho, int alto) {
        if (ancho <= RADIO * 2) return new Rectangle2D.Float(x, y, ancho, alto);
        Area a = new Area(new RoundRectangle2D.Float(x, y, ancho, alto, RADIO * 2, RADIO * 2));
        a.add(new Area(new Rectangle2D.Float(x, y, RADIO * 2, alto)));
        return a;
    }

    private static String recortar(Graphics2D g2, String texto, int anchoMax) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(texto) <= anchoMax) return texto;
        String puntos = "...";
        int n = texto.length();
        while (n > 1 && fm.stringWidth(texto.substring(0, n) + puntos) > anchoMax) n--;
        return texto.substring(0, n) + puntos;
    }
}
