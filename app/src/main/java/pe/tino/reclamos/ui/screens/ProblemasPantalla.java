package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CatalogoProblemas, como en la captura: busqueda por tipo de problema, la
 * tabla de resultados y las pestanias Agregar Problemas y Agregar Producto.
 */
public class ProblemasPantalla extends Pantalla {

    private final Tabla tabla = new Tabla(new String[]{"Problemas", "Producto"});
    private final PanelBusqueda busqueda;

    public ProblemasPantalla() {
        super("Catálogo de Problemas", "Problemas registrados y el producto al que aplican.");

        Map<String, List<String>> filtros = new LinkedHashMap<>();
        filtros.put("Tipo Problema", Prototipo.tiposDeProblema());
        busqueda = new PanelBusqueda(1, filtros);
        busqueda.alBuscar(this::buscar);

        tabla.anchos(380, 260);
        buscar();

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.addTab("Agregar Problemas", conContexto("Tipo Problema",
                Prototipo.tiposDeProblema(),
                new PanelHabilitacion(this, "Problemas", "Problema:", "Deshabilitar",
                        Prototipo.PROBLEMAS_EXISTENTES, Prototipo.PROBLEMAS_HABILITADOS,
                        "", false)));
        pestanias.addTab("Agregar Producto", conContexto("Problema:",
                Prototipo.PROBLEMAS_EXISTENTES,
                FormularioCatalogo.listaConCombo(this, "Lista de productos Existentes",
                        "Producto:", Prototipo.PRODUCTOS_SIMPLES, Prototipo.PRODUCTOS_SIMPLES)));

        contenido().add(FormularioCatalogo.armar(
                FormularioCatalogo.cabecera(busqueda, "Resultado de Búsqueda", tabla, 230),
                FormularioCatalogo.agregar(pestanias), 190), BorderLayout.CENTER);
    }

    private JComponent conContexto(String etiqueta, List<String> valores, JComponent cuerpo) {
        JPanel fila = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        fila.add(Ui.etiqueta(etiqueta), BorderLayout.WEST);
        fila.add(Ui.combo(valores), BorderLayout.CENTER);

        JPanel envoltura = Ui.panel(new BorderLayout());
        envoltura.add(fila, BorderLayout.WEST);
        envoltura.setBorder(Ui.relleno(Tema.ESP_SM, 0, Tema.ESP_MD, 0));

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_SM));
        p.add(envoltura, BorderLayout.NORTH);
        p.add(cuerpo, BorderLayout.CENTER);
        return p;
    }

    private void buscar() {
        tabla.limpiar();
        Prototipo.problemasPorProducto().forEach(f -> tabla.agregar((Object[]) f));
    }
}
