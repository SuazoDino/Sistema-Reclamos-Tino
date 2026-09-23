package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Prototipo.Persona;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario Reclamo (Cliente - Data Entry), como en las capturas: primero
 * los datos personales con el boton Validar, despues las compras del cliente
 * y los reclamos que arma sobre los productos comprados.
 */
public class ClienteDataEntryPantalla extends Pantalla {

    private final JTextField documento = Ui.texto(16);
    private final JTextField nombre = Ui.soloLectura();
    private final JTextField apellido = Ui.soloLectura();
    private final JTextField correo = Ui.soloLectura();
    private final JTextField direccion = Ui.soloLectura();
    private final JTextField telefono = Ui.soloLectura();

    private final Tabla compras = new Tabla(new String[]{"Cod. Compra", "Fecha", "Monto"});
    private final JComboBox<String> producto = Ui.combo(Prototipo.PRODUCTOS_COMPRA);
    private final JComboBox<String> tipoProblema = Ui.combo(Prototipo.tiposDeProblema());
    private final JComboBox<String> problema = new JComboBox<>();
    private final Tabla reclamos = new Tabla(new String[]{"Producto", "Tipo Problema", "Problema"});

    private final JPanel paso2 = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
    private final List<String[]> lineas = new ArrayList<>();
    private Persona persona;

    public ClienteDataEntryPantalla() {
        super("Formulario Reclamo", "Registro del reclamo del cliente sobre una compra.");

        tipoProblema.addActionListener(e -> recargarProblemas());
        recargarProblemas();

        compras.anchos(160, 180, 160);
        reclamos.anchos(200, 200, 260);

        armarPaso2();
        paso2.setVisible(false);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(datosPersonales(), BorderLayout.NORTH);
        raiz.add(paso2, BorderLayout.CENTER);

        Grupo registrar = new Grupo("Registrar Reclamo");
        registrar.add(raiz, BorderLayout.CENTER);
        contenido().add(registrar, BorderLayout.CENTER);
    }

    /* ---------------- paso 1: datos personales ---------------- */

    private JComponent datosPersonales() {
        JButton validar = Ui.boton("Validar");
        validar.addActionListener(e -> validar());
        documento.addActionListener(e -> validar());

        Formulario f = new Formulario();
        f.campo("DNI/RUC:", Ui.fila(documento, validar))
         .campo("NOMBRE:", nombre)
         .campo("APELLIDO:", apellido)
         .campo("Correo Electronico:", correo)
         .campo("Direccion:", direccion)
         .campo("Telefono:", telefono);

        JButton aceptar = Ui.boton("Aceptar");
        aceptar.addActionListener(e -> aceptar());
        JButton cancelar = Ui.boton("Cancelar");
        cancelar.addActionListener(e -> limpiar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 3, Tema.ESP_SM));
        pie.add(aceptar);
        pie.add(cancelar);

