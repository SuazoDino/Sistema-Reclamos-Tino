package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Estadisticas;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Tickets;
import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Grafico;
import pe.tino.reclamos.ui.components.Grafico.Punto;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Consulta de Indicadores: los once indicadores del prototipo, calculados
 * sobre los tickets de la sesion.
 *
 * Es una consulta gerencial, y el libro le pide informacion estadistica e
 * historica que muestre tendencia y se pueda proyectar. Por eso Detalle
 * grafica el indicador elegido: barras para comparar categorias y una linea
 * mes a mes, con la proyeccion del mes, para los que tienen historia. La
 * tabla de abajo sigue dando los once resultados en texto.
 *
 * Los que el prototipo todavia no puede calcular se dicen asi, en vez de
 * mostrar un numero inventado. En produccion el calculo lo haria el proceso
 * BAT-04 y esta pantalla solo leeria el resultado.
 */
public class IndicadoresPantalla extends Pantalla {

    private static final String SIN_DATO = "No calculable: falta el dato en el sistema";
    private static final Locale ES = Locale.forLanguageTag("es");

    private final DefaultListModel<String> modelo = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(modelo);
    private final Tabla detalle = new Tabla(new String[]{"Indicador", "Resultado"});
    private final Grafico grafico = new Grafico();
    private final Grupo gGrafico = Grupo.ajustado("Detalle");
    private final JLabel lectura = Ui.suave("");
    private final JLabel base = Ui.suave("");

