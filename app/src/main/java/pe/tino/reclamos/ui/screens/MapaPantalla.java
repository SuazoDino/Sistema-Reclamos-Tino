package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapa conceptual del sistema: el diagrama de diseno arquitectonico del
 * informe, con sus mismos modulos, su misma jerarquia y sus mismos colores.
 * Azul para los modulos, amarillo para los submodulos y rosa para las
 * pantallas; las cajas que tienen pantalla se pulsan para entrar.
 */
public class MapaPantalla extends JPanel {

    /** Nivel del nodo en el diagrama; decide el color de la caja. */
    private enum Estilo { MODULO, SUBMODULO, PANTALLA }

    /** Como se acomodan los hijos: en fila o apilados debajo. */
    private enum Disposicion { FILA, APILADA }

    private record Nodo(Estilo estilo, String clave, String texto,
                        Disposicion disposicion, List<Nodo> hijos) {

        static Nodo modulo(String texto, Disposicion d, Nodo... hijos) {
            return new Nodo(Estilo.MODULO, null, texto, d, List.of(hijos));
        }

        static Nodo modulo(String clave, String texto, Disposicion d, Nodo... hijos) {
            return new Nodo(Estilo.MODULO, clave, texto, d, List.of(hijos));
        }

        static Nodo submodulo(String texto, Nodo... hijos) {
            return new Nodo(Estilo.SUBMODULO, null, texto, Disposicion.APILADA, List.of(hijos));
        }

        static Nodo pantalla(String clave, String texto) {
            return new Nodo(Estilo.PANTALLA, clave, texto, Disposicion.FILA, List.of());
        }

        boolean esHoja()      { return hijos.isEmpty(); }
        boolean navega()      { return clave != null; }
        boolean hijosHoja()   { return !hijos.isEmpty() && hijos.stream().allMatch(Nodo::esHoja); }
    }

    /** El arbol del diagrama de diseno arquitectonico. */
    private static final Nodo RAIZ = Nodo.modulo("SISTEMA DE RECLAMOS", Disposicion.FILA,
            Nodo.modulo("seguridad-perfiles", "SEGURIDAD", Disposicion.FILA,
                    Nodo.modulo("ONLINE", Disposicion.FILA,
                            Nodo.modulo("GERENCIAL", Disposicion.APILADA,
                                    Nodo.submodulo("MANT-PARAM",
                                            Nodo.pantalla("cat-general", "Parámetro General"),
                                            Nodo.pantalla("cat-reclamos", "Catálogo de Reclamo"),
                                            Nodo.pantalla("cat-productos", "Catálogo de Producto"),
                                            Nodo.pantalla("cat-servicios", "Catálogo de Servicios"),
                                            Nodo.pantalla("cat-problemas", "Catálogo de Problemas"),
                                            Nodo.pantalla("cat-clientes", "Catálogo de Cliente"),
                                            Nodo.pantalla("cat-protocolos", "Catálogo de Protocolos"),
                                            Nodo.pantalla("cat-reglas", "Catálogo de Reglas"),
                                            Nodo.pantalla("cat-politicas", "Catálogo de Políticas"),
                                            Nodo.pantalla("cat-atencion", "Catálogo de Atención")),
                                    Nodo.submodulo("CONSULTA",
                                            Nodo.pantalla("tickets", "Consulta de Tickets"),
                                            Nodo.pantalla("indicadores", "Consulta de Indicadores"))),
                            Nodo.modulo("OPERATIVO", Disposicion.FILA,
                                    Nodo.submodulo("ÁREA",
                                            Nodo.pantalla("area-dataentry", "DATA ENTRY"),
                                            Nodo.pantalla("area-reportes", "REPORTES")),
                                    Nodo.submodulo("CLIENTE",
                                            Nodo.pantalla("cliente-dataentry", "DATA ENTRY"),
                                            Nodo.pantalla("cliente-reportes", "REPORTES")))),
                    Nodo.modulo("BATCH", Disposicion.FILA,
                            Nodo.modulo("APLICATIVO", Disposicion.APILADA,
                                    Nodo.submodulo("ACT-BD"),
                                    Nodo.submodulo("ESTADISTICAS")),
                            Nodo.modulo("TECNICO", Disposicion.APILADA,
                                    Nodo.submodulo("MANT-BD"),
                                    Nodo.submodulo("CONTINGENCIA")))));

    public MapaPantalla() {
        super(new BorderLayout(0, Tema.ESP_MD));
        setOpaque(false);
        setBorder(Ui.relleno(Tema.ESP_LG));

        JPanel titulo = Ui.panel(new BorderLayout(0, 2));
        titulo.add(Ui.titulo("Diseño arquitectónico"), BorderLayout.NORTH);
        titulo.add(Ui.suave("Seleccione la pantalla a la que desea entrar."), BorderLayout.CENTER);

        JPanel leyenda = Ui.panel(new BorderLayout());
        leyenda.add(Ui.suave("Las cajas rosadas son las pantallas del prototipo. "
                + "Los modulos sin pantalla no se pueden pulsar."), BorderLayout.WEST);

        add(titulo, BorderLayout.NORTH);
        add(Ui.scroll(new Diagrama()), BorderLayout.CENTER);
        add(leyenda, BorderLayout.SOUTH);
    }

