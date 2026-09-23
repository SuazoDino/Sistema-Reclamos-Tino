package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Catalogo de Protocolos, como en la captura: se busca el producto y su
 * problema, se elige el evento y abajo se define el protocolo, que es la
 * lista de acciones con su tiempo maximo, operario y criticidad.
 */
public class ProtocolosPantalla extends Pantalla {

    private final JComboBox<String> tipoProblema = Ui.combo(Prototipo.tiposDeProblema());
    private final JComboBox<String> instancia = Ui.combo(Prototipo.INSTANCIA);
    private final JComboBox<String> criticidad = Ui.combo(Prototipo.CRITICIDAD);
    private final JComboBox<String> familia = Ui.combo(familias());
    private final JComboBox<String> evento = Ui.combo(Prototipo.EVENTOS_EXISTENTES);
    private final JComboBox<String> protocolo = Ui.combo(
            Prototipo.protocolos().stream().map(f -> f[0]).toList());

    private final Tabla resultados = new Tabla(new String[]{"Tipo Problema", "Problema"});
    private final Tabla acciones = new Tabla(new String[]{
            "Acción", "Tiempo Máximo (min)", "Tipo Operario", "Criticidad"});

    private final JTextField accion = Ui.texto();
    private final JTextField tiempoMaximo = Ui.texto();
    private final JComboBox<String> tipoOperario = Ui.combo(Prototipo.TIPO_OPERARIO);
    private final JComboBox<String> criticidadAccion = Ui.combo(Prototipo.CRITICIDAD_ACCION);

    private List<String[]> filas = Prototipo.accionesDe("PR-FUNC-01");

    public ProtocolosPantalla() {
        super("Catálogo de Protocolos",
                "Acciones que componen el protocolo de cada evento.");

        resultados.anchos(300, 380);
        tipoProblema.addActionListener(e -> buscar());
        buscar();

        acciones.anchos(240, 150, 160, 150);
        refrescar();

        contenido().add(cabecera(), BorderLayout.NORTH);
        contenido().add(grupoProtocolo(), BorderLayout.CENTER);
    }

    private static List<String> familias() {
        return new ArrayList<>(new LinkedHashSet<>(
                Datos.JERARQUIA_PRODUCTO.stream().map(r -> r[1]).toList()));
    }

