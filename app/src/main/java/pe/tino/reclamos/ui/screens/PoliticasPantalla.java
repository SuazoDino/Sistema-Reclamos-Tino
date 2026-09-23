package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogo de Politicas, como en la captura: una politica es el cruce de dos
 * catalogos (Producto con Garantia, Reclamo con Canal). Arriba se arma el
 * cruce y abajo se habilitan o deshabilitan sus tipos.
 */
public class PoliticasPantalla extends Pantalla {

    private final JComboBox<String> catalogo1 = Ui.combo(Prototipo.CATALOGOS);
    private final JComboBox<String> catalogo2 = Ui.combo(Prototipo.CATALOGOS);
    private final Tabla politicas = new Tabla(new String[]{"Catalogo1", "Catalogo2"});
    private final Tabla tipos = new Tabla(new String[]{"Producto", "Garantia", "Plazo (dias)", "Estado"});
    private final JComboBox<String> estado;
    private final List<String[]> filasPolitica = Prototipo.politicas();
    private final List<String[]> filasTipo = Prototipo.tiposDePolitica();

    public PoliticasPantalla() {
        super("Catalogo de Politicas",
                "Una politica cruza dos catalogos; abajo se habilitan sus tipos.");

        List<String> estados = new ArrayList<>(List.of(""));
        estados.addAll(Prototipo.ESTADO);
        estado = Ui.combo(estados);
        estado.addActionListener(e -> refrescarTipos());

        politicas.anchos(240, 240);
        filasPolitica.forEach(f -> politicas.agregar((Object[]) f));

        tipos.anchos(190, 190, 150, 170).centrar(2);
        refrescarTipos();

        JPanel arriba = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        arriba.add(grupoAgregar(), BorderLayout.WEST);
        arriba.add(grupoPoliticas(), BorderLayout.CENTER);
        arriba.setPreferredSize(Ui.dim(100, 200));

        contenido().add(arriba, BorderLayout.NORTH);
        contenido().add(grupoTipos(), BorderLayout.CENTER);
    }

    private JComponent grupoAgregar() {
        Grupo g = new Grupo("Agregar");
        g.setPreferredSize(Ui.dim(320, 100));

        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> {
            String c1 = String.valueOf(catalogo1.getSelectedItem());
            String c2 = String.valueOf(catalogo2.getSelectedItem());
            if (c1.equals(c2)) { avisar("Una politica cruza dos catalogos distintos."); return; }
            filasPolitica.add(new String[]{c1, c2});
            politicas.agregar(c1, c2);
        });

        Formulario f = new Formulario();
        f.campo("Catalogo 1:", catalogo1)
         .campo("Catalogo 2:", catalogo2);

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_SM));
        pie.add(agregar);

        g.add(f, BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        return g;
    }

    private JComponent grupoPoliticas() {
        JButton verTipos = Ui.boton("Tipos");
        verTipos.addActionListener(e -> {
            if (politicas.filaModelo() < 0) { avisar("Seleccione una politica."); return; }
            refrescarTipos();
        });
        JButton eliminar = Ui.boton("Eliminar");
        eliminar.addActionListener(e -> {
            int i = politicas.filaModelo();
            if (i < 0) { avisar("Seleccione la politica que desea eliminar."); return; }
            if (!confirmar("Eliminar la politica seleccionada?")) return;
            filasPolitica.remove(i);
            politicas.limpiar();
            filasPolitica.forEach(f -> politicas.agregar((Object[]) f));
        });

        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(verTipos);
        columna.add(eliminar);
        JPanel botones = Ui.panel(new GridBagLayout());
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        Grupo g = new Grupo("Politicas");
        g.add(politicas.enScroll(), BorderLayout.CENTER);
        g.add(botones, BorderLayout.EAST);
        return g;
    }

    private JComponent grupoTipos() {
        JButton habilitar = Ui.boton("Habilitar");
        habilitar.addActionListener(e -> cambiarEstado("Habilitado"));
        JButton deshabilitar = Ui.boton("Deshabilitar");
        deshabilitar.addActionListener(e -> cambiarEstado("Deshabilitado"));

        JPanel columna = Ui.panel(new GridLayout(2, 1, 0, Tema.ESP_SM));
        columna.add(habilitar);
        columna.add(deshabilitar);
        JPanel botones = Ui.panel(new GridBagLayout());
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        JPanel filtro = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        JPanel izq = Ui.panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_SM, 0));
        izq.add(Ui.etiqueta("Estado:"));
        izq.add(estado);
        filtro.add(izq, BorderLayout.WEST);
        filtro.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));

        Grupo g = new Grupo("Tipos de Politicas");
        g.add(filtro, BorderLayout.NORTH);
        g.add(tipos.enScroll(), BorderLayout.CENTER);
        g.add(botones, BorderLayout.EAST);
        return g;
    }

    private void cambiarEstado(String nuevo) {
        int i = tipos.filaModelo();
        if (i < 0) { avisar("Seleccione un tipo de politica."); return; }
        String producto = String.valueOf(tipos.modelo().getValueAt(i, 0));
        String garantia = String.valueOf(tipos.modelo().getValueAt(i, 1));
        filasTipo.stream()
                .filter(f -> f[0].equals(producto) && f[1].equals(garantia))
                .forEach(f -> f[3] = nuevo);
        refrescarTipos();
    }

    private void refrescarTipos() {
        String filtro = estado == null ? "" : String.valueOf(estado.getSelectedItem());
        tipos.limpiar();
        filasTipo.stream()
                .filter(f -> filtro == null || filtro.isBlank() || filtro.equals(f[3]))
                .forEach(f -> tipos.agregar((Object[]) f));
    }
}
