package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Regla;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogo de reglas, como en la captura: tres pestanias, Regla, Condiciones
 * y Asignar Regla. Las condiciones se dan de alta en una ventana aparte,
 * "Agregar/Modificar Regla".
 */
public class ReglasPantalla extends Pantalla {

    private static final List<String> NOMBRES_REGLA =
            List.of("Regla de Garantia", "Regla de Reembolso", "Regla de Intercambio");

    private final Tabla condiciones = new Tabla(new String[]{
            "Condicion", "Parametro", "Operador", "Variable", "AccionV", "AccionF", "Descripcion"});
    private final JComboBox<String> reglaElegida = Ui.combo(NOMBRES_REGLA);

    public ReglasPantalla() {
        super("Catalogo de reglas",
                "Reglas de negocio y las condiciones encadenadas de cada una.");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());
        pestanias.addTab("Regla", panelRegla());
        pestanias.addTab("Condiciones", panelCondiciones());
        pestanias.addTab("Asignar Regla", panelAsignar());

        contenido().add(pestanias, BorderLayout.CENTER);
    }

    /* ---------------- pestania Regla ---------------- */

    private JComponent panelRegla() {
        DefaultListModel<String> existentes = new DefaultListModel<>();
        DefaultListModel<String> habilitadas = new DefaultListModel<>();
        NOMBRES_REGLA.forEach(existentes::addElement);

        JList<String> listaExist = new JList<>(existentes);
        JList<String> listaHab = new JList<>(habilitadas);
        listaExist.setFont(Tema.cuerpo());
        listaHab.setFont(Tema.cuerpo());

        JTextField nombre = Ui.texto(16);
        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> {
            String v = nombre.getText().trim();
            if (v.isEmpty()) { avisar("Escriba el nombre de la regla."); return; }
            if (existentes.contains(v)) { avisar("Esa regla ya existe."); return; }
            existentes.addElement(v);
            nombre.setText("");
        });

        Grupo grupoAgregar = new Grupo("Agregar");
        Formulario f = new Formulario();
        f.campo("Nombre de regla", nombre);
        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_SM));
        pie.add(agregar);
        grupoAgregar.add(f, BorderLayout.CENTER);
        grupoAgregar.add(pie, BorderLayout.SOUTH);

        JButton pasar = Ui.boton(">");
        pasar.addActionListener(e -> {
            String v = listaExist.getSelectedValue();
            if (v == null) { avisar("Seleccione una regla existente."); return; }
            if (habilitadas.contains(v)) { avisar("Esa regla ya esta habilitada."); return; }
            habilitadas.addElement(v);
        });
        JButton quitar = Ui.boton("<");
        quitar.addActionListener(e -> {
            String v = listaHab.getSelectedValue();
            if (v == null) { avisar("Seleccione una regla habilitada."); return; }
            habilitadas.removeElement(v);
        });

        JPanel botones = Ui.panel(new GridBagLayout());
        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(pasar);
        columna.add(quitar);
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, Tema.ESP_SM, 0, Tema.ESP_SM));

        JPanel izq = Ui.panel(new BorderLayout(0, Tema.ESP_XS));
        izq.add(Ui.etiqueta("Reglas Existentes"), BorderLayout.NORTH);
        izq.add(Ui.scroll(listaExist), BorderLayout.CENTER);
        JPanel der = Ui.panel(new BorderLayout(0, Tema.ESP_XS));
        der.add(Ui.etiqueta("Reglas Habilitadas"), BorderLayout.NORTH);
        der.add(Ui.scroll(listaHab), BorderLayout.CENTER);

        Grupo grupoHabilitar = new Grupo("Habilitar", new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1; gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 1; grupoHabilitar.add(izq, gc);
        gc.gridx = 1; gc.weightx = 0; grupoHabilitar.add(botones, gc);
        gc.gridx = 2; gc.weightx = 1; grupoHabilitar.add(der, gc);

        JPanel p = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(grupoAgregar);
        p.add(grupoHabilitar);
        return p;
    }

    /* ---------------- pestania Condiciones ---------------- */

    private JComponent panelCondiciones() {
        condiciones.anchos(110, 150, 100, 110, 140, 110, 220);
        refrescar();

        JButton formula = Ui.boton("Formula");
        formula.addActionListener(e -> avisar("La formula se define en el catalogo de clientes."));

        JPanel arriba = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        JPanel izq = Ui.panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_SM, 0));
        izq.add(Ui.etiqueta("Seleccionar Regla"));
        izq.add(reglaElegida);
        arriba.add(izq, BorderLayout.WEST);
        arriba.add(Ui.filaDerecha(formula), BorderLayout.EAST);
        arriba.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        JButton modificar = Ui.boton("Modificar");
        modificar.addActionListener(e -> {
            int i = condiciones.filaModelo();
            if (i < 0) { avisar("Seleccione la condicion que desea modificar."); return; }
            abrirDialogo(Estado.reglas().get(i), i);
        });
        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> abrirDialogo(null, -1));
        JButton cancelar = Ui.boton("Cancelar");
        cancelar.addActionListener(e -> condiciones.clearSelection());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG, Tema.ESP_SM));
        pie.add(modificar);
        pie.add(agregar);
        pie.add(cancelar);

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(arriba, BorderLayout.NORTH);
        p.add(condiciones.enScroll(), BorderLayout.CENTER);
        p.add(pie, BorderLayout.SOUTH);
        return p;
    }

    /** La ventana "Agregar/Modificar Regla" de la captura. */
    private void abrirDialogo(Regla regla, int indice) {
        JComboBox<String> condicion = Ui.combo(codigos());
        condicion.setEditable(true);
        JComboBox<String> parametro = Ui.combo(parametros());
        parametro.setEditable(true);
        JComboBox<String> operador = Ui.combo(Datos.OPERADORES);
        JComboBox<String> variable = Ui.combo(Datos.TIPOS_VARIABLE);
        JComboBox<String> accionV = Ui.combo(Datos.ACCIONES);
        JComboBox<String> accionF = Ui.combo(codigos());
        accionF.setEditable(true);

        if (regla != null) {
            condicion.setSelectedItem(regla.condicion());
            parametro.setSelectedItem(regla.parametro());
            operador.setSelectedItem(regla.operador());
            variable.setSelectedItem(regla.variable());
            accionV.setSelectedItem(regla.accionVerdadero());
            accionF.setSelectedItem(regla.accionFalso());
        }

        Formulario f = new Formulario();
        f.campo("Condicion", condicion)
         .campo("Parametro", parametro)
         .campo("Operador", operador)
         .campo("Variable", variable)
         .campo("Accion V", accionV)
         .campo("Accion F", accionF);
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Agregar/Modificar Regla",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        Regla nueva = new Regla(String.valueOf(condicion.getSelectedItem()),
                String.valueOf(parametro.getSelectedItem()),
                String.valueOf(operador.getSelectedItem()),
                String.valueOf(variable.getSelectedItem()),
                String.valueOf(accionV.getSelectedItem()),
                String.valueOf(accionF.getSelectedItem()),
                regla == null ? "" : regla.descripcion());

        if (indice < 0) Estado.reglas().add(nueva); else Estado.reglas().set(indice, nueva);
        refrescar();
    }

    private static List<String> codigos() {
        List<String> l = new ArrayList<>();
        Estado.reglas().forEach(r -> l.add(r.condicion()));
        return l;
    }

    private static List<String> parametros() {
        List<String> l = new ArrayList<>();
        Estado.reglas().forEach(r -> l.add(r.parametro()));
        return l;
    }

    /* ---------------- pestania Asignar Regla ---------------- */

    private JComponent panelAsignar() {
        JPanel fila = Ui.panel(new GridLayout(2, 2, Tema.ESP_MD, Tema.ESP_SM));
        fila.add(Ui.etiqueta("Tipo de Reclamo:"));
        fila.add(Ui.combo(Prototipo.TIPO_PROBLEMA));
        fila.add(Ui.etiqueta("Regla:"));
        fila.add(Ui.combo(NOMBRES_REGLA));

        Tabla asignadas = new Tabla(new String[]{"Tipo de Reclamo", "Regla"});
        asignadas.anchos(280, 280);

        JButton asignar = Ui.boton("Asignar");
        asignar.addActionListener(e -> avisar("Regla asignada al tipo de reclamo."));

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_SM));
        pie.add(asignar);

        Grupo g = new Grupo("Asignar Regla");
        g.add(fila, BorderLayout.NORTH);
        g.add(asignadas.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(g, BorderLayout.CENTER);
        return p;
    }

    private void refrescar() {
        condiciones.limpiar();
        for (Regla r : Estado.reglas()) {
            condiciones.agregar(r.condicion(), r.parametro(), r.operador(), r.variable(),
                    r.accionVerdadero(), r.accionFalso(), r.descripcion());
        }
    }
}
