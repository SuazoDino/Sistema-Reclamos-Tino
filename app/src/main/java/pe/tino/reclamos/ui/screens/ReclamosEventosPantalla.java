package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Catalogo de Reclamos y Eventos.
 *
 * Entrada : datos de tipo de reclamo y de tipo de evento.
 * Funcion : registrar los reclamos con sus respectivos eventos.
 * Salida  : tipos de reclamo y tipos de evento sincronizados.
 */
public class ReclamosEventosPantalla extends Pantalla {

    public ReclamosEventosPantalla() {
        super("Catalogo de Reclamos y Eventos",
                "Un tipo de reclamo es un segmento mas un tipo de problema; cada evento es un paso del protocolo.");

        Tabla eventos = new Tabla(new String[]{"Evento del protocolo", "Valores que admite"});
        Datos.catalogoEventos().forEach(f -> eventos.agregar((Object[]) f));
        eventos.anchos(220, 420);

        Grupo grupoEventos = Grupo.ajustado("Catalogo de eventos");
        grupoEventos.add(eventos.enScroll(), BorderLayout.CENTER);
        grupoEventos.setPreferredSize(new Dimension(100, 190));

        contenido().add(new TablaCatalogo(this, "Catalogo de reclamos",
                List.of(Campo.lista("Segmento", Datos.SEGMENTOS_RECLAMO),
                        Campo.lista("Tipo de problema", Datos.TIPOS_PROBLEMA_RECLAMO),
                        Campo.lista("Estado", List.of("Habilitado", "Deshabilitado"))),
                Datos.catalogoReclamos(),
                new int[]{380, 200, 150}), BorderLayout.CENTER);
        contenido().add(grupoEventos, BorderLayout.SOUTH);
    }
}
