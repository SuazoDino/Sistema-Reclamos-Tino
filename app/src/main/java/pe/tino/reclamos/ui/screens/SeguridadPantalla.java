package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Perfil;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.Grupo;
import pe.tino.reclamos.ui.components.Ui;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seguridad - Perfiles y Accesos.
 *
 * Reproduce la hoja SEGURIDAD: los perfiles del sistema, los accesos que se
 * les puede conceder y los permisos especiales. La hoja solo documenta la
 * asignacion del perfil gerencial; el resto se asigna desde aqui.
 */
public class SeguridadPantalla extends Pantalla {

    private final DefaultListModel<Perfil> modeloPerfiles = new DefaultListModel<>();
    private final JList<Perfil> perfiles = new JList<>(modeloPerfiles);
    private final Map<String, JCheckBox> accesos = new LinkedHashMap<>();
    private final Map<String, JCheckBox> permisos = new LinkedHashMap<>();
    private final JLabel resumen = Ui.suave("");

    public SeguridadPantalla() {
        super("Seguridad - Perfiles y Accesos",
                "Que modulos puede abrir cada perfil de usuario del sistema.");

        Estado.perfiles().forEach(modeloPerfiles::addElement);
        perfiles.setFont(Tema.cuerpo());
        perfiles.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarPerfil();
        });

        contenido().add(panelPerfiles(), BorderLayout.WEST);
        contenido().add(panelAccesos(), BorderLayout.CENTER);

        perfiles.setSelectedIndex(0);
    }

    private JComponent panelPerfiles() {
        Grupo g = Grupo.ajustado("Perfiles");
        g.setPreferredSize(Ui.dim(250, 100));
        g.add(Ui.scroll(perfiles), BorderLayout.CENTER);

        JTextField nuevo = Ui.texto(10);
        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> {
            String nombre = nuevo.getText().trim();
            if (nombre.isEmpty()) { avisar("Escriba el nombre del perfil."); return; }
            Perfil p = new Perfil(nombre, List.of());
            Estado.perfiles().add(p);
            modeloPerfiles.addElement(p);
            nuevo.setText("");
            perfiles.setSelectedValue(p, true);
        });

        JPanel pie = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        pie.setBorder(Ui.relleno(Tema.ESP_SM));
        pie.add(nuevo, BorderLayout.CENTER);
        pie.add(agregar, BorderLayout.EAST);
        g.add(pie, BorderLayout.SOUTH);
        return g;
    }

    private JComponent panelAccesos() {
        Grupo gAccesos = new Grupo("Accesos del perfil");
        JPanel rejilla = Ui.panel(new GridLayout(0, 2, Tema.ESP_MD, Tema.ESP_XS));
        for (String acceso : Datos.ACCESOS) {
            JCheckBox c = new JCheckBox(acceso);
            c.setFont(Tema.cuerpo());
            c.setOpaque(false);
            accesos.put(acceso, c);
            rejilla.add(c);
        }
        JPanel envoltura = Ui.panel(new BorderLayout());
        envoltura.add(rejilla, BorderLayout.NORTH);
        gAccesos.add(resumen, BorderLayout.NORTH);
        gAccesos.add(envoltura, BorderLayout.CENTER);

        Grupo gPermisos = new Grupo("Permisos");
        JPanel lista = Ui.panel(new GridLayout(0, 1, 0, Tema.ESP_XS));
        for (String permiso : Datos.PERMISOS) {
            JCheckBox c = new JCheckBox(permiso);
            c.setFont(Tema.cuerpo());
            c.setOpaque(false);
            permisos.put(permiso, c);
            lista.add(c);
        }
        gPermisos.add(lista, BorderLayout.CENTER);

        JButton guardar = Ui.boton("Guardar");
        guardar.addActionListener(e -> guardar());
        JButton todos = Ui.boton("Marcar todos");
        todos.addActionListener(e -> accesos.values().forEach(c -> c.setSelected(true)));
        JButton ninguno = Ui.boton("Desmarcar todos");
        ninguno.addActionListener(e -> accesos.values().forEach(c -> c.setSelected(false)));

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(Ui.fila(todos, ninguno), BorderLayout.WEST);
        pie.add(Ui.filaDerecha(guardar), BorderLayout.EAST);

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(gAccesos, BorderLayout.CENTER);
        JPanel abajo = Ui.panel(new BorderLayout(0, Tema.ESP_SM));
        abajo.add(gPermisos, BorderLayout.CENTER);
        abajo.add(pie, BorderLayout.SOUTH);
        p.add(abajo, BorderLayout.SOUTH);
        return p;
    }

    private void cargarPerfil() {
        Perfil p = perfiles.getSelectedValue();
        if (p == null) return;
        resumen.setText(p.accesos().size() + " de " + Datos.ACCESOS.size()
                + " accesos concedidos al perfil " + p.nombre() + ".");
        accesos.forEach((acceso, casilla) -> casilla.setSelected(p.accesos().contains(acceso)));
    }

    private void guardar() {
        int i = perfiles.getSelectedIndex();
        if (i < 0) { avisar("Seleccione un perfil."); return; }

        List<String> concedidos = new ArrayList<>();
        accesos.forEach((acceso, casilla) -> { if (casilla.isSelected()) concedidos.add(acceso); });

        Perfil actualizado = new Perfil(perfiles.getSelectedValue().nombre(), concedidos);
        Estado.perfiles().set(i, actualizado);
        modeloPerfiles.set(i, actualizado);
        cargarPerfil();
        avisar("Accesos del perfil \"" + actualizado.nombre() + "\" actualizados.");
    }
}
