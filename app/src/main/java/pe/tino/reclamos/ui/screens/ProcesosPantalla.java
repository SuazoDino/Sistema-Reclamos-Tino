package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pantalla de procesos programados. La comparten los modulos Batch, Act-BD,
 * Mant-BD y Contingencia: todos muestran una lista de procesos, permiten
 * lanzarlos y dejan constancia en una bitacora.
 */
public class ProcesosPantalla extends Pantalla {

    private static final DateTimeFormatter RELOJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Tabla tabla = new Tabla(new String[]{
            "Proceso", "Descripcion", "Frecuencia", "Ultima ejecucion", "Estado"});
    private final JTextArea bitacora = Ui.area("", 8);
    private final List<String[]> procesos;

    public ProcesosPantalla(String ruta, String titulo, String descripcion, List<String[]> procesos) {
        super(ruta, titulo, descripcion);
        this.procesos = procesos;

        tabla.anchos(95, 310, 150, 165, 140);
        tabla.columnaEstado(4, Estados::color);
        procesos.forEach(p -> tabla.agregar((Object[]) p));

        JButton ejecutar = Ui.primario("Ejecutar ahora", "check");
        ejecutar.addActionListener(e -> ejecutar());
        JButton alternar = Ui.secundario("Habilitar / deshabilitar", "ajustes");
        alternar.addActionListener(e -> alternar());

        Tarjeta listado = new Tarjeta("Procesos definidos",
                "Un proceso deshabilitado queda documentado pero no se lanza.");
        listado.acciones(alternar, ejecutar);
        listado.sinRelleno();
        listado.add(tabla.enScroll(), BorderLayout.CENTER);

        bitacora.setEditable(false);
        bitacora.setFont(Tema.mono());
        Tarjeta log = new Tarjeta("Bitacora de ejecucion",
                "Queda en memoria mientras dure la sesion.");
        log.sinRelleno();
        log.add(Ui.scroll(bitacora), BorderLayout.CENTER);
        log.setPreferredSize(new Dimension(100, 200));

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(listado, BorderLayout.CENTER);
        raiz.add(log, BorderLayout.SOUTH);
        contenido().add(raiz, BorderLayout.CENTER);

        anotar("Modulo abierto. " + procesos.size() + " procesos cargados.");
    }

    private int indiceSeleccionado() {
        int f = tabla.getSelectedRow();
        return f < 0 ? -1 : tabla.convertRowIndexToModel(f);
    }

    private void ejecutar() {
        int i = indiceSeleccionado();
        if (i < 0) { avisar("Seleccione el proceso que desea ejecutar."); return; }

        String[] p = procesos.get(i);
        if ("Deshabilitado".equals(p[4])) {
            avisar("El proceso " + p[0] + " esta deshabilitado. Habilitelo antes de ejecutarlo.");
            return;
        }
        String ahora = LocalDateTime.now().format(RELOJ);
        p[3] = ahora.substring(0, 16);
        refrescar();
        anotar(ahora + "  " + p[0] + "  " + p[1] + "  — ejecutado correctamente");
    }

    private void alternar() {
        int i = indiceSeleccionado();
        if (i < 0) { avisar("Seleccione un proceso."); return; }

        String[] p = procesos.get(i);
        p[4] = "Habilitado".equals(p[4]) ? "Deshabilitado" : "Habilitado";
        refrescar();
        anotar(LocalDateTime.now().format(RELOJ) + "  " + p[0] + "  — ahora esta "
                + p[4].toLowerCase());
    }

    private void refrescar() {
        int seleccion = tabla.getSelectedRow();
        tabla.limpiar();
        procesos.forEach(p -> tabla.agregar((Object[]) p));
        if (seleccion >= 0 && seleccion < tabla.getRowCount()) {
            tabla.setRowSelectionInterval(seleccion, seleccion);
        }
    }

    private void anotar(String linea) {
        bitacora.append(linea + "\n");
        bitacora.setCaretPosition(bitacora.getDocument().getLength());
    }

    /* ---------------- los cuatro modulos ---------------- */

    public static ProcesosPantalla batch() {
        return new ProcesosPantalla("Mapa › Aplicativo › Batch", "Procesos batch",
                "Procesos que corren solos, fuera de la atencion en linea.",
                Datos.procesosBatch());
    }

    public static ProcesosPantalla actBd() {
        return new ProcesosPantalla("Mapa › Act-BD", "Actualizacion de base de datos",
                "Cargas y sincronizaciones de los datos maestros del sistema.",
                Datos.procesosActBd());
    }

    public static ProcesosPantalla mantBd() {
        return new ProcesosPantalla("Mapa › Tecnico › Mant-BD", "Mantenimiento de base de datos",
                "Respaldos, reorganizacion de indices y depuracion.",
                Datos.procesosMantBd());
    }

    public static ProcesosPantalla contingencia() {
        return new ProcesosPantalla("Mapa › Tecnico › Contingencia", "Plan de contingencia",
                "Que se hace si el sistema o la base dejan de responder.",
                Datos.procesosContingencia());
    }
}
