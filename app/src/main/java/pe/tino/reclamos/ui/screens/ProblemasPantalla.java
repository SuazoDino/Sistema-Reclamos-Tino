package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;

import java.awt.BorderLayout;
import java.util.List;

/**
 * Catalogo de Problemas.
 *
 * Entrada : datos de problemas.
 * Funcion : registrar problemas y sincronizarlos con tipo de problema y producto.
 * Salida  : problemas registrados y sincronizados.
 */
public class ProblemasPantalla extends Pantalla {

    public ProblemasPantalla() {
        super("Catalogo de Problemas",
                "Problemas reconocidos por el sistema, sincronizados con su tipo y su producto.");

        contenido().add(new TablaCatalogo(this, "Problemas registrados",
                List.of(Campo.lista("Segmento", Datos.SEGMENTOS),
                        Campo.lista("Familia", Datos.FAMILIAS),
                        Campo.lista("Tipo de problema", Datos.TIPOS_PROBLEMA_CAT),
                        Campo.libre("Problema")),
                Datos.catalogoProblemas(),
                new int[]{280, 230, 160, 180}), BorderLayout.CENTER);
    }
}
