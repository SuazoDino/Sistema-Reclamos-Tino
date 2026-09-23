package pe.tino.reclamos.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Navegacion clasica por menus: se entra por el mapa de modulos, se baja al
 * menu del modulo y de ahi al catalogo. El historial permite volver sobre los
 * propios pasos, como en el prototipo original.
 */
public final class Navegacion {

    public static final String MAPA = "mapa";

    private static Consumer<String> destino;
    private static final Deque<String> HISTORIAL = new ArrayDeque<>();
    private static String actual = MAPA;

    private Navegacion() {}

    /** Punto de entrada para las pruebas, que no levantan una ventana. */
    public static void instalarParaPruebas(Consumer<String> consumidor) {
        instalar(consumidor);
    }

    static void instalar(Consumer<String> consumidor) {
        destino = consumidor;
        HISTORIAL.clear();
        actual = MAPA;
    }

    /** Entra a una pantalla apilando la actual en el historial. */
    public static void ir(String clave) {
        if (clave.equals(actual)) return;
        HISTORIAL.push(actual);
        actual = clave;
        if (destino != null) destino.accept(clave);
    }

    /** Vuelve a la pantalla anterior; si no hay, vuelve al mapa. */
    public static void volver() {
        String anterior = HISTORIAL.isEmpty() ? MAPA : HISTORIAL.pop();
        actual = anterior;
        if (destino != null) destino.accept(anterior);
    }

    /** Vuelve al mapa de modulos y limpia el historial. */
    public static void inicio() {
        HISTORIAL.clear();
        actual = MAPA;
        if (destino != null) destino.accept(MAPA);
    }

    public static boolean hayDondeVolver() { return !HISTORIAL.isEmpty(); }

    public static String actual() { return actual; }
}
