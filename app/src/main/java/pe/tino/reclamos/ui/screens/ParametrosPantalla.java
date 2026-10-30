package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Asignacion;
import pe.tino.reclamos.repo.Auditoria;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Integridad;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Parametros;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Prototipo.Empleado;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Parametro General: generalidades de la empresa (areas y especialistas) y
 * del sistema (jornada, plazo legal, muestra de calidad, garantia).
 *
 * Todo lo que hay aqui lo lee el flujo. Magnitud e Idioma se quitaron: el
 * libro trata la magnitud como una caracteristica del perfil del sistema que
 * se decide al disenarlo, y ningun proceso usaba el idioma.
 */
public class ParametrosPantalla extends Pantalla {

    private final Tabla areas = new Tabla(new String[]{"Área", "Estado", "La usa"});
    private final Tabla especialistas = new Tabla(new String[]{"Código", "Tipo de operario", "Área", "Estado"});
    private final Tabla parametros = new Tabla(new String[]{"Parámetro", "Valor", "Unidad", "Qué cambia en el sistema"});
    private List<Empleado> listaEspecialistas = List.of();

    public ParametrosPantalla() {
        super("Parámetro General", "Generalidades de la empresa y del sistema.");

        areas.anchos(300, 120, 360);
        especialistas.anchos(90, 140, 320, 120);
        parametros.anchos(200, 90, 110, 520);

        JPanel empresa = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        empresa.add(grupo("Áreas", areas, this::estadoArea));
        empresa.add(grupo("Especialistas por área", especialistas, this::estadoEspecialista));
        empresa.setPreferredSize(Ui.dim(100, 280));

        JButton cambiar = Ui.boton("Cambiar valor");
        cambiar.addActionListener(e -> cambiarParametro());
        Grupo sistema = new Grupo("Generalidades del sistema");
        sistema.add(parametros.enScroll(), BorderLayout.CENTER);
        sistema.add(Ui.filaDerecha(cambiar), BorderLayout.SOUTH);

        contenido().add(empresa, BorderLayout.NORTH);
        contenido().add(sistema, BorderLayout.CENTER);
        refrescar();
    }

    private interface CambioEstado { void aplicar(int fila, boolean habilitar); }

    private JComponent grupo(String titulo, Tabla tabla, CambioEstado cambio) {
        JButton habilitar = Ui.boton("Habilitar");
        JButton deshabilitar = Ui.boton("Deshabilitar");
        habilitar.addActionListener(e -> cambiar(tabla, cambio, true));
        deshabilitar.addActionListener(e -> cambiar(tabla, cambio, false));
        Grupo g = new Grupo(titulo);
        g.add(tabla.enScroll(), BorderLayout.CENTER);
        g.add(Ui.filaDerecha(habilitar, deshabilitar), BorderLayout.SOUTH);
        return g;
    }

    private void cambiar(Tabla tabla, CambioEstado cambio, boolean si) {
        int i = tabla.filaModelo();
        if (i < 0) { avisar("Seleccione una fila."); return; }
        try {
            cambio.aplicar(i, si);
        } catch (IllegalArgumentException ex) {
            advertir(ex.getMessage());
        }
        refrescar();
    }

    private void estadoArea(int fila, boolean si) {
        String area = Datos.catalogo("areas").existentes().get(fila);
        Integridad.aplicar(Auditoria.SIN_SESION, "Parámetro General", (si ? "Habilitar " : "Deshabilitar ") + area,
                () -> { Catalogos.habilitarArea(area, si); return area + (si ? " habilitada." : " deshabilitada."); });
    }

    private void estadoEspecialista(int fila, boolean si) {
        avisar(Asignacion.cambiarEstado(listaEspecialistas.get(fila).id(), si, Auditoria.SIN_SESION));
    }

    private void cambiarParametro() {
        int i = parametros.filaModelo();
        if (i < 0) { avisar("Seleccione el parámetro que desea cambiar."); return; }
        String[] p = Parametros.lista().get(i);
        String valor = JOptionPane.showInputDialog(this, p[0] + " (" + p[2] + "):", p[1]);
        if (valor == null) return;
        try {
            Integridad.aplicar(Auditoria.SIN_SESION, "Generalidades del sistema", "Cambiar " + p[0],
                    () -> { Parametros.cambiar(p[0], valor); return p[0] + " = " + valor.trim() + "."; });
        } catch (IllegalArgumentException ex) {
            advertir(ex.getMessage());
        }
        refrescar();
    }

    private void refrescar() {
        var cat = Datos.catalogo("areas");
        areas.limpiar();
        cat.existentes().forEach(a -> areas.agregar(a,
                cat.habilitados().contains(a) ? "Habilitado" : "Deshabilitado", Catalogos.usoDeArea(a)));

        listaEspecialistas = Prototipo.EMPLEADOS.values().stream().filter(e -> !e.esSupervisor()).toList();
        especialistas.limpiar();
        listaEspecialistas.forEach(e -> especialistas.agregar(e.id(), e.tipo(), e.area(),
                Prototipo.habilitado(e.id()) ? "Habilitado" : "Deshabilitado"));

        parametros.limpiar();
        Parametros.lista().forEach(f -> parametros.agregar((Object[]) f));
    }
}
