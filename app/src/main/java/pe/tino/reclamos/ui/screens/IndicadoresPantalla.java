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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta de Indicadores.
 *
 * Muestra los once indicadores que enumera el prototipo. Los que se pueden
 * derivar de los reclamos registrados se calculan; los que necesitan datos
 * que el prototipo no captura se marcan como no calculables, en vez de
 * inventarles un valor.
 */
public class IndicadoresPantalla extends Pantalla {

    private static final String SIN_DATO = "No calculable en el prototipo";

    private final Tabla tabla = new Tabla(new String[]{"Indicador", "Resultado"});
    private final JLabel base = Ui.suave("");

    public IndicadoresPantalla() {
        super("Consulta de Indicadores",
                "Resultados estadisticos de los indicadores del sistema de reclamos.");

        tabla.anchos(380, 340);

        JButton calcular = Ui.boton("Calcular");
        calcular.addActionListener(e -> calcular());

        JPanel barra = Ui.panel(new BorderLayout());
        barra.setBorder(Ui.relleno(0, 0, Tema.ESP_SM, 0));
        barra.add(base, BorderLayout.WEST);
        barra.add(Ui.filaDerecha(calcular), BorderLayout.EAST);

        Grupo g = Grupo.ajustado("Indicadores");
        g.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(barra, BorderLayout.NORTH);
        contenido().add(g, BorderLayout.CENTER);

        calcular();
    }

    private void calcular() {
        List<Reclamo> reclamos = Estado.reclamos();
        base.setText("Calculado sobre los " + reclamos.size() + " reclamos registrados en la sesion.");

        tabla.limpiar();
        for (String indicador : Datos.INDICADORES) {
            tabla.agregar(indicador, resultado(indicador, reclamos));
        }
    }

    private static String resultado(String indicador, List<Reclamo> reclamos) {
        if (reclamos.isEmpty()) return "Sin reclamos registrados";

        return switch (indicador) {
            case "Tiempo promedio de solucion por reclamo" -> tiempoPromedio(reclamos);
            case "Tipo de bienes mas reclamados" -> masFrecuente(reclamos, r -> r.producto().bien());
            case "Areas con mayor demanda de reclamo" -> masFrecuente(reclamos, Reclamo::area);
            case "Numero de reclamos por clase de producto" -> conteoPor(reclamos, r -> r.producto().clase());
            case "Porcentaje de reclamos en un mes" -> porcentajeDelMes(reclamos);
            default -> SIN_DATO;
        };
    }

    private static String tiempoPromedio(List<Reclamo> reclamos) {
        List<Reclamo> cerrados = reclamos.stream()
                .filter(r -> r.fechaAtencion() != null)
                .toList();
        if (cerrados.isEmpty()) return "Sin reclamos atendidos todavia";

        double dias = cerrados.stream()
                .mapToLong(r -> ChronoUnit.DAYS.between(r.fechaEmision(), r.fechaAtencion()))
                .average().orElse(0);
        return String.format("%.1f dias  (sobre %d reclamos atendidos)", dias, cerrados.size());
    }

    private static String masFrecuente(List<Reclamo> reclamos,
                                       java.util.function.Function<Reclamo, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        reclamos.forEach(r -> conteo.merge(clave.apply(r), 1, Integer::sum));
        return conteo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + "  (" + e.getValue() + " reclamos)")
                .orElse(SIN_DATO);
    }

    private static String conteoPor(List<Reclamo> reclamos,
                                    java.util.function.Function<Reclamo, String> clave) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        reclamos.forEach(r -> conteo.merge(clave.apply(r), 1, Integer::sum));
        StringBuilder sb = new StringBuilder();
        conteo.forEach((k, v) -> sb.append(sb.isEmpty() ? "" : "; ").append(k).append(": ").append(v));
        return sb.toString();
    }

    private static String porcentajeDelMes(List<Reclamo> reclamos) {
        LocalDate hoy = LocalDate.now();
        long delMes = reclamos.stream()
                .filter(r -> r.fechaEmision().getMonth() == hoy.getMonth()
                          && r.fechaEmision().getYear() == hoy.getYear())
                .count();
        return String.format("%.1f %%  (%d de %d reclamos)",
                delMes * 100.0 / reclamos.size(), delMes, reclamos.size());
    }
}
