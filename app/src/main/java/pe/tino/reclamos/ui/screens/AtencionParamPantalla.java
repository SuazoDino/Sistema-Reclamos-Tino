package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Dominio;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Tabla;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Parametros de Atencion: los parametros que gobiernan el ciclo de vida del
 * ticket y que antes se decidian dentro del codigo o no existian.
 *
 * No es un catalogo: no enumera cosas del negocio, fija las variables que
 * definen su escenario. Es lo que el libro llama MANT-PARAM.
 *
 * Reune canales, instancias con su plazo de impugnacion, tiempos de atencion
 * (el SLA), la regla de prioridad y la maquina de estados.
 */
public class AtencionParamPantalla extends Pantalla {

    public AtencionParamPantalla() {
        super("Parametros de Atencion",
                "Canales, instancias, plazos, prioridad y estados del ticket.");

        JTabbedPane pestanias = new JTabbedPane();
        pestanias.setFont(Tema.cuerpo());
        pestanias.addTab("Canales", canales());
        pestanias.addTab("Instancias", instancias());
        pestanias.addTab("Tiempos de atencion", tiempos());
        pestanias.addTab("Prioridad", prioridad());
        pestanias.addTab("Estados", estados());

        contenido().add(pestanias, BorderLayout.CENTER);
    }

    private JComponent canales() {
        Tabla t = new Tabla(new String[]{"Canal", "Horario", "Requiere local", "Estado"});
        Catalogos.canales().forEach(f -> t.agregar((Object[]) f));
        t.anchos(160, 300, 150, 150);
        return envolver(t, "Canales de ingreso",
                "Por donde puede entrar un reclamo. Un canal deshabilitado no ofrece atencion.");
    }

    private JComponent instancias() {
        Tabla t = new Tabla(new String[]{
                "Instancia", "Quien resuelve", "Dias de impugnacion", "Admite impugnacion"});
        Catalogos.instancias().forEach(f -> t.agregar((Object[]) f));
        t.anchos(150, 280, 180, 170).centrar(2, 3);
        return envolver(t, "Instancias de atencion",
                "De aqui sale la fecha limite de impugnacion del ticket. "
                        + "La ultima instancia no admite impugnacion.");
    }

    private JComponent tiempos() {
        Tabla t = new Tabla(new String[]{"Tipo de cliente", "Prioridad", "Tiempo", "Unidad"});
        Catalogos.tiemposAtencion().forEach(f -> t.agregar((Object[]) f));
        t.anchos(180, 150, 120, 150).centrar(2);
        return envolver(t, "Tiempos de atencion",
                "El plazo que tiene el area segun quien reclama y que tan urgente es. "
                        + "Es lo que conecta la categorizacion del cliente con la operacion.");
    }

    private JComponent prioridad() {
        Tabla t = new Tabla(new String[]{"Tipo de problema", "Tipo de cliente", "Prioridad"});
        Catalogos.reglasPrioridad().forEach(f -> t.agregar((Object[]) f));
        t.anchos(220, 200, 160);
        return envolver(t, "Regla de prioridad",
                "Con que prioridad nace el ticket. Antes la criticidad se cargaba a mano "
                        + "y no se derivaba de nada.");
    }

    private JComponent estados() {
        Tabla t = new Tabla(new String[]{"Estado", "Que significa", "Puede pasar a"});
        Catalogos.estados().forEach(f -> t.agregar((Object[]) f));
        t.anchos(150, 340, 260);
        return envolver(t, "Estados del ticket",
                "Es la maquina de estados: un ticket solo puede moverse a los estados "
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
