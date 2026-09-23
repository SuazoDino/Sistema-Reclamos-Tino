package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.model.Modelo.Indicador;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Ficha de indicador: cifra grande, nombre y variacion respecto del periodo
 * anterior. La variacion lleva flecha y signo explicitos, de modo que no
 * dependa del color para leerse.
 */
public class Ficha extends Tarjeta {

    private static final DecimalFormat FMT = new DecimalFormat("0.0");

    public Ficha(Indicador ind) {
        super();
        relleno(Tema.ESP_MD);
        cuerpo().setLayout(new BoxLayout(cuerpo(), BoxLayout.Y_AXIS));

        JLabel nombre = new JLabel("<html><body style='width:150px'>" + ind.nombre() + "</body></html>");
        nombre.setFont(Tema.micro());
        nombre.setForeground(Tema.textoSuave());
        nombre.setAlignmentX(LEFT_ALIGNMENT);

        JLabel valor = new JLabel(ind.valor());
        valor.setFont(Tema.fuente(26, Font.BOLD));
        valor.setForeground(Tema.texto());
        valor.setAlignmentX(LEFT_ALIGNMENT);
        valor.setBorder(Ui.relleno(Tema.ESP_SM, 0, 2, 0));

        JLabel unidad = new JLabel(ind.unidad());
        unidad.setFont(Tema.micro());
        unidad.setForeground(Tema.textoSuave());
        unidad.setAlignmentX(LEFT_ALIGNMENT);

        boolean sube = ind.variacion() >= 0;
        boolean bueno = sube == ind.masEsMejor();
        String flecha = sube ? "▲" : "▼";
        String signo = sube ? "+" : "";
        JLabel delta = new JLabel(flecha + " " + signo + FMT.format(ind.variacion()) + " vs. mes anterior");
        delta.setFont(Tema.fuente(11, Font.BOLD));
        delta.setForeground(bueno ? Tema.exito() : Tema.peligro());
        delta.setAlignmentX(LEFT_ALIGNMENT);
        delta.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));

        cuerpo().add(nombre);
        cuerpo().add(valor);
        cuerpo().add(unidad);
        cuerpo().add(delta);
    }
}
