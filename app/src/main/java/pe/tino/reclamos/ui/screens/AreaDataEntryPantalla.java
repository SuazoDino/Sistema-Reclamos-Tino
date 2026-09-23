package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.*;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Area - Data Entry.
 *
 * Permite el ingreso de un usuario para atender a los reclamos en cola:
 * se identifica el especialista, se toma un reclamo de la cola, se le asigna
 * area y se hace avanzar su estado.
 */
public class AreaDataEntryPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JComboBox<String> usuario = Ui.combo(Datos.catalogo("empleados").habilitados());
    private final Tabla cola = new Tabla(new String[]{
            "Reclamo", "Emision", "Cliente", "Problema", "Area", "Especialista", "Estado"});
    private final JComboBox<String> area = Ui.combo(Datos.catalogo("areas").habilitados());
    private final JComboBox<EstadoReclamo> estado = Ui.combo(List.of(EstadoReclamo.values()));
    private final JTextArea observaciones = Ui.area(3);
    private final JTextField resumen = Ui.soloLectura();

    private List<Reclamo> visibles = new ArrayList<>();

    public AreaDataEntryPantalla() {
        super("Area - Data Entry",
                "Ingreso del usuario del area para atender los reclamos en cola.");

        cola.anchos(80, 90, 170, 160, 170, 100, 110);
        cola.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Grupo gCola = Grupo.ajustado("Reclamos en cola");
        gCola.add(cola.enScroll(), BorderLayout.CENTER);

        contenido().add(barraUsuario(), BorderLayout.NORTH);
        contenido().add(gCola, BorderLayout.CENTER);
        contenido().add(atencion(), BorderLayout.SOUTH);

        refrescar();
        Estado.alCambiarReclamos(r -> refrescar());
    }

    private JComponent barraUsuario() {
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));
        p.add(Ui.fila(Ui.etiqueta("Usuario del area:"), usuario), BorderLayout.WEST);
        return p;
    }

    private JComponent atencion() {
        Grupo g = new Grupo("Atencion del reclamo");

        Formulario f = new Formulario();
        f.campo("Reclamo", resumen)
         .campo("Area asignada", area)
         .campo("Estado", estado)
         .campo("Observaciones", Ui.scroll(observaciones));

        JButton aplicar = Ui.boton("Aplicar");
        aplicar.addActionListener(e -> aplicar());
        JButton rechazar = Ui.boton("Rechazar");
        rechazar.addActionListener(e -> rechazar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(Ui.filaDerecha(rechazar, aplicar), BorderLayout.EAST);

        g.add(f, BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        return g;
    }

    private void refrescar() {
        visibles = Estado.reclamos().stream()
                .filter(r -> r.estado() != EstadoReclamo.ENTREGADO
                          && r.estado() != EstadoReclamo.RECHAZADO)
                .toList();
        cola.limpiar();
        for (Reclamo r : visibles) {
            cola.agregar(r.id(), r.fechaEmision().format(FECHA), r.cliente().nombre(),
                    r.problema().descripcion(), r.area(), r.especialista(), r.estado().etiqueta());
        }
        cargarSeleccion();
    }

    private Reclamo seleccionado() {
        int i = cola.filaModelo();
        return i < 0 || i >= visibles.size() ? null : visibles.get(i);
    }

    private void cargarSeleccion() {
        Reclamo r = seleccionado();
        boolean hay = r != null;
        area.setEnabled(hay);
        estado.setEnabled(hay);
        observaciones.setEnabled(hay);

        if (!hay) {
            resumen.setText("");
            observaciones.setText("");
            return;
        }
        resumen.setText(r.id() + "  -  " + r.cliente().nombre() + "  -  "
                + r.problema().tipo() + ": " + r.problema().descripcion());
        area.setSelectedItem(r.area());
        estado.setSelectedItem(r.estado());
        observaciones.setText(r.detalle());
    }

    private void aplicar() {
        Reclamo r = seleccionado();
        if (r == null) { avisar("Seleccione un reclamo de la cola."); return; }

        EstadoReclamo nuevo = (EstadoReclamo) estado.getSelectedItem();
        boolean cierra = nuevo == EstadoReclamo.ATENDIDO || nuevo == EstadoReclamo.ENTREGADO;

        Estado.reemplazar(r, new Reclamo(r.id(), r.fechaEmision(), r.cliente(), r.nroCompra(),
                r.producto(), r.problema(), r.canal(), nuevo,
                String.valueOf(area.getSelectedItem()),
                String.valueOf(usuario.getSelectedItem()),
                cierra ? LocalDate.now() : r.fechaAtencion(),
                cierra ? LocalTime.now().withSecond(0).withNano(0) : r.horaAtencion(),
                observaciones.getText().trim()));

        avisar("Reclamo " + r.id() + " actualizado a estado \"" + nuevo.etiqueta() + "\".");
    }

    private void rechazar() {
        Reclamo r = seleccionado();
        if (r == null) { avisar("Seleccione un reclamo de la cola."); return; }
        if (!confirmar("Rechazar el reclamo " + r.id() + "?")) return;

        Estado.reemplazar(r, new Reclamo(r.id(), r.fechaEmision(), r.cliente(), r.nroCompra(),
                r.producto(), r.problema(), r.canal(), EstadoReclamo.RECHAZADO,
                String.valueOf(area.getSelectedItem()),
                String.valueOf(usuario.getSelectedItem()),
                LocalDate.now(), LocalTime.now().withSecond(0).withNano(0),
                observaciones.getText().trim()));
    }
}
