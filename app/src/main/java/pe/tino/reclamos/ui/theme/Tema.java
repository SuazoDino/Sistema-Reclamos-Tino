package pe.tino.reclamos.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Aspecto clasico de aplicacion de escritorio: fondo gris de control, paneles
 * con borde y titulo, esquinas rectas y la tipografia del sistema. Sin
 * adornos: lo que se ve es lo que el prototipo documenta.
 */
public final class Tema {

    private Tema() {}

    /* ---------- espaciado ---------- */
    public static final int ESP_XS = 4;
    public static final int ESP_SM = 8;
    public static final int ESP_MD = 12;
    public static final int ESP_LG = 16;

    /* ---------- colores ---------- */
    public static final Color FONDO      = new Color(0xF0F0F0);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color BORDE      = new Color(0xA0A0A0);
    public static final Color BORDE_FINO = new Color(0xC8C8C8);
    public static final Color TEXTO      = Color.BLACK;
    public static final Color TEXTO_SUAVE= new Color(0x505050);
    public static final Color SELECCION  = new Color(0xCCE0FF);
    public static final Color FILA_ALTERNA = new Color(0xF5F5F5);
    public static final Color CABECERA   = new Color(0xE4E4E4);

    /* ---------- tipografia ---------- */
    public static Font fuente(int tam, int estilo) { return new Font(Font.SANS_SERIF, estilo, tam); }
    public static Font cuerpo()  { return fuente(12, Font.PLAIN); }
    public static Font fuerte()  { return fuente(12, Font.BOLD); }
    public static Font titulo()  { return fuente(16, Font.BOLD); }
    public static Font mono()    { return new Font(Font.MONOSPACED, Font.PLAIN, 12); }

    /** Instala el look and feel. Se llama una sola vez, al arrancar. */
    public static void instalar() {
        FlatLightLaf.setup();

        UIManager.put("defaultFont", cuerpo());
        UIManager.put("Component.arc", 0);
        UIManager.put("Button.arc", 0);
        UIManager.put("TextComponent.arc", 0);
        UIManager.put("ScrollBar.thumbArc", 0);
        UIManager.put("ScrollBar.trackArc", 0);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 1);

        UIManager.put("Panel.background", FONDO);
        UIManager.put("Component.borderColor", BORDE);
        UIManager.put("Component.disabledBorderColor", BORDE_FINO);

        UIManager.put("Table.rowHeight", 22);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.gridColor", BORDE_FINO);
        UIManager.put("Table.intercellSpacing", new Dimension(1, 1));
        UIManager.put("Table.selectionBackground", SELECCION);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("TableHeader.height", 24);
        UIManager.put("TableHeader.background", CABECERA);
        UIManager.put("TableHeader.separatorColor", BORDE_FINO);

        UIManager.put("List.selectionBackground", SELECCION);
        UIManager.put("List.selectionForeground", TEXTO);
        UIManager.put("List.selectionInactiveBackground", SELECCION);
        UIManager.put("List.selectionInactiveForeground", TEXTO);
        UIManager.put("Tree.selectionBackground", SELECCION);
        UIManager.put("Tree.selectionForeground", TEXTO);
        UIManager.put("Tree.selectionInactiveBackground", SELECCION);
        UIManager.put("Tree.selectionInactiveForeground", TEXTO);
        UIManager.put("TitledBorder.titleColor", TEXTO);
    }
}
