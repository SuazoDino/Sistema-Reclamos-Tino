package pe.tino.reclamos.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Tokens visuales del sistema: una sola fuente de verdad para colores,
 * tipografia y espaciado. Las pantallas nunca declaran colores propios.
 */
public final class Tema {

    private Tema() {}

    /* ---------- espaciado (escala de 4) ---------- */
    public static final int ESP_XS = 4;
    public static final int ESP_SM = 8;
    public static final int ESP_MD = 16;
    public static final int ESP_LG = 24;
    public static final int ESP_XL = 32;

    /* ---------- paleta ---------- */
    private static boolean oscuro = false;

    public static Color fondo()        { return oscuro ? new Color(0x1E1F22) : new Color(0xF4F5F7); }
    public static Color superficie()   { return oscuro ? new Color(0x2B2D31) : Color.WHITE; }
    public static Color borde()        { return oscuro ? new Color(0x3C3F45) : new Color(0xDCDFE4); }
    public static Color texto()        { return oscuro ? new Color(0xE6E6E6) : new Color(0x1B1F26); }
    public static Color textoSuave()   { return oscuro ? new Color(0x9AA0A6) : new Color(0x62676E); }
    public static Color acento()       { return serie(); }
    public static Color acentoSuave()  { return oscuro ? new Color(0x2A3A55) : new Color(0xE3ECFC); }
    public static Color barraLateral() { return oscuro ? new Color(0x17181B) : new Color(0x16243B); }
    public static Color barraTexto()   { return new Color(0xC3CBD8); }

    /* ---------- tokens de visualizacion ----------
     * Color de serie y paleta de estado tomados de la paleta validada:
     * un solo tono por grafico (serie unica), estado siempre con punto + etiqueta,
     * nunca color solo. Contraste verificado >= 3:1 en ambas superficies. */
    public static Color serie()     { return oscuro ? new Color(0x3987E5) : new Color(0x2A78D6); }
    public static Color grilla()    { return oscuro ? new Color(0x3A3D42) : new Color(0xE1E0D9); }
    public static Color ejeBase()   { return oscuro ? new Color(0x4A4D53) : new Color(0xC3C2B7); }
    public static Color textoEje()  { return new Color(0x898781); }

    /* estados: good / warning / serious / critical (fijos, nunca tematizados) */
    public static Color exito()   { return new Color(0x0CA30C); }
    public static Color aviso()   { return new Color(0xFAB219); }
    public static Color serio()   { return new Color(0xEC835A); }
    public static Color peligro() { return new Color(0xD03B3B); }
    public static Color neutro()  { return textoSuave(); }

    /* ---------- tipografia ---------- */
    private static final String FAMILIA = "Inter";

    public static Font fuente(int tam, int estilo) {
        Font f = new Font(FAMILIA, estilo, tam);
        // si Inter no esta instalada, Java cae a la logica por defecto del sistema
        if (!f.getFamily().equalsIgnoreCase(FAMILIA)) f = new Font(Font.SANS_SERIF, estilo, tam);
        return f;
    }

    public static Font cuerpo()   { return fuente(13, Font.PLAIN); }
    public static Font fuerte()   { return fuente(13, Font.BOLD); }
    public static Font titulo()   { return fuente(20, Font.BOLD); }
    public static Font subtitulo(){ return fuente(15, Font.BOLD); }
    public static Font micro()    { return fuente(11, Font.PLAIN); }
    public static Font mono()     { return new Font(Font.MONOSPACED, Font.PLAIN, 12); }

    /* ---------- instalacion ---------- */
    public static boolean esOscuro() { return oscuro; }

    public static void instalar(boolean modoOscuro) {
        oscuro = modoOscuro;
        if (modoOscuro) FlatDarkLaf.setup(); else FlatLightLaf.setup();

        UIManager.put("defaultFont", cuerpo());
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("ScrollBar.trackArc", 8);
        UIManager.put("Component.accentColor", acento());
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TableHeader.height", 30);
        UIManager.put("Table.gridColor", borde());
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("Panel.background", fondo());
        UIManager.put("List.selectionBackground", acentoSuave());
        UIManager.put("List.selectionForeground", texto());
        UIManager.put("List.selectionInactiveBackground", acentoSuave());
        UIManager.put("List.selectionInactiveForeground", texto());
        UIManager.put("Tree.selectionBackground", acentoSuave());
        UIManager.put("Tree.selectionForeground", texto());
        UIManager.put("Tree.selectionInactiveBackground", acentoSuave());
        UIManager.put("Tree.selectionInactiveForeground", texto());
    }

    /** Relanza el look&feel en el otro modo y repinta todas las ventanas abiertas. */
    public static void alternarModo() {
        instalar(!oscuro);
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }
    }
}
