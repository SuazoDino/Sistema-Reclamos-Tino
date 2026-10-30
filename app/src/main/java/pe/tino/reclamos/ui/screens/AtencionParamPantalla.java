package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Dominio;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.screens.TablaCatalogo.Campo;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Parametros de Atencion: los parametros que gobiernan el ciclo de vida del
 * reclamo y que antes se decidian dentro del codigo o no existian.
 *
 * No es un catalogo: no enumera cosas del negocio, fija las variables que
 * definen su escenario. Es lo que el libro llama MANT-PARAM.
 *
 * Reune canales, instancias con su plazo de impugnacion, tiempos de atencion
 * (el SLA), la regla de prioridad y la maquina de estados. Tambien lo que
 * usa la asignacion automatica: el area de cada tipo de problema, el
 * horario laboral y los feriados de la agenda.
 *
 * Canales y locales se editan sobre la misma lista que lee el formulario:
 * lo que se deshabilita aqui deja de ofrecerse sin reiniciar.
 */
public class AtencionParamPantalla extends Pantalla {

    public AtencionParamPantalla() {
        super("Parámetros de Atención",
                "Canales, instancias, plazos, prioridad y estados del reclamo.");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());
        pestanias.addTab("Canales", canales());
        pestanias.addTab("Locales", locales());
        pestanias.addTab("Área por tipo de problema", areas());
        pestanias.addTab("Feriados", horario());
        pestanias.addTab("Instancias", instancias());
        pestanias.addTab("Tiempos de atención", tiempos());
        pestanias.addTab("Prioridad", prioridad());
        pestanias.addTab("Estados", estados());

        contenido().add(pestanias, BorderLayout.CENTER);
    }

    private static final List<String> ESTADOS = List.of("Habilitado", "Deshabilitado");

    private JComponent canales() {
        return editable(new TablaCatalogo(this, "Canales de ingreso",
                List.of(Campo.lista("Canal", java.util.Arrays.stream(Dominio.Canal.values())
                                .map(Dominio.Canal::etiqueta).toList()),
                        Campo.libre("Horario"),
                        Campo.lista("Requiere local", List.of("Sí", "No")),
                        Campo.lista("Estado", ESTADOS)),
                Catalogos.canales(), new int[]{160, 300, 150, 150}));
    }

    private JComponent locales() {
        return editable(new TablaCatalogo(this, "Locales",
                List.of(Campo.libre("Local"), Campo.libre("Dirección"), Campo.lista("Estado", ESTADOS)),
                Catalogos.locales(), new int[]{120, 400, 150}));
    }

    private JComponent areas() {
        return editable(new TablaCatalogo(this, "Área responsable por tipo de problema",
                List.of(Campo.lista("Tipo de problema", Prototipo.tiposDeProblema()),
                        Campo.lista("Área responsable", Datos.catalogo("areas").habilitados())),
                Catalogos.areasPorTipo(), new int[]{220, 360}));
    }

    private JComponent horario() {
        Tabla t = new Tabla(new String[]{"Fecha", "Feriado"});
        Catalogos.feriados().forEach(f -> t.agregar((Object[]) f));
        t.anchos(160, 220);
        return envolver(t, "Feriados: lunes a sábado, sin feriados",
                "La hora de inicio y fin de la jornada está en Parámetro General.");
    }

    private JComponent editable(TablaCatalogo tabla) {
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(tabla, BorderLayout.CENTER);
        return p;
    }

    private JComponent instancias() {
        Tabla t = new Tabla(new String[]{
                "Instancia", "Quién resuelve", "Días de impugnación", "Admite impugnación"});
        Catalogos.instancias().forEach(f -> t.agregar((Object[]) f));
        t.anchos(150, 280, 180, 170).centrar(2, 3);
        return envolver(t, "Instancias de atención",
                "De aquí sale la fecha límite de impugnación del reclamo. "
                        + "La última instancia no admite impugnación.");
    }

    private JComponent tiempos() {
        return editable(new TablaCatalogo(this, "Tiempos de atención (horas laborables; un día es una jornada)",
                List.of(Campo.lista("Tipo de cliente", Datos.catalogo("clientes").habilitados()),
                        Campo.lista("Prioridad", List.of("Alta", "Media", "Baja")),
                        Campo.libre("Tiempo"),
                        Campo.lista("Unidad", List.of("Horas", "Días"))),
                Catalogos.tiemposAtencion(), new int[]{180, 150, 120, 150}));
    }

    private JComponent prioridad() {
        return editable(new TablaCatalogo(this, "Regla de prioridad",
                List.of(Campo.lista("Tipo de problema", Prototipo.tiposDeProblema()),
                        Campo.lista("Tipo de cliente", Datos.catalogo("clientes").habilitados()),
                        Campo.lista("Prioridad", List.of("Alta", "Media", "Baja"))),
                Catalogos.reglasPrioridad(), new int[]{220, 200, 160}));
    }

    private JComponent estados() {
        Tabla t = new Tabla(new String[]{"Estado", "Qué significa", "Puede pasar a"});
        Catalogos.estados().forEach(f -> t.agregar((Object[]) f));
        t.anchos(150, 340, 260);
        return envolver(t, "Estados del reclamo",
                "Es la máquina de estados: un reclamo solo puede moverse a los estados "
                        + "que su estado actual permite.");
    }

    private JComponent envolver(Tabla tabla, String titulo, String explicacion) {
        Grupo g = Grupo.ajustado(titulo);
        g.add(tabla.enScroll(), BorderLayout.CENTER);

        JPanel nota = Ui.panel(new BorderLayout());
        nota.setBorder(Ui.relleno(Tema.ESP_SM));
        nota.add(Ui.suave(explicacion), BorderLayout.WEST);
        g.add(nota, BorderLayout.SOUTH);

        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(Ui.relleno(Tema.ESP_MD));
        p.add(g, BorderLayout.CENTER);
        return p;
    }
}
