package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mant-Param - Catalogo de productos. Muestra la jerarquia de cuatro niveles
 * del prototipo (Segmento > Familia > Clase > Bien) como arbol navegable,
 * con la ruta completa del nodo seleccionado al costado.
 */
public class ProductosPantalla extends Pantalla {

    private final JTree arbol;
    private final JPanel ficha = Ui.panel(new BorderLayout());
    private final JTextField filtro = Ui.busqueda("Filtrar por bien, clase o familia");

    public ProductosPantalla() {
        super("Mant-Param", "Catalogo de productos",
                "Jerarquia Segmento › Familia › Clase › Bien sobre la que se clasifica cada reclamo.");

        arbol = new JTree(construirModelo(""));
        arbol.setRootVisible(true);
        arbol.setShowsRootHandles(true);
        arbol.setFont(Tema.cuerpo());
        arbol.setRowHeight(26);
        arbol.setBorder(Ui.relleno(Tema.ESP_SM));
        arbol.addTreeSelectionListener(e -> mostrarFicha());
        expandirTodo();

        filtro.setPreferredSize(new Dimension(280, 30));
        filtro.addActionListener(e -> aplicarFiltro());
        JButton buscar = Ui.secundario("Filtrar", "lupa");
        buscar.addActionListener(e -> aplicarFiltro());
        acciones(filtro, buscar);

        Tarjeta t = new Tarjeta("Jerarquia del catalogo",
                Datos.JERARQUIA_PRODUCTO.size() + " bienes registrados en 4 niveles.");
        t.sinRelleno();
        t.add(Ui.scroll(arbol), BorderLayout.CENTER);

        ficha.setPreferredSize(new Dimension(340, 100));

        contenido().add(t, BorderLayout.CENTER);
        contenido().add(ficha, BorderLayout.EAST);
        mostrarFicha();
    }

    /** Arma el arbol agrupando la tabla plana del prototipo por sus tres primeros niveles. */
    private DefaultTreeModel construirModelo(String filtroTexto) {
        String q = filtroTexto.trim().toLowerCase();
        Map<String, Map<String, Map<String, java.util.List<String>>>> jerarquia = new LinkedHashMap<>();

        for (String[] ruta : Datos.JERARQUIA_PRODUCTO) {
            if (!q.isEmpty() && !String.join(" ", ruta).toLowerCase().contains(q)) continue;
            jerarquia
                    .computeIfAbsent(ruta[0], k -> new LinkedHashMap<>())
                    .computeIfAbsent(ruta[1], k -> new LinkedHashMap<>())
                    .computeIfAbsent(ruta[2], k -> new java.util.ArrayList<>())
                    .add(ruta[3]);
        }

        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("Catalogo de productos");
        jerarquia.forEach((segmento, familias) -> {
            DefaultMutableTreeNode nSeg = new DefaultMutableTreeNode(segmento);
            familias.forEach((familia, clases) -> {
                DefaultMutableTreeNode nFam = new DefaultMutableTreeNode(familia);
                clases.forEach((clase, bienes) -> {
                    DefaultMutableTreeNode nCla = new DefaultMutableTreeNode(clase);
                    bienes.forEach(b -> nCla.add(new DefaultMutableTreeNode(b)));
                    nFam.add(nCla);
                });
                nSeg.add(nFam);
            });
            raiz.add(nSeg);
        });
        return new DefaultTreeModel(raiz);
    }

    private void aplicarFiltro() {
        arbol.setModel(construirModelo(filtro.getText()));
        expandirTodo();
        mostrarFicha();
    }

    private void expandirTodo() {
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);
    }

    private void mostrarFicha() {
        ficha.removeAll();
        TreePath ruta = arbol.getSelectionPath();

        if (ruta == null) {
            ficha.add(new Tarjeta("Nodo seleccionado",
                    "Elija un nodo del arbol para ver su ruta completa."), BorderLayout.CENTER);
        } else {
            Object[] nodos = ruta.getPath();
            String[] niveles = {"Raiz", "Segmento", "Familia", "Clase", "Bien"};

            Tarjeta t = new Tarjeta(String.valueOf(nodos[nodos.length - 1]),
                    "Nivel: " + niveles[Math.min(nodos.length - 1, niveles.length - 1)]);

            JPanel campos = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_SM));
            for (int i = 1; i < nodos.length; i++) {
                JPanel p = Ui.panel(new BorderLayout(0, 2));
                p.add(Ui.micro(niveles[i].toUpperCase()), BorderLayout.NORTH);
                JLabel v = Ui.etiqueta("<html>" + nodos[i] + "</html>");
                v.setFont(Tema.fuente(12, Font.BOLD));
                p.add(v, BorderLayout.CENTER);
                campos.add(p);
            }
            JPanel envoltura = Ui.panel(new BorderLayout());
            envoltura.add(campos, BorderLayout.NORTH);
            t.add(Ui.scrollVertical(envoltura), BorderLayout.CENTER);
            ficha.add(t, BorderLayout.CENTER);
        }
        ficha.revalidate();
        ficha.repaint();
    }
}
