package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.*;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Entry - Formulario de reclamo. Reproduce la hoja DATA_ENTRY_CLIENTE:
 * se identifica al cliente por documento, se elige la compra y el producto y
 * se clasifica el problema en dos niveles (tipo -> problema).
 */
public class RegistroPantalla extends Pantalla {

    private final JTextField documento = Ui.texto("DNI de 8 digitos o RUC de 11");
    private final JTextField nombre = Ui.texto("Se completa al identificar al cliente");
    private final JTextField correo = Ui.texto("correo@dominio.com");
    private final JPanel categoria = Ui.panel(new BorderLayout());

    private final JTextField nroCompra = Ui.texto("COMP000");
    private final JComboBox<Producto> producto = Ui.combo(Datos.PRODUCTOS);
    private final JComboBox<String> tipoProblema = Ui.combo(Datos.tiposDeProblema());
    private final JComboBox<String> problema = new JComboBox<>();
    private final JComboBox<Canal> canal = Ui.combo(List.of(Canal.values()));
    private final JTextArea detalle = Ui.area("Describa el problema tal como lo relata el cliente.", 5);

    private Cliente clienteActual;

    public RegistroPantalla() {
        super("Aplicativo - Data Entry", "Registro de reclamo",
                "Alta de un reclamo nuevo a nombre de un cliente identificado.");

        nombre.setEditable(false);
        correo.setEditable(false);
        mostrarCategoria(null);

        tipoProblema.addActionListener(e -> recargarProblemas());
        recargarProblemas();

        JButton identificar = Ui.secundario("Identificar", "lupa");
        identificar.addActionListener(e -> identificarCliente());
        documento.addActionListener(e -> identificarCliente());

        JPanel filaDocumento = Ui.panel(new BorderLayout(Tema.ESP_SM, 0));
        filaDocumento.add(documento, BorderLayout.CENTER);
        filaDocumento.add(identificar, BorderLayout.EAST);

        Formulario f = new Formulario();
        f.grupo("Datos del cliente")
         .campo("DNI / RUC", filaDocumento,
                 "Prueba con 10112387203, 70775031, 76729288 o 20548712341.")
         .campo("Nombre", nombre)
         .campo("Correo electronico", correo)
         .campo("Categoria", categoria)
         .grupo("Datos del reclamo")
         .campo("Nro de compra", nroCompra)
         .campo("Producto", producto)
         .campo("Tipo de problema", tipoProblema)
         .campo("Problema", problema)
         .campo("Canal de ingreso", canal)
         .campo("Detalle", Ui.scrollConBorde(detalle))
         .finalizar();

        JButton registrar = Ui.primario("Registrar reclamo", "guardar");
        registrar.addActionListener(e -> registrar());
        JButton limpiar = Ui.secundario("Limpiar", "cruz");
        limpiar.addActionListener(e -> limpiar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.borde()),
                Ui.relleno(Tema.ESP_MD)));
        // el texto va en CENTER para que se recorte en vez de pisar los botones
        pie.add(Ui.micro("El reclamo se crea en estado \"En cola\" y se deriva al area "
                + "que corresponde al tipo de problema."), BorderLayout.CENTER);
        pie.add(Ui.filaDerecha(Tema.ESP_SM, limpiar, registrar), BorderLayout.EAST);

        Tarjeta tarjeta = new Tarjeta("Formulario de reclamo",
                "Los campos del cliente se completan solos al identificar el documento.");
        tarjeta.add(Ui.scrollVertical(f), BorderLayout.CENTER);
        tarjeta.cuerpo().add(pie, BorderLayout.SOUTH);
        tarjeta.relleno(0);
        f.setBorder(Ui.relleno(Tema.ESP_MD, Tema.ESP_LG, Tema.ESP_MD, Tema.ESP_LG));

        JPanel centro = Ui.panel(new BorderLayout());
        centro.add(tarjeta, BorderLayout.CENTER);
        contenido().add(centro, BorderLayout.CENTER);
        contenido().add(ayuda(), BorderLayout.EAST);
    }

    /** Panel lateral con el vocabulario del catalogo, como referencia del operador. */
    private JComponent ayuda() {
        Tarjeta t = new Tarjeta("Referencia rapida",
                "Valores vigentes del catalogo de problemas.");
        t.setPreferredSize(new Dimension(300, 100));

        JPanel lista = Ui.panel(new BoxLayout(null, BoxLayout.Y_AXIS));
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        for (String tipo : Datos.tiposDeProblema()) {
            JLabel cab = new JLabel(tipo);
            cab.setFont(Tema.fuente(12, Font.BOLD));
            cab.setForeground(Tema.texto());
            cab.setAlignmentX(LEFT_ALIGNMENT);
            cab.setBorder(Ui.relleno(Tema.ESP_SM, 0, 2, 0));
            lista.add(cab);
            for (String p : Datos.problemasDe(tipo)) {
                JLabel l = Ui.micro("·  " + p);
                l.setAlignmentX(LEFT_ALIGNMENT);
                lista.add(l);
            }
        }
        t.add(Ui.scroll(lista), BorderLayout.CENTER);
        return t;
    }

    private void recargarProblemas() {
        String tipo = (String) tipoProblema.getSelectedItem();
        problema.setModel(new DefaultComboBoxModel<>(
                Datos.problemasDe(tipo).toArray(new String[0])));
    }

    private void identificarCliente() {
        String doc = documento.getText().trim();
        clienteActual = Datos.CLIENTES.stream()
                .filter(c -> c.documento().equals(doc))
                .findFirst().orElse(null);

        if (clienteActual == null) {
            nombre.setText("");
            correo.setText("");
            mostrarCategoria(null);
            avisar("No hay ningun cliente registrado con el documento " +
                    (doc.isEmpty() ? "(vacio)" : doc) + ".");
            return;
        }
        nombre.setText(clienteActual.nombre());
        correo.setText(clienteActual.correo());
        mostrarCategoria(clienteActual.tipo());
    }

    /** Pinta la categoria del cliente como distintivo, o un guion si no hay cliente. */
    private void mostrarCategoria(TipoCliente tipo) {
        categoria.removeAll();
        categoria.add(tipo == null ? Ui.suave("\u2014")
                                   : Ui.chip(tipo.etiqueta(), Tema.acento()), BorderLayout.WEST);
        categoria.revalidate();
        categoria.repaint();
    }

    private void registrar() {
        if (clienteActual == null) {
            avisar("Primero identifique al cliente con su DNI o RUC.");
            documento.requestFocusInWindow();
            return;
        }
        if (nroCompra.getText().isBlank()) {
            avisar("Indique el numero de compra asociado al reclamo.");
            nroCompra.requestFocusInWindow();
            return;
        }
        String tipo = (String) tipoProblema.getSelectedItem();
        String desc = (String) problema.getSelectedItem();

        Reclamo r = new Reclamo(
                Estado.siguienteCodigo(),
                LocalDate.now(),
                clienteActual,
                nroCompra.getText().trim(),
                (Producto) producto.getSelectedItem(),
                new Problema(tipo, desc),
                (Canal) canal.getSelectedItem(),
                EstadoReclamo.EN_COLA,
                areaSegun(tipo),
                "Sin asignar",
                null, null,
                detalle.getText().trim());

        Estado.agregar(r);
        avisar("Reclamo " + r.id() + " registrado y derivado a " + r.area() + ".");
        limpiar();
    }

    /** Derivacion inicial segun el tipo de problema (regla del diseno arquitectonico). */
    private static String areaSegun(String tipoProblema) {
        return switch (tipoProblema) {
            case "Cobro de producto" -> "Ventas";
            case "Entrega de producto" -> "Logistica";
            case "Instalacion de producto" -> "Operaciones";
            case "Funcionamiento", "Parte de producto" -> "Area Tecnica de Reparacion e Inspeccion";
            default -> "PostVenta";
        };
    }

    private void limpiar() {
        documento.setText("");
        nombre.setText("");
        correo.setText("");
        nroCompra.setText("");
        detalle.setText("");
        producto.setSelectedIndex(0);
        tipoProblema.setSelectedIndex(0);
        canal.setSelectedIndex(0);
        clienteActual = null;
        mostrarCategoria(null);
        documento.requestFocusInWindow();
    }
}