        Grupo g = new Grupo("Datos Personales");
        g.add(f, BorderLayout.CENTER);
        g.add(pie, BorderLayout.SOUTH);
        return g;
    }

    private void validar() {
        persona = Prototipo.PERSONAS.get(documento.getText().trim());
        if (persona == null) {
            nombre.setText("");
            apellido.setText("");
            correo.setText("");
            direccion.setText("");
            telefono.setText("");
            JOptionPane.showMessageDialog(this, "Su usuario no es valido.",
                    "Validacion de Usuario", JOptionPane.WARNING_MESSAGE);
            return;
        }
        nombre.setText(persona.nombres());
        apellido.setText(persona.apellidos());
        correo.setText(persona.correo());
        direccion.setText(persona.direccion());
        telefono.setText(persona.telefono());
        JOptionPane.showMessageDialog(this, "Su usuario es valido!",
                "Validacion de Usuario", JOptionPane.INFORMATION_MESSAGE);
    }

    private void aceptar() {
        if (persona == null) { avisar("Primero valide el documento del cliente."); return; }
        compras.limpiar();
        Prototipo.comprasDe(persona.documento()).forEach(f -> compras.agregar((Object[]) f));
        paso2.setVisible(true);
        paso2.revalidate();
    }

    /* ---------------- paso 2: compras y reclamo ---------------- */

    private void armarPaso2() {
        Grupo gCompras = Grupo.ajustado("Compras");
        gCompras.add(compras.enScroll(), BorderLayout.CENTER);
        gCompras.setPreferredSize(new Dimension(100, 160));

        JButton agregar = Ui.boton("Agregar");
        agregar.addActionListener(e -> agregarLinea());

        Formulario f = new Formulario();
        f.campo("Producto:", producto)
         .campo("Tipo de Problema:", tipoProblema)
         .campo("Problema:", problema);

        JPanel pieAlta = Ui.panel(new FlowLayout(FlowLayout.CENTER, 0, Tema.ESP_SM));
        pieAlta.add(agregar);

        Grupo gDatos = new Grupo("Datos de Reclamo");
        gDatos.add(f, BorderLayout.CENTER);
        gDatos.add(pieAlta, BorderLayout.SOUTH);

        Grupo gLista = Grupo.ajustado("Reclamos a registrar");
        gLista.add(reclamos.enScroll(), BorderLayout.CENTER);

        JPanel medio = Ui.panel(new GridLayout(1, 2, Tema.ESP_MD, 0));
        medio.add(gDatos);
        medio.add(gLista);

        JButton confirmar = Ui.boton("Confirmar");
        confirmar.addActionListener(e -> confirmarReclamo());
        JButton cancelar = Ui.boton("Cancelar");
        cancelar.addActionListener(e -> limpiar());

        JPanel pie = Ui.panel(new FlowLayout(FlowLayout.CENTER, Tema.ESP_LG * 3, Tema.ESP_MD));
        pie.add(confirmar);
        pie.add(cancelar);

        paso2.add(gCompras, BorderLayout.NORTH);
        paso2.add(medio, BorderLayout.CENTER);
        paso2.add(pie, BorderLayout.SOUTH);
    }

    private void recargarProblemas() {
        problema.setModel(new DefaultComboBoxModel<>(
                Prototipo.problemasDe((String) tipoProblema.getSelectedItem()).toArray(new String[0])));
        problema.setFont(Tema.cuerpo());
    }

    private void agregarLinea() {
        if (compras.filaModelo() < 0) { avisar("Seleccione la compra del listado."); return; }
        String[] linea = {String.valueOf(producto.getSelectedItem()),
                String.valueOf(tipoProblema.getSelectedItem()),
                String.valueOf(problema.getSelectedItem())};
        lineas.add(linea);
        reclamos.agregar((Object[]) linea);
    }

    private void confirmarReclamo() {
        if (lineas.isEmpty()) { avisar("Agregue al menos un reclamo."); return; }
        if (!confirmar("Confirma el registro de " + lineas.size() + " reclamo(s)?")) return;
        mostrarResumen();
        limpiar();
    }

    /** El cuadro resumen que el prototipo muestra al final. */
    private void mostrarResumen() {
        Tabla resumen = new Tabla(new String[]{"Producto", "Tipo Problema", "Problema", "Estado"});
        resumen.anchos(180, 170, 230, 130);
        lineas.forEach(l -> resumen.agregar(l[0], l[1], l[2], "Registrado"));

        JPanel p = Ui.panel(new BorderLayout(0, Tema.ESP_SM));
        p.add(Ui.fuerte("Reclamos registrados a nombre de " + persona.nombres()
                + " " + persona.apellidos()), BorderLayout.NORTH);
        p.add(resumen.enScroll(), BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(740, 220));

        JOptionPane.showMessageDialog(this, p, "Resumen del Reclamo",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void limpiar() {
        documento.setText("");
        nombre.setText("");
        apellido.setText("");
        correo.setText("");
        direccion.setText("");
        telefono.setText("");
        compras.limpiar();
        reclamos.limpiar();
        lineas.clear();
        persona = null;
        paso2.setVisible(false);
        documento.requestFocusInWindow();
    }
}
