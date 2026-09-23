package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Datos.Catalogo;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Parametros Generales.
 *
 * Entrada : datos generales de la empresa y del sistema.
 * Funcion : registrar generalidades de la empresa y del sistema.
 * Salida  : areas identificadas, magnitud e idioma del sistema.
 *
 * Cada parametro se administra con el mismo mecanismo del prototipo: una
 * lista de valores existentes y otra de valores habilitados.
 */
public class ParametrosPantalla extends Pantalla {

    private final JComboBox<String> selector;
    private final DefaultListModel<String> existentes = new DefaultListModel<>();
    private final DefaultListModel<String> habilitados = new DefaultListModel<>();
    private final JList<String> listaExistentes = new JList<>(existentes);
    private final JList<String> listaHabilitados = new JList<>(habilitados);
    private final JTextField nuevo = Ui.texto(18);
    private final JLabel conteo = Ui.suave("");

    public ParametrosPantalla() {
        super("Parametros Generales",
                "Generalidades de la empresa y del sistema: areas, magnitud, sectores, locales e idiomas.");

        List<String> claves = List.copyOf(Datos.clavesCatalogo());
        selector = Ui.combo(claves.stream().map(c -> Datos.catalogo(c).titulo()).toList());
        selector.setPreferredSize(new Dimension(230, selector.getPreferredSize().height));
        selector.addActionListener(e -> cargar(claves.get(selector.getSelectedIndex())));

        listaExistentes.setFont(Tema.cuerpo());
        listaHabilitados.setFont(Tema.cuerpo());

        JPanel seleccion = Ui.panel(new BorderLayout());
        seleccion.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));
        seleccion.add(Ui.fila(Ui.etiqueta("Parametro:"), selector), BorderLayout.WEST);

        contenido().add(seleccion, BorderLayout.NORTH);
        contenido().add(listas(), BorderLayout.CENTER);

        cargar(claves.get(0));
    }

    private JComponent listas() {
        Grupo izq = Grupo.ajustado("Valores existentes");
        izq.add(Ui.scroll(listaExistentes), BorderLayout.CENTER);
        izq.add(pieAgregar(), BorderLayout.SOUTH);

        Grupo der = Grupo.ajustado("Valores habilitados");
        der.add(Ui.scroll(listaHabilitados), BorderLayout.CENTER);
        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM));
        pie.add(conteo, BorderLayout.WEST);
        der.add(pie, BorderLayout.SOUTH);

        JButton habilitar = Ui.boton("Habilitar  >");
        habilitar.addActionListener(e -> habilitar());
        JButton quitar = Ui.boton("<  Quitar");
        quitar.addActionListener(e -> quitar());

        JPanel botones = Ui.panel(new GridBagLayout());
        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(habilitar);
        columna.add(quitar);
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, Tema.ESP_SM, 0, Tema.ESP_SM));

        JPanel p = Ui.panel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 1; p.add(izq, gc);
        gc.gridx = 1; gc.weightx = 0; p.add(botones, gc);
        gc.gridx = 2; gc.weightx = 1; p.add(der, gc);
        return p;
    }

    private JComponent pieAgregar() {
        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> agregar());
        nuevo.addActionListener(e -> agregar());

        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        p.setBorder(Ui.relleno(Tema.ESP_SM));
        p.add(Ui.etiqueta("Nuevo valor:"), BorderLayout.WEST);
        p.add(nuevo, BorderLayout.CENTER);
        p.add(agregar, BorderLayout.EAST);
        return p;
    }

    private void agregar() {
        String v = nuevo.getText().trim();
        if (v.isEmpty()) { avisar("Escriba el valor que desea agregar."); return; }
        if (existentes.contains(v)) { avisar("Ese valor ya existe."); return; }
        existentes.addElement(v);
        nuevo.setText("");
        listaExistentes.setSelectedValue(v, true);
        actualizarConteo();
    }

    private void habilitar() {
        String v = listaExistentes.getSelectedValue();
        if (v == null) { avisar("Seleccione un valor existente."); return; }
        if (habilitados.contains(v)) { avisar("\"" + v + "\" ya esta habilitado."); return; }
        habilitados.addElement(v);
        actualizarConteo();
    }

    private void quitar() {
        String v = listaHabilitados.getSelectedValue();
        if (v == null) { avisar("Seleccione un valor habilitado."); return; }
        habilitados.removeElement(v);
        actualizarConteo();
    }

    private void actualizarConteo() {
        conteo.setText(habilitados.size() + " habilitados de " + existentes.size() + " existentes");
    }

    private void cargar(String clave) {
        Catalogo c = Datos.catalogo(clave);
        existentes.clear();
        habilitados.clear();
        c.existentes().forEach(existentes::addElement);
        c.habilitados().forEach(habilitados::addElement);
        actualizarConteo();
    }
}
