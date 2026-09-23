package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Indicador;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Modulo de Estadisticas. Los once indicadores del prototipo: los ocho con
 * valor del periodo como fichas, mas el detalle tabulado para exportacion.
 */
public class EstadisticasPantalla extends Pantalla {

    public EstadisticasPantalla() {
        super("Gerencial - Estadisticas", "Indicadores de gestion",
                "Medicion del desempeno del proceso de atencion de reclamos.");

        acciones(Ui.secundario("Exportar a reporte", "reporte"));

        List<Indicador> indicadores = Datos.indicadores();

        JPanel fichas = Ui.panel(new GridLayout(0, 4, Tema.ESP_MD, Tema.ESP_MD));
        indicadores.forEach(i -> fichas.add(new Ficha(i)));

        Tabla tabla = new Tabla(new String[]{"Indicador", "Valor", "Unidad", "Variacion", "Lectura"});
        for (Indicador i : indicadores) {
            boolean bueno = (i.variacion() >= 0) == i.masEsMejor();
            tabla.agregar(i.nombre(), i.valor(), i.unidad(),
                    (i.variacion() >= 0 ? "+" : "") + i.variacion(),
                    bueno ? "Favorable" : "Desfavorable");
        }
        tabla.anchos(320, 100, 130, 110, 130);
        tabla.centrar(1, 3);
        tabla.columnaEstado(4, v -> "Favorable".equals(v) ? Tema.exito() : Tema.peligro());

        Tarjeta detalle = new Tarjeta("Detalle de indicadores",
                "Misma informacion que las fichas, en formato tabular para exportar.");
        detalle.sinRelleno();
        detalle.add(tabla.enScroll(), BorderLayout.CENTER);
        detalle.setPreferredSize(new Dimension(100, 300));

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(fichas, BorderLayout.NORTH);
        raiz.add(detalle, BorderLayout.CENTER);

        contenido().add(Ui.scroll(raiz), BorderLayout.CENTER);
    }
}
