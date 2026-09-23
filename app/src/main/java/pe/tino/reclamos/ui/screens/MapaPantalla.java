package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Tarjeta;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Iconos;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapa de modulos: la pantalla de entrada del sistema. Reproduce el diagrama
 * "DISENIO ARQUITECTONICO - MODULOS TIPICOS" del prototipo, con las cajas en
 * las mismas posiciones relativas que en la hoja de Excel. Cada caja es un
 * boton: al pulsarla se entra al menu de ese modulo o directo a su catalogo.
 */
public class MapaPantalla extends Pantalla {

    /**
     * Caja del diagrama. Las coordenadas estan en pulgadas, tal como vienen del
     * archivo de dibujo del prototipo, y se escalan al tamanio de la ventana.
     */
    private record Caja(String clave, String texto, String icono,
                        double x, double y, double ancho, double alto, boolean raiz) {}

    /* posiciones tomadas del dibujo original (xl/drawings/drawing2.xml) */
    private static final List<Caja> CAJAS = List.of(
            new Caja(null,            "SISTEMA DE RECLAMOS", "bandeja",   4.84, 0.17, 3.38, 0.40, true),
            new Caja("seguridad",     "SEGURIDAD",           "escudo",    4.83, 0.83, 1.55, 0.40, false),
            new Caja("batch",         "BATCH",               "reloj",     8.64, 1.42, 1.55, 0.40, false),
            new Caja("atencion",      "ON-LINE",             "bandeja",   2.31, 1.50, 1.55, 0.40, false),
            new Caja("m-operativo",   "OPERATIVO",           "personas",  3.91, 2.14, 1.70, 0.40, false),
            new Caja("m-aplicativo",  "APLICATIVO",          "tablero",   7.21, 2.15, 1.70, 0.40, false),
            new Caja("m-gerencial",   "GERENCIAL",           "grafico",   0.37, 2.16, 1.70, 0.40, false),
            new Caja("m-tecnico",     "TECNICO",             "ajustes",   9.82, 2.17, 1.70, 0.40, false),
            new Caja("m-mantparam",   "MANT-PARAM",          "ajustes",   0.68, 3.00, 1.70, 0.40, false),
            new Caja("actbd",         "ACT-BD",              "base",      7.61, 3.09, 1.60, 0.40, false),
            new Caja("mantbd",        "MANT-BD",             "base",     10.34, 3.10, 1.60, 0.40, false),
            new Caja("m-area",        "AREA",                "personas",  3.04, 3.34, 1.70, 0.40, false),
            new Caja("m-cliente",     "CLIENTE",             "personas",  5.26, 3.34, 1.70, 0.40, false),
            new Caja("estadisticas",  "ESTADISTICAS",        "grafico",   7.66, 3.86, 1.95, 0.40, false),
            new Caja("contingencia",  "CONTINGENCIA",        "escudo",   10.26, 4.04, 1.95, 0.40, false),
            new Caja("registro",      "DATA ENTRY",          "formulario", 5.69, 4.09, 1.75, 0.40, false),
            new Caja("registro",      "DATA ENTRY",          "formulario", 3.49, 4.19, 1.75, 0.40, false),
            new Caja("reportes",      "REPORTES",            "reporte",   5.69, 4.69, 1.75, 0.40, false),
            new Caja("reportes",      "REPORTES",            "reporte",   3.47, 4.71, 1.75, 0.40, false),
            new Caja("consulta",      "CONSULTA",            "lupa",      0.70, 6.50, 1.60, 0.40, false)
    );

    /** Ramas del arbol: primer indice el padre, el resto sus hijos. */
    private static final int[][] RAMAS = {
            {0, 1, 3, 2, 6, 4, 5, 7},   // sistema -> seguridad, on-line, batch, gerencial,
                                        //            operativo, aplicativo, tecnico
            {5, 9, 13},                 // aplicativo -> act-bd, estadisticas
            {7, 10, 14},                // tecnico -> mant-bd, contingencia
            {4, 11, 12},                // operativo -> area, cliente
            {11, 16, 18},               // area -> data entry, reportes
            {12, 15, 17},               // cliente -> data entry, reportes
            {6, 8, 19}                  // gerencial -> mant-param, consulta
    };

    public MapaPantalla() {
        super("Diseno arquitectonico", "Mapa de modulos",
                "Pulse un modulo para entrar. Es el mismo diagrama de modulos tipicos del prototipo.");

        Tarjeta lienzo = new Tarjeta();
        lienzo.relleno(Tema.ESP_LG);
        lienzo.add(new Diagrama(), BorderLayout.CENTER);
        contenido().add(lienzo, BorderLayout.CENTER);
    }

    /* ------------------------------------------------------------------ */

    /**
     * Dibuja los conectores y coloca las cajas. El posicionamiento es propio
     * (las coordenadas son del diagrama, no del layout de Swing), pero cada
     * caja es un JButton de verdad: conserva foco, teclado y accesibilidad.
     */
    private static class Diagrama extends JPanel {

        private static final double MARGEN = 0.25;   // pulgadas
        private final List<JComponent> botones = new ArrayList<>();
        private double escala = 1;
        private double desX, desY;

