package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Armazon comun de las pantallas: cabecera con titulo, ruta del modulo y
 * acciones, mas el area de contenido. Mantiene identica la altura y el
 * margen de todas las vistas.
 */
public abstract class Pantalla extends JPanel {

    private final JPanel acciones = Ui.panel(new FlowLayout(FlowLayout.RIGHT, Tema.ESP_SM, 0));
    private final JPanel contenido = Ui.panel(new BorderLayout(Tema.ESP_MD, Tema.ESP_MD));

    protected Pantalla(String ruta, String titulo, String descripcion) {
        super(new BorderLayout(0, Tema.ESP_MD));
        setOpaque(false);
        setBorder(Ui.relleno(Tema.ESP_LG, Tema.ESP_LG, Tema.ESP_LG, Tema.ESP_LG));

        JPanel textos = Ui.panel(new GridLayout(0, 1, 0, 3));
        JLabel migaja = new JLabel(ruta.toUpperCase());
        migaja.setFont(Tema.fuente(11, Font.BOLD));
        migaja.setForeground(Tema.acento());
        textos.add(migaja);
        textos.add(Ui.titulo(titulo));
        textos.add(Ui.suave(descripcion));

        JPanel cabecera = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        cabecera.add(textos, BorderLayout.CENTER);
        cabecera.add(acciones, BorderLayout.EAST);

        add(cabecera, BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
    }

    /** Controles de la esquina superior derecha de la pantalla. */
    protected void acciones(Component... componentes) {
        for (Component c : componentes) acciones.add(c);
    }

    protected JPanel contenido() { return contenido; }

    /** Aviso breve y no bloqueante, para confirmar el efecto de una accion. */
    protected void avisar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Sistema de Reclamos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(this, mensaje, "Confirmar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
