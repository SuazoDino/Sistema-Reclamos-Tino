package pe.tino.reclamos.ui;

import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.screens.*;
import pe.tino.reclamos.ui.theme.Iconos;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana principal: barra lateral fija con los modulos y area de contenido
 * con CardLayout. Cada pantalla se construye la primera vez que se abre.
 */
public class VentanaPrincipal extends JFrame {

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final Map<String, Supplier<JComponent>> fabricas = new HashMap<>();
    private final Map<String, JComponent> creadas = new HashMap<>();
    private final BarraLateral barra;

    private String pantallaActual;

    public VentanaPrincipal(String pantallaInicial) {
        super("Sistema de Reclamos — Diseno externo");

        // mapa y menus: la navegacion clasica, de lo general a lo particular
        fabricas.put(Navegacion.MAPA, MapaPantalla::new);
        fabricas.put("m-aplicativo", MenuPantalla::aplicativo);
        fabricas.put("m-tecnico",    MenuPantalla::tecnico);
        fabricas.put("m-operativo",  MenuPantalla::operativo);
        fabricas.put("m-gerencial",  MenuPantalla::gerencial);
        fabricas.put("m-area",       MenuPantalla::area);
        fabricas.put("m-cliente",    MenuPantalla::cliente);
        fabricas.put("m-mantparam",  MenuPantalla::mantParam);
        fabricas.put("seguridad",    MenuPantalla::seguridad);

        // pantallas de trabajo
        fabricas.put("tablero",      TableroPantalla::new);
        fabricas.put("registro",     RegistroPantalla::new);
        fabricas.put("atencion",     AtencionPantalla::new);
        fabricas.put("consulta",     ConsultaPantalla::new);
        fabricas.put("catalogos",    CatalogoPantalla::new);
        fabricas.put("productos",    ProductosPantalla::new);
        fabricas.put("bienes",       CatalogoTablaPantalla::bienes);
        fabricas.put("problemas",    CatalogoTablaPantalla::problemas);
        fabricas.put("politicas",    CatalogoTablaPantalla::politicas);
        fabricas.put("categorizacion", CategorizacionPantalla::new);
        fabricas.put("protocolos",   ProtocolosPantalla::new);
        fabricas.put("reglas",       ReglasPantalla::new);
        fabricas.put("estadisticas", EstadisticasPantalla::new);
        fabricas.put("reportes",     ReportesPantalla::new);
        fabricas.put("seguridad-perfiles", SeguridadPantalla::new);
        fabricas.put("batch",        ProcesosPantalla::batch);
        fabricas.put("actbd",        ProcesosPantalla::actBd);
        fabricas.put("mantbd",       ProcesosPantalla::mantBd);
        fabricas.put("contingencia", ProcesosPantalla::contingencia);

        contenido.setOpaque(true);
        contenido.setBackground(Tema.fondo());

        Navegacion.instalar(this::mostrar);
        barra = new BarraLateral(Navegacion::ir);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.fondo());
        raiz.add(barra, BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);
        raiz.add(barraEstado(), BorderLayout.SOUTH);

        setContentPane(raiz);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(new Dimension(1440, 880));
        setLocationRelativeTo(null);

        mostrar(pantallaInicial);
    }

    private JComponent barraEstado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.superficie());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.borde()),
                BorderFactory.createEmptyBorder(6, Tema.ESP_MD, 6, Tema.ESP_MD)));

        JLabel izquierda = Ui.micro("Prototipo de diseno externo · los datos viven solo en memoria");

        JLabel fecha = Ui.micro(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        JButton modo = new JButton(Iconos.de("luna", 14, Tema.textoSuave()));
        modo.setToolTipText("Alternar modo claro / oscuro");
        modo.setFocusable(false);
        modo.putClientProperty("JButton.buttonType", "toolBarButton");
        modo.addActionListener(e -> alternarTema());

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tema.ESP_MD, 0));
        derecha.setOpaque(false);
        derecha.add(fecha);
        derecha.add(modo);

        p.add(izquierda, BorderLayout.WEST);
        p.add(derecha, BorderLayout.EAST);
        return p;
    }

    private void mostrar(String clave) {
        pantallaActual = clave;
        barra.marcar(clave);
        creadas.computeIfAbsent(clave, k -> {
            JComponent pantalla = fabricas.get(k).get();
            contenido.add(pantalla, k);
            return pantalla;
        });
        tarjetas.show(contenido, clave);
    }

    /**
     * El cambio de tema reconstruye la ventana. Es mas simple y mas fiable que
     * repintar: muchos componentes toman su color al construirse, y asi ninguno
     * queda con la paleta anterior.
     */
    private void alternarTema() {
        String destino = pantallaActual;
        Rectangle marco = getBounds();
        Tema.instalar(!Tema.esOscuro());
        dispose();

        VentanaPrincipal nueva = new VentanaPrincipal(destino == null ? Navegacion.MAPA : destino);
        nueva.setBounds(marco);
        nueva.setVisible(true);
    }
}