        Diagrama() {
            super(null);
            setOpaque(false);

            for (Caja c : CAJAS) {
                JComponent b = c.raiz() ? new CajaRaiz(c) : new CajaModulo(c);
                botones.add(b);
                add(b);
            }
        }

        @Override public Dimension getPreferredSize() { return new Dimension(900, 560); }

        @Override public void doLayout() {
            double anchoDiagrama = 0, altoDiagrama = 0;
            for (Caja c : CAJAS) {
                anchoDiagrama = Math.max(anchoDiagrama, c.x() + c.ancho());
                altoDiagrama = Math.max(altoDiagrama, c.y() + c.alto());
            }
            anchoDiagrama += MARGEN * 2;
            altoDiagrama += MARGEN * 2;

            escala = Math.min(getWidth() / anchoDiagrama, getHeight() / altoDiagrama);
            desX = (getWidth() - anchoDiagrama * escala) / 2 + MARGEN * escala;
            desY = (getHeight() - altoDiagrama * escala) / 2 + MARGEN * escala;

            for (int i = 0; i < CAJAS.size(); i++) {
                botones.get(i).setBounds(rect(CAJAS.get(i)));
            }
        }

        private Rectangle rect(Caja c) {
            return new Rectangle(
                    (int) Math.round(desX + c.x() * escala),
                    (int) Math.round(desY + c.y() * escala),
                    (int) Math.round(c.ancho() * escala),
                    (int) Math.round(c.alto() * escala));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.borde());
            g2.setStroke(new BasicStroke(1.4f));

            for (int[] rama : RAMAS) rama(g2, rama);
            g2.dispose();
        }

        /**
         * Rutea una rama completa. Si los hijos estan en fila se usa un bus
         * horizontal por encima de ellos; si estan apilados en columna, una
         * espina vertical a su izquierda con derivaciones. Las dos formas
         * evitan que una linea atraviese una caja.
         */
        private void rama(Graphics2D g2, int[] rama) {
            Rectangle padre = rect(CAJAS.get(rama[0]));
            Rectangle[] hijos = new Rectangle[rama.length - 1];
            for (int i = 1; i < rama.length; i++) hijos[i - 1] = rect(CAJAS.get(rama[i]));

            int minCentro = Integer.MAX_VALUE, maxCentro = Integer.MIN_VALUE;
            int minIzq = Integer.MAX_VALUE, minArriba = Integer.MAX_VALUE;
            for (Rectangle h : hijos) {
                minCentro = Math.min(minCentro, h.x + h.width / 2);
                maxCentro = Math.max(maxCentro, h.x + h.width / 2);
                minIzq = Math.min(minIzq, h.x);
                minArriba = Math.min(minArriba, h.y);
            }
            int salidaX = padre.x + padre.width / 2;
            int salidaY = padre.y + padre.height;

            boolean enColumna = (maxCentro - minCentro) < escala * 1.0;
            if (enColumna) {
                int espina = minIzq - (int) Math.round(escala * 0.18);
                int quiebre = salidaY + (int) Math.round(escala * 0.12);
                Rectangle ultimo = hijos[hijos.length - 1];
                g2.drawLine(salidaX, salidaY, salidaX, quiebre);
                g2.drawLine(salidaX, quiebre, espina, quiebre);
                g2.drawLine(espina, quiebre, espina, ultimo.y + ultimo.height / 2);
                for (Rectangle h : hijos) {
                    g2.drawLine(espina, h.y + h.height / 2, h.x, h.y + h.height / 2);
                }
                return;
            }
            int bus = minArriba - (int) Math.round(escala * 0.14);
            g2.drawLine(salidaX, salidaY, salidaX, bus);
            g2.drawLine(minCentro, bus, maxCentro, bus);
            for (Rectangle h : hijos) {
                g2.drawLine(h.x + h.width / 2, bus, h.x + h.width / 2, h.y);
            }
        }
    }

    /* ------------------------------------------------------------------ */

    /** Caja raiz del diagrama: rotulo del sistema, no navega. */
    private static class CajaRaiz extends JLabel {
        CajaRaiz(Caja c) {
            super(c.texto(), SwingConstants.CENTER);
            setFont(Tema.fuente(13, Font.BOLD));
            setForeground(Color.WHITE);
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.barraLateral());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Caja navegable: icono a la izquierda, rotulo del modulo y borde de acento al pasar. */
    private static class CajaModulo extends JButton {
        private boolean encima;

        CajaModulo(Caja c) {
            super(c.texto());
            setIcon(Iconos.de(c.icono(), 14, Tema.acento()));
            setIconTextGap(Tema.ESP_XS + 2);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(Tema.fuente(11, Font.BOLD));
            setForeground(Tema.texto());
            setMargin(new Insets(0, 4, 0, 4));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Entrar a " + c.texto());

            addActionListener(e -> Navegacion.ir(c.clave()));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { encima = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean resaltado = encima || hasFocus();
            g2.setColor(resaltado ? Tema.acentoSuave() : Tema.superficie());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
            g2.setColor(resaltado ? Tema.acento() : Tema.borde());
            g2.setStroke(new BasicStroke(resaltado ? 1.8f : 1.2f));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
