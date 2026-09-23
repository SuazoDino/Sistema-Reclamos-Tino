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
 * Catalogo de Reclamos y Eventos General, como en la captura: dos pestanias,
 * Reclamo y Eventos. Cada una con su busqueda, sus resultados y su bloque
 * para agregar valores.
 */
public class ReclamosEventosPantalla extends Pantalla {

    private final Tabla reclamos = new Tabla(new String[]{
            "Tipo de Problema", "Instancia", "Criticidad", "Estado"});
    private final Tabla eventos = new Tabla(new String[]{"Evento", "Estado"});

    public ReclamosEventosPantalla() {
        super("Catálogo de Reclamos y Eventos General",
                "Tipos de reclamo y eventos del protocolo, con su estado.");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());
        pestanias.addTab("Reclamo", panelReclamo());
        pestanias.addTab("Eventos", panelEventos());

        contenido().add(pestanias, BorderLayout.CENTER);
    }

    /* ---------------- pestania Reclamo ---------------- */

    private JComponent panelReclamo() {
        Map<String, List<String>> filtros = new LinkedHashMap<>();
        filtros.put("Estado", Prototipo.ESTADO);
        PanelBusqueda busqueda = new PanelBusqueda(1, filtros);
        busqueda.setPreferredSize(Ui.dim(250, 100));

        reclamos.anchos(180, 120, 120, 140);
        Prototipo.reclamos().forEach(f -> reclamos.agregar((Object[]) f));
        busqueda.alBuscar(() -> filtrar(reclamos, Prototipo.reclamos(), 3, busqueda.valor("Estado")));

        JTabbedPane agregar = new JTabbedPane();
        agregar.setFont(Tema.cuerpo());
        agregar.addTab("Tipo Problema", new PanelHabilitacion(this, "Tipos de Problema",
                "Tipo Problema", "Quitar", Prototipo.TIPO_PROBLEMA, Prototipo.TIPO_PROBLEMA));
        agregar.addTab("Instancia", new PanelHabilitacion(this, "Instancias",
                "Instancia", "Quitar", Prototipo.INSTANCIA, Prototipo.INSTANCIA));
        agregar.addTab("Criticidad", new PanelHabilitacion(this, "Criticidades",
                "Criticidad", "Quitar", Prototipo.CRITICIDAD, Prototipo.CRITICIDAD));

        Grupo grupoAgregar = new Grupo("Agregar");
        grupoAgregar.add(agregar, BorderLayout.CENTER);

        JPanel arriba = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        arriba.add(busqueda, BorderLayout.WEST);
        arriba.add(resultados("Resultados de Búsqueda", reclamos), BorderLayout.CENTER);
        arriba.setPreferredSize(Ui.dim(100, 200));

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(arriba, BorderLayout.NORTH);
        p.add(grupoAgregar, BorderLayout.CENTER);
        return p;
    }

    /* ---------------- pestania Eventos ---------------- */

    private JComponent panelEventos() {
        Map<String, List<String>> filtros = new LinkedHashMap<>();
        filtros.put("Tipo Problema", Prototipo.TIPO_PROBLEMA);
        filtros.put("Instancia", Prototipo.INSTANCIA);
        filtros.put("Criticidad", Prototipo.CRITICIDAD);
        PanelBusqueda busqueda = new PanelBusqueda(1, filtros);
        busqueda.setPreferredSize(Ui.dim(280, 100));

        eventos.anchos(320, 180);
        Prototipo.eventos().forEach(f -> eventos.agregar((Object[]) f));
        busqueda.alBuscar(() -> {
            eventos.limpiar();
            Prototipo.eventos().forEach(f -> eventos.agregar((Object[]) f));
        });

        Grupo grupoAgregar = new Grupo("Agregar");
        grupoAgregar.add(new PanelHabilitacion(this, "Eventos", "Evento", "Deshabilitar",
                Prototipo.EVENTOS_EXISTENTES, Prototipo.EVENTOS_HABILITADOS), BorderLayout.CENTER);

        JPanel arriba = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        arriba.add(busqueda, BorderLayout.WEST);
        arriba.add(resultados("Resultados de Búsqueda", eventos), BorderLayout.CENTER);
        arriba.setPreferredSize(Ui.dim(100, 200));

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(arriba, BorderLayout.NORTH);
        p.add(grupoAgregar, BorderLayout.CENTER);
        return p;
    }

    /* ---------------- piezas comunes ---------------- */

    /** Tabla de resultados con los botones Habilitar y Deshabilitar al costado. */
    private JComponent resultados(String titulo, Tabla tabla) {
        JButton habilitar = Ui.boton("Habilitar");
        habilitar.addActionListener(e -> cambiarEstado(tabla, "Habilitado"));
        JButton deshabilitar = Ui.boton("Deshabilitar");
        deshabilitar.addActionListener(e -> cambiarEstado(tabla, "Deshabilitado"));

        JPanel botones = Ui.panel(new GridBagLayout());
        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(habilitar);
        columna.add(deshabilitar);
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        Grupo g = new Grupo(titulo);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(botones, BorderLayout.EAST);
        return g;
    }

    /** La columna de estado es siempre la ultima de la tabla. */
    private void cambiarEstado(Tabla tabla, String estado) {
        int fila = tabla.filaModelo();
        if (fila < 0) { avisar("Seleccione una fila de los resultados."); return; }
        tabla.modelo().setValueAt(estado, fila, tabla.getColumnCount() - 1);
    }

    private static void filtrar(Tabla tabla, List<String[]> datos, int columna, String valor) {
        tabla.limpiar();
        datos.stream()
                .filter(f -> valor == null || valor.isBlank() || valor.equals(f[columna]))
                .forEach(f -> tabla.agregar((Object[]) f));
    }
}