    /* ------------------------------------------------------------------ */

    /**
     * Organigrama calculado desde el arbol. Un nodo en fila reparte a sus
     * hijos en horizontal y se centra sobre ellos; un nodo apilado los cuelga
     * en vertical con una espina a la izquierda. Las lineas terminan en punta
     * de flecha, como en el diagrama del informe.
     */
    private static class Diagrama extends JPanel {

        private static final int ALTO_CAJA = 24;
        private static final int SEP_NIVEL = 22;
        private static final int SEP_RAMA  = 16;
        private static final int SEP_HIJO  = 5;
        private static final int SANGRIA   = 22;
        private static final int MARGEN    = 16;
        private static final int HOLGURA   = 18;
        private static final int FLECHA    = 5;

        private record Caja(Nodo nodo, JComponent control, Rectangle marco, List<Caja> hijos) {}

        private final Map<Nodo, JComponent> controles = new IdentityHashMap<>();
        private final Map<Nodo, Integer> anchos = new IdentityHashMap<>();
        private final List<Caja> todas = new ArrayList<>();
        private Caja raiz;
        private Dimension medida = new Dimension(820, 560);

        Diagrama() {
            super(null);
            setBackground(Tema.SUPERFICIE);
            construir(RAIZ);
        }

        private void construir(Nodo n) {
            JComponent c = n.navega() ? new CajaBoton(n) : new CajaFija(n);
            controles.put(n, c);
            add(c);
            n.hijos().forEach(this::construir);
        }

        @Override public Dimension getPreferredSize() { return medida; }

        /** Las hojas de un mismo grupo comparten ancho, para que la pila quede pareja. */
        private void calcularAnchos(Nodo n) {
            anchos.put(n, controles.get(n).getPreferredSize().width + HOLGURA);
            n.hijos().forEach(this::calcularAnchos);

            if (n.hijosHoja()) {
                int max = n.hijos().stream().mapToInt(anchos::get).max().orElse(0);
                n.hijos().forEach(h -> anchos.put(h, max));
            }
        }

        @Override public void doLayout() {
            anchos.clear();
            todas.clear();
            calcularAnchos(RAIZ);

            Dimension arbol = medir(RAIZ);
            medida = new Dimension(arbol.width + MARGEN * 2, arbol.height + MARGEN * 2);

            int desX = Math.max(MARGEN, (getWidth() - arbol.width) / 2);
            int desY = Math.max(MARGEN, (getHeight() - arbol.height) / 2);
            raiz = ubicar(RAIZ, desX, desY);

            for (Caja caja : todas) caja.control().setBounds(caja.marco());
        }

        private Dimension medir(Nodo n) {
            int propio = anchos.get(n);
            if (n.esHoja()) return new Dimension(propio, ALTO_CAJA);

            if (n.disposicion() == Disposicion.APILADA) {
                int ancho = 0, alto = 0;
                for (Nodo h : n.hijos()) {
                    Dimension d = medir(h);
                    ancho = Math.max(ancho, d.width);
                    alto += d.height;
                }
                alto += SEP_HIJO * (n.hijos().size() - 1);
                return new Dimension(Math.max(propio, SANGRIA + ancho), ALTO_CAJA + SEP_NIVEL + alto);
            }
            int ancho = 0, alto = 0;
            for (Nodo h : n.hijos()) {
                Dimension d = medir(h);
                ancho += d.width;
                alto = Math.max(alto, d.height);
            }
            ancho += SEP_RAMA * (n.hijos().size() - 1);
            return new Dimension(Math.max(propio, ancho), ALTO_CAJA + SEP_NIVEL + alto);
        }

        private Caja ubicar(Nodo n, int x, int y) {
            Dimension propio = medir(n);
            int ancho = anchos.get(n);
            List<Caja> hijos = new ArrayList<>();
            Rectangle marco;

            if (n.esHoja()) {
                marco = new Rectangle(x, y, ancho, ALTO_CAJA);
            } else if (n.disposicion() == Disposicion.APILADA) {
                marco = new Rectangle(x, y, ancho, ALTO_CAJA);
                int yHijo = y + ALTO_CAJA + SEP_NIVEL;
                for (Nodo h : n.hijos()) {
                    hijos.add(ubicar(h, x + SANGRIA, yHijo));
                    yHijo += medir(h).height + SEP_HIJO;
                }
            } else {
                marco = new Rectangle(x + (propio.width - ancho) / 2, y, ancho, ALTO_CAJA);
                int xHijo = x;
                int yHijo = y + ALTO_CAJA + SEP_NIVEL;
                for (Nodo h : n.hijos()) {
                    hijos.add(ubicar(h, xHijo, yHijo));
                    xHijo += medir(h).width + SEP_RAMA;
                }
            }
            Caja caja = new Caja(n, controles.get(n), marco, hijos);
            todas.add(caja);
            return caja;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (raiz == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.DIAG_LINEA);
            enlaces(g2, raiz);
            g2.dispose();
        }

