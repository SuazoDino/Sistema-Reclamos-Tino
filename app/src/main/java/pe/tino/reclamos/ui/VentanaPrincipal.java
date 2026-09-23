package pe.tino.reclamos.ui;

import pe.tino.reclamos.ui.screens.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana unica de la aplicacion. No hay barra de menus ni panel lateral:
 * se entra por el mapa conceptual y se vuelve a el desde cualquier pantalla.
 */
public class VentanaPrincipal extends JFrame {

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final Map<String, Supplier<JComponent>> fabricas = new HashMap<>();
    private final Map<String, JComponent> creadas = new HashMap<>();

    public VentanaPrincipal(String pantallaInicial) {
        super("Sistema de Reclamos");

        fabricas.put(Navegacion.MAPA,      MapaPantalla::new);

        // Seguridad
        fabricas.put("seguridad-perfiles", SeguridadPantalla::new);

        // Gerencial - Mantenimiento de Parametros
        fabricas.put("cat-general",        ParametrosPantalla::new);
        fabricas.put("cat-reclamos",       ReclamosEventosPantalla::new);
        fabricas.put("cat-productos",      ProductosPantalla::new);
        fabricas.put("cat-servicios",      ServiciosPantalla::new);
        fabricas.put("cat-problemas",      ProblemasPantalla::new);
        fabricas.put("cat-clientes",       ClientesPantalla::new);
        fabricas.put("cat-protocolos",     ProtocolosPantalla::new);
        fabricas.put("cat-reglas",         ReglasPantalla::new);
        fabricas.put("cat-politicas",      PoliticasPantalla::new);
        fabricas.put("cat-atencion",       AtencionParamPantalla::new);

        // Gerencial - Consulta
        fabricas.put("tickets",            TicketPantalla::new);
        fabricas.put("batch",              BatchPantalla::new);
        fabricas.put("indicadores",        IndicadoresPantalla::new);

        // Operativo
        fabricas.put("area-dataentry",     AreaDataEntryPantalla::new);
        fabricas.put("area-reportes",      AreaReportePantalla::new);
        fabricas.put("cliente-dataentry",  ClienteDataEntryPantalla::new);
        fabricas.put("cliente-reportes",   ClienteReportePantalla::new);

        contenido.setBackground(Tema.FONDO);
        // al rearmar por un cambio de escala, el historial se conserva
        if (Navegacion.actual().equals(pantallaInicial) && !Navegacion.MAPA.equals(pantallaInicial)) {
            Navegacion.reenganchar(this::mostrar);
        } else {
            Navegacion.instalar(this::mostrar);
        }

        setContentPane(contenido);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        // maximizada: el mapa se ajusta al espacio, asi que cuanto mas hay, mas grande se ve
        setExtendedState(MAXIMIZED_BOTH);

        mostrar(pantallaInicial);
    }

    /**
     * Cambia el tamanio del texto de toda la interfaz y rearma la ventana.
     * Se reconstruye en vez de repintar porque muchos controles fijan su
     * fuente al crearse; los datos viven en los repositorios, asi que no se
     * pierde nada.
     */
    public static void reescalarTexto(Component origen, int direccion) {
        double nueva = Tema.escalaVecina(direccion);
        if (Math.abs(nueva - Tema.escalaTexto()) < 0.01) return;

        Window ventana = SwingUtilities.getWindowAncestor(origen);
        boolean maximizada = ventana instanceof JFrame f
                && (f.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        Rectangle marco = ventana == null ? null : ventana.getBounds();

        Tema.escalaTexto(nueva);
        Tema.instalar();

        VentanaPrincipal nueva2 = new VentanaPrincipal(Navegacion.actual());
        if (marco != null && !maximizada) nueva2.setBounds(marco);
        nueva2.setExtendedState(maximizada ? MAXIMIZED_BOTH : NORMAL);
        nueva2.setVisible(true);
        if (ventana != null) ventana.dispose();
    }

    private void mostrar(String clave) {
        creadas.computeIfAbsent(clave, k -> {
            JComponent pantalla = fabricas.get(k).get();
            contenido.add(pantalla, k);
            return pantalla;
        });
        tarjetas.show(contenido, clave);
    }
}
