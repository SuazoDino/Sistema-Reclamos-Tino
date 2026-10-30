package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Atencion;
import pe.tino.reclamos.repo.Calidad;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Dominio.TipoObjeto;
import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Reclamos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Estado de Reclamo (Cliente - Reportes): el cliente consulta sus reclamos y
 * ve el tiempo que le queda.
 *
 * El detalle resuelve los tres casos del informe, pero ahora el plazo de
 * impugnacion sale del catalogo de instancias en vez de ser un parametro
 * suelto en pantalla.
 *
 * El cliente ve cuando se le atiende (la fecha programada, que es fija) y,
 * cuando su reclamo esta en control de calidad, confirma si quedo
 * solucionado.
 */
public class ClienteReportePantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JTextField documento = Ui.texto(14);
    private final JTextField nombres = Ui.soloLectura();
    private final JTextField apellidos = Ui.soloLectura();
    private final Tabla tabla = new Tabla(new String[]{
            "Nro Reclamo", "Objeto", "Tipo Problema", "Instancia",
            "Estado", "Fecha programada", "Límite de atención", "Tiempo restante"});
    private List<Reclamo> visibles = List.of();

    public ClienteReportePantalla() {
        super("Estado de Reclamo",
                "Seguimiento del estado y del tiempo restante de los reclamos del cliente.");

        tabla.anchos(110, 210, 140, 90, 110, 140, 140, 120);

        JButton aceptar = Ui.boton("Aceptar");
        aceptar.addActionListener(e -> buscar());
        documento.addActionListener(e -> buscar());

        JButton detalle = Ui.boton("Detalle");
        detalle.addActionListener(e -> mostrarDetalle());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_MD));
        pie.add(detalle);

        Formulario f = new Formulario();
        f.campo("DNI/RUC:", Ui.fila(documento, aceptar))
         .campo("Nombres:", nombres)
         .campo("Apellidos:", apellidos);

        Grupo g = new Grupo("Estado de Reclamo");
        g.add(f, BorderLayout.NORTH);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);

        contenido().add(g, BorderLayout.CENTER);
        Reclamos.alCambiar(t -> { if (!visibles.isEmpty()) buscar(); });
    }

    private void buscar() {
        Cliente p = Datos.clientePorDocumento(documento.getText());
        tabla.limpiar();
        visibles = List.of();

        if (p == null) {
            nombres.setText("");
            apellidos.setText("");
            JOptionPane.showMessageDialog(this, "Su usuario no es válido.",
                    "Validación de Usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        nombres.setText(p.nombres());
        apellidos.setText(p.apellidos());
        visibles = Reclamos.de(p.documento());

        for (Reclamo t : visibles) {
            tabla.agregar(t.numero(), t.objeto(), t.tipoProblema(), t.instancia().etiqueta(),
                    t.estado().etiqueta(),
                    t.fechaProgramada() == null ? "-" : t.fechaProgramada().format(RELOJ),
                    t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ),
                    restante(t));
        }
        if (visibles.isEmpty()) avisar("El cliente no tiene reclamos registrados.");
    }

    /** Tiempo que le queda al area para atender, o el estado si ya cerro. */
    private static String restante(Reclamo t) {
        if (!t.estado().abierto()) return "Cerrado";
        if (t.limiteAtencion() == null) return "-";
        long horas = ChronoUnit.HOURS.between(LocalDateTime.now(), t.limiteAtencion());
        if (horas < 0) return "Vencido hace " + (-horas) + " h";
        return horas + " h";
    }

    private void mostrarDetalle() {
        int i = tabla.filaModelo();
        if (i < 0 || i >= visibles.size()) { avisar("Seleccione el reclamo que desea ver."); return; }
        Reclamo t = visibles.get(i);

        JPanel datos = Ui.panel(new GridLayout(0, 2, Tema.ESP_MD, Tema.ESP_XS));
        dato(datos, "Nro Reclamo:", t.numero());
        dato(datos, "Apertura:", t.apertura().format(RELOJ));
        dato(datos, "Reclama sobre:", t.tipoObjeto().etiqueta() + ": " + t.objeto());
        dato(datos, "Tipo Problema:", t.tipoProblema());
        dato(datos, "Problema:", t.problema());
        dato(datos, "Instancia:", t.instancia().etiqueta()
                + " (" + t.instancia().resuelve() + ")");
        dato(datos, "Estado:", t.estado().etiqueta());
        dato(datos, "Área a cargo:", t.area());
        dato(datos, "Fecha programada:",
                t.fechaProgramada() == null ? "-" : t.fechaProgramada().format(RELOJ));
        dato(datos, "Límite de atención:",
                t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ));
        dato(datos, "Límite de impugnación:",
                t.limiteImpugnacion() == null ? "-" : t.limiteImpugnacion().format(RELOJ));
        dato(datos, "Sustento:",
                t.comprobante().isBlank() ? "Sin comprobante" : "Comprobante " + t.comprobante()
                        + (t.tipoObjeto() == TipoObjeto.PRODUCTO && t.fueraDeGarantia()
                        ? " (fuera de garantía)" : ""));
        if (!t.solucionAplicada().isBlank()) dato(datos, "Solución:", t.solucionAplicada());

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(datos, BorderLayout.NORTH);
        p.add(new JLabel("<html><body style='width:420px'>" + mensaje(t) + "</body></html>"),
                BorderLayout.CENTER);
        p.setPreferredSize(Ui.dim(480, 400));

        if (Calidad.esperaConformidad(t)) {
            conformidad(t, p);
            return;
        }
        if (t.estado() != Estado.RECHAZADO) {
            JOptionPane.showMessageDialog(this, p, "Detalle del Reclamo", JOptionPane.PLAIN_MESSAGE);
            return;
        }
        if (!puedeImpugnar(t)) {
            JOptionPane.showMessageDialog(this, p, "Detalle del Reclamo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object[] opciones = {"Impugnar", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, p, "Detalle del Reclamo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (r == 0) impugnar(t);
    }

    /**
     * Los casos del informe, ahora resueltos con datos del reclamo: un mensaje
     * por cada estado real, para que lo que lee el cliente coincida con la
     * columna Estado.
     */
    private static String mensaje(Reclamo t) {
        if (t.estado() == Estado.RECHAZADO) {
            if (!t.instancia().admiteImpugnacion()) {
                return "Su reclamo fue rechazado en la última instancia. "
                        + "No admite nuevas impugnaciones.";
            }
            if (!puedeImpugnar(t)) {
                return "Su reclamo fue rechazado y el plazo de impugnación de "
                        + t.instancia().diasImpugnacion() + " días expiró.";
            }
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), t.limiteImpugnacion().toLocalDate());
            return "Su reclamo ha sido rechazado. Le quedan " + Math.max(dias, 0)
                    + " días para impugnar ante " + t.instancia().siguiente().resuelve() + ".";
        }
        String plazo = t.vencido()
                ? " Ya excedió el plazo comprometido."
                : " Está dentro del plazo comprometido.";
        String cita = t.fechaProgramada() == null ? ""
                : " Será atendido el " + t.fechaProgramada().format(RELOJ) + ".";
        return switch (t.estado()) {
            case REGISTRADO -> "Su reclamo fue registrado y espera que se le asigne un área."
                    + plazo;
            case ASIGNADO -> "Su reclamo fue asignado a " + t.area() + "." + cita + plazo;
            case EN_ATENCION -> "Su reclamo está en atención por " + t.area() + "." + cita + plazo;
            case IMPUGNADO -> "Su impugnación fue recibida. El reclamo pasó a "
                    + t.instancia().etiqueta().toLowerCase() + " instancia y espera asignación."
                    + plazo;
            case RESUELTO -> "Su reclamo fue resuelto y pasa a control de calidad.";
            case EN_REVISION -> Calidad.esperaConformidad(t)
                    ? "Su reclamo fue atendido con " + t.solucionAplicada()
                            + ". Cuéntenos si quedó solucionado."
                    : "Su reclamo está en control de calidad. Le avisaremos el resultado.";
            case CERRADO -> "Su reclamo fue atendido y cerrado.";
            case RECHAZADO -> throw new IllegalStateException("resuelto arriba");
        };
    }

    private static boolean puedeImpugnar(Reclamo t) {
        return t.instancia().admiteImpugnacion() && t.limiteImpugnacion() != null
                && !LocalDateTime.now().isAfter(t.limiteImpugnacion());
    }

    private void impugnar(Reclamo t) {
        JTextArea motivo = Ui.area(4);
        Formulario f = new Formulario();
        f.campo("Reclamo:", Ui.fuerte(t.numero()))
         .campo("Pasa a:", Ui.fuerte(t.instancia().siguiente().etiqueta()
                 + " - " + t.instancia().siguiente().resuelve()))
         .campo("Motivo:", Ui.scroll(motivo));
        f.setBorder(Ui.relleno(Tema.ESP_MD));

        int r = JOptionPane.showConfirmDialog(this, f, "Enviar Impugnación",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (motivo.getText().isBlank()) { avisar("Indique el motivo de la impugnación."); return; }

        if (!Atencion.impugnar(t, motivo.getText().trim())) {
            advertir("El reclamo ya no admite impugnación.");
            return;
        }
        buscar();
        avisar("Impugnación enviada. El reclamo pasa a " + t.instancia().etiqueta()
                + " instancia y será atendido el " + t.fechaProgramada().format(RELOJ) + ".");
    }

    /** "Su reclamo quedo solucionado?" con una nota del 1 al 5. */
    private void conformidad(Reclamo t, JPanel detalle) {
        JRadioButton si = new JRadioButton("Sí", true);
        JRadioButton no = new JRadioButton("No");
        ButtonGroup g = new ButtonGroup();
        g.add(si);
        g.add(no);
        for (JRadioButton r : List.of(si, no)) { r.setOpaque(false); r.setFont(Tema.cuerpo()); }
        JComboBox<Integer> nota = Ui.combo(List.of(5, 4, 3, 2, 1));

        Formulario f = new Formulario();
        f.campo("¿Su reclamo quedó solucionado?", Ui.fila(si, no))
         .campo("Nota (1 a 5):", nota);

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(detalle, BorderLayout.CENTER);
        p.add(f, BorderLayout.SOUTH);

        Object[] opciones = {"Enviar", "Cancelar"};
        int r = JOptionPane.showOptionDialog(this, p, "Detalle del Reclamo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (r != 0) return;

        Calidad.conformidad(t, si.isSelected(), (Integer) nota.getSelectedItem());
        buscar();
        if (si.isSelected()) {
            avisar(t.estado() == Estado.CERRADO ? "Gracias. Su reclamo quedó cerrado."
                    : "Gracias. El reclamo se cerrará cuando termine la revisión de calidad.");
        } else {
            avisar("Lamentamos el inconveniente. Su reclamo vuelve a atención y será atendido el "
                    + t.fechaProgramada().format(RELOJ) + ".");
        }
    }

    private static void dato(JPanel destino, String etiqueta, String valor) {
        destino.add(Ui.etiqueta(etiqueta));
        destino.add(Ui.fuerte(valor));
    }
}