    public IndicadoresPantalla() {
        super("Consulta de Indicadores",
                "Indicadores de gestión calculados sobre los tickets registrados.");

        Prototipo.INDICADORES.forEach(modelo::addElement);
        lista.setFont(Tema.cuerpo());
        lista.setSelectedIndex(0);
        lista.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) graficar(); });
        detalle.anchos(360, 380);

        JButton verDetalle = Ui.boton("Detalle");
        verDetalle.addActionListener(e -> graficar());
        JButton salir = Ui.boton("Salir");
        salir.addActionListener(e -> Navegacion.volver());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 2, Tema.ESP_MD));
        pie.add(verDetalle);
        pie.add(salir);

        Grupo gLista = Grupo.ajustado("Indicadores");
        gLista.add(Ui.scroll(lista), BorderLayout.CENTER);
        gLista.add(pie, BorderLayout.SOUTH);
        gLista.setPreferredSize(Ui.dim(430, 100));

        JPanel notaGrafico = Ui.panel(new BorderLayout());
        notaGrafico.setBorder(Ui.relleno(Tema.ESP_SM));
        notaGrafico.add(lectura, BorderLayout.WEST);
        gGrafico.add(grafico, BorderLayout.CENTER);
        gGrafico.add(notaGrafico, BorderLayout.SOUTH);

        Grupo gResumen = Grupo.ajustado("Resultado del periodo");
        gResumen.add(detalle.enScroll(), BorderLayout.CENTER);
        JPanel cabecera = Ui.panel(new BorderLayout());
        cabecera.setBorder(Ui.relleno(Tema.ESP_SM));
        cabecera.add(base, BorderLayout.WEST);
        gResumen.add(cabecera, BorderLayout.NORTH);
        gResumen.setPreferredSize(Ui.dim(100, 250));

        JPanel derecha = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        derecha.add(gGrafico, BorderLayout.CENTER);
        derecha.add(gResumen, BorderLayout.SOUTH);

        contenido().add(gLista, BorderLayout.WEST);
        contenido().add(derecha, BorderLayout.CENTER);

        calcular();
        Tickets.alCambiar(t -> calcular());
    }

    private void calcular() {
        List<Ticket> tickets = Tickets.todos();
        base.setText("Calculado sobre los " + tickets.size()
                + " tickets registrados en la sesión.");

        detalle.limpiar();
        for (String indicador : Prototipo.INDICADORES) {
            detalle.agregar(indicador, resultado(indicador, tickets));
        }
        graficar();
    }

    /* ---------------- grafico del indicador elegido ---------------- */

    private void graficar() {
        String indicador = lista.getSelectedValue();
        if (indicador == null) return;
        List<Ticket> tickets = Tickets.todos();

        gGrafico.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.BORDE), " Detalle: " + indicador + " ",
                0, 0, Tema.fuerte(), Tema.TEXTO));
        lectura.setText(resultado(indicador, tickets));

        switch (indicador) {
            case "Tiempo promedio de solución por reclamo" -> tendenciaTiempo(tickets);
            case "% de reclamos en un mes" -> tendenciaReclamos(tickets);
            case "Tipo de bienes más reclamados" ->
                    grafico.barras(conteoOrdenado(tickets, Ticket::objeto), "tickets");
            case "Áreas con mayor demanda de reclamo" ->
                    grafico.barras(conteoOrdenado(tickets, Ticket::area), "tickets");
            case "Número de reclamos por clase de producto" ->
                    grafico.barras(conteoOrdenado(tickets, t -> t.tipoObjeto().etiqueta()), "tickets");
            case "% de reclamos críticos" ->
                    grafico.barras(conteoOrdenado(tickets, t -> "Prioridad " + t.prioridad().etiqueta()),
                            "tickets");
            case "Porcentaje de reclamos solucionados fuera de plazo" ->
                    grafico.barras(conteoOrdenado(tickets,
                            t -> t.vencido() ? "Fuera de plazo" : "Dentro del plazo"), "tickets");
            case "Número de rotación de especialistas por reclamo" ->
                    grafico.barras(conteoOrdenado(tickets,
                            t -> asignaciones(t) + (asignaciones(t) == 1 ? " asignación" : " asignaciones")),
                            "tickets");
            default -> grafico.vacio(SIN_DATO);
        }
    }

    /** Reclamos por mes: historico cerrado, proyeccion y lo que va del mes. */
    private void tendenciaReclamos(List<Ticket> tickets) {
        List<Estadisticas.Mes> historico = Estadisticas.historico();
        List<Punto> meses = new ArrayList<>();
        List<Double> serie = new ArrayList<>();
        for (Estadisticas.Mes m : historico) {
            meses.add(new Punto(mes(m.mes()), m.reclamos(),
                    nombreMes(m.mes()) + ": " + m.reclamos() + " reclamos"));
            serie.add((double) m.reclamos());
        }

        YearMonth actual = YearMonth.now();
        long delMes = tickets.stream().filter(t -> YearMonth.from(t.apertura()).equals(actual)).count();
        double proyectado = Estadisticas.proyectar(serie);

        grafico.tendencia(meses,
                new Punto(mes(actual), delMes, nombreMes(actual) + " en curso: " + delMes
                        + " reclamos registrados en la sesión"),
                new Punto(mes(actual), Math.round(proyectado), nombreMes(actual) + " proyectado: "
                        + Math.round(proyectado) + " reclamos, extrapolando los meses cerrados"),
                "(reclamos)");
    }

    /** Horas promedio hasta la solucion por mes. */
    private void tendenciaTiempo(List<Ticket> tickets) {
        List<Punto> meses = new ArrayList<>();
        List<Double> serie = new ArrayList<>();
        for (Estadisticas.Mes m : Estadisticas.historico()) {
            meses.add(new Punto(mes(m.mes()), m.horasSolucion(),
                    nombreMes(m.mes()) + String.format(": %.1f horas en promedio", m.horasSolucion())));
            serie.add(m.horasSolucion());
        }

        YearMonth actual = YearMonth.now();
        List<Ticket> cerrados = tickets.stream()
                .filter(t -> !t.estado().abierto() && YearMonth.from(t.apertura()).equals(actual))
                .toList();
        Punto enCurso = cerrados.isEmpty() ? null
                : new Punto(mes(actual), horasPromedio(cerrados), nombreMes(actual)
                        + String.format(" en curso: %.1f horas sobre %d tickets cerrados",
                        horasPromedio(cerrados), cerrados.size()));
        double proyectado = Estadisticas.proyectar(serie);

        grafico.tendencia(meses, enCurso,
                new Punto(mes(actual), Math.round(proyectado * 10) / 10.0, nombreMes(actual)
                        + String.format(" proyectado: %.1f horas, extrapolando los meses cerrados",
                        proyectado)),
                "(horas)");
    }

    /** Conteo por categoria, de la mas frecuente a la menos. */
    private static List<Punto> conteoOrdenado(List<Ticket> tickets, Function<Ticket, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        tickets.forEach(t -> conteo.merge(clave.apply(t), 1, Integer::sum));
        int total = tickets.size();
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new Punto(e.getKey(), e.getValue(), String.format("%s: %d de %d tickets (%.1f %%)",
                        e.getKey(), e.getValue(), total, e.getValue() * 100.0 / total)))
                .toList();
    }

    private static String mes(YearMonth m) {
        String corto = m.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return Character.toUpperCase(corto.charAt(0)) + corto.substring(1);
    }

    private static String nombreMes(YearMonth m) {
        String largo = m.getMonth().getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(largo.charAt(0)) + largo.substring(1) + " " + m.getYear();
    }

    /* ---------------- resultado en texto ---------------- */

    private static String resultado(String indicador, List<Ticket> tickets) {
        if (tickets.isEmpty()) return "Sin tickets registrados";

        return switch (indicador) {
            case "Tiempo promedio de solución por reclamo" -> tiempoPromedio(tickets);
            case "Tipo de bienes más reclamados" -> masFrecuente(tickets, Ticket::objeto);
            case "Áreas con mayor demanda de reclamo" -> masFrecuente(tickets, Ticket::area);
            case "Número de reclamos por clase de producto" ->
                    conteo(tickets, t -> t.tipoObjeto().etiqueta());
            case "% de reclamos en un mes" -> porcentajeDelMes(tickets);
            case "% de reclamos críticos" ->
                    porcentaje(tickets, t -> t.prioridad().etiqueta().equals("Alta"),
                            "de prioridad alta");
            case "Porcentaje de reclamos solucionados fuera de plazo" ->
                    porcentaje(tickets, Ticket::vencido, "fuera del plazo comprometido");
            case "Número de rotación de especialistas por reclamo" -> rotacion(tickets);
            default -> SIN_DATO;
        };
    }

    private static String tiempoPromedio(List<Ticket> tickets) {
        List<Ticket> cerrados = tickets.stream().filter(t -> !t.estado().abierto()).toList();
        if (cerrados.isEmpty()) return "Todavía no hay tickets cerrados";
        return String.format("%.1f horas  (sobre %d tickets cerrados)",
                horasPromedio(cerrados), cerrados.size());
    }

    private static double horasPromedio(List<Ticket> cerrados) {
        return cerrados.stream()
                .mapToLong(t -> ChronoUnit.HOURS.between(t.apertura(),
                        t.bitacora().get(t.bitacora().size() - 1).momento()))
                .average().orElse(0);
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

    private static String porcentaje(List<Ticket> tickets, Predicate<Ticket> criterio, String que) {
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

    private static long asignaciones(Ticket t) {
        return t.bitacora().stream().filter(e -> e.accion().equals("Asignación")).count();
    }

    /** Cuantas veces cambio de responsable un ticket, en promedio. */
    private static String rotacion(List<Ticket> tickets) {
        double promedio = tickets.stream().mapToLong(IndicadoresPantalla::asignaciones)
                .average().orElse(0);
        return String.format("%.2f asignaciones por ticket", promedio);
    }
}
