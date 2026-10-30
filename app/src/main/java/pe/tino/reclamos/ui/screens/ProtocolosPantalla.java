package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Catalogo de Protocolos: un protocolo por tipo de reclamo, formado por
 * eventos (Inspeccion, Solucion, Seguimiento...), y cada evento por sus
 * pasos con su tiempo maximo, operario y criticidad.
 *
 * Los pasos se editan sobre la misma lista que usa la atencion: la duracion
 * de la cita y el operario que pide cada evento salen de aqui.
 */
public class ProtocolosPantalla extends Pantalla {

    public ProtocolosPantalla() {
        super("Catálogo de Protocolos",
                "Cada protocolo está formado por eventos, y cada evento por sus pasos.");

        Tabla eventos = new Tabla(new String[]{"Protocolo", "Orden", "Evento", "Duración de la cita", "Pide como mínimo"});
        for (String[] e : Prototipo.eventosDeProtocolo()) {
            eventos.agregar(e[0], e[1], e[2], Prototipo.duracionDe(e[0], e[2]) + " min",
                    Prototipo.operarioRequerido(e[0], e[2]));
        }
        eventos.anchos(110, 80, 240, 160, 160);
        Grupo gEventos = Grupo.ajustado("Eventos de cada protocolo");
        gEventos.add(eventos.enScroll(), BorderLayout.CENTER);
        gEventos.setPreferredSize(Ui.dim(100, 200));

        List<String> codigos = Prototipo.protocolos().stream().map(f -> f[0]).toList();
        List<String> nombres = List.copyOf(new LinkedHashSet<>(
                Prototipo.eventosDeProtocolo().stream().map(e -> e[2]).toList()));

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(gEventos, BorderLayout.NORTH);
        p.add(new TablaCatalogo(this, "Pasos del evento",
                List.of(Campo.lista("Protocolo", codigos), Campo.lista("Evento", nombres),
                        Campo.libre("Paso"), Campo.libre("Acción"), Campo.libre("Tiempo máximo (min)"),
                        Campo.lista("Tipo de operario", Prototipo.TIPO_OPERARIO),
                        Campo.lista("Criticidad", Prototipo.CRITICIDAD_ACCION)),
                Prototipo.pasosDeProtocolo(), new int[]{100, 190, 60, 260, 150, 140, 120}), BorderLayout.CENTER);
        contenido().add(p, BorderLayout.CENTER);
    }
}
