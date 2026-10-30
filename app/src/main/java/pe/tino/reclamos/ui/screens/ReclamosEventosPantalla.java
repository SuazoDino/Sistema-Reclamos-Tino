package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de Reclamos y Eventos: el unico catalogo de tipos de problema, con
 * su naturaleza (reclamo o queja), y los eventos por los que pasan.
 *
 * Antes tenia una columna "Nivel" que duplicaba la prioridad y listas de
 * instancias y eventos habilitados que nada leia; se quitaron.
 */
public class ReclamosEventosPantalla extends Pantalla {

    public ReclamosEventosPantalla() {
        super("Catálogo de Reclamos y Eventos",
                "Los tipos de reclamo que acepta el sistema y los eventos por los que pasan.");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());
        pestanias.addTab("Tipos de reclamo", new TablaCatalogo(this, "Tipos de reclamo",
                List.of(Campo.libre("Tipo de problema"),
                        Campo.lista("Naturaleza", Prototipo.NATURALEZAS),
                        Campo.lista("Estado", Prototipo.ESTADO)),
                Prototipo.tipos(), new int[]{240, 180, 160}));
        pestanias.addTab("Eventos", eventos());
        contenido().add(pestanias, BorderLayout.CENTER);
    }

    private JComponent eventos() {
        Map<String, StringBuilder> usos = new LinkedHashMap<>();
        for (String[] e : Prototipo.eventosDeProtocolo()) {
            usos.computeIfAbsent(e[2], k -> new StringBuilder()).append(usos.get(e[2]).isEmpty() ? "" : ", ").append(e[0]);
        }
        Tabla t = new Tabla(new String[]{"Evento", "Lo usa"});
        usos.forEach((evento, protocolos) -> t.agregar(evento, protocolos.toString()));
        t.agregar(Prototipo.EVENTO_SEGUIMIENTO, "Control de calidad");
        t.anchos(260, 320);

        Grupo g = Grupo.ajustado("Eventos y protocolos que los usan");
        g.add(t.enScroll(), BorderLayout.CENTER);
        g.add(Ui.suave("Los eventos se ordenan y se definen paso a paso en el Catálogo de Protocolos."),
                BorderLayout.SOUTH);
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(g, BorderLayout.CENTER);
        return p;
    }
}
