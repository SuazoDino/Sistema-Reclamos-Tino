package pe.tino.reclamos.app;

import pe.tino.reclamos.ui.VentanaPrincipal;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;

/** Punto de entrada del prototipo de diseno externo. */
public final class Main {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("flatlaf.useWindowDecorations", "false");

        boolean oscuro = args.length > 0 && args[0].equalsIgnoreCase("--oscuro");

        SwingUtilities.invokeLater(() -> {
            Tema.instalar(oscuro);
            new VentanaPrincipal(pe.tino.reclamos.ui.Navegacion.MAPA).setVisible(true);
        });
    }
}
