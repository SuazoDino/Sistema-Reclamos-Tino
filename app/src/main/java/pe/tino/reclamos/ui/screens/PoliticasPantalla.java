package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;

import java.awt.BorderLayout;
import java.util.List;

/**
 * Catalogo de Politicas.
 *
 * Entrada : catalogos y estados de politicas.
 * Funcion : establecer politicas y sus tipos.
 * Salida  : politicas registradas.
 */
public class PoliticasPantalla extends Pantalla {

    public PoliticasPantalla() {
        super("Catalogo de Politicas",
                "Garantias legales, explicitas e implicitas, y el estado de cada una.");

        contenido().add(new TablaCatalogo(this, "Politicas registradas",
                List.of(Campo.libre("Codigo"),
                        Campo.lista("Tipo de garantia", Datos.TIPOS_GARANTIA),
                        Campo.libre("Politica"),
                        Campo.lista("Plazo", Datos.PLAZOS),
                        Campo.lista("Estado", List.of("Habilitado", "Deshabilitado"))),
                Datos.catalogoPoliticas(),
                new int[]{90, 140, 300, 110, 140}), BorderLayout.CENTER);
    }
}
