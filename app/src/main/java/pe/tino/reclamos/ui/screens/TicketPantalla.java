package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Dominio;
import pe.tino.reclamos.repo.Reclamos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Consulta de Reclamos: la vista completa de un reclamo, con su ficha, sus
 * plazos y su bitacora.
 *
 * Es la pantalla que responde "que es un reclamo y que lo compone": arriba el
 * listado, abajo todo lo que el sistema controla de el. El comprobante de
 * compra aparece como un dato mas del sustento, no como el reclamo.
 *
 * Es de consulta: el estado lo mueve el flujo (asignacion, atencion,
 * control de calidad), nunca un operador a mano.
 */
public class TicketPantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Tabla tabla = new Tabla(new String[]{
            "Nro Reclamo", "Apertura", "Cliente", "Objeto", "Problema",
            "Instancia", "Prioridad", "Estado", "Fecha programada", "Límite atención"});
    private final JPanel ficha = Ui.panel(new BorderLayout());
    private final Tabla bitacora = new Tabla(new String[]{"Momento", "Usuario", "Acción", "Detalle"});
    private final Tabla eventos = new Tabla(new String[]{
            "Evento", "Protocolo", "Responsable", "Cita", "Estado", "Resultado"});

    public TicketPantalla() {
        super("Consulta de Reclamos",
                "Un reclamo por reclamo presentado: lo que el sistema numera, asigna y mide.");

        tabla.anchos(110, 120, 180, 210, 160, 80, 80, 100, 120, 120);
        eventos.anchos(150, 110, 110, 120, 100, 200);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarFicha();
        });
        bitacora.anchos(130, 90, 140, 400);

        Grupo listado = Grupo.ajustado("Reclamos registrados");
        listado.add(tabla.enScroll(), BorderLayout.CENTER);
        listado.setPreferredSize(Ui.dim(100, 220));

        Grupo detalle = new Grupo("Ficha del reclamo");
        detalle.add(ficha, BorderLayout.CENTER);

        Grupo log = Grupo.ajustado("Bitácora");
        log.add(bitacora.enScroll(), BorderLayout.CENTER);
        log.setPreferredSize(Ui.dim(420, 100));

        JPanel abajo = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        abajo.add(detalle, BorderLayout.CENTER);
        abajo.add(log, BorderLayout.EAST);

        contenido().add(listado, BorderLayout.NORTH);
        contenido().add(abajo, BorderLayout.CENTER);

        refrescar();
        Reclamos.alCambiar(t -> refrescar());
        mostrarFicha();
    }

    private void refrescar() {
        int seleccion = tabla.getSelectedRow();
        tabla.limpiar();
        for (Reclamo t : Reclamos.todos()) {
            tabla.agregar(t.numero(), t.apertura().format(RELOJ), t.nombreCliente(),
                    t.objeto(), t.problema(), t.instancia().etiqueta(),
                    t.prioridad().etiqueta(), t.estado().etiqueta(),
                    t.fechaProgramada() == null ? "-" : t.fechaProgramada().format(RELOJ),
                    t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ));
        }
        if (seleccion >= 0 && seleccion < tabla.getRowCount()) {
            tabla.setRowSelectionInterval(seleccion, seleccion);
        }
    }

    private Reclamo seleccionado() {
        int i = tabla.filaModelo();
        List<Reclamo> todos = Reclamos.todos();
        return i < 0 || i >= todos.size() ? null : todos.get(i);
    }

    private void mostrarFicha() {
        ficha.removeAll();
        bitacora.limpiar();
        Reclamo t = seleccionado();

        if (t == null) {
            ficha.add(Ui.suave("Seleccione un reclamo del listado."), BorderLayout.NORTH);
            ficha.revalidate();
            ficha.repaint();
            return;
        }

        JPanel datos = Ui.panel(new GridLayout(0, 4, Tema.ESP_MD, Tema.ESP_XS));
        dato(datos, "Nro de reclamo:", t.numero());
        dato(datos, "Apertura:", t.apertura().format(RELOJ));
        dato(datos, "Canal:", t.canal().etiqueta());
        dato(datos, "Local:", t.local());
        dato(datos, "Cliente:", t.nombreCliente());
        dato(datos, "Documento:", t.documentoCliente());
        dato(datos, "Tipo de cliente:", t.tipoCliente());
        dato(datos, "Instancia:", t.instancia().etiqueta());
        dato(datos, "Reclama sobre:", t.tipoObjeto().etiqueta());
        dato(datos, "Objeto:", t.objeto());
        dato(datos, "Tipo de problema:", t.tipoProblema());
        dato(datos, "Problema:", t.problema());
        dato(datos, "Prioridad:", t.prioridad().etiqueta());
        dato(datos, "Estado:", t.estado().etiqueta());
        dato(datos, "Área:", t.area());
        dato(datos, "Especialista:", t.especialista());
        dato(datos, "Protocolo:", t.protocolo());
        dato(datos, "Límite de atención:",
                t.limiteAtencion() == null ? "-" : t.limiteAtencion().format(RELOJ));
        dato(datos, "Límite de impugnación:",
                t.limiteImpugnacion() == null ? "-" : t.limiteImpugnacion().format(RELOJ));
        dato(datos, "Vencido:", t.vencido() ? "Sí" : "No");
        dato(datos, "Fecha programada:",
                t.fechaProgramada() == null ? "-" : t.fechaProgramada().format(RELOJ));
        dato(datos, "Capacidad:", t.sinCapacidad() ? "Sin capacidad dentro del plazo" : "A tiempo");
        dato(datos, "Solución sugerida:", t.solucionSugerida().isBlank() ? "-" : t.solucionSugerida());
        dato(datos, "Solución aplicada:", t.solucionAplicada().isBlank() ? "-" : t.solucionAplicada());
        dato(datos, "Revisión de calidad:", t.revision() == null ? "-" : t.revision().etiqueta());
        dato(datos, "Conformidad:", t.conforme() == null ? "-"
                : (t.conforme() ? "Conforme" : "No conforme") + ", nota " + t.nota());

        Grupo sustento = new Grupo("Sustento (documento del cliente, no es el reclamo)",
                new GridLayout(1, 4, Tema.ESP_MD, 0));
        sustento.add(Ui.etiqueta("Comprobante de compra:"));
        sustento.add(Ui.fuerte(t.comprobante().isBlank() ? "Sin comprobante"
                : t.comprobante() + (t.fueraDeGarantia() ? " (fuera de garantía)" : "")));
        sustento.add(Ui.etiqueta("Fecha del comprobante:"));
        sustento.add(Ui.fuerte(t.fechaComprobante().isBlank() ? "-" : t.fechaComprobante()));

        eventos.limpiar();
        t.eventos().forEach(e -> eventos.agregar(e.evento(), e.protocolo(), e.especialista(),
                e.inicio() == null ? "-" : e.inicio().format(RELOJ), e.estado().etiqueta(),
                e.resultado().isBlank() ? (e.dentroDelPlazo() ? "" : "Sin capacidad dentro del plazo")
                        : e.resultado()));
        Grupo grupoEventos = Grupo.ajustado("Eventos del reclamo");
        grupoEventos.add(eventos.enScroll(), BorderLayout.CENTER);
        grupoEventos.setPreferredSize(Ui.dim(100, 140));

        JPanel medio = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        medio.add(sustento, BorderLayout.NORTH);
        medio.add(grupoEventos, BorderLayout.CENTER);

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(datos, BorderLayout.NORTH);
        cuerpo.add(medio, BorderLayout.CENTER);
        cuerpo.add(estados(t), BorderLayout.SOUTH);
        ficha.add(cuerpo, BorderLayout.NORTH);

        t.bitacora().forEach(e -> bitacora.agregar(e.momento().format(RELOJ),
                e.usuario(), e.accion(), e.detalle()));

        ficha.revalidate();
        ficha.repaint();
    }

    /** A donde puede ir el reclamo. Lo informa; quien lo mueve es el flujo, no el operador. */
    private JComponent estados(Reclamo t) {
        JPanel p = Ui.panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_SM, 0));
        p.setBorder(Ui.relleno(Tema.ESP_MD, 0, 0, 0));
        p.add(Ui.etiqueta("Puede pasar a:"));
        p.add(Ui.suave(t.estado().siguientes().isEmpty() ? "ningún estado; el reclamo está cerrado"
                : String.join(", ", t.estado().siguientes().stream()
                        .map(Dominio.Estado::etiqueta).toList())
                        + " (lo decide el flujo del sistema)"));
        return p;
    }

    private static void dato(JPanel destino, String etiqueta, String valor) {
        destino.add(Ui.etiqueta(etiqueta));
        destino.add(Ui.fuerte(valor));
    }
}
