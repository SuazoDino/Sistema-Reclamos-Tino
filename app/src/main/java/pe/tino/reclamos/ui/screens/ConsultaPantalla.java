package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.*;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modulo de Consulta: busqueda libre mas filtros por estado y area, con el
 * detalle del reclamo seleccionado al costado.
 */
public class ConsultaPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TODOS = "Todos";

    private final JTextField busqueda = Ui.busqueda("Buscar por codigo, cliente, producto o problema");
    private final JComboBox<String> filtroEstado;
    private final JComboBox<String> filtroArea;
    private final Tabla tabla = new Tabla(new String[]{
            "Reclamo", "Emision", "Cliente", "Producto", "Problema", "Area", "Estado"});
    private final JLabel contador = Ui.micro("");
    private final JPanel detalle = Ui.panel(new BorderLayout());

    private List<Reclamo> visibles = new ArrayList<>();

    public ConsultaPantalla() {
        super("Aplicativo - Consulta", "Consulta de reclamos",
                "Seguimiento del estado de cada reclamo registrado.");

        List<String> estados = new ArrayList<>(List.of(TODOS));
        for (EstadoReclamo e : EstadoReclamo.values()) estados.add(e.etiqueta());
        filtroEstado = Ui.combo(estados);

        List<String> areas = new ArrayList<>(List.of(TODOS));
        areas.addAll(Datos.catalogo("areas").habilitados());
        filtroArea = Ui.combo(areas);

        busqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { refrescar(); }
            @Override public void removeUpdate(DocumentEvent e)  { refrescar(); }
            @Override public void changedUpdate(DocumentEvent e) { refrescar(); }
        });
        filtroEstado.addActionListener(e -> refrescar());
        filtroArea.addActionListener(e -> refrescar());

        JButton limpiar = Ui.plano("Limpiar filtros", "cruz");
        limpiar.addActionListener(e -> {
            busqueda.setText("");
            filtroEstado.setSelectedIndex(0);
            filtroArea.setSelectedIndex(0);
        });

        busqueda.setPreferredSize(new Dimension(300, 30));
        filtroEstado.setPreferredSize(new Dimension(150, 30));
        filtroArea.setPreferredSize(new Dimension(230, 30));

        Tarjeta filtros = new Tarjeta();
        filtros.relleno(Tema.ESP_MD);
        filtros.add(Ui.fila(Tema.ESP_SM,
                busqueda,
                etiquetaFiltro("Estado"), filtroEstado,
                etiquetaFiltro("Area"), filtroArea,
                limpiar), BorderLayout.WEST);

        tabla.anchos(70, 100, 150, 135, 145, 145, 120);
        tabla.columnaEstado(6, Estados::color);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarDetalle();
        });

        Tarjeta listado = new Tarjeta("Reclamos", "Ordena haciendo clic en el encabezado de la columna.");
        listado.acciones(contador);
        listado.sinRelleno();
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        detalle.setPreferredSize(new Dimension(330, 100));

        JPanel centro = Ui.panel(new BorderLayout(Tema.ESP_MD, Tema.ESP_MD));
        centro.add(listado, BorderLayout.CENTER);
        centro.add(detalle, BorderLayout.EAST);

        contenido().add(filtros, BorderLayout.NORTH);
        contenido().add(centro, BorderLayout.CENTER);

        refrescar();
        Estado.alCambiarReclamos(r -> refrescar());
    }

    private JLabel etiquetaFiltro(String texto) {
        JLabel l = Ui.micro(texto);
        l.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_SM));
        return l;
    }

    private void refrescar() {
        String q = busqueda.getText().trim().toLowerCase();
        String est = (String) filtroEstado.getSelectedItem();
        String are = (String) filtroArea.getSelectedItem();

        visibles = Estado.reclamos().stream()
                .filter(r -> est == null || TODOS.equals(est) || r.estado().etiqueta().equals(est))
                .filter(r -> are == null || TODOS.equals(are) || r.area().equals(are))
                .filter(r -> q.isEmpty() || textoDe(r).contains(q))
                .toList();

        tabla.limpiar();
        for (Reclamo r : visibles) {
            tabla.agregar(r.id(), r.fechaEmision().format(FECHA), r.cliente().nombre(),
                    r.producto().bien(), r.problema().descripcion(), r.area(),
                    r.estado().etiqueta());
        }
        contador.setText(visibles.size() + " de " + Estado.reclamos().size() + " reclamos");
        mostrarDetalle();
    }

    private static String textoDe(Reclamo r) {
        return (r.id() + " " + r.cliente().nombre() + " " + r.cliente().documento() + " "
                + r.producto().bien() + " " + r.producto().marca() + " "
                + r.problema().descripcion() + " " + r.area() + " " + r.nroCompra()).toLowerCase();
    }

    private void mostrarDetalle() {
        detalle.removeAll();
        int fila = tabla.getSelectedRow();

        if (fila < 0 || fila >= visibles.size()) {
            Tarjeta vacia = new Tarjeta("Detalle del reclamo",
                    "Seleccione una fila para ver la ficha completa.");
            detalle.add(vacia, BorderLayout.CENTER);
        } else {
            Reclamo r = visibles.get(tabla.convertRowIndexToModel(fila));
            Tarjeta t = new Tarjeta("Reclamo " + r.id(),
                    r.fechaEmision().format(FECHA) + " · canal " + r.canal().name().toLowerCase());

            JPanel campos = Ui.panel(new GridLayout(0, 1, 0, 2));
            campos.add(Ui.chip(r.estado().etiqueta(), Estados.color(r.estado())));
            campos.add(dato("Cliente", r.cliente().nombre()));
            campos.add(dato("Documento", r.cliente().documento()));
            campos.add(dato("Categoria", r.cliente().tipo().etiqueta()));
            campos.add(dato("Nro de compra", r.nroCompra()));
            campos.add(dato("Producto", r.producto().id() + " · " + r.producto().bien()));
            campos.add(dato("Marca / modelo", r.producto().marca() + " " + r.producto().modelo()));
            campos.add(dato("Segmento", r.producto().segmento()));
            campos.add(dato("Tipo de problema", r.problema().tipo()));
            campos.add(dato("Problema", r.problema().descripcion()));
            campos.add(dato("Area asignada", r.area()));
            campos.add(dato("Especialista", r.especialista()));
            campos.add(dato("Fecha de atencion",
                    r.fechaAtencion() == null ? "Pendiente" : r.fechaAtencion().format(FECHA)));
            campos.add(dato("Hora de atencion",
                    r.horaAtencion() == null ? "Pendiente" : r.horaAtencion().toString()));

            JTextArea texto = Ui.area("", 4);
            texto.setText(r.detalle());
            texto.setEditable(false);
            texto.setOpaque(false);

            JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
            cuerpo.add(campos, BorderLayout.NORTH);
            JPanel bloqueDetalle = Ui.panel(new BorderLayout(0, 4));
            bloqueDetalle.add(Ui.micro("DESCRIPCION DEL CLIENTE"), BorderLayout.NORTH);
            bloqueDetalle.add(texto, BorderLayout.CENTER);
            cuerpo.add(bloqueDetalle, BorderLayout.CENTER);

            t.add(Ui.scrollVertical(cuerpo), BorderLayout.CENTER);
            detalle.add(t, BorderLayout.CENTER);
        }
        detalle.revalidate();
        detalle.repaint();
    }

    private static JComponent dato(String etiqueta, String valor) {
        JPanel p = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        p.setBorder(Ui.relleno(3, 0, 3, 0));
        JLabel e = Ui.micro(etiqueta);
        e.setPreferredSize(new Dimension(118, 18));
        JLabel v = Ui.etiqueta("<html>" + valor + "</html>");
        v.setFont(Tema.fuente(12, Font.BOLD));
        p.add(e, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}
