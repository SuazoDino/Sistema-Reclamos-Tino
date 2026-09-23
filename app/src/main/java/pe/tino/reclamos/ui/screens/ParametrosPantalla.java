package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Parametro General, como en la captura del informe: dos bloques grandes,
 * "Generalidades Empresa" con Areas y Especialistas por Area, y
 * "Generalidades Sistema" con Magnitud e Idioma.
 */
public class ParametrosPantalla extends Pantalla {

    public ParametrosPantalla() {
        super("General", "Generalidades de la empresa y del sistema.");

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(generalidadesEmpresa(), BorderLayout.NORTH);
        raiz.add(generalidadesSistema(), BorderLayout.CENTER);
        contenido().add(raiz, BorderLayout.CENTER);
    }

    /* ---------------- generalidades de la empresa ---------------- */

    private JComponent generalidadesEmpresa() {
        Grupo g = new Grupo("Generalidades Empresa", new GridLayout(1, 2, Tema.ESP_MD, 0));
        g.add(areas());
        g.add(especialistas());
        g.setPreferredSize(new Dimension(100, 250));
        return g;
    }

    private JComponent areas() {
        Grupo g = new Grupo("Areas");
        var cat = Datos.catalogo("areas");
        g.add(alta("Ingresar Area:", Ui.texto(14)), BorderLayout.NORTH);
        g.add(dobleLista("Areas Existentes", cat.existentes(),
                "Areas Habilitadas", cat.habilitados()), BorderLayout.CENTER);
        return g;
    }

    private JComponent especialistas() {
        Grupo g = new Grupo("Especialistas Areas");
        var areas = Datos.catalogo("areas").habilitados();
        var empleados = Datos.catalogo("empleados");

        JPanel alta = Ui.panel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(Tema.ESP_XS, 0, Tema.ESP_XS, Tema.ESP_SM);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.LINE_START;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        alta.add(Ui.etiqueta("Area:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        alta.add(Ui.combo(areas), gc);
        gc.gridx = 2; gc.weightx = 0;
        alta.add(Ui.boton("Ingresar"), gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        alta.add(Ui.etiqueta("Ingresar Especialista:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        alta.add(Ui.texto(10), gc);

        alta.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));

        g.add(alta, BorderLayout.NORTH);
        g.add(dobleLista("Especialistas Existentes", empleados.existentes(),
                "Especialistas Habilitados", empleados.habilitados()), BorderLayout.CENTER);
        return g;
    }

    /* ---------------- generalidades del sistema ---------------- */

    private JComponent generalidadesSistema() {
        Grupo g = new Grupo("Generalidades Sistema", new GridLayout(1, 2, Tema.ESP_MD, 0));
        g.add(magnitud());
        g.add(idioma());
        return g;
    }

    private JComponent magnitud() {
        Grupo g = new Grupo("Magnitud");

        JPanel alta = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        alta.add(Ui.etiqueta("Ingresar Magnitud:"), BorderLayout.WEST);
        alta.add(Ui.combo(Prototipo.MAGNITUDES), BorderLayout.CENTER);
        alta.add(Ui.boton("Ingresar"), BorderLayout.EAST);
        alta.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        DefaultListModel<String> modelo = new DefaultListModel<>();
        Prototipo.MAGNITUD_EXISTENTE.forEach(modelo::addElement);
        JList<String> lista = new JList<>(modelo);
        lista.setFont(Tema.cuerpo());

        JButton eliminar = Ui.boton("Eliminar");
        eliminar.addActionListener(e -> {
            String v = lista.getSelectedValue();
            if (v == null) { avisar("Seleccione la magnitud que desea eliminar."); return; }
            if (confirmar("Eliminar la magnitud \"" + v + "\"?")) modelo.removeElement(v);
        });

        JPanel botones = Ui.panel(new GridBagLayout());
        botones.add(eliminar);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        JPanel centro = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        centro.add(Ui.etiqueta("Magnitud Existente"), BorderLayout.NORTH);
        centro.add(Ui.scroll(lista), BorderLayout.CENTER);
        centro.add(botones, BorderLayout.EAST);

        g.add(alta, BorderLayout.NORTH);
        g.add(centro, BorderLayout.CENTER);
        return g;
    }

    private JComponent idioma() {
        Grupo g = new Grupo("Idioma");
        g.add(alta("Ingresar Idioma:", Ui.texto(12)), BorderLayout.NORTH);
        g.add(dobleLista("Idiomas Existentes", Prototipo.IDIOMAS_EXISTENTES,
                "Idiomas Habilitadas", Prototipo.IDIOMAS_HABILITADOS), BorderLayout.CENTER);
        return g;
    }

    /* ---------------- piezas comunes ---------------- */

    /** Campo de alta con su boton Ingresar, como en la captura. */
    private JComponent alta(String etiqueta, JTextField campo) {
        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        p.add(Ui.etiqueta(etiqueta), BorderLayout.WEST);
        p.add(campo, BorderLayout.CENTER);
        p.add(Ui.boton("Ingresar"), BorderLayout.EAST);
        p.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));
        return p;
    }

    /**
     * Las dos listas enfrentadas con los botones ">" y "<" en el medio, que es
     * como el prototipo mueve un valor de existente a habilitado.
     */
    private JComponent dobleLista(String tituloIzq, List<String> valoresIzq,
                                  String tituloDer, List<String> valoresDer) {
        DefaultListModel<String> mIzq = new DefaultListModel<>();
        DefaultListModel<String> mDer = new DefaultListModel<>();
        valoresIzq.forEach(mIzq::addElement);
        valoresDer.forEach(mDer::addElement);

        JList<String> izq = new JList<>(mIzq);
        JList<String> der = new JList<>(mDer);
        izq.setFont(Tema.cuerpo());
        der.setFont(Tema.cuerpo());

        JButton pasar = Ui.boton(">");
        pasar.addActionListener(e -> {
            String v = izq.getSelectedValue();
            if (v == null) { avisar("Seleccione un valor de la lista de existentes."); return; }
            if (mDer.contains(v)) { avisar("\"" + v + "\" ya esta habilitado."); return; }
            mDer.addElement(v);
        });
        JButton quitar = Ui.boton("<");
        quitar.addActionListener(e -> {
            String v = der.getSelectedValue();
            if (v == null) { avisar("Seleccione un valor habilitado."); return; }
            mDer.removeElement(v);
        });

        JPanel botones = Ui.panel(new GridBagLayout());
        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(pasar);
        columna.add(quitar);
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, Tema.ESP_SM, 0, Tema.ESP_SM));

        JPanel pIzq = Ui.panel(new BorderLayout(0, Tema.ESP_XS));
        pIzq.add(Ui.etiqueta(tituloIzq), BorderLayout.NORTH);
        pIzq.add(Ui.scroll(izq), BorderLayout.CENTER);

        JPanel pDer = Ui.panel(new BorderLayout(0, Tema.ESP_XS));
        pDer.add(Ui.etiqueta(tituloDer), BorderLayout.NORTH);
        pDer.add(Ui.scroll(der), BorderLayout.CENTER);

        JPanel p = Ui.panel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 1; p.add(pIzq, gc);
        gc.gridx = 1; gc.weightx = 0; p.add(botones, gc);
        gc.gridx = 2; gc.weightx = 1; p.add(pDer, gc);
        return p;
    }
}
