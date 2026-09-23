package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Regla;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Catalogo de Reglas.
 *
 * Entrada : datos y parametros de las reglas de negocio.
 * Funcion : establecer reglas y registrar sus datos y parametros.
 * Salida  : reglas registradas.
 */
public class ReglasPantalla extends Pantalla {

    private final Tabla tabla = new Tabla(new String[]{
            "Condicion", "Parametro", "Operador", "Variable",
            "Si es verdadero", "Si es falso", "Descripcion"});

    private final JTextField condicion = Ui.texto();
    private final JTextField parametro = Ui.texto();
    private final JComboBox<String> operador = Ui.combo(Datos.OPERADORES);
    private final JComboBox<String> variable = Ui.combo(Datos.TIPOS_VARIABLE);
    private final JComboBox<String> accionV = Ui.combo(Datos.ACCIONES);
    private final JTextField accionF = Ui.texto();
    private final JTextField descripcion = Ui.texto();

    public ReglasPantalla() {
        super("Catalogo de Reglas",
                "Condiciones encadenadas: cada salida apunta a la siguiente condicion o a una accion.");

        tabla.anchos(95, 165, 85, 100, 140, 115, 230).centrar(2);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Grupo listado = Grupo.ajustado("Reglas registradas");
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(listado, BorderLayout.CENTER);
        contenido().add(editor(), BorderLayout.SOUTH);
        refrescar();
    }

    private JComponent editor() {
        Grupo g = new Grupo("Definicion de la condicion");

        Formulario f = new Formulario();
        f.campo("Condicion", condicion)
         .campo("Parametro", parametro)
         .campo("Operador", operador)
         .campo("Tipo de variable", variable)
         .campo("Si es verdadero", accionV)
         .campo("Si es falso", accionF)
         .campo("Descripcion", descripcion);

        JLabel leyenda = Ui.suave("EQ igual   NE distinto   GT mayor   "
                + "GE mayor o igual   LT menor   LE menor o igual");
        leyenda.setFont(Tema.mono());

        JButton nuevo = Ui.boton("Nueva");
        nuevo.addActionListener(e -> limpiar());
        JButton guardar = Ui.boton("Guardar");
        guardar.addActionListener(e -> guardar());
        JButton eliminar = Ui.boton("Eliminar");
        eliminar.addActionListener(e -> eliminar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(leyenda, BorderLayout.WEST);
        pie.add(Ui.filaDerecha(nuevo, eliminar, guardar), BorderLayout.EAST);

        g.add(f, BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        return g;
    }

    private void refrescar() {
        tabla.limpiar();
        for (Regla r : Estado.reglas()) {
            tabla.agregar(r.condicion(), r.parametro(), r.operador(), r.variable(),
                    r.accionVerdadero(), r.accionFalso(), r.descripcion());
        }
    }

    private void cargarSeleccion() {
        int i = tabla.filaModelo();
        if (i < 0) return;
        Regla r = Estado.reglas().get(i);
        condicion.setText(r.condicion());
        parametro.setText(r.parametro());
        operador.setSelectedItem(r.operador());
        variable.setSelectedItem(r.variable());
        accionV.setSelectedItem(r.accionVerdadero());
        accionF.setText(r.accionFalso());
        descripcion.setText(r.descripcion());
    }

    private void guardar() {
        if (condicion.getText().isBlank() || parametro.getText().isBlank()) {
            avisar("La condicion y el parametro son obligatorios.");
            return;
        }
        Regla r = new Regla(condicion.getText().trim(), parametro.getText().trim(),
                String.valueOf(operador.getSelectedItem()),
                String.valueOf(variable.getSelectedItem()),
                String.valueOf(accionV.getSelectedItem()),
                accionF.getText().trim(), descripcion.getText().trim());

        int i = tabla.filaModelo();
        if (i < 0) Estado.reglas().add(r); else Estado.reglas().set(i, r);
        refrescar();
        avisar(i < 0 ? "Regla agregada." : "Regla actualizada.");
    }

    private void eliminar() {
        int i = tabla.filaModelo();
        if (i < 0) { avisar("Seleccione la regla que desea eliminar."); return; }
        if (!confirmar("Eliminar la regla seleccionada?")) return;
        Estado.reglas().remove(i);
        refrescar();
        limpiar();
    }

    private void limpiar() {
        tabla.clearSelection();
        condicion.setText("");
        parametro.setText("");
        operador.setSelectedIndex(0);
        variable.setSelectedIndex(0);
        accionV.setSelectedIndex(0);
        accionF.setText("");
        descripcion.setText("");
        condicion.requestFocusInWindow();
    }
}
