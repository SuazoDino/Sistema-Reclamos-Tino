package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;

import java.awt.BorderLayout;
import java.util.List;

/**
 * Catalogo de Bienes.
 *
 * Entrada : datos de los tipos de bien.
 * Funcion : registrar y habilitar los tipos de bien y el problema que admiten.
 * Salida  : tipos de bien habilitados.
 */
public class BienesPantalla extends Pantalla {

    public BienesPantalla() {
        super("Catalogo de Bienes",
                "Tipos de bien reconocidos y el tipo de problema que admite cada uno.");

        contenido().add(new TablaCatalogo(this, "Tipos de bien",
                List.of(Campo.lista("Tipo de bien", Datos.TIPOS_BIEN),
                        Campo.lista("Tipo de problema", Datos.TIPOS_PROBLEMA_BIEN),
                        Campo.lista("Estado", List.of("Habilitado", "Deshabilitado"))),
                Datos.catalogoBienes(),
                new int[]{280, 240, 160}), BorderLayout.CENTER);
    }
}
