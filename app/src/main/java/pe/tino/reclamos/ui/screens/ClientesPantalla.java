package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.Grupo;
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

        JPanel cuerpo = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        cuerpo.add(gTipos, BorderLayout.WEST);
        cuerpo.add(gCons, BorderLayout.CENTER);

        contenido().add(cuerpo, BorderLayout.CENTER);
        contenido().add(nota(), BorderLayout.SOUTH);
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

    /** La formula del calculo vive en su propio catalogo. */
    private JComponent nota() {
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        p.add(Ui.suave("Las condiciones, metodos y formulas del calculo se administran "
                + "en el Catalogo de Categorizacion."), BorderLayout.WEST);
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
