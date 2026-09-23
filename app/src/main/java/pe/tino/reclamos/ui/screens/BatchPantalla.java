package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Modulo BATCH: que procesos corren fuera de linea y por que.
 *
 * Esta pantalla documenta el modulo, no lo ejecuta. El criterio para mandar
 * un proceso al batch es el del libro: no tiene usuario esperando, toca
 * muchos registros de una vez, y su tiempo de proceso es mayor que el que
 * un usuario aceptaria frente a la pantalla.
 */
public class BatchPantalla extends Pantalla {

    /** Proceso | Que hace | Por que es batch | Frecuencia | Ventana. */
    private static final List<String[]> PROCESOS = List.of(
            new String[]{"BAT-01", "Vencimiento de plazos de atención",
                    "Recorre los tickets abiertos y marca los que pasaron su límite "
                            + "de atención, para que el área los priorice.",
                    "Toca todos los tickets abiertos a la vez; ningún usuario espera el "
                            + "resultado, pero el área lo necesita antes de abrir.",
                    "Diaria", "00:30 - 01:00"},

            new String[]{"BAT-02", "Cierre de plazos de impugnación",
                    "Los tickets rechazados cuyo plazo de impugnación expiró pasan a "
                            + "Cerrado, porque ya no admiten nueva instancia.",
                    "Es una transición de estado masiva que depende del paso del tiempo, "
                            + "no de una acción del usuario.",
                    "Diaria", "01:00 - 01:30"},

            new String[]{"BAT-03", "Recálculo de categorización de cliente",
                    "Aplica las condiciones, métodos y fórmulas del Catálogo de Cliente "
                            + "sobre el histórico de compras y reasigna la categoría.",
                    "Lee todo el histórico de compras de cada cliente: es el proceso más "
                            + "pesado del sistema y su resultado no cambia de un día al otro.",
                    "Mensual", "Día 1, 02:00 - 04:00"},

            new String[]{"BAT-04", "Cálculo de indicadores",
                    "Calcula los once indicadores de gestión y deja el resultado listo "
                            + "para que Consulta de Indicadores solo lo lea.",
                    "Son agregaciones sobre todos los tickets del periodo. Calcularlas "
                            + "en línea haría esperar al gerente varios minutos.",
                    "Mensual", "Día 1, 04:00 - 05:00"},

            new String[]{"BAT-05", "Depuración de tickets cerrados",
                    "Mueve al histórico los tickets cerrados con más de un año y libera "
                            + "espacio en las tablas de operación.",
                    "Es una actualización masiva que degrada el tiempo de respuesta de "
                            + "todas las consultas si se hace en horario de atención.",
                    "Mensual", "Día 1, 05:00 - 06:00"},

            new String[]{"BAT-06", "Alerta de tickets por vencer",
                    "Notifica al área los tickets cuyo plazo vence dentro del día, "
                            + "según el SLA de Parámetros de Atención.",
                    "Barre toda la cola y emite avisos en lote; corre antes de que "
                            + "empiece la atención.",
                    "Diaria", "07:00 - 07:15"}
    );

    public BatchPantalla() {
        super("Módulo BATCH",
                "Procesos que corren fuera de línea, sin usuario esperando frente a la pantalla.");

        Tabla tabla = new Tabla(new String[]{
                "Proceso", "Qué hace", "Por qué es batch", "Frecuencia", "Ventana"});
        PROCESOS.forEach(p -> tabla.agregar(p[0], p[1] + ": " + p[2], p[3], p[4], p[5]));
        tabla.anchos(90, 400, 380, 110, 150);

        Grupo listado = Grupo.ajustado("Procesos del módulo");
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        contenido().add(criterio(), BorderLayout.NORTH);
        contenido().add(listado, BorderLayout.CENTER);
        contenido().add(dependencias(), BorderLayout.SOUTH);
    }

    /** El criterio del libro para decidir que va al batch. */
    private JComponent criterio() {
        JPanel p = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        p.add(Ui.fuerte("Cuando un proceso va al batch"));
        p.add(Ui.etiqueta("1.  No hay un usuario esperando el resultado frente a la pantalla."));
        p.add(Ui.etiqueta("2.  Toca muchos registros de una vez, no uno."));
        p.add(Ui.etiqueta("3.  Su tiempo de proceso es mayor que el que un usuario "
                + "aceptaría en línea."));
        p.add(Ui.etiqueta("4.  Depende del paso del tiempo, no de una acción del usuario."));

        Grupo g = new Grupo("Criterio");
        g.add(p, BorderLayout.CENTER);
        return g;
    }

    /** El orden importa: un proceso necesita que el anterior haya terminado. */
    private JComponent dependencias() {
        Tabla t = new Tabla(new String[]{"Proceso", "Necesita que antes haya corrido", "Por qué"});
        t.agregar("BAT-02", "BAT-01",
                "El cierre por plazo vencido usa el estado que BAT-01 acaba de actualizar");
        t.agregar("BAT-04", "BAT-03",
                "Los indicadores por tipo de cliente usan la categoría que BAT-03 recalcula");
        t.agregar("BAT-05", "BAT-04",
                "No se depura un ticket antes de que entre en el cálculo del periodo");
        t.agregar("BAT-06", "BAT-01",
                "La alerta se arma sobre los plazos que BAT-01 dejó marcados");
        t.anchos(110, 260, 560);

        Grupo g = Grupo.ajustado("Orden de ejecución");
        g.add(t.enScroll(), BorderLayout.CENTER);
        g.setPreferredSize(Ui.dim(100, 170));
        return g;
    }
}
