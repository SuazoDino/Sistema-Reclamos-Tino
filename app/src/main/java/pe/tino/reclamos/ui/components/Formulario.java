package pe.tino.reclamos.ui.components;

import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

/**
 * Constructor de formularios de dos columnas: etiqueta a la izquierda,
 * control a la derecha, con anchos y separaciones constantes. Evita que cada
 * pantalla reinvente su propio GridBagLayout.
 */
public class Formulario extends JPanel {

    private final GridBagConstraints gcEtiqueta = new GridBagConstraints();
    private final GridBagConstraints gcCampo = new GridBagConstraints();
    private int fila = 0;

    public Formulario() {
        super(new GridBagLayout());
        setOpaque(false);

        gcEtiqueta.gridx = 0;
        gcEtiqueta.anchor = GridBagConstraints.LINE_END;
        gcEtiqueta.insets = new Insets(Tema.ESP_XS, 0, Tema.ESP_XS, Tema.ESP_MD);

        gcCampo.gridx = 1;
        gcCampo.fill = GridBagConstraints.HORIZONTAL;
        gcCampo.insets = new Insets(Tema.ESP_XS, 0, Tema.ESP_XS, 0);

        // una tercera columna vacia absorbe el ancho sobrante: asi los campos
        // conservan un ancho de formulario y no se estiran de lado a lado
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridx = 2; relleno.gridy = 0; relleno.weightx = 1;
        relleno.fill = GridBagConstraints.HORIZONTAL;
        add(Box.createHorizontalGlue(), relleno);
    }

    /** Ancho de trabajo de la columna de campos. */
    private static final int ANCHO_CAMPO = 340;

    /** Fila etiqueta + control. */
    public Formulario campo(String etiqueta, JComponent control) {
        Dimension d = control.getPreferredSize();
        control.setMinimumSize(new Dimension(Math.min(d.width, 90), d.height));
        if (d.width < ANCHO_CAMPO) control.setPreferredSize(new Dimension(ANCHO_CAMPO, d.height));
        gcEtiqueta.gridy = fila;
        gcCampo.gridy = fila;
        add(Ui.etiqueta(etiqueta), gcEtiqueta);
        add(control, gcCampo);
        fila++;
        return this;
    }

    /** Fila etiqueta + control + texto de ayuda debajo del control. */
    public Formulario campo(String etiqueta, JComponent control, String ayuda) {
        JPanel envoltorio = Ui.panel(new BorderLayout(0, 3));
        envoltorio.add(control, BorderLayout.CENTER);
        JLabel a = Ui.suave(ayuda);
        envoltorio.add(a, BorderLayout.SOUTH);
        return campo(etiqueta, envoltorio);
    }

    /** Encabezado que abarca las dos columnas. */
    public Formulario grupo(String texto) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = fila; gc.gridwidth = 3;
        gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(fila == 0 ? 0 : Tema.ESP_MD, 0, Tema.ESP_XS, 0);
        add(Ui.fuerte(texto), gc);
        fila++;
        return this;
    }

    /** Componente libre que abarca las dos columnas (botonera, aviso, tabla). */
    public Formulario ancho(JComponent c) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = fila; gc.gridwidth = 2;
        gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(Tema.ESP_SM, 0, Tema.ESP_SM, 0);
        add(c, gc);
        fila++;
        return this;
    }

    /** Empuja todo el contenido hacia arriba cuando sobra alto. */
    public Formulario finalizar() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = fila; gc.gridwidth = 3;
        gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        add(Box.createGlue(), gc);
        return this;
    }
}
