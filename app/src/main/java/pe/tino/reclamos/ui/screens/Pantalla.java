package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.VentanaPrincipal;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Armazon comun: una barra con Volver y Mapa, el titulo de la pantalla y
 * debajo el contenido. Todas las pantallas se ven igual de arriba.
 */
public abstract class Pantalla extends JPanel {

    private final JPanel contenido = Ui.panel(new BorderLayout(Tema.ESP_MD, Tema.ESP_MD));

    protected Pantalla(String titulo, String descripcion) {
        super(new BorderLayout(0, Tema.ESP_MD));
        setOpaque(false);
        setBorder(Ui.relleno(Tema.ESP_MD, Tema.ESP_LG, Tema.ESP_LG, Tema.ESP_LG));

        JPanel cabecera = Ui.panel(new BorderLayout(0, Tema.ESP_XS));
        cabecera.add(barraNavegacion(), BorderLayout.NORTH);

        JPanel textos = Ui.panel(new BorderLayout(0, 2));
        textos.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        textos.add(Ui.titulo(titulo), BorderLayout.NORTH);
        textos.add(Ui.suave(descripcion), BorderLayout.CENTER);
        cabecera.add(textos, BorderLayout.CENTER);

        add(cabecera, BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
    }

    private JComponent barraNavegacion() {
        JButton volver = Ui.boton("< Volver");
        volver.addActionListener(e -> Navegacion.volver());
        JButton mapa = Ui.boton("Mapa");
        mapa.addActionListener(e -> Navegacion.inicio());

        JPanel p = Ui.panel(new BorderLayout());
        p.add(Ui.fila(volver, mapa), BorderLayout.WEST);
        p.add(controlesDeTexto(), BorderLayout.EAST);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_FINO),
                Ui.relleno(0, 0, Tema.ESP_SM, 0)));
        return p;
    }

    /**
     * Tamanio del texto de toda la interfaz. Sirve para proyectar: en una
     * sala, el tamanio de monitor no se lee desde atras.
     */
    private JComponent controlesDeTexto() {
        JButton menos = Ui.boton("A\u2212");
        menos.setToolTipText("Achicar el texto de toda la interfaz");
        menos.addActionListener(e -> VentanaPrincipal.reescalarTexto(this, -1));

        JButton mas = Ui.boton("A+");
        mas.setToolTipText("Agrandar el texto de toda la interfaz");
        mas.addActionListener(e -> VentanaPrincipal.reescalarTexto(this, 1));

        JLabel nivel = Ui.suave((int) Math.round(Tema.escalaTexto() * 100) + " %");
        return Ui.fila(Ui.suave("Texto:"), menos, mas, nivel);
    }

    protected JPanel contenido() { return contenido; }

    protected void avisar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Sistema de Reclamos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    protected void advertir(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Advertencia",
                JOptionPane.WARNING_MESSAGE);
    }

    protected boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
