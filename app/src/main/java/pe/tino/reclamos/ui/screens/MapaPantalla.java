package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapa conceptual del sistema: el arbol del diseno arquitectonico dibujado
 * como diagrama, con la raiz arriba y las ramas abriendose hacia abajo.
 *
 * Es la pantalla de entrada; cada hoja del arbol es un boton que lleva a su
 * catalogo o a su opcion.
 */
public class MapaPantalla extends JPanel {

    /** Nodo del arbol. Sin clave es un rotulo que solo agrupa. */
    private record Nodo(String clave, String texto, List<Nodo> hijos) {

        static Nodo grupo(String texto, Nodo... hijos) {
            return new Nodo(null, texto, List.of(hijos));
        }

        static Nodo hoja(String clave, String texto) {
            return new Nodo(clave, texto, List.of());
        }

        boolean esHoja() { return hijos.isEmpty(); }

        /** Un nodo peine es el que solo tiene hojas: sus hijos se apilan. */
        boolean esPeine() {
            return !hijos.isEmpty() && hijos.stream().allMatch(Nodo::esHoja);
        }
    }

    /** El arbol, tal como lo describe el diseno arquitectonico del entregable. */
    private static final Nodo RAIZ = Nodo.grupo("SISTEMA DE RECLAMOS",
            Nodo.grupo("MODULO ONLINE",
                    Nodo.grupo("GERENCIAL",
                            Nodo.grupo("MANTENIMIENTO DE PARAMETROS",
                                    Nodo.hoja("param-generales", "Parametros Generales"),
                                    Nodo.hoja("cat-reclamos", "Cat. de Reclamos y Eventos"),
                                    Nodo.hoja("cat-productos", "Cat. de Productos"),
                                    Nodo.hoja("cat-problemas", "Cat. de Problemas"),
                                    Nodo.hoja("cat-clientes", "Cat. de Clientes"),
                                    Nodo.hoja("cat-protocolos", "Cat. de Protocolos"),
                                    Nodo.hoja("cat-reglas", "Cat. de Reglas"),
                                    Nodo.hoja("cat-politicas", "Cat. de Politicas")),
                            Nodo.grupo("CONSULTA",
                                    Nodo.hoja("indicadores", "Indicadores"))),
                    Nodo.grupo("OPERATIVO",
                            Nodo.grupo("AREA",
                                    Nodo.hoja("area-dataentry", "Data Entry"),
                                    Nodo.hoja("area-reportes", "Reportes")),
                            Nodo.grupo("CLIENTE",
                                    Nodo.hoja("cliente-dataentry", "Data Entry"),
                                    Nodo.hoja("cliente-reportes", "Reportes")))));

    public MapaPantalla() {
        super(new BorderLayout(0, Tema.ESP_MD));
        setOpaque(false);
        setBorder(Ui.relleno(Tema.ESP_LG));

        JPanel titulo = Ui.panel(new BorderLayout(0, 2));
        titulo.add(Ui.titulo("Diseno arquitectonico"), BorderLayout.NORTH);
        titulo.add(Ui.suave("Seleccione el catalogo o la opcion a la que desea entrar."),
                BorderLayout.CENTER);

        add(titulo, BorderLayout.NORTH);
        add(Ui.scroll(new Diagrama()), BorderLayout.CENTER);
    }

    /* ------------------------------------------------------------------ */

    /**
     * Dibuja el arbol como organigrama. Un nodo con ramas reparte a sus hijos
     * en horizontal y se centra sobre ellos; un nodo peine cuelga sus hojas en
     * vertical, con una espina a la izquierda. Los dos casos juntos dan el
     * ancho que el diagrama necesita sin desbordarse.
     */
    private static class Diagrama extends JPanel {

        private static final int ALTO_CAJA = 28;
        private static final int SEP_NIVEL = 30;   // separacion vertical entre niveles
        private static final int SEP_RAMA  = 18;   // separacion horizontal entre ramas
        private static final int SEP_HOJA  = 6;    // separacion entre hojas apiladas
        private static final int SANGRIA   = 24;   // desplazamiento de las hojas del peine
        private static final int MARGEN    = 20;

        /** Una caja ya ubicada: el nodo, su control y el rectangulo que ocupa. */
        private record Caja(Nodo nodo, JComponent control, Rectangle marco, List<Caja> hijos) {}

        private final List<Caja> todas = new ArrayList<>();
        private Caja raiz;
        private int anchoCaja;
        private Dimension medida = new Dimension(900, 520);

        Diagrama() {
            super(null);
            setBackground(Tema.SUPERFICIE);
            construir(RAIZ);
        }

        /** Crea los controles y los agrega al panel, en profundidad. */
        private JComponent construir(Nodo n) {
            JComponent c = n.esHoja() ? new CajaOpcion(n) : new CajaGrupo(n);
            add(c);
            n.hijos().forEach(this::construir);
            return c;
        }

        @Override public Dimension getPreferredSize() {
            if (anchoCaja == 0) calcularAnchoCaja();
            return medida;
        }

        /** Todas las cajas miden igual: el ancho del rotulo mas largo. */
        private void calcularAnchoCaja() {
            anchoCaja = 0;
            for (Component c : getComponents()) {
                anchoCaja = Math.max(anchoCaja, c.getPreferredSize().width);
            }
            anchoCaja += 22;
        }

        @Override public void doLayout() {
            calcularAnchoCaja();
            todas.clear();

            Dimension arbol = medir(RAIZ);
            medida = new Dimension(arbol.width + MARGEN * 2, arbol.height + MARGEN * 2);

            // si sobra espacio el diagrama queda centrado, no pegado a un borde
            int desX = Math.max(MARGEN, (getWidth() - arbol.width) / 2);
            int desY = Math.max(MARGEN, (getHeight() - arbol.height) / 2);
            raiz = ubicar(RAIZ, desX, desY, indiceControl(RAIZ));

            for (Caja caja : todas) caja.control().setBounds(caja.marco());
        }

