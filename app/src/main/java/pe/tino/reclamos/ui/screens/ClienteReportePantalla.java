package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Prototipo.Detalle;
import pe.tino.reclamos.repo.Prototipo.Persona;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Estado de Reclamo (Cliente - Reportes), como en las capturas: se identifica
 * al cliente, se elige el reclamo y al ver el detalle aparecen los tres casos
 * del informe segun el estado y el plazo de impugnacion.
 */
public class ClienteReportePantalla extends Pantalla {

    private final JTextField documento = Ui.texto(14);
    private final JTextField nombres = Ui.soloLectura();
    private final JTextField apellidos = Ui.soloLectura();
    private final JComboBox<String> idReclamo = Ui.combo(List.of("R001", "R002", "R003", "R004"));
    private final Tabla tabla = new Tabla(new String[]{
            "Productos", "Tipo Problema", "Fecha Limite", "Estado Reclamo"});

    public ClienteReportePantalla() {
        super("Estado de Reclamo", "Seguimiento del estado del reclamo del cliente.");

        tabla.anchos(230, 220, 180, 200);
        idReclamo.addActionListener(e -> seleccionarPorId());

        JButton buscar = Ui.boton("Aceptar");
        buscar.addActionListener(e -> buscar());
        documento.addActionListener(e -> buscar());

        JButton detalle = Ui.boton("Detalle");
        detalle.addActionListener(e -> mostrarDetalle());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_MD));
        pie.add(detalle);

        Formulario f = new Formulario();
        f.campo("DNI/RUC:", Ui.fila(documento, buscar))
         .campo("Nombres:", nombres)
         .campo("Apellidos:", apellidos)
         .campo("ID Reclamo:", idReclamo);

        Grupo g = new Grupo("Estado de Reclamo");
        g.add(f, BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(g, BorderLayout.CENTER);
    }

    private void buscar() {
        Persona p = Prototipo.PERSONAS.get(documento.getText().trim());
        tabla.limpiar();

        if (p == null) {
            nombres.setText("");
            apellidos.setText("");
            JOptionPane.showMessageDialog(this, "Su usuario no es valido.",
                    "Validacion de Usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        nombres.setText(p.nombres());
        apellidos.setText(p.apellidos());
        Prototipo.estadoDeReclamos().forEach(fila -> tabla.agregar((Object[]) fila));
    }

    /** El combo de ID selecciona la fila correspondiente, como en la captura. */
    private void seleccionarPorId() {
        int i = idReclamo.getSelectedIndex();
        if (i >= 0 && i < tabla.getRowCount()) tabla.setRowSelectionInterval(i, i);
    }

    /** La ventana "Detalle del Reclamo" con sus tres casos. */
    private void mostrarDetalle() {
        int i = tabla.filaModelo();
        if (i < 0) { avisar("Seleccione el reclamo que desea visualizar."); return; }

        Detalle d = Prototipo.detalleDe(i);
        JPanel datos = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        datos.add(Ui.etiqueta("ID Reclamo: " + d.id()));
        datos.add(Ui.etiqueta("Producto: " + d.producto()));
        datos.add(Ui.etiqueta("Marca : " + d.marca()));
        datos.add(Ui.etiqueta("Tipo Problema: " + d.tipoProblema()));
        datos.add(Ui.etiqueta("Problema: " + d.problema()));
        datos.add(Ui.etiqueta("Fecha de Emision: " + d.emision()));
        datos.add(Ui.etiqueta("Fecha de Respuesta: " + d.respuesta()));
        datos.add(Ui.etiqueta("Fecha Limite de Impugnacion : " + d.limite()));
        datos.add(new JLabel("<html><body style='width:380px'>Descripcion: "
                + d.descripcion() + "</body></html>"));

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(datos, BorderLayout.NORTH);
        p.setPreferredSize(new Dimension(440, 300));

        switch (d.estado()) {
            case "Rechazado" -> casoRechazado(p, d);
            case "Aceptado" -> {
                p.add(Ui.fuerte("Su reclamo ha sido aceptado."), BorderLayout.CENTER);
                JOptionPane.showMessageDialog(this, p, "Detalle del Reclamo",
                        JOptionPane.PLAIN_MESSAGE);
            }
            default -> {
                p.add(Ui.fuerte("Su reclamo se encuentra pendiente de respuesta."),
                        BorderLayout.CENTER);
                JOptionPane.showMessageDialog(this, p, "Detalle del Reclamo",
                        JOptionPane.PLAIN_MESSAGE);
            }
        }
    }

    /**
     * Caso 1 y caso 2 del informe: si el plazo sigue vigente se ofrece
     * impugnar; si expiro, solo la advertencia.
     */
    private void casoRechazado(JPanel p, Detalle d) {
        if (!vigente(d.limite())) {
            p.add(Ui.fuerte("El tiempo de impugnacion expiro. El reclamo no admite "
                    + "nuevas instancias."), BorderLayout.CENTER);
            JOptionPane.showMessageDialog(this, p, "Detalle del Reclamo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        p.add(Ui.fuerte("Su reclamo ha sido rechazado. Desea impugnar esta respuesta?"),
                BorderLayout.CENTER);

        Object[] opciones = {"Impugnar", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, p, "Detalle del Reclamo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (r == 0) impugnar(d);
    }

    /** El formulario de impugnacion que el informe describe para el caso 1. */
    private void impugnar(Detalle d) {
        JTextArea motivo = Ui.area(4);
        JComboBox<String> instancia = Ui.combo(List.of("Segunda instancia", "Area especializada",
                "Gerencia de PostVenta"));

        Formulario f = new Formulario();
        f.campo("Reclamo:", Ui.fuerte(d.id() + " - " + d.problema()))
         .campo("Instancia:", instancia)
         .campo("Motivo:", Ui.scroll(motivo));
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Enviar Impugnacion",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        if (motivo.getText().isBlank()) { avisar("Indique el motivo de la impugnacion."); return; }
        avisar("Impugnacion enviada a " + instancia.getSelectedItem() + ".");
    }

    /** La fecha limite del prototipo esta en dd/MM/yyyy. */
    private static boolean vigente(String limite) {
        try {
            var fecha = java.time.LocalDate.parse(limite,
                    java.time.format.DateTimeFormatter.ofPattern("d/MM/yyyy"));
            return !java.time.LocalDate.now().isAfter(fecha);
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
}
