package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Catalogo de Clientes.
 *
 * Entrada : tipos de cliente.
 * Funcion : sincronizar las condiciones con los tipos de cliente.
 * Salida  : tipos de cliente registrados y sincronizados con las condiciones.
 */
public class ClientesPantalla extends Pantalla {

    private final DefaultListModel<String> tipos = new DefaultListModel<>();
    private final JList<String> listaTipos = new JList<>(tipos);
    private final DefaultListModel<String> consideraciones = new DefaultListModel<>();
    private final JList<String> listaConsideraciones = new JList<>(consideraciones);

    public ClientesPantalla() {
        super("Catalogo de Clientes",
                "Tipos de cliente que considera la empresa y las consideraciones asignadas a cada uno.");

        Datos.catalogo("clientes").habilitados().forEach(tipos::addElement);
        listaTipos.setFont(Tema.cuerpo());
        listaTipos.setSelectedIndex(0);

        Datos.catalogo("consideraciones").existentes().forEach(consideraciones::addElement);
        listaConsideraciones.setFont(Tema.cuerpo());
        listaConsideraciones.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        seleccionarHabilitadas();

        Grupo gTipos = Grupo.ajustado("Tipos de cliente");
        gTipos.add(Ui.scroll(listaTipos), BorderLayout.CENTER);
        gTipos.add(pieTipos(), BorderLayout.SOUTH);
        gTipos.setPreferredSize(new Dimension(240, 100));

        Grupo gCons = Grupo.ajustado("Consideraciones asignadas");
        gCons.add(Ui.scroll(listaConsideraciones), BorderLayout.CENTER);
        JPanel ayuda = Ui.panel(new BorderLayout());
        ayuda.setBorder(Ui.relleno(Tema.ESP_SM));
        ayuda.add(Ui.suave("Marque las consideraciones que intervienen en el tipo seleccionado."),
                BorderLayout.WEST);
        gCons.add(ayuda, BorderLayout.SOUTH);

        JPanel superior = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        superior.add(gTipos, BorderLayout.WEST);
        superior.add(gCons, BorderLayout.CENTER);
        superior.setPreferredSize(new Dimension(100, 200));

        contenido().add(superior, BorderLayout.NORTH);
        contenido().add(condicionesYMetodos(), BorderLayout.CENTER);
    }

    private JComponent pieTipos() {
        JTextField nuevo = Ui.texto(10);
        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> {
            String v = nuevo.getText().trim();
            if (v.isEmpty()) { avisar("Escriba el tipo de cliente."); return; }
            if (tipos.contains(v)) { avisar("Ese tipo ya existe."); return; }
            tipos.addElement(v);
            nuevo.setText("");
        });

        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        p.setBorder(Ui.relleno(Tema.ESP_SM));
        p.add(nuevo, BorderLayout.CENTER);
        p.add(agregar, BorderLayout.EAST);
        return p;
    }

    /** Los tres bloques de la hoja MANT-PARAM que definen el calculo. */
    private JComponent condicionesYMetodos() {
        Tabla condiciones = new Tabla(new String[]{
                "Condicion", "Parametro", "Operador", "Variable", "Si es verdadero", "Si es falso"});
        Datos.condicionesCliente().forEach(f -> condiciones.agregar((Object[]) f));
        condiciones.anchos(100, 130, 90, 110, 130, 110).centrar(2);

        Tabla metodos = new Tabla(new String[]{"Metodo", "Formula"});
        Datos.metodosCliente().forEach(f -> metodos.agregar((Object[]) f));
        metodos.anchos(90, 380);

        Tabla formulas = new Tabla(new String[]{"Metodo", "Variable", "Secuencia", "Operador"});
        Datos.formulasCliente().forEach(f -> formulas.agregar((Object[]) f));
        formulas.anchos(90, 180, 90, 130).centrar(2);

        Grupo gCond = Grupo.ajustado("Condiciones");
        gCond.add(condiciones.enScroll(), BorderLayout.CENTER);
        gCond.setPreferredSize(new Dimension(100, 150));

        Grupo gMet = Grupo.ajustado("Metodos de calculo");
        gMet.add(metodos.enScroll(), BorderLayout.CENTER);

        Grupo gFor = Grupo.ajustado("Secuencia de cada metodo");
        gFor.add(formulas.enScroll(), BorderLayout.CENTER);

        JPanel inferior = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        inferior.add(gMet);
        inferior.add(gFor);

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(gCond, BorderLayout.NORTH);
        p.add(inferior, BorderLayout.CENTER);
        return p;
    }

    private void seleccionarHabilitadas() {
        var habilitadas = Datos.catalogo("consideraciones").habilitados();
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        for (int i = 0; i < consideraciones.size(); i++) {
            if (habilitadas.contains(consideraciones.get(i))) indices.add(i);
        }
        listaConsideraciones.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
    }
}
