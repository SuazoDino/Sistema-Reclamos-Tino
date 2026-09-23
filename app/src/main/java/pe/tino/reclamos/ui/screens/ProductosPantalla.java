package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.Formulario;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogo de Productos.
 *
 * Entrada : datos de los tipos de producto.
 * Funcion : registrar los productos clasificados en segmento, familia y clase.
 * Salida  : productos clasificados.
 */
public class ProductosPantalla extends Pantalla {

    private final JTree arbol;
    private final JTextField segmento = Ui.soloLectura();
    private final JTextField familia = Ui.soloLectura();
    private final JTextField clase = Ui.soloLectura();
    private final JTextField bien = Ui.soloLectura();

    public ProductosPantalla() {
        super("Catalogo de Productos",
                "Clasificacion en cuatro niveles: segmento, familia, clase y bien.");

        arbol = new JTree(construirModelo());
        arbol.setFont(Tema.cuerpo());
        arbol.setRowHeight(20);
        arbol.setShowsRootHandles(true);
        arbol.addTreeSelectionListener(e -> mostrarRuta());
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);

        Grupo gArbol = Grupo.ajustado("Jerarquia del catalogo");
        gArbol.add(Ui.scroll(arbol), BorderLayout.CENTER);

        contenido().add(gArbol, BorderLayout.CENTER);
        contenido().add(ficha(), BorderLayout.SOUTH);
    }

    private JComponent ficha() {
        Grupo g = new Grupo("Clasificacion del nodo seleccionado");
        Formulario f = new Formulario();
        f.campo("Segmento", segmento)
         .campo("Familia", familia)
         .campo("Clase", clase)
         .campo("Bien", bien);
        g.add(f, BorderLayout.CENTER);
        return g;
    }

    /** Agrupa la tabla plana del prototipo por sus tres primeros niveles. */
    private static DefaultTreeModel construirModelo() {
        Map<String, Map<String, Map<String, java.util.List<String>>>> jerarquia = new LinkedHashMap<>();
        for (String[] ruta : Datos.JERARQUIA_PRODUCTO) {
            jerarquia.computeIfAbsent(ruta[0], k -> new LinkedHashMap<>())
                     .computeIfAbsent(ruta[1], k -> new LinkedHashMap<>())
                     .computeIfAbsent(ruta[2], k -> new java.util.ArrayList<>())
                     .add(ruta[3]);
        }

        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("Catalogo de productos");
        jerarquia.forEach((seg, familias) -> {
            DefaultMutableTreeNode nSeg = new DefaultMutableTreeNode(seg);
            familias.forEach((fam, clases) -> {
                DefaultMutableTreeNode nFam = new DefaultMutableTreeNode(fam);
                clases.forEach((cla, bienes) -> {
                    DefaultMutableTreeNode nCla = new DefaultMutableTreeNode(cla);
                    bienes.forEach(b -> nCla.add(new DefaultMutableTreeNode(b)));
                    nFam.add(nCla);
                });
                nSeg.add(nFam);
            });
            raiz.add(nSeg);
        });
        return new DefaultTreeModel(raiz);
    }

    private void mostrarRuta() {
        TreePath ruta = arbol.getSelectionPath();
        JTextField[] campos = {segmento, familia, clase, bien};
        for (JTextField c : campos) c.setText("");
        if (ruta == null) return;

        Object[] nodos = ruta.getPath();
        for (int i = 1; i < nodos.length && i - 1 < campos.length; i++) {
            campos[i - 1].setText(String.valueOf(nodos[i]));
        }
    }
}
