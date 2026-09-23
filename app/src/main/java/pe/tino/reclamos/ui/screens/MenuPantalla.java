package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.ui.Navegacion;
import pe.tino.reclamos.ui.components.Tarjeta;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Iconos;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Menu de modulo: la pantalla intermedia entre el mapa y los catalogos. Una
 * rejilla de opciones grandes, cada una con su icono y una linea que explica
 * a donde lleva. Es la navegacion clasica del prototipo, en dos pasos.
 */
public class MenuPantalla extends Pantalla {

    /** Opcion del menu: a donde va, como se llama y que hace. */
    public record Opcion(String clave, String titulo, String descripcion, String icono) {}

    public MenuPantalla(String ruta, String titulo, String descripcion, List<Opcion> opciones) {
        super(ruta, titulo, descripcion);

        int columnas = opciones.size() <= 4 ? 2 : 3;
        JPanel rejilla = Ui.panel(new GridLayout(0, columnas, Tema.ESP_MD, Tema.ESP_MD));
        opciones.forEach(o -> rejilla.add(new Boton(o)));

        JPanel envoltura = Ui.panel(new BorderLayout());
        envoltura.add(rejilla, BorderLayout.NORTH);
        contenido().add(Ui.scrollVertical(envoltura), BorderLayout.CENTER);
    }

    /** Boton grande: icono, titulo y descripcion, con realce al pasar o al enfocar. */
    private static class Boton extends JButton {
        private boolean encima;

