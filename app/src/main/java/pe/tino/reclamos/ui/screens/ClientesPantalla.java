package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Catalogo Clientes, como en la captura: dos pestanias, "Agregar Tipo Cliente"
 * y "Asignar Condiciones".
 */
public class ClientesPantalla extends Pantalla {

    public ClientesPantalla() {
        super("Catalogo Clientes",
                "Tipos de cliente y las consideraciones asignadas a cada uno.");

        var clientes = Datos.catalogo("clientes");
        var consideraciones = Datos.catalogo("consideraciones");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());

        JPanel tipos = Ui.panel(new BorderLayout());
        tipos.setBorder(Ui.relleno(Tema.ESP_MD));
        tipos.add(new PanelHabilitacion(this, "Tipos de Clientes", "Tipo de Cliente:",
                "Deshabilitar", clientes.existentes(), clientes.habilitados(), "", false),
                BorderLayout.CENTER);
        pestanias.addTab("Agregar Tipo Cliente", tipos);

        pestanias.addTab("Asignar Condiciones", asignarCondiciones(
                clientes.habilitados(), consideraciones.existentes(),
                consideraciones.habilitados()));

        contenido().add(pestanias, BorderLayout.CENTER);
    }

    /**
     * Las consideraciones no se dan de alta aqui: solo se habilitan o quitan
     * para el tipo de cliente elegido, como en la captura.
     */
    private JComponent asignarCondiciones(List<String> tiposCliente,
                                          List<String> existentes, List<String> habilitadas) {
        JPanel fila = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        fila.add(Ui.etiqueta("Tipo Cliente"), BorderLayout.WEST);
        fila.add(Ui.combo(tiposCliente), BorderLayout.CENTER);

        JPanel encabezado = Ui.panel(new BorderLayout());
        encabezado.add(fila, BorderLayout.WEST);
        encabezado.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        DefaultListModel<String> mExist = new DefaultListModel<>();
        DefaultListModel<String> mHab = new DefaultListModel<>();
        existentes.forEach(mExist::addElement);
        habilitadas.forEach(mHab::addElement);

        JList<String> listaExist = new JList<>(mExist);
        JList<String> listaHab = new JList<>(mHab);
        listaExist.setFont(Tema.cuerpo());
        listaHab.setFont(Tema.cuerpo());

        JButton habilitar = Ui.boton("Habilitar");
        habilitar.addActionListener(e -> {
            String v = listaExist.getSelectedValue();
            if (v == null) { avisar("Seleccione una consideracion existente."); return; }
            if (mHab.contains(v)) { avisar("\"" + v + "\" ya esta habilitada."); return; }
            mHab.addElement(v);
        });
        JButton deshabilitar = Ui.boton("Deshabilitar");
        deshabilitar.addActionListener(e -> {
            String v = listaHab.getSelectedValue();
            if (v == null) { avisar("Seleccione una consideracion habilitada."); return; }
            mHab.removeElement(v);
        });

        Grupo izq = new Grupo("Lista de Consideraciones Existentes");
        izq.add(Ui.scroll(listaExist), BorderLayout.CENTER);
        izq.add(centrado(habilitar), BorderLayout.EAST);

        Grupo der = new Grupo("Consideraciones habilitados");
        der.add(Ui.scroll(listaHab), BorderLayout.CENTER);
        der.add(centrado(deshabilitar), BorderLayout.EAST);

        JPanel listas = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        listas.add(izq);
        listas.add(der);

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(encabezado, BorderLayout.NORTH);
        p.add(listas, BorderLayout.CENTER);
        return p;
    }

    private static JComponent centrado(JComponent boton) {
        JPanel p = Ui.panel(new GridBagLayout());
        p.add(boton);
        p.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));
        return p;
    }
}
