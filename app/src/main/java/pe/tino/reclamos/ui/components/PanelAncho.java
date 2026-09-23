package pe.tino.reclamos.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Contenedor que dentro de un JScrollPane adopta el ancho del viewport en vez
 * de imponer el suyo. Sin esto, un formulario mas ancho que el panel se recorta
 * por la derecha cuando la barra horizontal esta desactivada.
 */
public class PanelAncho extends JPanel implements Scrollable {

    public PanelAncho(Component contenido) {
        super(new BorderLayout());
        setOpaque(false);
        add(contenido, BorderLayout.CENTER);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public boolean getScrollableTracksViewportWidth()     { return true; }
    @Override public boolean getScrollableTracksViewportHeight()    { return false; }

    @Override public int getScrollableUnitIncrement(Rectangle v, int orientacion, int direccion) { return 16; }

    @Override public int getScrollableBlockIncrement(Rectangle v, int orientacion, int direccion) {
        return orientacion == SwingConstants.VERTICAL ? v.height : v.width;
    }
}
