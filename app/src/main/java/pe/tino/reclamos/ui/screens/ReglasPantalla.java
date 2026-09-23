package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Regla;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Mant-Param - Reglas. Motor de decision parametrizable: cada condicion
 * compara un parametro con una variable y encadena la siguiente condicion
 * segun el resultado sea verdadero o falso.
 */
public class ReglasPantalla extends Pantalla {

    private final Tabla tabla = new Tabla(new String[]{
            "Condicion", "Parametro", "Operador", "Variable",
            "Si es verdadero", "Si es falso", "Descripcion"});

    private final JTextField condicion = Ui.texto("CND0");
    private final JTextField parametro = Ui.texto("Nombre del parametro evaluado");
    private final JComboBox<String> operador = Ui.combo(Datos.OPERADORES);
    private final JComboBox<String> variable = Ui.combo(Datos.TIPOS_VARIABLE);
    private final JComboBox<String> accionV = Ui.combo(Datos.ACCIONES);
    private final JTextField accionF = Ui.texto("Accion o codigo de la siguiente condicion");
    private final JTextArea descripcion = Ui.area("Que decide esta condicion, en lenguaje de negocio.", 2);

    public ReglasPantalla() {
        super("Mant-Param", "Reglas de negocio",
                "Condiciones encadenadas que deciden la solucion aplicable a un reclamo.");

        tabla.anchos(95, 165, 85, 100, 140, 115, 230);
        tabla.centrar(2);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Tarjeta listado = new Tarjeta("Cadena de condiciones",
                "Se evaluan en orden; cada salida apunta a la siguiente condicion o a una accion final.");
        listado.sinRelleno();
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(listado, BorderLayout.CENTER);
        contenido().add(editor(), BorderLayout.EAST);
        contenido().add(leyenda(), BorderLayout.SOUTH);

        refrescar();
    }

    private JComponent leyenda() {
        Tarjeta t = new Tarjeta();
        t.relleno(Tema.ESP_MD);
        JLabel l = Ui.micro("Operadores:  EQ igual  ·  NE distinto  ·  GT mayor  ·  "
                + "GE mayor o igual  ·  LT menor  ·  LE menor o igual");
        l.setFont(Tema.mono());
        l.setForeground(Tema.textoSuave());
        t.add(l, BorderLayout.WEST);
        return t;
    }

    private JComponent editor() {
        Tarjeta t = new Tarjeta("Definicion de la condicion",
                "Alta o edicion de una condicion del motor de reglas.");
        t.setPreferredSize(new Dimension(370, 100));

        Formulario f = new Formulario();
        f.grupo("Identificacion")
         .campo("Codigo", condicion, "Convencion: CND1, CND2, CND3...")
         .campo("Parametro", parametro)
         .grupo("Comparacion")
         .campo("Operador", operador)
         .campo("Tipo de variable", variable)
         .grupo("Salidas")
         .campo("Si es verdadero", accionV)
         .campo("Si es falso", accionF)
         .campo("Descripcion", Ui.scrollConBorde(descripcion))
         .finalizar();

        JButton guardar = Ui.primario("Guardar", "guardar");
        guardar.addActionListener(e -> guardar());
        JButton nuevo = Ui.secundario("Nueva", "mas");
        nuevo.addActionListener(e -> limpiar());
        JButton eliminar = Ui.plano("Eliminar", "cruz");
        eliminar.addActionListener(e -> eliminar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        pie.add(eliminar, BorderLayout.WEST);
        pie.add(Ui.filaDerecha(Tema.ESP_SM, nuevo, guardar), BorderLayout.EAST);

        JPanel cuerpo = Ui.panel(new BorderLayout());
        cuerpo.add(f, BorderLayout.CENTER);
        cuerpo.add(pie, BorderLayout.SOUTH);
        t.add(Ui.scrollVertical(cuerpo), BorderLayout.CENTER);
        return t;
    }

    private void refrescar() {
        tabla.limpiar();
        for (Regla r : Estado.reglas()) {
            tabla.agregar(r.condicion(), r.parametro(), r.operador(), r.variable(),
                    r.accionVerdadero(), r.accionFalso(), r.descripcion());
        }
    }

    private int indiceSeleccionado() {
        int f = tabla.getSelectedRow();
        return f < 0 ? -1 : tabla.convertRowIndexToModel(f);
    }

    private void cargarSeleccion() {
        int i = indiceSeleccionado();
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
            avisar("El codigo de condicion y el parametro son obligatorios.");
            return;
        }
        Regla r = new Regla(condicion.getText().trim(), parametro.getText().trim(),
                String.valueOf(operador.getSelectedItem()),
                String.valueOf(variable.getSelectedItem()),
                String.valueOf(accionV.getSelectedItem()),
                accionF.getText().trim(),
                descripcion.getText().trim());

        int i = indiceSeleccionado();
        if (i < 0) Estado.reglas().add(r);
        else Estado.reglas().set(i, r);
        refrescar();
        avisar(i < 0 ? "Condicion agregada." : "Condicion actualizada.");
    }

    private void eliminar() {
        int i = indiceSeleccionado();
        if (i < 0) { avisar("Seleccione la condicion que desea eliminar."); return; }
        if (!confirmar("Eliminar la condicion seleccionada?")) return;
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
