package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Tickets;
import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Consulta de Indicadores: los once indicadores del prototipo, calculados
 * sobre los tickets de la sesion.
 *
 * Los que el prototipo todavia no puede calcular se dicen asi, en vez de
 * mostrar un numero inventado. En produccion el calculo lo haria el proceso
 * BAT-04 y esta pantalla solo leeria el resultado.
 */
public class IndicadoresPantalla extends Pantalla {

    private static final String SIN_DATO = "No calculable: falta el dato en el sistema";

    private final DefaultListModel<String> modelo = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(modelo);
    private final Tabla detalle = new Tabla(new String[]{"Indicador", "Resultado"});
    private final JLabel base = Ui.suave("");

    public IndicadoresPantalla() {
        super("Consulta de Indicadores",
                "Indicadores de gestion calculados sobre los tickets registrados.");

        Prototipo.INDICADORES.forEach(modelo::addElement);
        lista.setFont(Tema.cuerpo());
        lista.setSelectedIndex(0);
        detalle.anchos(360, 380);

        JButton calcular = Ui.boton("Detalle");
        calcular.addActionListener(e -> calcular());
        JButton salir = Ui.boton("Salir");
        salir.addActionListener(e -> Navegacion.volver());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 2, Tema.ESP_MD));
        pie.add(calcular);
        pie.add(salir);

        Grupo gLista = Grupo.ajustado("Indicadores");
        gLista.add(Ui.scroll(lista), BorderLayout.CENTER);
        gLista.add(pie, BorderLayout.SOUTH);
        gLista.setPreferredSize(Ui.dim(430, 100));

        Grupo gDetalle = Grupo.ajustado("Resultado del periodo");
        gDetalle.add(detalle.enScroll(), BorderLayout.CENTER);
        JPanel cabecera = Ui.panel(new BorderLayout());
        cabecera.setBorder(Ui.relleno(Tema.ESP_SM));
        cabecera.add(base, BorderLayout.WEST);
        gDetalle.add(cabecera, BorderLayout.NORTH);

        contenido().add(gLista, BorderLayout.WEST);
        contenido().add(gDetalle, BorderLayout.CENTER);

        calcular();
        Tickets.alCambiar(t -> calcular());
    }

    private void calcular() {
        List<Ticket> tickets = Tickets.todos();
        base.setText("Calculado sobre los " + tickets.size()
                + " tickets registrados en la sesion.");

        detalle.limpiar();
        for (String indicador : Prototipo.INDICADORES) {
            detalle.agregar(indicador, resultado(indicador, tickets));
        }
    }

    private static String resultado(String indicador, List<Ticket> tickets) {
        if (tickets.isEmpty()) return "Sin tickets registrados";

        return switch (indicador) {
            case "tiempo promedio de solucion por reclamo" -> tiempoPromedio(tickets);
            case "tipo de bienes mas reclamados" -> masFrecuente(tickets, Ticket::objeto);
            case "areas con mayor demanda de reclamo" -> masFrecuente(tickets, Ticket::area);
            case "numeros de reclamos por clase de producto" ->
                    conteo(tickets, t -> t.tipoObjeto().etiqueta());
            case "% de reclamos en un mes" -> porcentajeDelMes(tickets);
            case "% de reclamos criticos" ->
                    porcentaje(tickets, t -> t.prioridad().etiqueta().equals("Alta"),
                            "de prioridad alta");
            case "porcentaje de reclamos solucionados fuera de plazo" ->
                    porcentaje(tickets, Ticket::vencido, "fuera del plazo comprometido");
            case "numero de rotacion de especialitas por reclamo" -> rotacion(tickets);
            default -> SIN_DATO;
        };
    }

    private static String tiempoPromedio(List<Ticket> tickets) {
        List<Ticket> cerrados = tickets.stream().filter(t -> !t.estado().abierto()).toList();
        if (cerrados.isEmpty()) return "Todavia no hay tickets cerrados";

        double horas = cerrados.stream()
                .mapToLong(t -> ChronoUnit.HOURS.between(t.apertura(),
                        t.bitacora().get(t.bitacora().size() - 1).momento()))
                .average().orElse(0);
        return String.format("%.1f horas  (sobre %d tickets cerrados)", horas, cerrados.size());
    }

    private static String masFrecuente(List<Ticket> tickets, Function<Ticket, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        tickets.forEach(t -> conteo.merge(clave.apply(t), 1, Integer::sum));
        return conteo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + "  (" + e.getValue() + " tickets)")
                .orElse(SIN_DATO);
    }

    private static String conteo(List<Ticket> tickets, Function<Ticket, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        tickets.forEach(t -> conteo.merge(clave.apply(t), 1, Integer::sum));
        StringBuilder sb = new StringBuilder();
        conteo.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : "; ").append(k).append(": ").append(v));
        return sb.toString();
    }

    private static String porcentaje(List<Ticket> tickets,
                                     java.util.function.Predicate<Ticket> criterio, String que) {
        long n = tickets.stream().filter(criterio).count();
        return String.format("%.1f %%  (%d de %d %s)",
                n * 100.0 / tickets.size(), n, tickets.size(), que);
    }

    private static String porcentajeDelMes(List<Ticket> tickets) {
        LocalDateTime hoy = LocalDateTime.now();
        long n = tickets.stream()
                .filter(t -> t.apertura().getMonth() == hoy.getMonth()
                          && t.apertura().getYear() == hoy.getYear())
                .count();
        return String.format("%.1f %%  (%d de %d tickets del mes)",
                n * 100.0 / tickets.size(), n, tickets.size());
    }

    /** Cuantas veces cambio de responsable un ticket, en promedio. */
    private static String rotacion(List<Ticket> tickets) {
        double promedio = tickets.stream()
                .mapToLong(t -> t.bitacora().stream()
                        .filter(e -> e.accion().equals("Asignacion")).count())
                .average().orElse(0);
        return String.format("%.2f asignaciones por ticket", promedio);
    }
}
