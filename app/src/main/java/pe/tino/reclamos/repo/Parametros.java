package pe.tino.reclamos.repo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generalidades del sistema: los parametros de alcance general.
 *
 * El libro define parametro como la "variable que permanece fija, para
 * definir escenarios posibles del comportamiento del modulo". Por eso aqui
 * solo hay valores que el flujo lee: si se cambia uno, el sistema se comporta
 * distinto. Magnitud e Idioma no estan porque no cambiaban nada (la magnitud,
 * segun el libro, es una caracteristica del perfil del sistema que se decide
 * al disenarlo, no un valor que se mantenga desde una pantalla).
 */
public final class Parametros {

    private Parametros() {}

    private static LocalTime inicioJornada = LocalTime.of(9, 0);
    private static LocalTime finJornada = LocalTime.of(18, 0);
    /** Plazo maximo para responder un reclamo, en dias habiles (Libro de Reclamaciones). */
    private static int plazoLegalDias = 15;
    /** Garantia de un producto que no tiene politica propia, en dias. */
    private static int garantiaPorDefectoDias = 365;

    public static LocalTime inicioJornada()   { return inicioJornada; }
    public static LocalTime finJornada()      { return finJornada; }
    public static int plazoLegalDias()        { return plazoLegalDias; }
    public static int garantiaPorDefectoDias() { return garantiaPorDefectoDias; }

    /** Minutos de una jornada laboral. */
    public static long minutosJornada() {
        return Duration.between(inicioJornada, finJornada).toMinutes();
    }

    /* ---------------- edicion ---------------- */

    /** Parametro | Valor | Unidad | Que cambia en el sistema. */
    public static List<String[]> lista() {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Inicio de jornada", inicioJornada.toString(), "hh:mm",
                "Desde qué hora se programan las citas y corre el plazo de atención"});
        filas.add(new String[]{"Fin de jornada", finJornada.toString(), "hh:mm",
                "Hasta qué hora se programan las citas y corre el plazo de atención"});
        filas.add(new String[]{"Plazo legal de respuesta", String.valueOf(plazoLegalDias), "días hábiles",
                "Tope del límite de atención: ningún reclamo puede tener un plazo mayor"});
        filas.add(new String[]{"Muestra de calidad", String.valueOf(Calidad.muestraCada()), "1 de cada N",
                "Cuántos reclamos aprobados revisa además el supervisor"});
        filas.add(new String[]{"Garantía por defecto", String.valueOf(garantiaPorDefectoDias), "días",
                "Garantía de un producto sin política propia; decide si está en garantía"});
        return filas;
    }

    /** Cambia un parametro; lanza IllegalArgumentException si el valor no es valido. */
    public static void cambiar(String parametro, String valor) {
        String v = valor == null ? "" : valor.trim();
        switch (parametro) {
            case "Inicio de jornada" -> {
                LocalTime t = hora(v);
                if (!t.isBefore(finJornada)) throw new IllegalArgumentException("El inicio debe ser antes del fin de jornada.");
                inicioJornada = t;
                ParqueTickets.ajustarJornada();
            }
            case "Fin de jornada" -> {
                LocalTime t = hora(v);
                if (!t.isAfter(inicioJornada)) throw new IllegalArgumentException("El fin debe ser después del inicio de jornada.");
                finJornada = t;
                ParqueTickets.ajustarJornada();
            }
            case "Plazo legal de respuesta" -> plazoLegalDias = entero(v, 1, 60);
            case "Muestra de calidad" -> Calidad.muestraCada(entero(v, 1, 100));
            case "Garantía por defecto" -> garantiaPorDefectoDias = entero(v, 0, 3650);
            default -> throw new IllegalArgumentException("No existe el parámetro " + parametro + ".");
        }
    }

    private static LocalTime hora(String v) {
        try {
            return LocalTime.parse(v.length() == 4 ? "0" + v : v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora debe tener el formato hh:mm.");
        }
    }

    private static int entero(String v, int min, int max) {
        try {
            int n = Integer.parseInt(v);
            if (n < min || n > max) throw new IllegalArgumentException("Debe estar entre " + min + " y " + max + ".");
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Debe ser un número entero.");
        }
    }

    /** Lo usa la reversa: deja los parametros como estaban en la foto. */
    static void restaurar(LocalTime inicio, LocalTime fin, int plazo, int garantia, int muestra) {
        inicioJornada = inicio;
        finJornada = fin;
        plazoLegalDias = plazo;
        garantiaPorDefectoDias = garantia;
        Calidad.muestraCada(muestra);
    }

    /** Vuelve a los valores iniciales; lo usan las pruebas. */
    public static void restablecer() {
        boolean otraJornada = !inicioJornada.equals(LocalTime.of(9, 0)) || !finJornada.equals(LocalTime.of(18, 0));
        inicioJornada = LocalTime.of(9, 0);
        finJornada = LocalTime.of(18, 0);
        if (otraJornada) ParqueTickets.ajustarJornada();
        plazoLegalDias = 15;
        garantiaPorDefectoDias = 365;
        Calidad.muestraCada(10);
    }

    /* ---------------- calendario laboral ---------------- */

    /**
     * Suma minutos de trabajo: solo cuentan los de la jornada de los dias
     * laborables. Un reclamo que entra a las 18:40 empieza a correr al dia
     * laborable siguiente, no a medianoche.
     */
    public static LocalDateTime sumarMinutosLaborables(LocalDateTime desde, long minutos) {
        LocalDateTime t = alInicioLaborable(desde);
        long faltan = minutos;
        while (true) {
            long quedanHoy = Duration.between(t, t.toLocalDate().atTime(finJornada)).toMinutes();
            if (faltan <= quedanHoy) return t.plusMinutes(faltan);
            faltan -= quedanHoy;
            t = siguienteLaborable(t.toLocalDate()).atTime(inicioJornada);
        }
    }

    /** Fin del plazo legal: N dias habiles despues, al cierre de la jornada. */
    public static LocalDateTime limiteLegal(LocalDateTime desde) {
        LocalDate d = alInicioLaborable(desde).toLocalDate();
        for (int i = 1; i < plazoLegalDias; i++) d = siguienteLaborable(d);
        return d.atTime(finJornada);
    }

    /** El primer momento laborable desde {@code t} (el mismo si ya lo es). */
    static LocalDateTime alInicioLaborable(LocalDateTime t) {
        LocalDate d = t.toLocalDate();
        if (Catalogos.esLaborable(d)) {
            if (t.toLocalTime().isBefore(inicioJornada)) return d.atTime(inicioJornada);
            if (t.toLocalTime().isBefore(finJornada)) return t;
        }
        return siguienteLaborable(d).atTime(inicioJornada);
    }

    private static LocalDate siguienteLaborable(LocalDate d) {
        LocalDate x = d.plusDays(1);
        while (!Catalogos.esLaborable(x)) x = x.plusDays(1);
        return x;
    }
}
