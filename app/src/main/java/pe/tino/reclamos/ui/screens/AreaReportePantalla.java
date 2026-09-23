package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.EstadoReclamo;
import pe.tino.reclamos.model.Modelo.Reclamo;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Area - Reportes.
 *
 * Muestra los avances y detalles que haya realizado el usuario del area:
 * los reclamos que tiene asignados y en que estado los dejo.
 */
public class AreaReportePantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JComboBox<String> usuario = Ui.combo(Datos.catalogo("empleados").habilitados());
    private final Tabla tabla = new Tabla(new String[]{
            "Reclamo", "Emision", "Cliente", "Problema", "Area", "Estado", "Atencion"});
    private final JLabel resumen = Ui.fuerte("");

    public AreaReportePantalla() {
        super("Area - Reportes",
                "Avances y detalle de los reclamos atendidos por el usuario del area.");

        tabla.anchos(80, 90, 170, 160, 170, 110, 100);
        usuario.addActionListener(e -> generar());

        JButton generar = Ui.boton("Generar");
        generar.addActionListener(e -> generar());

        JPanel barra = Ui.panel(new BorderLayout());
        barra.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));
        barra.add(Ui.fila(Ui.etiqueta("Usuario del area:"), usuario, generar), BorderLayout.WEST);

        Grupo g = Grupo.ajustado("Reclamos del usuario");
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM));
        pie.add(resumen, BorderLayout.WEST);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(barra, BorderLayout.NORTH);
        contenido().add(g, BorderLayout.CENTER);

        generar();
        Estado.alCambiarReclamos(r -> generar());
    }

    private void generar() {
        String elegido = String.valueOf(usuario.getSelectedItem());
        List<Reclamo> suyos = Estado.reclamos().stream()
                .filter(r -> elegido.equals(r.especialista()))
                .toList();

        tabla.limpiar();
        for (Reclamo r : suyos) {
            tabla.agregar(r.id(), r.fechaEmision().format(FECHA), r.cliente().nombre(),
                    r.problema().descripcion(), r.area(), r.estado().etiqueta(),
                    r.fechaAtencion() == null ? "Pendiente" : r.fechaAtencion().format(FECHA));
        }

        long atendidos = suyos.stream()
                .filter(r -> r.estado() == EstadoReclamo.ATENDIDO
                          || r.estado() == EstadoReclamo.ENTREGADO)
                .count();
        resumen.setText(suyos.size() + " reclamos asignados  -  " + atendidos + " atendidos  -  "
                + (suyos.size() - atendidos) + " en proceso");
    }
}