    /** Grupos "Busqueda" y "Resultados de Busqueda", con el evento debajo. */
    private JComponent cabecera() {
        Grupo busqueda = new Grupo("Búsqueda", new GridLayout(2, 4, Tema.ESP_MD, Tema.ESP_XS));
        busqueda.add(Ui.etiqueta("Tipo Problema:"));
        busqueda.add(tipoProblema);
        busqueda.add(Ui.etiqueta("Criticidad"));
        busqueda.add(criticidad);
        busqueda.add(Ui.etiqueta("Instancia"));
        busqueda.add(instancia);
        busqueda.add(Ui.etiqueta("Familia"));
        busqueda.add(familia);

        JButton verProtocolo = Ui.boton("Protocolo");
        verProtocolo.addActionListener(e -> verProtocolo());

        protocolo.addActionListener(e -> {
            String codigo = String.valueOf(protocolo.getSelectedItem());
            Prototipo.protocolos().stream()
                    .filter(f -> f[0].equals(codigo))
                    .findFirst()
                    .ifPresent(f -> evento.setSelectedItem(f[2]));
            filas = Prototipo.accionesDe(codigo);
            refrescar();
        });

        JPanel filaEvento = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        JPanel izq = Ui.panel(new FlowLayout(FlowLayout.LEFT, Tema.ESP_MD, 0));
        izq.add(Ui.etiqueta("Evento"));
        izq.add(evento);
        izq.add(Ui.etiqueta("   Protocolo"));
        izq.add(protocolo);
        filaEvento.add(izq, BorderLayout.WEST);
        filaEvento.add(Ui.filaDerecha(verProtocolo), BorderLayout.EAST);
        filaEvento.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));

        Grupo grupoResultados = new Grupo("Resultados de Búsqueda");
        grupoResultados.add(resultados.enScroll(), BorderLayout.CENTER);
        grupoResultados.add(filaEvento, BorderLayout.SOUTH);

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        p.add(busqueda, BorderLayout.NORTH);
        p.add(grupoResultados, BorderLayout.CENTER);
        p.setPreferredSize(Ui.dim(100, 290));
        return p;
    }

    private JComponent grupoProtocolo() {
        Grupo g = new Grupo("Acciones del protocolo");

        JPanel campos = Ui.panel(new GridLayout(2, 4, Tema.ESP_MD, Tema.ESP_SM));
        campos.add(Ui.etiqueta("Acción:"));
        campos.add(accion);
        campos.add(Ui.etiqueta("Tipo Operario:"));
        campos.add(tipoOperario);
        campos.add(Ui.etiqueta("Tiempo Máximo:"));
        campos.add(tiempoMaximo);
        campos.add(Ui.etiqueta("Criticidad de Acción:"));
        campos.add(criticidadAccion);
        campos.setBorder(Ui.relleno(0, 0, Tema.ESP_MD, 0));

        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> agregar());
        JButton modificar = Ui.boton("Modificar");
        modificar.addActionListener(e -> modificar());
        JButton eliminar = Ui.boton("Eliminar");
        eliminar.addActionListener(e -> eliminar());

        JPanel columna = Ui.panel(new GridLayout(3, 1, 0, Tema.ESP_SM));
        columna.add(agregar);
        columna.add(modificar);
        columna.add(eliminar);
        JPanel botones = Ui.panel(new GridBagLayout());
        botones.add(columna);
        botones.setBorder(Ui.relleno(0, 0, 0, Tema.ESP_MD));

        JPanel centro = Ui.panel(new BorderLayout(Tema.ESP_MD, 0));
        centro.add(acciones.enScroll(), BorderLayout.CENTER);
        centro.add(botones, BorderLayout.EAST);

        g.add(campos, BorderLayout.NORTH);
        g.add(centro, BorderLayout.CENTER);
        return g;
    }

    /** Los problemas del tipo elegido en la busqueda. */
    private void buscar() {
        String tipo = String.valueOf(tipoProblema.getSelectedItem());
        resultados.limpiar();
        Prototipo.problemasDe(tipo).forEach(p -> resultados.agregar(tipo, p));
    }

    /** El protocolo sale del tipo de problema, igual que al asignar un ticket. */
    private void verProtocolo() {
        int i = resultados.filaModelo();
        if (i < 0) { avisar("Seleccione un problema."); return; }
        String tipo = String.valueOf(resultados.modelo().getValueAt(i, 0));
        protocolo.setSelectedItem(Prototipo.protocoloDe(tipo));
    }

    private void agregar() {
        if (accion.getText().isBlank()) { avisar("Indique la acción."); return; }
        filas.add(new String[]{accion.getText().trim(), tiempoMaximo.getText().trim(),
                String.valueOf(tipoOperario.getSelectedItem()),
                String.valueOf(criticidadAccion.getSelectedItem())});
        refrescar();
        limpiar();
    }

    private void modificar() {
        int i = acciones.filaModelo();
        if (i < 0) { avisar("Seleccione la acción que desea modificar."); return; }
        filas.set(i, new String[]{accion.getText().trim(), tiempoMaximo.getText().trim(),
                String.valueOf(tipoOperario.getSelectedItem()),
                String.valueOf(criticidadAccion.getSelectedItem())});
        refrescar();
    }

    private void eliminar() {
        int i = acciones.filaModelo();
        if (i < 0) { avisar("Seleccione la acción que desea eliminar."); return; }
        if (!confirmar("¿Eliminar la acción seleccionada?")) return;
        filas.remove(i);
        refrescar();
        limpiar();
    }

    private void refrescar() {
        acciones.limpiar();
        filas.forEach(f -> acciones.agregar((Object[]) f));
    }

    private void limpiar() {
        accion.setText("");
        tiempoMaximo.setText("");
    }
}
