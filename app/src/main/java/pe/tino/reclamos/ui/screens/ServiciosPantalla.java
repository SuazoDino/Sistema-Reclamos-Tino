package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;

import java.awt.BorderLayout;
import java.util.List;

/**
 * Catalogo de Servicios.
 *
 * El sistema clasificaba productos pero no servicios, aunque ya se reclamaba
 * por delivery, cobro o instalacion. Sin este catalogo esos reclamos no
 * tenian sobre que colgar.
 */
public class ServiciosPantalla extends Pantalla {

    public ServiciosPantalla() {
        super("Catalogo de Servicios",
                "Servicios sobre los que se puede reclamar y como se atiende cada uno.");

        contenido().add(new TablaCatalogo(this, "Servicios",
                List.of(Campo.lista("Tipo de servicio", Catalogos.TIPOS_SERVICIO),
                        Campo.libre("Servicio"),
                        Campo.lista("Unidad de atencion", Catalogos.UNIDADES_ATENCION),
                        Campo.lista("Estado", List.of("Habilitado", "Deshabilitado"))),
                Catalogos.servicios(),
                new int[]{200, 260, 200, 160}), BorderLayout.CENTER);
    }
}
