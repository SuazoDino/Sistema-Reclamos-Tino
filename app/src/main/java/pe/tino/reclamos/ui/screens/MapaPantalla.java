package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapa conceptual del sistema: el diagrama de modulos tipicos del diseno
 * arquitectonico, con la raiz arriba y las ramas abriendose hacia abajo.
 *
 * Estan todos los modulos de la arquitectura. Los que desarrolla el 1er
 * entregable son botones y llevan a su pantalla; los demas se dibujan con
 * borde punteado, porque forman parte del diseno pero todavia no del
 * prototipo.
 */
public class MapaPantalla extends JPanel {

    private enum Tipo { GRUPO, OPCION, PENDIENTE }

    /** Nodo del arbol. */
    private record Nodo(Tipo tipo, String clave, String texto, List<Nodo> hijos) {

        static Nodo grupo(String texto, Nodo... hijos) {
            return new Nodo(Tipo.GRUPO, null, texto, List.of(hijos));
        }

        static Nodo opcion(String clave, String texto) {
            return new Nodo(Tipo.OPCION, clave, texto, List.of());
        }

        /** Modulo de la arquitectura que el entregable todavia no desarrolla. */
        static Nodo pendiente(String texto) {
            return new Nodo(Tipo.PENDIENTE, null, texto, List.of());
        }

        boolean esHoja() { return hijos.isEmpty(); }

        /** Un nodo peine solo tiene hojas: se apilan debajo de el. */
        boolean esPeine() {
            return !hijos.isEmpty() && hijos.stream().allMatch(Nodo::esHoja);
        }
    }

    /** Los modulos tipicos del diseno arquitectonico. */
    private static final Nodo RAIZ = Nodo.grupo("SISTEMA DE RECLAMOS",
            Nodo.pendiente("SEGURIDAD"),
            Nodo.grupo("APLICATIVO",
                    Nodo.grupo("ONLINE",
                            Nodo.grupo("GERENCIAL",
                                    Nodo.grupo("MANT-PARAM",
                                            Nodo.opcion("param-generales", "Parametros Generales"),
                                            Nodo.opcion("cat-reclamos", "Reclamos y Eventos"),
                                            Nodo.opcion("cat-productos", "Productos"),
                                            Nodo.opcion("cat-problemas", "Problemas"),
                                            Nodo.opcion("cat-clientes", "Clientes"),
                                            Nodo.opcion("cat-protocolos", "Protocolos"),
                                            Nodo.opcion("cat-reglas", "Reglas"),
                                            Nodo.opcion("cat-politicas", "Politicas")),
                                    Nodo.grupo("CONSULTA",
                                            Nodo.opcion("indicadores", "Indicadores"))),
                            Nodo.grupo("OPERATIVO",
                                    Nodo.grupo("AREA",
                                            Nodo.opcion("area-dataentry", "Data Entry"),
                                            Nodo.opcion("area-reportes", "Reportes")),
                                    Nodo.grupo("CLIENTE",
                                            Nodo.opcion("cliente-dataentry", "Data Entry"),
                                            Nodo.opcion("cliente-reportes", "Reportes")))),
                    Nodo.pendiente("BATCH")),
            Nodo.grupo("TECNICO",
                    Nodo.pendiente("ACT-BD"),
                    Nodo.pendiente("MANT-BD"),
                    Nodo.pendiente("ESTADISTICAS"),
                    Nodo.pendiente("CONTINGENCIA")));

    public MapaPantalla() {
        super(new BorderLayout(0, Tema.ESP_MD));
        setOpaque(false);
        setBorder(Ui.relleno(Tema.ESP_LG));

        JPanel titulo = Ui.panel(new BorderLayout(0, 2));
        titulo.add(Ui.titulo("Diseno arquitectonico"), BorderLayout.NORTH);
        titulo.add(Ui.suave("Modulos tipicos del sistema. Seleccione la opcion a la que desea entrar."),
                BorderLayout.CENTER);

        JPanel leyenda = Ui.panel(new BorderLayout());
        leyenda.add(Ui.suave("Los modulos con borde punteado forman parte de la arquitectura "
                + "pero no del 1er entregable."), BorderLayout.WEST);

        add(titulo, BorderLayout.NORTH);
        add(Ui.scroll(new Diagrama()), BorderLayout.CENTER);
        add(leyenda, BorderLayout.SOUTH);
    }

    /* ------------------------------------------------------------------ */

    /**
     * Organigrama calculado a partir del arbol. Una rama reparte a sus hijos
     * en horizontal y se centra sobre ellos; un nodo peine los cuelga en
     * vertical con una espina a la izquierda. Cada caja mide lo que mide su
     * rotulo, que es lo que permite que el diagrama entero entre a lo ancho.
     */
    private static class Diagrama extends JPanel {

