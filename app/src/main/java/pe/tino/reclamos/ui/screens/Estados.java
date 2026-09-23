package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.EstadoReclamo;
import pe.tino.reclamos.ui.theme.Tema;

import java.awt.Color;

/** Traduce el estado de un reclamo al color de estado que le corresponde. */
public final class Estados {

    private Estados() {}

    public static Color color(String etiqueta) {
        if (etiqueta == null) return Tema.neutro();
        return switch (etiqueta.trim().toLowerCase()) {
            case "entregado"   -> Tema.exito();
            case "atendido"    -> Tema.exito();
            case "en atencion" -> Tema.aviso();
            case "en cola"     -> Tema.serio();
            case "rechazado"   -> Tema.peligro();
            case "habilitado"  -> Tema.exito();
            case "deshabilitado" -> Tema.neutro();
            default -> Tema.neutro();
        };
    }

    public static Color color(EstadoReclamo e) { return color(e.etiqueta()); }
}
