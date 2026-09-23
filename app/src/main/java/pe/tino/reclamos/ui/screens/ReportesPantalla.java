package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.EstadoReclamo;
import pe.tino.reclamos.model.Modelo.Reclamo;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modulo de Reportes. Se elige el reporte y los parametros, y el sistema
 * arma la vista previa a partir de los reclamos cargados en la sesion.
 */
public class ReportesPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TODOS = "Todos";

    private final DefaultListModel<String> modeloReportes = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(modeloReportes);
    private final JComboBox<String> periodicidad;
    private final JComboBox<String> area;
    private final JPanel vista = Ui.panel(new BorderLayout());
    private final List<String[]> reportes = Datos.reportes();

    public ReportesPantalla() {
        super("Mapa › Reportes", "Reportes",
                "Reportes operativos y gerenciales sobre los reclamos registrados.");

        reportes.forEach(r -> modeloReportes.addElement(r[0] + "  ·  " + r[1]));
        lista.setFont(Tema.cuerpo());
        lista.setFixedCellHeight(30);
        lista.setBorder(Ui.relleno(Tema.ESP_SM));
        lista.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) generar(); });

        List<String> periodos = new ArrayList<>(List.of(TODOS, "Diario", "Semanal", "Mensual"));
        periodicidad = Ui.combo(periodos);

        List<String> areas = new ArrayList<>(List.of(TODOS));
        areas.addAll(Datos.catalogo("areas").habilitados());
        area = Ui.combo(areas);

        periodicidad.addActionListener(e -> generar());
        area.addActionListener(e -> generar());

        contenido().add(panelCatalogo(), BorderLayout.WEST);
        contenido().add(panelVista(), BorderLayout.CENTER);

        lista.setSelectedIndex(0);
    }

    private JComponent panelCatalogo() {
        Tarjeta t = new Tarjeta("Reportes disponibles",
                reportes.size() + " reportes definidos.");
        t.setPreferredSize(new Dimension(300, 100));
        t.sinRelleno();
        t.add(Ui.scroll(lista), BorderLayout.CENTER);
        return t;
    }

    private JComponent panelVista() {
        Tarjeta t = new Tarjeta("Vista previa",
                "Con los reclamos de la sesion.");
        t.acciones(Ui.etiqueta("Periodicidad:"), periodicidad,
                   Ui.etiqueta("Area:"), area,
                   Ui.secundario("Exportar", "reporte"));
        t.sinRelleno();
        t.add(vista, BorderLayout.CENTER);
        return t;
    }

    private void generar() {
        int i = lista.getSelectedIndex();
        vista.removeAll();
        if (i < 0) {
            vista.add(Ui.suave("Seleccione un reporte de la lista."), BorderLayout.NORTH);
            vista.revalidate();
            vista.repaint();
            return;
        }

        String[] def = reportes.get(i);
        String areaElegida = String.valueOf(area.getSelectedItem());
        List<Reclamo> datos = Estado.reclamos().stream()
                .filter(r -> TODOS.equals(areaElegida) || r.area().equals(areaElegida))
                .toList();

        JPanel encabezado = Ui.panel(new BorderLayout(0, 2));
        encabezado.setBorder(Ui.relleno(Tema.ESP_MD, Tema.ESP_MD, Tema.ESP_SM, Tema.ESP_MD));
        JLabel titulo = Ui.subtitulo(def[0] + " — " + def[1]);
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(Ui.micro("Modulo " + def[2] + "  ·  periodicidad "
                + def[3].toLowerCase() + "  ·  area " + areaElegida.toLowerCase()
                + "  ·  " + datos.size() + " reclamos"), BorderLayout.CENTER);

        vista.add(encabezado, BorderLayout.NORTH);
        vista.add(cuerpo(def[0], datos), BorderLayout.CENTER);
        vista.revalidate();
        vista.repaint();
    }

    /** Cada reporte resume los mismos datos de una manera distinta. */
    private JComponent cuerpo(String codigo, List<Reclamo> datos) {
        switch (codigo) {
            case "REP01" -> {
                return grafico(agrupar(datos, Reclamo::area), "reclamos");
            }
            case "REP02" -> {
                return grafico(agrupar(datos, r -> r.producto().familia()), "reclamos");
            }
            case "REP05" -> {
                return grafico(agrupar(datos, Reclamo::especialista), "reclamos");
            }
            case "REP07" -> {
                return grafico(agrupar(datos, r -> r.canal().name()), "reclamos");
            }
            case "REP03" -> {
                Tabla t = new Tabla(new String[]{"Reclamo", "Emision", "Cliente", "Area", "Estado"});
                datos.stream()
                        .filter(r -> r.estado() != EstadoReclamo.ENTREGADO)
                        .forEach(r -> t.agregar(r.id(), r.fechaEmision().format(FECHA),
                                r.cliente().nombre(), r.area(), r.estado().etiqueta()));
                t.anchos(90, 110, 220, 220, 140);
                t.columnaEstado(4, Estados::color);
                return t.enScroll();
            }
            default -> {
                Tabla t = new Tabla(new String[]{"Reclamo", "Emision", "Cliente", "Producto",
                        "Problema", "Area", "Estado"});
                datos.forEach(r -> t.agregar(r.id(), r.fechaEmision().format(FECHA),
                        r.cliente().nombre(), r.producto().bien(), r.problema().descripcion(),
                        r.area(), r.estado().etiqueta()));
                t.anchos(80, 95, 175, 150, 160, 165, 130);
                t.columnaEstado(6, Estados::color);
                return t.enScroll();
            }
        }
    }

    private static Map<String, Integer> agrupar(List<Reclamo> datos,
                                                java.util.function.Function<Reclamo, String> clave) {
        Map<String, Integer> m = new LinkedHashMap<>();
        datos.forEach(r -> m.merge(clave.apply(r), 1, Integer::sum));
        return m;
    }

    private static JComponent grafico(Map<String, Integer> datos, String unidad) {
        if (datos.isEmpty()) {
            JPanel p = Ui.panel(new BorderLayout());
            p.setBorder(Ui.relleno(Tema.ESP_LG));
            p.add(Ui.suave("No hay reclamos que cumplan los filtros elegidos."), BorderLayout.NORTH);
            return p;
        }
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(new GraficoBarras(datos, unidad), BorderLayout.NORTH);
        return p;
    }
}
