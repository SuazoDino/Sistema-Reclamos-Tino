package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;

import java.awt.*;
import java.util.List;

/**
 * Catalogo de Problemas: cada problema con su tipo y a que producto o
 * servicio aplica. Es la misma lista que usa el formulario del cliente para
 * no ofrecer combinaciones sin sentido.
 */
public class ProblemasPantalla extends Pantalla {

    public ProblemasPantalla() {
        super("Catálogo de Problemas", "Problemas de cada tipo y a qué producto o servicio aplican.");

        contenido().add(new TablaCatalogo(this, "Problemas registrados",
                List.of(Campo.lista("Tipo de problema", Prototipo.tipos().stream().map(f -> f[0]).toList()),
                        Campo.libre("Problema"),
                        Campo.libre("Aplica a")),
                Prototipo.problemas(),
                new int[]{200, 320, 380}), BorderLayout.CENTER);
    }
}
