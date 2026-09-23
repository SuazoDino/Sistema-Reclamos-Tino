package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.repo.Tickets;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Area - Reportes: los avances del usuario del area sobre los reclamos que
 * tiene registrados. Sigue la misma forma que "Atender Reclamo", que es la
 * ventana hermana del prototipo.
 */
public class AreaReportePantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JComboBox<String> empleado = Ui.combo(List.copyOf(Prototipo.EMPLEADOS.keySet()));
    private final JTextField tipoEmpleado = Ui.soloLectura();
    private final JTextField area = Ui.soloLectura();
    private final JTextField fecha = Ui.soloLectura();
    private final Tabla tabla = new Tabla(new String[]{
            "Nro Ticket", "Apertura", "Cliente", "Problema", "Prioridad", "Estado", "Vencido"});
    private final JLabel resumen = Ui.fuerte("");

    public AreaReportePantalla() {
        super("Reporte de Atención", "Avances y detalle de los reclamos del usuario del área.");

        tabla.anchos(110, 130, 190, 180, 100, 120, 90);
        fecha.setText(LocalDate.now().format(FECHA));
        empleado.addActionListener(e -> generar());

        JButton generar = Ui.boton("Generar");
        generar.addActionListener(e -> generar());

        JPanel datos = Ui.panel(new GridLayout(2, 4, Tema.ESP_MD, Tema.ESP_SM));
        datos.add(Ui.etiqueta("ID Empleado:"));
        datos.add(Ui.fila(empleado, generar));
        datos.add(Ui.etiqueta("Área:"));
        datos.add(area);
        datos.add(Ui.etiqueta("Tipo Empleado:"));
        datos.add(tipoEmpleado);
        datos.add(Ui.etiqueta("Fecha:"));
        datos.add(fecha);
        datos.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(resumen, BorderLayout.WEST);

        Grupo g = new Grupo("Reclamos Registrados");
        g.add(datos, BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(g, BorderLayout.CENTER);
        generar();
        Tickets.alCambiar(t -> generar());
    }

    private void generar() {
        Empleado e = Prototipo.EMPLEADOS.get(String.valueOf(empleado.getSelectedItem()));
        if (e == null) return;
        tipoEmpleado.setText(e.tipo());
        area.setText(e.area());

        // el reporte se arma sobre los tickets asignados al usuario, no sobre
        // una lista aparte: si no, Data Entry y Reportes muestran cosas distintas
        List<Ticket> suyos = Tickets.todos().stream()
                .filter(t -> e.id().equals(t.especialista()))
                .toList();

        tabla.limpiar();
        for (Ticket t : suyos) {
            tabla.agregar(t.numero(), t.apertura().format(RELOJ), t.nombreCliente(),
                    t.problema(), t.prioridad().etiqueta(), t.estado().etiqueta(),
                    t.vencido() ? "Sí" : "No");
        }

        long cerrados = suyos.stream().filter(t -> !t.estado().abierto()).count();
        long vencidos = suyos.stream().filter(Ticket::vencido).count();
        resumen.setText(suyos.size() + " tickets asignados  -  " + cerrados
                + " cerrados  -  " + (suyos.size() - cerrados) + " abiertos  -  "
                + vencidos + " fuera de plazo");
    }
}