        private void enlaces(Graphics2D g2, Caja caja) {
            if (caja.hijos().isEmpty()) return;
            Rectangle padre = caja.marco();

            if (caja.nodo().disposicion() == Disposicion.APILADA) {
                int espina = padre.x + SANGRIA / 2;
                Rectangle ultima = caja.hijos().get(caja.hijos().size() - 1).marco();
                g2.drawLine(espina, padre.y + padre.height, espina, ultima.y + ultima.height / 2);
                for (Caja h : caja.hijos()) {
                    Rectangle m = h.marco();
                    int medio = m.y + m.height / 2;
                    g2.drawLine(espina, medio, m.x - FLECHA, medio);
                    flecha(g2, m.x, medio, 0);
                }
            } else {
                int centro = padre.x + padre.width / 2;
                int abajo = padre.y + padre.height;
                int bus = abajo + SEP_NIVEL / 2;

                g2.drawLine(centro, abajo, centro, bus);
                int min = centro, max = centro;
                for (Caja h : caja.hijos()) {
                    int c = h.marco().x + h.marco().width / 2;
                    min = Math.min(min, c);
                    max = Math.max(max, c);
                    g2.drawLine(c, bus, c, h.marco().y - FLECHA);
                    flecha(g2, c, h.marco().y, 90);
                }
                g2.drawLine(min, bus, max, bus);
            }
            caja.hijos().forEach(h -> enlaces(g2, h));
        }

        /** Punta de flecha apuntando a (x, y); el angulo 0 mira a la derecha. */
        private static void flecha(Graphics2D g2, int x, int y, int grados) {
            Path2D.Double p = new Path2D.Double();
            p.moveTo(0, 0);
            p.lineTo(-FLECHA - 1, -FLECHA + 1);
            p.lineTo(-FLECHA - 1, FLECHA - 1);
            p.closePath();

            Graphics2D t = (Graphics2D) g2.create();
            t.translate(x, y);
            t.rotate(Math.toRadians(grados));
            t.fill(p);
            t.dispose();
        }
    }

    /* ------------------------------------------------------------------ */

    private static Color fondo(Estilo e) {
        return switch (e) {
            case MODULO -> Tema.DIAG_MODULO_FONDO;
            case SUBMODULO -> Tema.DIAG_SUBMODULO_FONDO;
            case PANTALLA -> Tema.DIAG_PANTALLA_FONDO;
        };
    }

    private static Color borde(Estilo e) {
        return switch (e) {
            case MODULO -> Tema.DIAG_MODULO_BORDE;
            case SUBMODULO -> Tema.DIAG_SUBMODULO_BORDE;
            case PANTALLA -> Tema.DIAG_PANTALLA_BORDE;
        };
    }

    /** Pinta la caja del diagrama con el color de su nivel. */
    private static void pintar(Graphics g, JComponent c, Estilo estilo, boolean resaltada) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth(), h = c.getHeight();
        int arco = estilo == Estilo.PANTALLA ? 10 : 0;

        g2.setColor(resaltada ? fondo(estilo).darker() : fondo(estilo));
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arco, arco));
        g2.setColor(borde(estilo));
        g2.setStroke(new BasicStroke(resaltada ? 2f : 1f));
        g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arco, arco));
        g2.dispose();
    }

    /** Caja sin pantalla: solo forma parte del diagrama. */
    private static class CajaFija extends JLabel {
        private final Estilo estilo;

        CajaFija(Nodo n) {
            super(n.texto(), SwingConstants.CENTER);
            this.estilo = n.estilo();
            setFont(Tema.fuente(11, estilo == Estilo.PANTALLA ? Font.PLAIN : Font.BOLD));
            setForeground(Tema.TEXTO);
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            pintar(g, this, estilo, false);
            super.paintComponent(g);
        }
    }

    /** Caja con pantalla: se pulsa para entrar. */
    private static class CajaBoton extends JButton {
        private final Estilo estilo;
        private boolean encima;

        CajaBoton(Nodo n) {
            super(n.texto());
            this.estilo = n.estilo();
            setFont(Tema.fuente(11, estilo == Estilo.PANTALLA ? Font.PLAIN : Font.BOLD));
            setForeground(Tema.TEXTO);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setMargin(new Insets(0, 4, 0, 4));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Entrar a " + n.texto());

            addActionListener(e -> Navegacion.ir(n.clave()));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { encima = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            pintar(g, this, estilo, encima || hasFocus());
            super.paintComponent(g);
        }
    }
}
