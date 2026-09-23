package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel con borde y titulo, el agrupador de toda la vida en una aplicacion de
 * escritorio. Sustituye a cualquier otra forma de separar bloques.
 */
public class Grupo extends JPanel {

    public Grupo(String titulo) {
        this(titulo, new BorderLayout(Tema.ESP_SM, Tema.ESP_SM));
    }

    public Grupo(String titulo, LayoutManager layout) {
        super(layout);
        setOpaque(false);
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.BORDE), " " + titulo + " ");
        borde.setTitleFont(Tema.fuerte());
        borde.setTitleColor(Tema.TEXTO);
        setBorder(BorderFactory.createCompoundBorder(borde,
                BorderFactory.createEmptyBorder(Tema.ESP_SM, Tema.ESP_SM, Tema.ESP_SM, Tema.ESP_SM)));
    }

    /** Grupo sin relleno interno, para que una tabla llegue hasta el borde. */
    public static Grupo ajustado(String titulo) {
        Grupo g = new Grupo(titulo);
        TitledBorder borde = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.BORDE), " " + titulo + " ");
        borde.setTitleFont(Tema.fuerte());
        borde.setTitleColor(Tema.TEXTO);
        g.setBorder(borde);
        return g;
    }
}
