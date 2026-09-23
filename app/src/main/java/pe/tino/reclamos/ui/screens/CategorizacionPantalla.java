package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Catalogo de Categorizacion de Cliente.
 *
 * Reproduce el bloque CATEGORIZACION CLIENTE de la hoja MANT-PARAM: las
 * condiciones que deciden el segmento, los metodos de calculo y la secuencia
 * de operaciones de cada metodo.
 */
public class CategorizacionPantalla extends Pantalla {

    public CategorizacionPantalla() {
        super("Catalogo de Categorizacion",
                "Condiciones, metodos y formulas con los que el sistema clasifica a un cliente.");

        Tabla condiciones = new Tabla(new String[]{
                "Condicion", "Parametro", "Operador", "Variable", "Si es verdadero", "Si es falso"});
        Datos.condicionesCliente().forEach(f -> condiciones.agregar((Object[]) f));
        condiciones.anchos(110, 150, 100, 130, 150, 130).centrar(2);

        Tabla metodos = new Tabla(new String[]{"Metodo", "Formula"});
        Datos.metodosCliente().forEach(f -> metodos.agregar((Object[]) f));
        metodos.anchos(90, 400);

        Tabla formulas = new Tabla(new String[]{"Metodo", "Variable", "Secuencia", "Operador"});
        Datos.formulasCliente().forEach(f -> formulas.agregar((Object[]) f));
        formulas.anchos(90, 190, 100, 140).centrar(2);

        Grupo gCond = Grupo.ajustado("Condiciones");
        gCond.add(condiciones.enScroll(), BorderLayout.CENTER);
        gCond.setPreferredSize(new Dimension(100, 170));

        Grupo gMet = Grupo.ajustado("Metodos de calculo");
        gMet.add(metodos.enScroll(), BorderLayout.CENTER);

        Grupo gFor = Grupo.ajustado("Secuencia de cada metodo");
        gFor.add(formulas.enScroll(), BorderLayout.CENTER);

        JPanel inferior = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        inferior.add(gMet);
        inferior.add(gFor);

        contenido().add(gCond, BorderLayout.NORTH);
        contenido().add(inferior, BorderLayout.CENTER);
        contenido().add(consideraciones(), BorderLayout.SOUTH);
    }

    /** Las consideraciones habilitadas son las que entran en el calculo. */
    private JComponent consideraciones() {
        var cat = Datos.catalogo("consideraciones");
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        p.add(Ui.suave("Consideraciones habilitadas: " + String.join("  -  ", cat.habilitados())
                + "   (se administran en el Catalogo General)"), BorderLayout.WEST);
        return p;
    }
}
