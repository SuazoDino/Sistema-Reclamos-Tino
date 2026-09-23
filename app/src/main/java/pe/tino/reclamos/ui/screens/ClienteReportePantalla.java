package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.EstadoReclamo;
import pe.tino.reclamos.model.Modelo.Reclamo;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cliente - Reportes.
 *
 * Entrada : DNI/RUC del cliente.
 * Funcion : buscar el proceso que sigue el reclamo del cliente solicitante.
 * Salida  : datos del proceso del reclamo.
 *
 * Segun el estado del reclamo y el tiempo de impugnacion se dan los tres
 * casos que describe el prototipo: rechazado dentro del plazo (se puede
 * impugnar), rechazado fuera del plazo (advertencia) y pendiente.
 */
public class ClienteReportePantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField documento = Ui.texto(16);
    /** El plazo es un parametro del sistema, no un dato fijo del programa. */
    private final JComboBox<String> plazoImpugnacion = Ui.combo(Datos.PLAZOS);
    private final Tabla tabla = new Tabla(new String[]{
            "Reclamo", "Emision", "Producto", "Problema", "Area", "Estado", "Atencion"});
    private final JPanel seguimiento = Ui.panel(new BorderLayout());

    private List<Reclamo> reclamos = List.of();

    public ClienteReportePantalla() {
        super("Cliente - Reportes",
                "Seguimiento del estado y del tiempo restante del reclamo del cliente.");

        plazoImpugnacion.setSelectedItem("15 Dias");
        tabla.anchos(80, 90, 150, 160, 170, 110, 100);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarSeguimiento();
        });

        JButton aceptar = Ui.boton("Aceptar");
        aceptar.addActionListener(e -> buscar());
        documento.addActionListener(e -> buscar());

        JPanel barra = Ui.panel(new BorderLayout());
        barra.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));
        barra.add(Ui.fila(Ui.etiqueta("DNI / RUC:"), documento, aceptar,
                Ui.etiqueta("   Plazo de impugnacion:"), plazoImpugnacion), BorderLayout.WEST);

        Grupo gLista = Grupo.ajustado("Reclamos del cliente");
        gLista.add(tabla.enScroll(), BorderLayout.CENTER);
        gLista.setPreferredSize(new Dimension(100, 210));

        Grupo gSeg = new Grupo("Estado del reclamo");
        gSeg.add(seguimiento, BorderLayout.CENTER);

        JPanel arriba = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        arriba.add(barra, BorderLayout.NORTH);
        arriba.add(gLista, BorderLayout.CENTER);

        contenido().add(arriba, BorderLayout.NORTH);
        contenido().add(gSeg, BorderLayout.CENTER);

        limpiarSeguimiento("Ingrese el numero de documento y presione Aceptar.");
    }

    private void buscar() {
        String doc = documento.getText().trim();
        boolean existe = Datos.CLIENTES.stream().anyMatch(c -> c.documento().equals(doc));

        tabla.limpiar();
        reclamos = List.of();

        if (!existe) {
            limpiarSeguimiento("");
            avisar("El documento " + (doc.isEmpty() ? "(vacio)" : doc)
                    + " no pertenece a un cliente de la empresa.");
            return;
        }
        reclamos = Estado.reclamos().stream()
                .filter(r -> r.cliente().documento().equals(doc))
                .toList();

        for (Reclamo r : reclamos) {
            tabla.agregar(r.id(), r.fechaEmision().format(FECHA), r.producto().bien(),
                    r.problema().descripcion(), r.area(), r.estado().etiqueta(),
                    r.fechaAtencion() == null ? "Pendiente" : r.fechaAtencion().format(FECHA));
        }
        limpiarSeguimiento(reclamos.isEmpty()
                ? "El cliente no tiene reclamos registrados."
                : "Seleccione el reclamo cuyo estado desea visualizar.");
    }

    private void mostrarSeguimiento() {
        int i = tabla.filaModelo();
        if (i < 0 || i >= reclamos.size()) return;

        Reclamo r = reclamos.get(i);
        seguimiento.removeAll();

        if (r.estado() == EstadoReclamo.RECHAZADO) {
            long transcurridos = ChronoUnit.DAYS.between(
                    r.fechaAtencion() == null ? r.fechaEmision() : r.fechaAtencion(), LocalDate.now());
            long plazo = diasDelPlazo();

            if (transcurridos <= plazo) {
                seguimiento.add(casoImpugnable(r, plazo - transcurridos), BorderLayout.CENTER);
            } else {
                seguimiento.add(casoFueraDePlazo(r, transcurridos, plazo), BorderLayout.CENTER);
            }
        } else {
            seguimiento.add(casoPendiente(r), BorderLayout.CENTER);
        }
        seguimiento.revalidate();
        seguimiento.repaint();
    }

    /** Caso 1: reclamo rechazado dentro del plazo de impugnacion. */
    private JComponent casoImpugnable(Reclamo r, long diasRestantes) {
        JTextArea motivo = Ui.area(3);
        JComboBox<String> instancia = Ui.combo(Datos.catalogo("areas").habilitados());

        JButton enviar = Ui.boton("Enviar impugnacion");
        enviar.addActionListener(e -> {
            if (motivo.getText().isBlank()) { avisar("Indique el motivo de la impugnacion."); return; }
            if (!confirmar("Enviar la impugnacion del reclamo " + r.id()
                    + " a " + instancia.getSelectedItem() + "?")) return;

            Estado.reemplazar(r, new Reclamo(r.id(), r.fechaEmision(), r.cliente(), r.nroCompra(),
                    r.producto(), r.problema(), r.canal(), EstadoReclamo.EN_COLA,
                    String.valueOf(instancia.getSelectedItem()), "Sin asignar", null, null,
                    "Impugnacion del cliente: " + motivo.getText().trim()));
            avisar("Impugnacion enviada. El reclamo " + r.id() + " vuelve a la cola de atencion.");
            buscar();
        });

        Formulario f = new Formulario();
        f.campo("Reclamo", Ui.fuerte(r.id() + " - " + r.problema().descripcion()))
         .campo("Estado", Ui.etiqueta("Rechazado"))
         .campo("Plazo restante", Ui.fuerte(diasRestantes + " dias para impugnar"))
         .campo("Instancia", instancia)
         .campo("Motivo", Ui.scroll(motivo));

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(Ui.filaDerecha(enviar), BorderLayout.EAST);

        JPanel p = Ui.panel(new BorderLayout());
        p.add(f, BorderLayout.CENTER);
        p.add(pie, BorderLayout.SOUTH);
        return p;
    }

    /** Caso 2: reclamo rechazado fuera del plazo de impugnacion. */
    private JComponent casoFueraDePlazo(Reclamo r, long transcurridos, long plazo) {
        Formulario f = new Formulario();
        f.campo("Reclamo", Ui.fuerte(r.id() + " - " + r.problema().descripcion()))
         .campo("Estado", Ui.etiqueta("Rechazado"))
         .campo("Plazo", Ui.etiqueta(plazo + " dias; transcurrieron " + transcurridos))
         .campo("Advertencia",
                 Ui.fuerte("El tiempo de impugnacion expiro. El reclamo no admite nuevas instancias."));
        return f;
    }

    /** Caso 3: reclamo pendiente. */
    private JComponent casoPendiente(Reclamo r) {
        Formulario f = new Formulario();
        f.campo("Reclamo", Ui.fuerte(r.id() + " - " + r.problema().descripcion()))
         .campo("Estado", Ui.etiqueta(r.estado().etiqueta()))
         .campo("Area a cargo", Ui.etiqueta(r.area()))
         .campo("Especialista", Ui.etiqueta(r.especialista()))
         .campo("Fecha de emision", Ui.etiqueta(r.fechaEmision().format(FECHA)))
         .campo("Fecha de atencion", Ui.etiqueta(
                 r.fechaAtencion() == null ? "Pendiente" : r.fechaAtencion().format(FECHA)))
         .campo("Dias transcurridos", Ui.etiqueta(
                 ChronoUnit.DAYS.between(r.fechaEmision(), LocalDate.now()) + " dias"));
        return f;
    }

    private long diasDelPlazo() {
        String plazo = String.valueOf(plazoImpugnacion.getSelectedItem());
        String[] partes = plazo.split(" ");
        long valor = Long.parseLong(partes[0]);
        return partes[1].startsWith("Mes") ? valor * 30 : valor;
    }

    private void limpiarSeguimiento(String mensaje) {
        seguimiento.removeAll();
        seguimiento.add(Ui.suave(mensaje), BorderLayout.NORTH);
        seguimiento.revalidate();
        seguimiento.repaint();
    }
}
