package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de Productos, como en la captura: busqueda por segmento, familia y
 * clase, la tabla de resultados y las pestanias para agregar familia, clase o
 * producto.
 */
public class ProductosPantalla extends Pantalla {

    private final Tabla tabla = new Tabla(new String[]{"Segmento", "Familia", "Clase", "Bien"});
    private final PanelBusqueda busqueda;

    public ProductosPantalla() {
        super("Producto", "Productos clasificados en segmento, familia y clase.");

        Map<String, List<String>> filtros = new LinkedHashMap<>();
        filtros.put("Segmento", nivel(0));
        filtros.put("Familia", nivel(1));
        filtros.put("Clase", nivel(2));
        busqueda = new PanelBusqueda(1, filtros);
        busqueda.alBuscar(this::buscar);

        tabla.anchos(230, 210, 200, 190);
        buscar();

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.addTab("Agregar Familia", conContexto("Segmento", nivel(0),
                new PanelHabilitacion(this, "Familia", "Familia", "Deshabilitar",
                        nivel(1), nivel(1).subList(0, Math.min(3, nivel(1).size())),
                        "Familia", false)));
        pestanias.addTab("Agregar Clase", conContexto("Familia", nivel(1),
                new PanelHabilitacion(this, "Clase", "Clase", "Deshabilitar",
                        nivel(2), nivel(2).subList(0, Math.min(3, nivel(2).size())),
                        "Clase", false)));
        pestanias.addTab("Agregar Producto", conContexto("Clase", nivel(2),
                new PanelHabilitacion(this, "Producto", "Producto", "Deshabilitar",
                        nivel(3), nivel(3).subList(0, Math.min(4, nivel(3).size())),
                        "Producto", false)));

        contenido().add(FormularioCatalogo.armar(
                FormularioCatalogo.cabecera(busqueda, "Resultado de Busqueda", tabla, 260),
                FormularioCatalogo.agregar(pestanias), 210), BorderLayout.CENTER);
    }

    /** Valores distintos de un nivel de la jerarquia, en su orden original. */
    private static List<String> nivel(int indice) {
        return new ArrayList<>(new LinkedHashSet<>(
                Datos.JERARQUIA_PRODUCTO.stream().map(r -> r[indice]).toList()));
    }

    /** El combo de contexto que el prototipo pone arriba de cada pestania. */
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
        String segmento = busqueda.valor("Segmento");
        String familia = busqueda.valor("Familia");
        String clase = busqueda.valor("Clase");

        tabla.limpiar();
        Datos.JERARQUIA_PRODUCTO.stream()
                .filter(r -> coincide(segmento, r[0]))
                .filter(r -> coincide(familia, r[1]))
                .filter(r -> coincide(clase, r[2]))
                .forEach(r -> tabla.agregar((Object[]) r));
    }

    private static boolean coincide(String filtro, String valor) {
        return filtro == null || filtro.isBlank() || filtro.equals(valor);
    }
}
