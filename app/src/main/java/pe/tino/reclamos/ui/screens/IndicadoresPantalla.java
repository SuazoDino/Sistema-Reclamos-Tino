package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Dominio.TipoObjeto;
import pe.tino.reclamos.repo.Estadisticas;
import pe.tino.reclamos.repo.Calidad;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Reclamos;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Consulta de Indicadores: los once indicadores del prototipo, calculados
 * sobre los reclamos de la sesion.
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
                "Indicadores de gestión calculados sobre los reclamos registrados.");

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
        Reclamos.alCambiar(t -> calcular());
    }

    private void calcular() {
        List<Reclamo> reclamos = Reclamos.todos();
        base.setText("Calculado sobre los " + reclamos.size()
                + " reclamos registrados en la sesión.");

        detalle.limpiar();
        for (String indicador : Prototipo.INDICADORES) {
            detalle.agregar(indicador, resultado(indicador, reclamos));
        }
        graficar();
    }

    /* ---------------- grafico del indicador elegido ---------------- */

    private void graficar() {
        String indicador = lista.getSelectedValue();
        if (indicador == null) return;
        List<Reclamo> reclamos = Reclamos.todos();

        gGrafico.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.BORDE), " Detalle: " + indicador + " ",
                0, 0, Tema.fuerte(), Tema.TEXTO));
        lectura.setText(resultado(indicador, reclamos));

        switch (indicador) {
            case "Tiempo promedio de solución por reclamo" -> tendenciaTiempo(reclamos);
            case "% de reclamos en un mes" -> tendenciaReclamos(reclamos);
            case "Tipo de bienes más reclamados" ->
                    grafico.barras(conteoOrdenado(bienes(reclamos), Reclamo::objeto), "reclamos");
            case "Áreas con mayor demanda de reclamo" ->
                    grafico.barras(conteoOrdenado(conArea(reclamos), Reclamo::area), "reclamos");
            case "Número de reclamos por clase de producto" ->
                    grafico.barras(conteoOrdenado(reclamos, t -> t.tipoObjeto().etiqueta()), "reclamos");
            case "% de reclamos críticos" ->
                    grafico.barras(conteoOrdenado(reclamos, t -> "Prioridad " + t.prioridad().etiqueta()),
                            "reclamos");
            case "Porcentaje de reclamos solucionados fuera de plazo" ->
                    grafico.barras(conteoOrdenado(reclamos,
                            t -> t.vencido() ? "Fuera de plazo" : "Dentro del plazo"), "reclamos");
            case "Número de rotación de especialistas por reclamo" ->
                    grafico.barras(conteoOrdenado(reclamos,
                            t -> asignaciones(t) + (asignaciones(t) == 1 ? " asignación" : " asignaciones")),
                            "reclamos");
            default -> grafico.vacio(SIN_DATO);
        }
    }

    /** Reclamos por mes: historico cerrado, proyeccion y lo que va del mes. */
    private void tendenciaReclamos(List<Reclamo> reclamos) {
        List<Estadisticas.Mes> historico = Estadisticas.historico();
        List<Punto> meses = new ArrayList<>();
        List<Double> serie = new ArrayList<>();
        for (Estadisticas.Mes m : historico) {
            meses.add(new Punto(mes(m.mes()), m.reclamos(),
                    nombreMes(m.mes()) + ": " + m.reclamos() + " reclamos"));
            serie.add((double) m.reclamos());
        }

        YearMonth actual = YearMonth.now();
        long delMes = reclamos.stream().filter(t -> YearMonth.from(t.apertura()).equals(actual)).count();
        double proyectado = Estadisticas.proyectar(serie);

        grafico.tendencia(meses,
                new Punto(mes(actual), delMes, nombreMes(actual) + " en curso: " + delMes
                        + " reclamos registrados en la sesión"),
                new Punto(mes(actual), Math.round(proyectado), nombreMes(actual) + " proyectado: "
                        + Math.round(proyectado) + " reclamos, extrapolando los meses cerrados"),
                "(reclamos)");
    }

    /** Horas promedio hasta la solucion por mes. */
    private void tendenciaTiempo(List<Reclamo> reclamos) {
        List<Punto> meses = new ArrayList<>();
        List<Double> serie = new ArrayList<>();
        for (Estadisticas.Mes m : Estadisticas.historico()) {
            meses.add(new Punto(mes(m.mes()), m.horasSolucion(),
                    nombreMes(m.mes()) + String.format(": %.1f horas en promedio", m.horasSolucion())));
            serie.add(m.horasSolucion());
        }

        YearMonth actual = YearMonth.now();
        List<Reclamo> cerrados = reclamos.stream()
                .filter(t -> !t.estado().abierto() && YearMonth.from(t.apertura()).equals(actual))
                .toList();
        Punto enCurso = cerrados.isEmpty() ? null
                : new Punto(mes(actual), horasPromedio(cerrados), nombreMes(actual)
                        + String.format(" en curso: %.1f horas sobre %d reclamos cerrados",
                        horasPromedio(cerrados), cerrados.size()));
        double proyectado = Estadisticas.proyectar(serie);

        grafico.tendencia(meses, enCurso,
                new Punto(mes(actual), Math.round(proyectado * 10) / 10.0, nombreMes(actual)
                        + String.format(" proyectado: %.1f horas, extrapolando los meses cerrados",
                        proyectado)),
                "(horas)");
    }

    /** Conteo por categoria, de la mas frecuente a la menos. */
    private static List<Punto> conteoOrdenado(List<Reclamo> reclamos, Function<Reclamo, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        reclamos.forEach(t -> conteo.merge(clave.apply(t), 1, Integer::sum));
        int total = reclamos.size();
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new Punto(e.getKey(), e.getValue(), String.format("%s: %d de %d reclamos (%.1f %%)",
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

    private static String resultado(String indicador, List<Reclamo> reclamos) {
        if (reclamos.isEmpty()) return "Sin reclamos registrados";

        return switch (indicador) {
            case "Tiempo promedio de solución por reclamo" -> tiempoPromedio(reclamos);
            case "Tipo de bienes más reclamados" -> masFrecuente(bienes(reclamos), Reclamo::objeto);
            case "Áreas con mayor demanda de reclamo" -> masFrecuente(conArea(reclamos), Reclamo::area);
            case "Número de reclamos por clase de producto" ->
                    conteo(reclamos, t -> t.tipoObjeto().etiqueta());
            case "% de reclamos en un mes" -> porcentajeDelMes(reclamos);
            case "% de reclamos críticos" ->
                    porcentaje(reclamos, t -> t.prioridad().etiqueta().equals("Alta"),
                            "de prioridad alta");
            case "Porcentaje de reclamos solucionados fuera de plazo" ->
                    porcentaje(reclamos, IndicadoresPantalla::fueraDePlazo, "fuera del plazo comprometido");
            case "Número de rotación de especialistas por reclamo" -> rotacion(reclamos);
            case "Nivel de satisfacción del cliente" -> satisfaccion(reclamos);
            case "% devueltos por calidad" -> devueltos();
            default -> SIN_DATO;
        };
    }

    /** Resuelto despues del limite, o abierto y ya vencido. */
    private static boolean fueraDePlazo(Reclamo t) {
        if (t.resolucion() != null) return t.resolucion().isAfter(t.limiteAtencion());
        return t.vencido();
    }

    /** Sale de las notas que el cliente da al confirmar su conformidad. */
    private static String satisfaccion(List<Reclamo> reclamos) {
        long respuestas = reclamos.stream().filter(t -> t.nota() > 0).count();
        if (respuestas == 0) return "Todavía ningún cliente respondió la conformidad";
        return String.format("%.1f de 5  (sobre %d respuestas)", Calidad.satisfaccion(), respuestas);
    }

    private static String devueltos() {
        double p = Calidad.porcentajeDevueltos();
        if (p < 0) return "Todavía ningún reclamo pasó por control de calidad";
        return String.format("%.1f %% de los reclamos revisados volvió a atención", p);
    }

    private static String tiempoPromedio(List<Reclamo> reclamos) {
        List<Reclamo> cerrados = reclamos.stream().filter(t -> !t.estado().abierto()).toList();
        if (cerrados.isEmpty()) return "Todavía no hay reclamos cerrados";
        return String.format("%.1f horas  (sobre %d reclamos cerrados)",
                horasPromedio(cerrados), cerrados.size());
    }

    private static double horasPromedio(List<Reclamo> cerrados) {
        return cerrados.stream()
                .mapToLong(t -> ChronoUnit.HOURS.between(t.apertura(),
                        t.bitacora().get(t.bitacora().size() - 1).momento()))
                .average().orElse(0);
    }

    /** Los bienes son productos: un servicio no es un bien. */
    private static List<Reclamo> bienes(List<Reclamo> reclamos) {
        return reclamos.stream().filter(t -> t.tipoObjeto() == TipoObjeto.PRODUCTO).toList();
    }

    /** La demanda de un area se mide con los reclamos que ya tienen area. */
    private static List<Reclamo> conArea(List<Reclamo> reclamos) {
        return reclamos.stream().filter(t -> !t.area().equals("Sin asignar")).toList();
    }

    /** El valor mas frecuente; si varios empatan, los nombra a todos. */
    private static String masFrecuente(List<Reclamo> reclamos, Function<Reclamo, String> clave) {
        if (reclamos.isEmpty()) return "Sin reclamos que cumplan el criterio";
        Map<String, Integer> conteo = new LinkedHashMap<>();
        reclamos.forEach(t -> conteo.merge(clave.apply(t), 1, Integer::sum));
        int maximo = Collections.max(conteo.values());
        List<String> primeros = conteo.entrySet().stream()
                .filter(e -> e.getValue() == maximo)
                .map(Map.Entry::getKey)
                .toList();
        String cantidad = maximo + (maximo == 1 ? " reclamo" : " reclamos");
        if (primeros.size() == 1) return primeros.get(0) + "  (" + cantidad + ")";
        return "Empate: " + String.join(", ", primeros) + "  (" + cantidad + " cada uno)";
    }

    private static String conteo(List<Reclamo> reclamos, Function<Reclamo, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        reclamos.forEach(t -> conteo.merge(clave.apply(t), 1, Integer::sum));
        StringBuilder sb = new StringBuilder();
        conteo.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : "; ").append(k).append(": ").append(v));
        return sb.toString();
    }

    private static String porcentaje(List<Reclamo> reclamos, Predicate<Reclamo> criterio, String que) {
        long n = reclamos.stream().filter(criterio).count();
        return String.format("%.1f %%  (%d de %d %s)",
                n * 100.0 / reclamos.size(), n, reclamos.size(), que);
    }

    private static String porcentajeDelMes(List<Reclamo> reclamos) {
        LocalDateTime hoy = LocalDateTime.now();
        long n = reclamos.stream()
                .filter(t -> t.apertura().getMonth() == hoy.getMonth()
                          && t.apertura().getYear() == hoy.getYear())
                .count();
        return String.format("%.1f %%  (%d de %d reclamos del mes)",
                n * 100.0 / reclamos.size(), n, reclamos.size());
    }

    private static long asignaciones(Reclamo t) {
        return t.bitacora().stream().filter(e -> e.accion().equals("Asignación")).count();
    }

    /** Cuantas veces cambio de responsable un reclamo, en promedio. */
    private static String rotacion(List<Reclamo> reclamos) {
        double promedio = reclamos.stream().mapToLong(IndicadoresPantalla::asignaciones)
                .average().orElse(0);
        return String.format("%.2f asignaciones por reclamo", promedio);
    }
}
