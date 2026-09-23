package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.MotorReglas;
import pe.tino.reclamos.repo.MotorReglas.*;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalogo de reglas: las condiciones del motor, las variables que puede
 * leer, las acciones con las que puede terminar y un simulador que corre la
 * cadena de punta a punta.
 *
 * El simulador existe porque una regla que no se puede ejecutar no se puede
 * defender: aqui se ve que condicion se evaluo, con que datos y por que se
 * llego a esa solucion.
 */
public class ReglasPantalla extends Pantalla {

    private final Tabla condiciones = new Tabla(new String[]{
            "Condición", "Variable", "Operador", "Valor", "Si es verdadero", "Si es falso",
            "Descripción"});
    private final Map<String, JTextField> campos = new LinkedHashMap<>();
    private final Tabla traza = new Tabla(new String[]{"Paso", "Comparación", "Resultado", "Salida"});
    private final JLabel resultado = Ui.fuerte("");

    public ReglasPantalla() {
        super("Catálogo de reglas",
                "Condiciones encadenadas que deciden la solución que se aplica a un reclamo.");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());
        pestanias.addTab("Condiciones", panelCondiciones());
        pestanias.addTab("Variables", panelVariables());
        pestanias.addTab("Acciones", panelAcciones());
        pestanias.addTab("Simulador", panelSimulador());
        pestanias.addTab("Asignar Regla", panelAsignar());

