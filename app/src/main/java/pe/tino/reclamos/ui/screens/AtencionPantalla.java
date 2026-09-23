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
 * Modulo On-Line: la cola de trabajo del especialista. Permite reasignar area
 * y especialista y hacer avanzar el estado del reclamo segun el protocolo.
 */
public class AtencionPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Tabla cola = new Tabla(new String[]{
            "Reclamo", "Cliente", "Problema", "Area", "Especialista", "Estado"});
    private final JComboBox<String> area = Ui.combo(Datos.catalogo("areas").habilitados());
    private final JComboBox<String> especialista = Ui.combo(Datos.catalogo("empleados").habilitados());
    private final JComboBox<EstadoReclamo> estado = Ui.combo(List.of(EstadoReclamo.values()));
    private final JTextArea bitacora = Ui.area("Observaciones de la atencion.", 4);
    private final JLabel resumen = Ui.suave("Seleccione un reclamo de la cola.");

    private List<Reclamo> visibles = new ArrayList<>();

    public AtencionPantalla() {
        super("Aplicativo - On-Line", "Atencion de reclamos",
                "Cola de trabajo: asignacion de area y especialista, y avance de estado.");

        cola.anchos(80, 175, 170, 175, 95, 135);
        cola.columnaEstado(5, Estados::color);
        cola.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarSeleccion();
        });

        Tarjeta tarjetaCola = new Tarjeta("Cola de atencion",
                "Reclamos que aun no estan entregados ni rechazados.");
        tarjetaCola.sinRelleno();
        tarjetaCola.add(cola.enScroll(), BorderLayout.CENTER);

        contenido().add(tarjetaCola, BorderLayout.CENTER);
        contenido().add(panelAtencion(), BorderLayout.EAST);

        refrescar();
        Estado.alCambiarReclamos(r -> refrescar());
    }

    private JComponent panelAtencion() {
        Tarjeta t = new Tarjeta("Atencion del reclamo",
                "Los cambios se aplican al reclamo seleccionado.");
        t.setPreferredSize(new Dimension(395, 100));

        Formulario f = new Formulario();
        f.ancho(resumen)
         .grupo("Asignacion")
         .campo("Area", area)
         .campo("Especialista", especialista)
         .grupo("Avance")
         .campo("Estado", estado)
         .campo("Bitacora", Ui.scrollConBorde(bitacora))
         .finalizar();

        JButton guardar = Ui.primario("Aplicar cambios", "check");
        guardar.addActionListener(e -> aplicar());
        JButton rechazar = Ui.secundario("Rechazar", "cruz");
        rechazar.addActionListener(e -> rechazar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        pie.add(Ui.filaDerecha(Tema.ESP_SM, rechazar, guardar), BorderLayout.EAST);

        JPanel cuerpo = Ui.panel(new BorderLayout());
        cuerpo.add(f, BorderLayout.CENTER);
        cuerpo.add(pie, BorderLayout.SOUTH);
        t.add(Ui.scrollVertical(cuerpo), BorderLayout.CENTER);
        return t;
    }

    private void refrescar() {
        visibles = Estado.reclamos().stream()
                .filter(r -> r.estado() != EstadoReclamo.ENTREGADO
                          && r.estado() != EstadoReclamo.RECHAZADO)
                .toList();
        cola.limpiar();
        for (Reclamo r : visibles) {
            cola.agregar(r.id(), r.cliente().nombre(), r.problema().descripcion(),
                    r.area(), r.especialista(), r.estado().etiqueta());
        }
        cargarSeleccion();
    }

    private Reclamo seleccionado() {
        int fila = cola.getSelectedRow();
        if (fila < 0 || fila >= visibles.size()) return null;
        return visibles.get(cola.convertRowIndexToModel(fila));
    }

    private void cargarSeleccion() {
        Reclamo r = seleccionado();
        boolean hay = r != null;
        area.setEnabled(hay);
        especialista.setEnabled(hay);
        estado.setEnabled(hay);
        bitacora.setEnabled(hay);

        if (!hay) {
            resumen.setText("<html><body style='width:250px'>Seleccione un reclamo de la cola.</body></html>");
            return;
        }
        resumen.setText("<html><b>" + r.id() + "</b> · " + r.cliente().nombre()
                + "<br>" + r.problema().tipo() + " — " + r.problema().descripcion() + "</html>");
        area.setSelectedItem(r.area());
        especialista.setSelectedItem(r.especialista());
        estado.setSelectedItem(r.estado());
        bitacora.setText(r.detalle());
    }

    private void aplicar() {
        Reclamo r = seleccionado();
        if (r == null) { avisar("Seleccione un reclamo de la cola."); return; }

        EstadoReclamo nuevo = (EstadoReclamo) estado.getSelectedItem();
        boolean cierra = nuevo == EstadoReclamo.ATENDIDO || nuevo == EstadoReclamo.ENTREGADO;

        Estado.reemplazar(r, new Reclamo(r.id(), r.fechaEmision(), r.cliente(), r.nroCompra(),
                r.producto(), r.problema(), r.canal(), nuevo,
                String.valueOf(area.getSelectedItem()),
                String.valueOf(especialista.getSelectedItem()),
                cierra ? LocalDate.now() : r.fechaAtencion(),
                cierra ? LocalTime.now().withSecond(0).withNano(0) : r.horaAtencion(),
                bitacora.getText().trim()));

        avisar("Reclamo " + r.id() + " actualizado a estado \"" + nuevo.etiqueta() + "\".");
    }

    private void rechazar() {
        Reclamo r = seleccionado();
        if (r == null) { avisar("Seleccione un reclamo de la cola."); return; }
        if (!confirmar("Rechazar el reclamo " + r.id() + "? Saldra de la cola de atencion.")) return;

        Estado.reemplazar(r, new Reclamo(r.id(), r.fechaEmision(), r.cliente(), r.nroCompra(),
                r.producto(), r.problema(), r.canal(), EstadoReclamo.RECHAZADO,
                String.valueOf(area.getSelectedItem()),
                String.valueOf(especialista.getSelectedItem()),
                LocalDate.now(), LocalTime.now().withSecond(0).withNano(0),
                bitacora.getText().trim()));
    }
}
