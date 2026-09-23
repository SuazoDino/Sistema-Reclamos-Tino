package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogo tabular con alta, edicion y baja. Un mismo componente sirve al
 * catalogo de bienes, al de problemas y al de politicas: cambian las columnas
 * y los valores que ofrece cada campo, no la mecanica.
 */
public class CatalogoTablaPantalla extends Pantalla {

    /** Un campo del formulario: su etiqueta y, si es cerrado, sus valores. */
    public record Campo(String etiqueta, List<String> valores) {
        public static Campo libre(String etiqueta)           { return new Campo(etiqueta, null); }
        public static Campo lista(String etiqueta, List<String> v) { return new Campo(etiqueta, v); }
        public boolean esCerrado() { return valores != null; }
    }

    private final List<Campo> campos;
    private final List<JComponent> controles = new ArrayList<>();
    private final List<String[]> filas;
    private final Tabla tabla;
    private final int columnaEstado;

    public CatalogoTablaPantalla(String ruta, String titulo, String descripcion,
                                 List<Campo> campos, List<String[]> filas,
                                 int[] anchos, int columnaEstado) {
        super(ruta, titulo, descripcion);
        this.campos = campos;
        this.filas = filas;
        this.columnaEstado = columnaEstado;

        tabla = new Tabla(campos.stream().map(Campo::etiqueta).toArray(String[]::new));
        tabla.anchos(anchos);
        if (columnaEstado >= 0) tabla.columnaEstado(columnaEstado, Estados::color);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Tarjeta listado = new Tarjeta(titulo, filas.size() + " registros en el catalogo.");
        listado.sinRelleno();
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(listado, BorderLayout.CENTER);
        contenido().add(editor(), BorderLayout.EAST);

        refrescar();
    }

    private JComponent editor() {
        Tarjeta t = new Tarjeta("Registro del catalogo",
                "Alta de un valor nuevo o edicion del seleccionado.");
        t.setPreferredSize(new Dimension(380, 100));

        Formulario f = new Formulario();
        for (Campo c : campos) {
            JComponent control = c.esCerrado() ? Ui.combo(c.valores()) : Ui.texto("");
            controles.add(control);
            f.campo(c.etiqueta(), control);
        }
        f.finalizar();

        JButton guardar = Ui.primario("Guardar", "guardar");
        guardar.addActionListener(e -> guardar());
        JButton nuevo = Ui.secundario("Nuevo", "mas");
        nuevo.addActionListener(e -> limpiar());
        JButton eliminar = Ui.plano("Eliminar", "cruz");
        eliminar.addActionListener(e -> eliminar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        pie.add(eliminar, BorderLayout.WEST);
        pie.add(Ui.filaDerecha(Tema.ESP_SM, nuevo, guardar), BorderLayout.EAST);

        JPanel cuerpo = Ui.panel(new BorderLayout());
        cuerpo.add(f, BorderLayout.CENTER);
        cuerpo.add(pie, BorderLayout.SOUTH);
        t.add(Ui.scrollVertical(cuerpo), BorderLayout.CENTER);
        return t;
    }

    private void refrescar() {
        tabla.limpiar();
        filas.forEach(fila -> tabla.agregar((Object[]) fila));
    }

    private int indiceSeleccionado() {
        int f = tabla.getSelectedRow();
        return f < 0 ? -1 : tabla.convertRowIndexToModel(f);
    }

    private void cargarSeleccion() {
        int i = indiceSeleccionado();
        if (i < 0) return;
        String[] fila = filas.get(i);
        for (int c = 0; c < controles.size() && c < fila.length; c++) {
            fijar(controles.get(c), fila[c]);
        }
    }

    private static void fijar(JComponent control, String valor) {
        if (control instanceof JTextField t) t.setText(valor);
        else if (control instanceof JComboBox<?> c) c.setSelectedItem(valor);
    }

    private static String leer(JComponent control) {
        if (control instanceof JTextField t) return t.getText().trim();
        if (control instanceof JComboBox<?> c) return String.valueOf(c.getSelectedItem());
        return "";
    }

    private void guardar() {
        String[] fila = new String[campos.size()];
        for (int c = 0; c < controles.size(); c++) fila[c] = leer(controles.get(c));

        if (fila[0].isBlank()) {
            avisar("El campo \"" + campos.get(0).etiqueta() + "\" es obligatorio.");
            return;
        }
        int i = indiceSeleccionado();
        if (i < 0) filas.add(fila); else filas.set(i, fila);
        refrescar();
        avisar(i < 0 ? "Registro agregado al catalogo." : "Registro actualizado.");
    }

    private void eliminar() {
        int i = indiceSeleccionado();
        if (i < 0) { avisar("Seleccione el registro que desea eliminar."); return; }
        if (!confirmar("Eliminar el registro seleccionado del catalogo?")) return;
        filas.remove(i);
        refrescar();
        limpiar();
    }

    private void limpiar() {
        tabla.clearSelection();
        for (JComponent c : controles) {
            if (c instanceof JTextField t) t.setText("");
            else if (c instanceof JComboBox<?> combo) combo.setSelectedIndex(0);
        }
    }

    /* ---------------- catalogos concretos ---------------- */

    public static CatalogoTablaPantalla bienes() {
        return new CatalogoTablaPantalla("Mant-Param › Catalogos", "Catalogo de bienes",
                "Tipos de bien reconocidos y el tipo de problema que cada uno admite.",
                List.of(Campo.lista("Tipo de bien", Datos.TIPOS_BIEN),
                        Campo.lista("Tipo de problema", Datos.TIPOS_PROBLEMA_CAT),
                        Campo.lista("Estado", List.of("Habilitado", "Deshabilitado"))),
                Datos.catalogoBienes(), new int[]{260, 220, 150}, 2);
    }

    public static CatalogoTablaPantalla problemas() {
        return new CatalogoTablaPantalla("Mant-Param › Catalogos", "Catalogo de problemas",
                "Problemas reconocidos por el sistema, clasificados por segmento y familia.",
                List.of(Campo.lista("Segmento", Datos.SEGMENTOS),
                        Campo.lista("Familia", Datos.FAMILIAS),
                        Campo.lista("Tipo de problema", Datos.TIPOS_PROBLEMA_CAT),
                        Campo.libre("Problema")),
                Datos.catalogoProblemas(), new int[]{280, 230, 160, 180}, -1);
    }

    public static CatalogoTablaPantalla politicas() {
        return new CatalogoTablaPantalla("Mant-Param › Catalogos", "Catalogo de politicas",
                "Garantias legales, explicitas e implicitas que amparan un reclamo.",
                List.of(Campo.libre("Codigo"),
                        Campo.lista("Tipo de garantia", Datos.TIPOS_GARANTIA),
                        Campo.libre("Politica"),
                        Campo.lista("Plazo", Datos.PLAZOS),
                        Campo.lista("Estado", List.of("Habilitado", "Deshabilitado"))),
                Datos.catalogoPoliticas(), new int[]{90, 140, 300, 110, 140}, 4);
    }
}
