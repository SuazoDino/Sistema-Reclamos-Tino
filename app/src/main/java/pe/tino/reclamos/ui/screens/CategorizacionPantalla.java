package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Mant-Param - Categorizacion de cliente. Reproduce los tres bloques de la
 * hoja MANT-PARAM: las condiciones que deciden el segmento, los metodos de
 * calculo y la secuencia de operaciones de cada metodo.
 */
public class CategorizacionPantalla extends Pantalla {

    public CategorizacionPantalla() {
        super("Mant-Param › Catalogos", "Categorizacion de cliente",
                "Como decide el sistema si un cliente es Nuevo, Ordinario, Vip o SuperVip.");

        Tabla condiciones = new Tabla(new String[]{
                "Condicion", "Parametro", "Operador", "Variable", "Si es verdadero", "Si es falso"});
        Datos.condicionesCliente().forEach(f -> condiciones.agregar((Object[]) f));
        condiciones.anchos(110, 150, 100, 130, 150, 130);
        condiciones.centrar(2);

        Tabla metodos = new Tabla(new String[]{"Metodo", "Formula"});
        Datos.metodosCliente().forEach(f -> metodos.agregar((Object[]) f));
        metodos.anchos(110, 420);

        Tabla formulas = new Tabla(new String[]{"Metodo", "Variable", "Secuencia", "Operador"});
        Datos.formulasCliente().forEach(f -> formulas.agregar((Object[]) f));
        formulas.anchos(110, 200, 110, 150);
        formulas.centrar(2);

        Tarjeta tCond = new Tarjeta("Condiciones",
                "Se evaluan en orden; cada salida apunta a la siguiente condicion o a un metodo.");
        tCond.sinRelleno();
        tCond.add(condiciones.enScroll(), BorderLayout.CENTER);
        tCond.setPreferredSize(new Dimension(100, 210));

        Tarjeta tMet = new Tarjeta("Metodos de calculo",
                "La formula de negocio que se aplica cuando la condicion se cumple.");
        tMet.sinRelleno();
        tMet.add(metodos.enScroll(), BorderLayout.CENTER);

        Tarjeta tFor = new Tarjeta("Secuencia de cada metodo",
                "Paso a paso de la formula, como lo documenta el prototipo.");
        tFor.sinRelleno();
        tFor.add(formulas.enScroll(), BorderLayout.CENTER);

        JPanel inferior = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        inferior.add(tMet);
        inferior.add(tFor);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(tCond, BorderLayout.NORTH);
        raiz.add(inferior, BorderLayout.CENTER);
        raiz.add(consideraciones(), BorderLayout.SOUTH);

        contenido().add(raiz, BorderLayout.CENTER);
    }

    /** Las consideraciones habilitadas son las que entran en el calculo. */
    private JComponent consideraciones() {
        Tarjeta t = new Tarjeta();
        t.relleno(Tema.ESP_MD);
        var cat = Datos.catalogo("consideraciones");
        JLabel l = Ui.micro("Consideraciones habilitadas: "
                + String.join("  ·  ", cat.habilitados())
                + "    (se administran en Catalogos generales)");
        t.add(l, BorderLayout.WEST);
        return t;
    }
}
