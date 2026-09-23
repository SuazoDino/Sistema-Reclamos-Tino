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
 * Mapa conceptual del sistema. Es la pantalla de entrada y muestra el arbol
 * completo del diseno arquitectonico del entregable: modulo, submodulo y
 * cada uno de los catalogos. Las hojas del arbol son botones.
 */
public class MapaPantalla extends JPanel {

    /**
     * Nodo del mapa. {@code clave} nula significa que el nodo solo agrupa:
     * es un rotulo del diagrama, no se entra a el.
     */
    private record Nodo(String clave, String texto, int nivel) {}

    /** El arbol, tal como lo lista el diseno arquitectonico del entregable. */
    private static final List<Nodo> NODOS = List.of(
            new Nodo(null, "SISTEMA DE RECLAMOS", 0),
            new Nodo(null, "MODULO ONLINE", 1),
            new Nodo(null, "GERENCIAL", 2),
            new Nodo(null, "MANTENIMIENTO DE PARAMETROS", 3),
            new Nodo("param-generales", "Parametros Generales", 4),
            new Nodo("cat-reclamos", "Catalogo de Reclamos y Eventos", 4),
            new Nodo("cat-productos", "Catalogo de Productos", 4),
            new Nodo("cat-problemas", "Catalogo de Problemas", 4),
            new Nodo("cat-clientes", "Catalogo de Clientes", 4),
            new Nodo("cat-protocolos", "Catalogo de Protocolos", 4),
            new Nodo("cat-reglas", "Catalogo de Reglas", 4),
            new Nodo("cat-politicas", "Catalogo de Politicas", 4),
            new Nodo(null, "CONSULTA", 3),
            new Nodo("indicadores", "Indicadores", 4),
            new Nodo(null, "OPERATIVO", 2),
            new Nodo(null, "AREA", 3),
            new Nodo("area-dataentry", "Data Entry", 4),
            new Nodo("area-reportes", "Reportes", 4),
            new Nodo(null, "CLIENTE", 3),
            new Nodo("cliente-dataentry", "Data Entry", 4),
            new Nodo("cliente-reportes", "Reportes", 4)
    );

    public MapaPantalla() {
        super(new BorderLayout(0, Tema.ESP_MD));
        setOpaque(false);
        setBorder(Ui.relleno(Tema.ESP_LG));

        JPanel titulo = Ui.panel(new BorderLayout(0, 2));
        titulo.add(Ui.titulo("Diseno arquitectonico"), BorderLayout.NORTH);
        titulo.add(Ui.suave("Seleccione el catalogo o la opcion a la que desea entrar."),
                BorderLayout.CENTER);

        add(titulo, BorderLayout.NORTH);
        add(new Diagrama(), BorderLayout.CENTER);
    }

    /* ------------------------------------------------------------------ */

    /**
     * Dibuja el arbol: una caja por nodo, escalonada segun su nivel, y las
     * lineas en angulo recto que unen cada nodo con su padre. La sangria es
     * la que hace legible la jerarquia, como en un indice.
     */
    private static class Diagrama extends JPanel {

        private static final int ALTO_MIN = 20;
        private static final int ALTO_MAX = 30;
        private static final int SANGRIA = 44;
        private static final int MARGEN = 16;

        private final List<JComponent> cajas = new ArrayList<>();
        private int altoFila = ALTO_MAX;
        private int anchoCaja = 200;

        Diagrama() {
            super(null);
            setBackground(Tema.SUPERFICIE);
            setBorder(BorderFactory.createLineBorder(Tema.BORDE));

            for (Nodo n : NODOS) {
                JComponent caja = n.clave() == null ? new CajaGrupo(n) : new CajaOpcion(n);
                cajas.add(caja);
                add(caja);
            }
        }

        /**
         * El mapa tiene que verse entero: la altura de fila se reparte segun el
         * espacio disponible, en vez de fijarla y sacar una barra de scroll.
         */
        @Override public void doLayout() {
            int disponible = getHeight() - MARGEN * 2;
            altoFila = Math.max(ALTO_MIN, Math.min(ALTO_MAX, disponible / NODOS.size()));

            anchoCaja = 0;
            for (JComponent c : cajas) anchoCaja = Math.max(anchoCaja, c.getPreferredSize().width);
            anchoCaja += 24;

            int alto = Math.max(18, altoFila - 4);
            for (int i = 0; i < NODOS.size(); i++) {
                cajas.get(i).setBounds(x(NODOS.get(i)), y(i), anchoCaja, alto);
            }
        }

        private static int x(Nodo n) { return MARGEN + n.nivel() * SANGRIA; }

        private int y(int indice) { return MARGEN + indice * altoFila; }

        private int medio(int indice) { return y(indice) + Math.max(18, altoFila - 4) / 2; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Tema.BORDE);

            // cada nodo se une a su padre: baja por la espina y entra de lado
            for (int i = 1; i < NODOS.size(); i++) {
                Nodo hijo = NODOS.get(i);
                int padre = -1;
                for (int j = i - 1; j >= 0; j--) {
                    if (NODOS.get(j).nivel() == hijo.nivel() - 1) { padre = j; break; }
                }
                if (padre < 0) continue;

                int espina = x(NODOS.get(padre)) + SANGRIA / 2;
                g2.drawLine(espina, medio(padre), espina, medio(i));
                g2.drawLine(espina, medio(i), x(hijo), medio(i));
            }
            g2.dispose();
        }
    }

    /* ------------------------------------------------------------------ */

    /** Nodo que solo agrupa: se ve como rotulo del diagrama y no navega. */
    private static class CajaGrupo extends JLabel {
        CajaGrupo(Nodo n) {
            super(n.texto());
            setFont(Tema.fuerte());
            setForeground(Tema.TEXTO);
            setOpaque(true);
            setBackground(Tema.CABECERA);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.BORDE),
                    BorderFactory.createEmptyBorder(0, Tema.ESP_SM, 0, Tema.ESP_SM)));
        }
    }

    /** Hoja del arbol: boton que entra al catalogo o a la opcion. */
    private static class CajaOpcion extends JButton {
        CajaOpcion(Nodo n) {
            super(n.texto());
            setFont(Tema.cuerpo());
            setHorizontalAlignment(SwingConstants.LEFT);
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
