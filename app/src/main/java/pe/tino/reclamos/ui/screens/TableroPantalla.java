package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.*;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Vista de entrada: indicadores del mes, distribucion de carga y ultimos reclamos. */
public class TableroPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Tabla tabla = new Tabla(new String[]{
            "Reclamo", "Fecha", "Cliente", "Producto", "Problema", "Area", "Estado"});

    public TableroPantalla() {
        super("Aplicativo - Gerencial", "Tablero de control",
                "Resumen operativo del periodo en curso.");

        acciones(Ui.secundario("Exportar resumen", "reporte"));

        JPanel columna = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        columna.add(fichas(), BorderLayout.NORTH);
        columna.add(graficos(), BorderLayout.CENTER);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(columna, BorderLayout.NORTH);
        raiz.add(ultimosReclamos(), BorderLayout.CENTER);

        contenido().add(Ui.scroll(raiz), BorderLayout.CENTER);

        refrescar(Estado.reclamos());
        Estado.alCambiarReclamos(this::refrescar);
    }

    private JComponent fichas() {
        JPanel p = Ui.panel(new GridLayout(1, 4, Tema.ESP_MD, 0));
        List<Indicador> indicadores = Datos.indicadores();
        for (int i = 0; i < 4; i++) p.add(new Ficha(indicadores.get(i)));
        return p;
    }

    private JComponent graficos() {
        JPanel p = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));

        Tarjeta porArea = new Tarjeta("Reclamos por area",
                "Carga acumulada del periodo, por area responsable.");
        porArea.add(new GraficoBarras(Datos.reclamosPorArea(), "reclamos"), BorderLayout.CENTER);

        Tarjeta porBien = new Tarjeta("Reclamos por tipo de bien",
                "Que familias de producto concentran los reclamos.");
        porBien.add(new GraficoBarras(Datos.reclamosPorTipoBien(), "reclamos"), BorderLayout.CENTER);

        p.add(porArea);
        p.add(porBien);
        return p;
    }

    private JComponent ultimosReclamos() {
        tabla.anchos(90, 100, 210, 180, 190, 220, 130);
        tabla.columnaEstado(6, Estados::color);

        Tarjeta t = new Tarjeta("Ultimos reclamos registrados",
                "Los mas recientes primero. Doble clic para abrir el detalle en Consulta.");
        t.sinRelleno();
        t.add(tabla.enScroll(), BorderLayout.CENTER);
        t.setPreferredSize(new Dimension(100, 320));
        return t;
    }

    private void refrescar(List<Reclamo> reclamos) {
        tabla.limpiar();
        reclamos.stream().limit(12).forEach(r -> tabla.agregar(
                r.id(),
                r.fechaEmision().format(FECHA),
                r.cliente().nombre(),
                r.producto().bien(),
                r.problema().descripcion(),
                r.area(),
                r.estado().etiqueta()));
    }
}
