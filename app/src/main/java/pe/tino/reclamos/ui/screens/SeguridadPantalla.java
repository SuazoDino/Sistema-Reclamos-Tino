package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.Perfil;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modulo de Seguridad. Perfiles a la izquierda, matriz de accesos a la
 * derecha: marcar una casilla concede el acceso al perfil seleccionado.
 */
public class SeguridadPantalla extends Pantalla {

    private final DefaultListModel<Perfil> modeloPerfiles = new DefaultListModel<>();
    private final JList<Perfil> perfiles = new JList<>(modeloPerfiles);
    private final Map<String, JCheckBox> casillas = new LinkedHashMap<>();
    private final JLabel cabeceraAccesos = Ui.suave("Seleccione un perfil.");

    public SeguridadPantalla() {
        super("Seguridad", "Perfiles y accesos",
                "Que modulos puede abrir cada perfil de usuario del sistema.");

        Estado.perfiles().forEach(modeloPerfiles::addElement);
        perfiles.setFont(Tema.cuerpo());
        perfiles.setFixedCellHeight(30);
        perfiles.setBorder(Ui.relleno(Tema.ESP_SM));
        perfiles.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarPerfil();
        });

        contenido().add(panelPerfiles(), BorderLayout.WEST);
        contenido().add(panelAccesos(), BorderLayout.CENTER);

        perfiles.setSelectedIndex(0);
    }

    private JComponent panelPerfiles() {
        Tarjeta t = new Tarjeta("Perfiles", "Definidos en el diseno arquitectonico.");
        t.setPreferredSize(new Dimension(280, 100));
        t.sinRelleno();
        t.add(Ui.scroll(perfiles), BorderLayout.CENTER);

        JButton nuevo = Ui.secundario("Nuevo perfil", "mas");
        nuevo.addActionListener(e -> crearPerfil());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.borde()),
                Ui.relleno(Tema.ESP_SM + 2)));
        pie.add(nuevo, BorderLayout.CENTER);
        t.cuerpo().add(pie, BorderLayout.SOUTH);
        return t;
    }

    private JComponent panelAccesos() {
        Tarjeta t = new Tarjeta("Accesos del perfil",
                "Los cambios se guardan sobre el perfil seleccionado.");

        JPanel grilla = Ui.panel(new GridLayout(0, 2, Tema.ESP_MD, Tema.ESP_SM));
        for (String acceso : Datos.ACCESOS) {
            JCheckBox c = new JCheckBox(acceso);
            c.setFont(Tema.cuerpo());
            c.setOpaque(false);
            casillas.put(acceso, c);
            grilla.add(c);
        }

        JButton guardar = Ui.primario("Guardar accesos", "guardar");
        guardar.addActionListener(e -> guardar());
        JButton todos = Ui.plano("Marcar todos", "check");
        todos.addActionListener(e -> casillas.values().forEach(c -> c.setSelected(true)));
        JButton ninguno = Ui.plano("Desmarcar todos", "cruz");
        ninguno.addActionListener(e -> casillas.values().forEach(c -> c.setSelected(false)));

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_LG, 0, 0, 0));
        pie.add(Ui.fila(Tema.ESP_SM, todos, ninguno), BorderLayout.WEST);
        pie.add(Ui.filaDerecha(Tema.ESP_SM, guardar), BorderLayout.EAST);

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(cabeceraAccesos, BorderLayout.NORTH);
        JPanel envoltura = Ui.panel(new BorderLayout());
        envoltura.add(grilla, BorderLayout.NORTH);
        cuerpo.add(envoltura, BorderLayout.CENTER);
        cuerpo.add(pie, BorderLayout.SOUTH);

        t.add(Ui.scrollVertical(cuerpo), BorderLayout.CENTER);
        return t;
    }

    private void cargarPerfil() {
        Perfil p = perfiles.getSelectedValue();
        if (p == null) return;
        cabeceraAccesos.setText("<html>Perfil <b>" + p.nombre() + "</b> · "
                + p.accesos().size() + " de " + Datos.ACCESOS.size() + " accesos concedidos.</html>");
        casillas.forEach((acceso, casilla) -> casilla.setSelected(p.accesos().contains(acceso)));
    }

    private void guardar() {
        int i = perfiles.getSelectedIndex();
        if (i < 0) { avisar("Seleccione un perfil."); return; }

        List<String> concedidos = new ArrayList<>();
        casillas.forEach((acceso, casilla) -> { if (casilla.isSelected()) concedidos.add(acceso); });

        Perfil actualizado = new Perfil(perfiles.getSelectedValue().nombre(), concedidos);
        Estado.perfiles().set(i, actualizado);
        modeloPerfiles.set(i, actualizado);
        cargarPerfil();
        avisar("Accesos del perfil \"" + actualizado.nombre() + "\" actualizados.");
    }

    private void crearPerfil() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del nuevo perfil:",
                "Nuevo perfil", JOptionPane.QUESTION_MESSAGE);
        if (nombre == null || nombre.isBlank()) return;

        Perfil p = new Perfil(nombre.trim(), List.of("General"));
        Estado.perfiles().add(p);
        modeloPerfiles.addElement(p);
        perfiles.setSelectedValue(p, true);
    }
}