        contenido().add(pestanias, BorderLayout.CENTER);
    }

    /* ---------------- condiciones ---------------- */

    private JComponent panelCondiciones() {
        condiciones.anchos(90, 170, 90, 150, 170, 170, 300);
        refrescar();

        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> editar(null));
        JButton modificar = Ui.boton("Modificar");
        modificar.addActionListener(e -> {
            int i = condiciones.filaModelo();
            if (i < 0) { avisar("Seleccione la condición que desea modificar."); return; }
            editar(MotorReglas.condiciones().get(i));
        });
        JButton verificar = Ui.boton("Verificar cadena");
        verificar.addActionListener(e -> verificar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 2, Tema.ESP_SM));
        pie.add(agregar);
        pie.add(modificar);
        pie.add(verificar);

        Grupo g = Grupo.ajustado("Cadena de condiciones");
        g.add(condiciones.enScroll(), BorderLayout.CENTER);

        JPanel nota = Ui.panel(new BorderLayout());
        nota.setBorder(Ui.relleno(Tema.ESP_SM));
        nota.add(Ui.suave("Cada salida dice si va a otra condición (Ir a) o si termina en una "
                + "solución (Aplicar). Antes las dos cosas compartian la misma columna."),
                BorderLayout.WEST);
        g.add(nota, BorderLayout.SOUTH);

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(g, BorderLayout.CENTER);
        p.add(pie, BorderLayout.SOUTH);
        return p;
    }

    private void refrescar() {
        condiciones.limpiar();
        for (Condicion c : MotorReglas.condiciones()) {
            condiciones.agregar(c.codigo(), c.variable(), c.operador(), c.valor(),
                    c.siVerdadero().toString(), c.siFalso().toString(), c.descripcion());
        }
    }

    /** Alta o edicion de una condicion; cada salida declara su tipo. */
    private void editar(Condicion actual) {
        JTextField codigo = Ui.texto(10);
        JComboBox<String> variable = Ui.combo(MotorReglas.nombresDeVariable());
        JComboBox<String> operador = Ui.combo(MotorReglas.OPERADORES);
        JTextField valor = Ui.texto(12);
        JTextField descripcion = Ui.texto(24);

        JComboBox<TipoSalida> tipoV = Ui.combo(List.of(TipoSalida.values()));
        JComboBox<String> destinoV = new JComboBox<>();
        JComboBox<TipoSalida> tipoF = Ui.combo(List.of(TipoSalida.values()));
        JComboBox<String> destinoF = new JComboBox<>();

        tipoV.addActionListener(e -> cargarDestinos(tipoV, destinoV));
        tipoF.addActionListener(e -> cargarDestinos(tipoF, destinoF));
        cargarDestinos(tipoV, destinoV);
        cargarDestinos(tipoF, destinoF);

        if (actual != null) {
            codigo.setText(actual.codigo());
            variable.setSelectedItem(actual.variable());
            operador.setSelectedItem(actual.operador());
            valor.setText(actual.valor());
            descripcion.setText(actual.descripcion());
            tipoV.setSelectedItem(actual.siVerdadero().tipo());
            cargarDestinos(tipoV, destinoV);
            destinoV.setSelectedItem(actual.siVerdadero().destino());
            tipoF.setSelectedItem(actual.siFalso().tipo());
            cargarDestinos(tipoF, destinoF);
            destinoF.setSelectedItem(actual.siFalso().destino());
        }

        Formulario f = new Formulario();
        f.campo("Condición:", codigo)
         .campo("Variable:", variable)
         .campo("Operador:", operador)
         .campo("Valor:", valor, "Un número, true/false, o el nombre de otra variable")
         .grupo("Si es verdadero")
         .campo("Salida:", tipoV)
         .campo("Destino:", destinoV)
         .grupo("Si es falso")
         .campo("Salida:", tipoF)
         .campo("Destino:", destinoF)
         .grupo("Documentación")
         .campo("Descripción:", descripcion);
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, Ui.scrollVertical(f),
                "Agregar/Modificar Condición", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (codigo.getText().isBlank()) { avisar("Indique el código de la condición."); return; }

        Condicion nueva = new Condicion(codigo.getText().trim(),
                String.valueOf(variable.getSelectedItem()),
                String.valueOf(operador.getSelectedItem()),
                valor.getText().trim(),
                new Salida((TipoSalida) tipoV.getSelectedItem(),
                        String.valueOf(destinoV.getSelectedItem())),
                new Salida((TipoSalida) tipoF.getSelectedItem(),
                        String.valueOf(destinoF.getSelectedItem())),
                descripcion.getText().trim());

        List<Condicion> lista = MotorReglas.condiciones();
        if (actual == null) lista.add(nueva);
        else lista.set(lista.indexOf(actual), nueva);
        refrescar();
    }

    /** El destino depende del tipo: otra condicion o una accion del catalogo. */
    private static void cargarDestinos(JComboBox<TipoSalida> tipo, JComboBox<String> destino) {
        List<String> opciones = tipo.getSelectedItem() == TipoSalida.CONDICION
                ? MotorReglas.codigos() : MotorReglas.ACCIONES;
        destino.setModel(new DefaultComboBoxModel<>(opciones.toArray(new String[0])));
        destino.setFont(Tema.cuerpo());
    }

    private void verificar() {
        List<String> fallas = MotorReglas.problemas();
        if (fallas.isEmpty()) {
            avisar("La cadena está bien armada: todas las salidas existen y "
                    + "todas las variables están en el catálogo.");
            return;
        }
        advertir("La cadena tiene problemas:\n\n" + String.join("\n", fallas));
    }

    /* ---------------- variables y acciones ---------------- */

    private JComponent panelVariables() {
        Tabla t = new Tabla(new String[]{"Variable", "Tipo", "De dónde sale", "Unidad"});
        MotorReglas.variables().forEach(v ->
                t.agregar(v.nombre(), v.tipo().name(), v.origen(), v.unidad()));
        t.anchos(180, 110, 420, 110);

        Grupo g = Grupo.ajustado("Variables que el motor puede leer");
        g.add(t.enScroll(), BorderLayout.CENTER);

        JPanel nota = Ui.panel(new BorderLayout());
        nota.setBorder(Ui.relleno(Tema.ESP_SM));
        nota.add(Ui.suave("Una condición solo puede comparar variables de esta lista. "
                + "Antes el parámetro era texto libre y no coincidía con nada."),
                BorderLayout.WEST);
        g.add(nota, BorderLayout.SOUTH);

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(g, BorderLayout.CENTER);
        return p;
    }

    private JComponent panelAcciones() {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        MotorReglas.ACCIONES.forEach(modelo::addElement);
        JList<String> lista = new JList<>(modelo);
        lista.setFont(Tema.cuerpo());

        Grupo g = Grupo.ajustado("Soluciones con las que puede terminar la cadena");
        g.add(Ui.scroll(lista), BorderLayout.CENTER);

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(g, BorderLayout.CENTER);
        return p;
    }

    /* ---------------- simulador ---------------- */

    private JComponent panelSimulador() {
        Formulario f = new Formulario();
        MotorReglas.valoresDeEjemplo().forEach((nombre, valor) -> {
            JTextField campo = Ui.texto(12);
            campo.setText(valor);
            campos.put(nombre, campo);
            Variable v = MotorReglas.variable(nombre);
            f.campo(nombre + ":", campo, v == null ? "" : v.origen());
        });

        JButton correr = Ui.boton("Evaluar cadena");
        correr.addActionListener(e -> evaluar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_SM));
        pie.add(correr);

        Grupo entradas = new Grupo("Datos del caso");
        entradas.add(Ui.scrollVertical(f), BorderLayout.CENTER);
        entradas.add(pie, BorderLayout.SOUTH);
        entradas.setPreferredSize(Ui.dim(430, 100));

        traza.anchos(90, 330, 110, 220);
        Grupo salida = Grupo.ajustado("Cómo se llegó a la solución");
        salida.add(traza.enScroll(), BorderLayout.CENTER);

        JPanel cabecera = Ui.panel(new BorderLayout());
        cabecera.setBorder(Ui.relleno(Tema.ESP_SM));
        cabecera.add(resultado, BorderLayout.WEST);
        salida.add(cabecera, BorderLayout.NORTH);

        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(entradas, BorderLayout.WEST);
        p.add(salida, BorderLayout.CENTER);
        return p;
    }

    private void evaluar() {
        Map<String, String> valores = new LinkedHashMap<>();
        campos.forEach((nombre, campo) -> valores.put(nombre, campo.getText().trim()));

        Resultado r = MotorReglas.evaluar(valores);
        traza.limpiar();
        r.traza().forEach(p -> traza.agregar(p.codigo(), p.comparacion(),
                p.resultado() ? "Verdadero" : "Falso", p.salida()));

        resultado.setText(r.valido()
                ? "Solución que aplica el sistema: " + r.accion()
                : "La cadena no pudo resolverse: " + r.error());
    }

    /* ---------------- asignar regla ---------------- */

    private JComponent panelAsignar() {
        JPanel fila = Ui.panel(new GridLayout(2, 2, Tema.ESP_MD, Tema.ESP_SM));
        fila.add(Ui.etiqueta("Tipo de Reclamo:"));
        fila.add(Ui.combo(Prototipo.TIPO_PROBLEMA));
        fila.add(Ui.etiqueta("Condición inicial:"));
        fila.add(Ui.combo(MotorReglas.codigos()));

        Tabla asignadas = new Tabla(new String[]{"Tipo de Reclamo", "Condición inicial"});
        asignadas.anchos(280, 280);
        List<String[]> asignaciones = new ArrayList<>(List.of(
                new String[]{"Funcionamiento", "CND1"},
                new String[]{"Entrega", "CND1"},
                new String[]{"Cobranza", "CND2"}));
        asignaciones.forEach(a -> asignadas.agregar((Object[]) a));

        JButton asignar = Ui.boton("Asignar");
        asignar.addActionListener(e -> avisar("La cadena asignada arranca en la condición elegida."));

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
}
