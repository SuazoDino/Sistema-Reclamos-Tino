package pe.tino.reclamos.repo;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Historico de los meses cerrados: lo que el submodulo BATCH ESTADISTICAS
 * (proceso BAT-04) deja elaborado para que la consulta gerencial solo lo lea.
 *
 * El libro pide que la consulta gerencial muestre tendencia y la proyecte
 * extrapolando; sin historia no hay tendencia. Los tickets de la sesion son
 * todos del mes en curso, asi que los meses anteriores salen de aqui.
 */
public final class Estadisticas {

    private Estadisticas() {}

    /** Un mes cerrado: reclamos recibidos y horas promedio hasta la solucion. */
    public record Mes(YearMonth mes, int reclamos, double horasSolucion) {}

    private static final int[] RECLAMOS = {38, 42, 35, 47, 51, 49};
    private static final double[] HORAS = {52.0, 49.5, 50.8, 46.2, 44.0, 41.5};

    /** Los seis meses cerrados anteriores al actual, del mas antiguo al mas reciente. */
    public static List<Mes> historico() {
        YearMonth actual = YearMonth.now();
        List<Mes> meses = new ArrayList<>();
        for (int i = 0; i < RECLAMOS.length; i++) {
            meses.add(new Mes(actual.minusMonths(RECLAMOS.length - i), RECLAMOS[i], HORAS[i]));
        }
        return meses;
    }

    /**
     * El valor que sigue en la serie, por minimos cuadrados: la recta que mejor
     * pasa por los puntos, extendida un periodo mas.
     */
    public static double proyectar(List<Double> serie) {
        int n = serie.size();
        if (n == 0) return 0;
        if (n == 1) return serie.get(0);

        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (int x = 0; x < n; x++) {
            double y = serie.get(x);
            sx += x; sy += y; sxy += x * y; sxx += (double) x * x;
        }
        double pendiente = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double origen = (sy - pendiente * sx) / n;
        return Math.max(0, origen + pendiente * n);
    }
}
