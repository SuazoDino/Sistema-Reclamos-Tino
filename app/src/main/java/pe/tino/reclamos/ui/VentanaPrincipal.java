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

        // Gerencial - Mantenimiento de Parametros
        fabricas.put("param-generales",    ParametrosPantalla::new);
        fabricas.put("cat-reclamos",       ReclamosEventosPantalla::new);
        fabricas.put("cat-productos",      ProductosPantalla::new);
        fabricas.put("cat-problemas",      ProblemasPantalla::new);
        fabricas.put("cat-clientes",       ClientesPantalla::new);
        fabricas.put("cat-protocolos",     ProtocolosPantalla::new);
        fabricas.put("cat-reglas",         ReglasPantalla::new);
        fabricas.put("cat-politicas",      PoliticasPantalla::new);

        // Gerencial - Consulta
        fabricas.put("indicadores",        IndicadoresPantalla::new);

        // Operativo
        fabricas.put("area-dataentry",     AreaDataEntryPantalla::new);
        fabricas.put("area-reportes",      AreaReportePantalla::new);
        fabricas.put("cliente-dataentry",  ClienteDataEntryPantalla::new);
        fabricas.put("cliente-reportes",   ClienteReportePantalla::new);

        contenido.setBackground(Tema.FONDO);
        Navegacion.instalar(this::mostrar);

        setContentPane(contenido);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        mostrar(pantallaInicial);
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