        Boton(Opcion o) {
            super();
            setLayout(new BorderLayout(Tema.ESP_MD, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(Ui.relleno(Tema.ESP_MD + 2));
            setPreferredSize(new Dimension(280, 92));
            setToolTipText(o.descripcion());
            getAccessibleContext().setAccessibleName(o.titulo());

            JLabel icono = new JLabel(Iconos.de(o.icono(), 24, Tema.acento()));
            icono.setVerticalAlignment(SwingConstants.TOP);

            JPanel textos = Ui.panel(new BorderLayout(0, 4));
            JLabel titulo = new JLabel(o.titulo());
            titulo.setFont(Tema.fuente(14, Font.BOLD));
            titulo.setForeground(Tema.texto());
            JLabel desc = new JLabel("<html><body style='width:210px'>" + o.descripcion() + "</body></html>");
            desc.setFont(Tema.micro());
            desc.setForeground(Tema.textoSuave());
            textos.add(titulo, BorderLayout.NORTH);
            textos.add(desc, BorderLayout.CENTER);

            add(icono, BorderLayout.WEST);
            add(textos, BorderLayout.CENTER);

            addActionListener(e -> Navegacion.ir(o.clave()));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { encima = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean resaltado = encima || hasFocus();
            g2.setColor(resaltado ? Tema.acentoSuave() : Tema.superficie());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.setColor(resaltado ? Tema.acento() : Tema.borde());
            g2.setStroke(new BasicStroke(resaltado ? 1.8f : 1f));
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /* ---------------- definicion de los menus del sistema ---------------- */

    public static MenuPantalla aplicativo() {
        return new MenuPantalla("Mapa › Aplicativo", "Modulo Aplicativo",
                "Los procesos que atienden el dia a dia de los reclamos.", List.of(
                new Opcion("m-operativo", "Operativo", "Registro y atencion de reclamos por area y por cliente.", "personas"),
                new Opcion("m-gerencial", "Gerencial", "Consulta, indicadores y parametros del sistema.", "grafico"),
                new Opcion("atencion", "On-Line", "Atencion en linea: cola de trabajo del especialista.", "bandeja"),
                new Opcion("batch", "Batch", "Procesos programados que corren sin operador.", "reloj"),
                new Opcion("actbd", "Act-BD", "Cargas y sincronizaciones contra la base de datos.", "base"),
                new Opcion("estadisticas", "Estadisticas", "Los indicadores de gestion del proceso.", "grafico")));
    }

    public static MenuPantalla tecnico() {
        return new MenuPantalla("Mapa › Tecnico", "Modulo Tecnico",
                "Soporte de la plataforma: base de datos y continuidad del servicio.", List.of(
                new Opcion("mantbd", "Mant-BD", "Respaldos, indices y depuracion de la base.", "base"),
                new Opcion("contingencia", "Contingencia", "Plan de continuidad ante caida del sistema.", "escudo"),
                new Opcion("actbd", "Act-BD", "Actualizacion y carga de datos maestros.", "base"),
                new Opcion("m-mantparam", "Mant-Param", "Catalogos y parametros que configuran el sistema.", "ajustes")));
    }

    public static MenuPantalla operativo() {
        return new MenuPantalla("Mapa › Aplicativo › Operativo", "Modulo Operativo",
                "Por donde entra y se atiende un reclamo.", List.of(
                new Opcion("m-area", "Area", "Lo que hace el area: registro y reportes internos.", "personas"),
                new Opcion("m-cliente", "Cliente", "Lo que ve el cliente: registro y reportes de su reclamo.", "personas"),
                new Opcion("atencion", "Atencion de reclamos", "Cola de trabajo, asignacion y avance de estado.", "bandeja"),
                new Opcion("consulta", "Consulta de reclamos", "Seguimiento por estado, area y texto libre.", "lupa")));
    }

    public static MenuPantalla gerencial() {
        return new MenuPantalla("Mapa › Aplicativo › Gerencial", "Modulo Gerencial",
                "La vista de conduccion: que se mide y que se parametriza.", List.of(
                new Opcion("m-mantparam", "Mant-Param", "Catalogos, protocolos y reglas de negocio.", "ajustes"),
                new Opcion("consulta", "Consulta", "Consulta de reclamos con filtros.", "lupa"),
                new Opcion("estadisticas", "Estadisticas", "Los indicadores de gestion.", "grafico"),
                new Opcion("reportes", "Reportes", "Reportes operativos y gerenciales.", "reporte")));
    }

    public static MenuPantalla area() {
        return new MenuPantalla("Mapa › Operativo › Area", "Modulo Area",
                "Lo que opera el personal del area responsable.", List.of(
                new Opcion("registro", "Data Entry", "Registro de un reclamo recibido en el area.", "formulario"),
                new Opcion("reportes", "Reportes", "Reportes de carga y desempenio del area.", "reporte"),
                new Opcion("atencion", "Atencion", "Cola de reclamos asignados al area.", "bandeja")));
    }

    public static MenuPantalla cliente() {
        return new MenuPantalla("Mapa › Operativo › Cliente", "Modulo Cliente",
                "Lo que el cliente puede hacer sobre su reclamo.", List.of(
                new Opcion("registro", "Data Entry", "Alta del reclamo a nombre del cliente.", "formulario"),
                new Opcion("reportes", "Reportes", "Constancia y estado del reclamo del cliente.", "reporte"),
                new Opcion("consulta", "Consulta", "Seguimiento del reclamo por codigo o documento.", "lupa")));
    }

    public static MenuPantalla mantParam() {
        return new MenuPantalla("Mapa › Mant-Param", "Mantenimiento de parametros",
                "Los catalogos del sistema, tal como los lista la hoja de Seguridad del prototipo.", List.of(
                new Opcion("productos", "Catalogo de Productos", "Jerarquia Segmento › Familia › Clase › Bien.", "jerarquia"),
                new Opcion("bienes", "Catalogo de Bienes", "Tipos de bien y el tipo de problema que admiten.", "jerarquia"),
                new Opcion("problemas", "Catalogo de Problemas", "Problemas reconocidos por segmento y familia.", "formulario"),
                new Opcion("protocolos", "Catalogo de Protocolos", "Pasos de atencion por tipo de reclamo.", "flujo"),
                new Opcion("reglas", "Catalogo de Reglas", "Condiciones encadenadas del motor de decision.", "reglas"),
                new Opcion("politicas", "Catalogo de Politicas", "Garantias legales, explicitas e implicitas.", "escudo"),
                new Opcion("categorizacion", "Categorizacion Cliente", "Condiciones, metodos y formulas de segmentacion.", "personas"),
                new Opcion("catalogos", "Catalogos generales", "Areas, empleados, locales, idiomas y demas tablas.", "ajustes")));
    }

    public static MenuPantalla seguridad() {
        return new MenuPantalla("Mapa › Seguridad", "Modulo Seguridad",
                "Quien entra al sistema y que puede abrir.", List.of(
                new Opcion("seguridad-perfiles", "Perfiles y accesos", "Matriz de perfiles contra modulos del sistema.", "escudo"),
                new Opcion("catalogos", "Catalogos generales", "Empleados y areas habilitadas.", "ajustes")));
    }
}
