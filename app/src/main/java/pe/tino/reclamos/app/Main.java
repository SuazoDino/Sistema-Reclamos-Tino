package pe.tino.reclamos.app;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.VentanaPrincipal;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;

/** Punto de entrada del prototipo de diseno externo. */
public final class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            new VentanaPrincipal(Navegacion.MAPA).setVisible(true);
        });
    }
}
