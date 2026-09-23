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
            new String[]{"BAT-01", "Vencimiento de plazos de atencion",
                    "Recorre los tickets abiertos y marca los que pasaron su limite "
                            + "de atencion, para que el area los priorice.",
                    "Toca todos los tickets abiertos a la vez; ningun usuario espera el "
                            + "resultado, pero el area lo necesita antes de abrir.",
                    "Diaria", "00:30 - 01:00"},

            new String[]{"BAT-02", "Cierre de plazos de impugnacion",
                    "Los tickets rechazados cuyo plazo de impugnacion expiro pasan a "
                            + "Cerrado, porque ya no admiten nueva instancia.",
                    "Es una transicion de estado masiva que depende del paso del tiempo, "
                            + "no de una accion del usuario.",
                    "Diaria", "01:00 - 01:30"},

            new String[]{"BAT-03", "Recalculo de categorizacion de cliente",
                    "Aplica las condiciones, metodos y formulas del Catalogo de Cliente "
                            + "sobre el historico de compras y reasigna la categoria.",
                    "Lee todo el historico de compras de cada cliente: es el proceso mas "
                            + "pesado del sistema y su resultado no cambia de un dia al otro.",
                    "Mensual", "Dia 1, 02:00 - 04:00"},

            new String[]{"BAT-04", "Calculo de indicadores",
                    "Calcula los once indicadores de gestion y deja el resultado listo "
                            + "para que Consulta de Indicadores solo lo lea.",
                    "Son agregaciones sobre todos los tickets del periodo. Calcularlas "
                            + "en linea haria esperar al gerente varios minutos.",
                    "Mensual", "Dia 1, 04:00 - 05:00"},

            new String[]{"BAT-05", "Depuracion de tickets cerrados",
                    "Mueve al historico los tickets cerrados con mas de un anio y libera "
                            + "espacio en las tablas de operacion.",
                    "Es una actualizacion masiva que degrada el tiempo de respuesta de "
                            + "todas las consultas si se hace en horario de atencion.",
                    "Mensual", "Dia 1, 05:00 - 06:00"},

            new String[]{"BAT-06", "Alerta de tickets por vencer",
                    "Notifica al area los tickets cuyo plazo vence dentro del dia, "
                            + "segun el SLA de Parametros de Atencion.",
                    "Barre toda la cola y emite avisos en lote; corre antes de que "
                            + "empiece la atencion.",
                    "Diaria", "07:00 - 07:15"}
    );

    public BatchPantalla() {
        super("Modulo BATCH",
                "Procesos que corren fuera de linea, sin usuario esperando frente a la pantalla.");

        Tabla tabla = new Tabla(new String[]{
                "Proceso", "Que hace", "Por que es batch", "Frecuencia", "Ventana"});
        PROCESOS.forEach(p -> tabla.agregar(p[0], p[1] + ": " + p[2], p[3], p[4], p[5]));
        tabla.anchos(90, 400, 380, 110, 150);

        Grupo listado = Grupo.ajustado("Procesos del modulo");
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
                + "aceptaria en linea."));
        p.add(Ui.etiqueta("4.  Depende del paso del tiempo, no de una accion del usuario."));

        Grupo g = new Grupo("Criterio");
        g.add(p, BorderLayout.CENTER);
        return g;
    }

    /** El orden importa: un proceso necesita que el anterior haya terminado. */
    private JComponent dependencias() {
        Tabla t = new Tabla(new String[]{"Proceso", "Necesita que antes haya corrido", "Porque"});
        t.agregar("BAT-02", "BAT-01",
                "El cierre por plazo vencido usa el estado que BAT-01 acaba de actualizar");
        t.agregar("BAT-04", "BAT-03",
                "Los indicadores por tipo de cliente usan la categoria que BAT-03 recalcula");
        t.agregar("BAT-05", "BAT-04",
                "No se depura un ticket antes de que entre en el calculo del periodo");
        t.agregar("BAT-06", "BAT-01",
                "La alerta se arma sobre los plazos que BAT-01 dejo marcados");
        t.anchos(110, 260, 560);

        Grupo g = Grupo.ajustado("Orden de ejecucion");
        g.add(t.enScroll(), BorderLayout.CENTER);
        g.setPreferredSize(Ui.dim(100, 170));
        return g;
    }
}
