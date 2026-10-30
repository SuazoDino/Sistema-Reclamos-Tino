package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Producto;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;

import java.awt.*;
import java.util.List;

/**
 * Catalogo de Politicas: la garantia de cada producto. Vale la mayor
 * garantia habilitada; el motor de reglas la lee como Tiempo_Garantia.
 *
 * Antes una politica "cruzaba dos catalogos" (Reclamo x Canal) sin que se
 * supiera que significaba ni que nada la usara; se quito.
 */
public class PoliticasPantalla extends Pantalla {

    public PoliticasPantalla() {
        super("Catálogo de Políticas", "Las políticas de garantía de cada producto.");

        contenido().add(new TablaCatalogo(this, "Garantía por producto",
                List.of(Campo.lista("Producto", Datos.PRODUCTOS.stream().map(Producto::id).toList()),
                        Campo.lista("Garantía", Prototipo.TIPOS_GARANTIA),
                        Campo.libre("Plazo (días)"),
                        Campo.lista("Estado", Prototipo.ESTADO)),
                Prototipo.tiposDePolitica(), new int[]{200, 160, 140, 160}), BorderLayout.CENTER);
    }
}