        /** Tamanio que ocupa el subarbol de un nodo. */
        private Dimension medir(Nodo n) {
            if (n.esHoja()) return new Dimension(anchoCaja, ALTO_CAJA);

            if (n.esPeine()) {
                int hijos = n.hijos().size();
                return new Dimension(SANGRIA + anchoCaja,
                        ALTO_CAJA + SEP_NIVEL + hijos * ALTO_CAJA + (hijos - 1) * SEP_HOJA);
            }
            int ancho = 0, alto = 0;
            for (Nodo h : n.hijos()) {
                Dimension d = medir(h);
                ancho += d.width;
                alto = Math.max(alto, d.height);
            }
            ancho += SEP_RAMA * (n.hijos().size() - 1);
            return new Dimension(Math.max(anchoCaja, ancho), ALTO_CAJA + SEP_NIVEL + alto);
        }

        /** Ubica el subarbol dentro del rectangulo que empieza en (x, y). */
        private Caja ubicar(Nodo n, int x, int y, JComponent control) {
            Dimension propio = medir(n);
            List<Caja> hijos = new ArrayList<>();

            Rectangle marco;
            if (n.esPeine()) {
                // el padre a la izquierda y las hojas colgando, indentadas
                marco = new Rectangle(x, y, anchoCaja, ALTO_CAJA);
                int yHija = y + ALTO_CAJA + SEP_NIVEL;
                for (Nodo h : n.hijos()) {
                    hijos.add(ubicar(h, x + SANGRIA, yHija, indiceControl(h)));
                    yHija += ALTO_CAJA + SEP_HOJA;
                }
            } else if (n.esHoja()) {
                marco = new Rectangle(x, y, anchoCaja, ALTO_CAJA);
            } else {
                // el padre centrado sobre la fila de sus ramas
                marco = new Rectangle(x + (propio.width - anchoCaja) / 2, y, anchoCaja, ALTO_CAJA);
                int xHijo = x;
                int yHijo = y + ALTO_CAJA + SEP_NIVEL;
                for (Nodo h : n.hijos()) {
                    hijos.add(ubicar(h, xHijo, yHijo, indiceControl(h)));
                    xHijo += medir(h).width + SEP_RAMA;
                }
            }
            Caja caja = new Caja(n, control, marco, hijos);
            todas.add(caja);
            return caja;
        }

        /** Los controles se crearon en el mismo orden en que se recorre el arbol. */
        private JComponent indiceControl(Nodo n) {
            int i = orden(RAIZ, n, new int[]{0});
            return (JComponent) getComponent(i);
        }

        private static int orden(Nodo actual, Nodo buscado, int[] contador) {
            if (actual == buscado) return contador[0];
            contador[0]++;
            for (Nodo h : actual.hijos()) {
                int r = orden(h, buscado, contador);
                if (r >= 0) return r;
            }
            return -1;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (raiz == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Tema.BORDE);
            dibujarEnlaces(g2, raiz);
            g2.dispose();
        }

        private void dibujarEnlaces(Graphics2D g2, Caja caja) {
            if (caja.hijos().isEmpty()) return;
            Rectangle padre = caja.marco();

            if (caja.nodo().esPeine()) {
                // espina vertical a la izquierda y una derivacion por hoja
                int espina = padre.x + SANGRIA / 2;
                Rectangle ultima = caja.hijos().get(caja.hijos().size() - 1).marco();
                g2.drawLine(espina, padre.y + padre.height, espina, ultima.y + ultima.height / 2);
                for (Caja h : caja.hijos()) {
                    Rectangle m = h.marco();
                    g2.drawLine(espina, m.y + m.height / 2, m.x, m.y + m.height / 2);
                }
            } else {
                // bus horizontal a media altura entre el padre y la fila de hijos
                int centroPadre = padre.x + padre.width / 2;
                int abajo = padre.y + padre.height;
                int bus = abajo + SEP_NIVEL / 2;

                g2.drawLine(centroPadre, abajo, centroPadre, bus);
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                for (Caja h : caja.hijos()) {
                    int c = h.marco().x + h.marco().width / 2;
                    min = Math.min(min, c);
                    max = Math.max(max, c);
                    g2.drawLine(c, bus, c, h.marco().y);
                }
                g2.drawLine(Math.min(min, centroPadre), bus, Math.max(max, centroPadre), bus);
            }
            caja.hijos().forEach(h -> dibujarEnlaces(g2, h));
        }
    }

    /* ------------------------------------------------------------------ */

    /** Nodo que agrupa: rotulo del diagrama, no navega. */
    private static class CajaGrupo extends JLabel {
        CajaGrupo(Nodo n) {
            super(n.texto(), SwingConstants.CENTER);
            setFont(Tema.fuerte());
            setForeground(Tema.TEXTO);
            setOpaque(true);
            setBackground(Tema.CABECERA);
            setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        }
    }

    /** Hoja del arbol: boton que entra al catalogo o a la opcion. */
    private static class CajaOpcion extends JButton {
        CajaOpcion(Nodo n) {
            super(n.texto());
            setFont(Tema.cuerpo());
            setMargin(new Insets(0, Tema.ESP_SM, 0, Tema.ESP_SM));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> Navegacion.ir(n.clave()));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(Tema.SELECCION); }
                @Override public void mouseExited(MouseEvent e)  { setBackground(null); }
            });
        }
    }
}