        private static final int ALTO_CAJA = 26;
        private static final int SEP_NIVEL = 26;
        private static final int SEP_RAMA  = 14;
        private static final int SEP_HOJA  = 5;
        private static final int SANGRIA   = 20;
        private static final int MARGEN    = 16;
        private static final int HOLGURA   = 20;   // aire a los lados del rotulo

        private record Caja(Nodo nodo, JComponent control, Rectangle marco, List<Caja> hijos) {}

        private final Map<Nodo, JComponent> controles = new IdentityHashMap<>();
        private final Map<Nodo, Integer> anchos = new IdentityHashMap<>();
        private final List<Caja> todas = new ArrayList<>();
        private Caja raiz;
        private Dimension medida = new Dimension(900, 520);

        Diagrama() {
            super(null);
            setBackground(Tema.SUPERFICIE);
            construir(RAIZ);
        }

        private void construir(Nodo n) {
            JComponent c = switch (n.tipo()) {
                case OPCION -> new CajaOpcion(n);
                case PENDIENTE -> new CajaPendiente(n);
                case GRUPO -> new CajaGrupo(n);
            };
            controles.put(n, c);
            add(c);
            n.hijos().forEach(this::construir);
        }

        @Override public Dimension getPreferredSize() { return medida; }

        /**
         * Ancho de la caja de un nodo. Las hojas de un mismo peine comparten
         * el ancho de la mas larga, para que la pila quede pareja.
         */
        private void calcularAnchos(Nodo n) {
            anchos.put(n, controles.get(n).getPreferredSize().width + HOLGURA);
            n.hijos().forEach(this::calcularAnchos);

            if (n.esPeine()) {
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

        /** Tamanio que ocupa el subarbol de un nodo. */
        private Dimension medir(Nodo n) {
            int propio = anchos.get(n);
            if (n.esHoja()) return new Dimension(propio, ALTO_CAJA);

            if (n.esPeine()) {
                int hijos = n.hijos().size();
                return new Dimension(Math.max(propio, SANGRIA + anchos.get(n.hijos().get(0))),
                        ALTO_CAJA + SEP_NIVEL + hijos * ALTO_CAJA + (hijos - 1) * SEP_HOJA);
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

        /** Ubica el subarbol dentro del rectangulo que empieza en (x, y). */
        private Caja ubicar(Nodo n, int x, int y) {
            Dimension propio = medir(n);
            int ancho = anchos.get(n);
            List<Caja> hijos = new ArrayList<>();
            Rectangle marco;

            if (n.esPeine()) {
                marco = new Rectangle(x, y, ancho, ALTO_CAJA);
                int yHija = y + ALTO_CAJA + SEP_NIVEL;
                for (Nodo h : n.hijos()) {
                    hijos.add(ubicar(h, x + SANGRIA, yHija));
                    yHija += ALTO_CAJA + SEP_HOJA;
                }
            } else if (n.esHoja()) {
                marco = new Rectangle(x, y, ancho, ALTO_CAJA);
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
            g2.setColor(Tema.BORDE);
            enlaces(g2, raiz);
            g2.dispose();
        }

        private void enlaces(Graphics2D g2, Caja caja) {
            if (caja.hijos().isEmpty()) return;
            Rectangle padre = caja.marco();

            if (caja.nodo().esPeine()) {
                int espina = padre.x + SANGRIA / 2;
                Rectangle ultima = caja.hijos().get(caja.hijos().size() - 1).marco();
                g2.drawLine(espina, padre.y + padre.height, espina, ultima.y + ultima.height / 2);
                for (Caja h : caja.hijos()) {
                    Rectangle m = h.marco();
                    g2.drawLine(espina, m.y + m.height / 2, m.x, m.y + m.height / 2);
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
                    g2.drawLine(c, bus, c, h.marco().y);
                }
                g2.drawLine(min, bus, max, bus);
            }
            caja.hijos().forEach(h -> enlaces(g2, h));
        }
    }

    /* ------------------------------------------------------------------ */

    /** Nodo que agrupa: rotulo del diagrama. */
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

    /** Modulo de la arquitectura que este entregable no desarrolla. */
    private static class CajaPendiente extends JLabel {
        CajaPendiente(Nodo n) {
            super(n.texto(), SwingConstants.CENTER);
            setFont(Tema.fuerte());
            setForeground(Tema.TEXTO_SUAVE);
            setOpaque(false);
            setToolTipText("Modulo del diseno arquitectonico; no forma parte del 1er entregable.");
            setBorder(BorderFactory.createDashedBorder(Tema.BORDE, 1f, 3f, 3f, false));
        }
    }

    /** Hoja desarrollada: boton que entra a su pantalla. */
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
