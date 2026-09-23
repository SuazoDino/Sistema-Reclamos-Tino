package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Datos.Catalogo;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Mant-Param - Catalogos. Reproduce el patron dominante de los prototipos:
 * dos listas enfrentadas, "existentes" contra "habilitados", y el traspaso
 * de valores entre ambas. Un solo componente sirve a los once catalogos.
 */
public class CatalogoPantalla extends Pantalla {

    private final JComboBox<String> selector;
    private final DefaultListModel<String> modeloExistentes = new DefaultListModel<>();
    private final DefaultListModel<String> modeloHabilitados = new DefaultListModel<>();
    private final JList<String> existentes = new JList<>(modeloExistentes);
    private final JList<String> habilitados = new JList<>(modeloHabilitados);
    private final JLabel descripcion = Ui.suave("");
    private final JTextField nuevo = Ui.texto("Nombre del valor a agregar");

    public CatalogoPantalla() {
        super("Mant-Param", "Catalogos parametrizables",
                "Que valores existen en el sistema y cuales estan habilitados para usarse.");

        List<String> claves = List.copyOf(Datos.clavesCatalogo());
        selector = Ui.combo(claves.stream().map(c -> Datos.catalogo(c).titulo()).toList());
        selector.setPreferredSize(new Dimension(260, 30));
        selector.addActionListener(e -> cargar(claves.get(selector.getSelectedIndex())));

        acciones(Ui.etiqueta("Catalogo:"), selector);

        existentes.setFont(Tema.cuerpo());
        habilitados.setFont(Tema.cuerpo());
        existentes.setFixedCellHeight(26);
        habilitados.setFixedCellHeight(26);
        existentes.setBorder(Ui.relleno(Tema.ESP_SM));
        habilitados.setBorder(Ui.relleno(Tema.ESP_SM));

        contenido().add(panelPrincipal(), BorderLayout.CENTER);
        cargar(claves.get(0));
    }

    private JComponent panelPrincipal() {
        Tarjeta izq = new Tarjeta("Valores existentes", "Universo completo del catalogo.");
        izq.sinRelleno();
        izq.add(Ui.scroll(existentes), BorderLayout.CENTER);
        izq.cuerpo().add(pieAgregar(), BorderLayout.SOUTH);

        Tarjeta der = new Tarjeta("Valores habilitados", "Los que el aplicativo ofrece al operador.");
        der.sinRelleno();
        der.add(Ui.scroll(habilitados), BorderLayout.CENTER);
        der.cuerpo().add(pieQuitar(), BorderLayout.SOUTH);

        JButton habilitar = Ui.icono("flechaDer", "Habilitar el valor seleccionado");
        habilitar.addActionListener(e -> mover(existentes, modeloHabilitados, true));
        JButton deshabilitar = Ui.icono("flechaIzq", "Deshabilitar el valor seleccionado");
        deshabilitar.addActionListener(e -> mover(habilitados, modeloHabilitados, false));

        JPanel botonera = Ui.panel(new GridBagLayout());
        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(habilitar);
        columna.add(deshabilitar);
        botonera.add(columna);
        botonera.setBorder(Ui.relleno(0, Tema.ESP_SM, 0, Tema.ESP_SM));

        // las dos listas ocupan mitad y mitad; la botonera queda al ancho que pide
        JPanel listas = Ui.panel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 1; listas.add(izq, gc);
        gc.gridx = 1; gc.weightx = 0; listas.add(botonera, gc);
        gc.gridx = 2; gc.weightx = 1; listas.add(der, gc);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        Tarjeta cabecera = new Tarjeta();
        cabecera.relleno(Tema.ESP_MD);
        cabecera.add(descripcion, BorderLayout.CENTER);
        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(listas, BorderLayout.CENTER);
        return raiz;
    }

    private JComponent pieAgregar() {
        JButton agregar = Ui.secundario("Agregar", "mas");
        agregar.addActionListener(e -> {
            String v = nuevo.getText().trim();
            if (v.isEmpty()) { avisar("Escriba el nombre del valor a agregar."); return; }
            if (modeloExistentes.contains(v)) { avisar("Ese valor ya existe en el catalogo."); return; }
            modeloExistentes.addElement(v);
            nuevo.setText("");
            existentes.setSelectedValue(v, true);
        });
        nuevo.addActionListener(e -> agregar.doClick());

        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.borde()),
                Ui.relleno(Tema.ESP_SM + 2)));
        p.add(nuevo, BorderLayout.CENTER);
        p.add(agregar, BorderLayout.EAST);
        return p;
    }

    private JComponent pieQuitar() {
        JLabel conteo = Ui.micro("");
        modeloHabilitados.addListDataListener(new javax.swing.event.ListDataListener() {
            @Override public void intervalAdded(javax.swing.event.ListDataEvent e)   { actualizar(); }
            @Override public void intervalRemoved(javax.swing.event.ListDataEvent e) { actualizar(); }
            @Override public void contentsChanged(javax.swing.event.ListDataEvent e) { actualizar(); }
            private void actualizar() {
                conteo.setText(modeloHabilitados.size() + " habilitados de "
                        + modeloExistentes.size() + " existentes");
            }
        });

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.borde()),
                Ui.relleno(Tema.ESP_SM + 6)));
        p.add(conteo, BorderLayout.WEST);
        return p;
    }

    private void mover(JList<String> origen, DefaultListModel<String> destino, boolean habilitando) {
        String valor = origen.getSelectedValue();
        if (valor == null) {
            avisar(habilitando ? "Seleccione un valor existente para habilitarlo."
                               : "Seleccione un valor habilitado para quitarlo.");
            return;
        }
        if (habilitando) {
            if (destino.contains(valor)) { avisar("\"" + valor + "\" ya esta habilitado."); return; }
            destino.addElement(valor);
        } else {
            destino.removeElement(valor);
        }
    }

    private void cargar(String clave) {
        Catalogo c = Datos.catalogo(clave);
        descripcion.setText("<html><b>" + c.titulo() + "</b> — " + c.descripcion() + "</html>");
        modeloExistentes.clear();
        modeloHabilitados.clear();
        c.existentes().forEach(modeloExistentes::addElement);
        c.habilitados().forEach(modeloHabilitados::addElement);
    }
}
