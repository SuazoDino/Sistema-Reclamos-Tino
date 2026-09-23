package pe.tino.reclamos.ui.screens;

import pe.tino.reclamos.model.Modelo.*;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;
import pe.tino.reclamos.ui.components.*;
import pe.tino.reclamos.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cliente - Data Entry.
 *
 * Entrada : datos personales del cliente y datos sobre el reclamo.
 * Funcion : registrar el reclamo.
 * Salida  : reclamo registrado.
 *
 * El recorrido es el del prototipo: validar el documento, completar los datos
 * del reclamo, confirmar y ver el cuadro resumen.
 */
public class ClienteDataEntryPantalla extends Pantalla {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField documento = Ui.texto(16);
    private final JTextField nombre = Ui.soloLectura();
    private final JTextField correo = Ui.soloLectura();
    private final JTextField tipoCliente = Ui.soloLectura();

    private final JTextField nroCompra = Ui.texto();
    private final JComboBox<Producto> producto = Ui.combo(Datos.PRODUCTOS);
    private final JComboBox<String> tipoProblema = Ui.combo(Datos.tiposDeProblema());
    private final JComboBox<String> problema = new JComboBox<>();
    private final JComboBox<Canal> canal = Ui.combo(List.of(Canal.values()));
    private final JTextArea detalle = Ui.area(3);

    private final Grupo grupoReclamo = new Grupo("Datos del reclamo");
    private final Tabla resumen = new Tabla(new String[]{"Campo", "Valor"});
    private final Grupo grupoResumen = Grupo.ajustado("Cuadro resumen del reclamo registrado");
    private final JButton confirmar = Ui.boton("Confirmar");

    private Cliente cliente;

    public ClienteDataEntryPantalla() {
        super("Cliente - Data Entry",
                "Registro del reclamo de un cliente sobre una compra realizada.");

        tipoProblema.addActionListener(e -> recargarProblemas());
        recargarProblemas();
        habilitarReclamo(false);

        resumen.anchos(180, 420);
        grupoResumen.add(resumen.enScroll(), BorderLayout.CENTER);
        grupoResumen.setVisible(false);

        JPanel cuerpo = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        cuerpo.add(datosCliente(), BorderLayout.NORTH);
        cuerpo.add(datosReclamo(), BorderLayout.CENTER);

        JPanel raiz = Ui.panel(new BorderLayout(0, Tema.ESP_MD));
        raiz.add(cuerpo, BorderLayout.NORTH);
        raiz.add(grupoResumen, BorderLayout.CENTER);

        contenido().add(Ui.scrollVertical(raiz), BorderLayout.CENTER);
    }

    private JComponent datosCliente() {
        Grupo g = new Grupo("Datos del cliente");

        JButton validar = Ui.boton("Validar");
        validar.addActionListener(e -> validar());
        documento.addActionListener(e -> validar());

        JPanel filaDoc = Ui.fila(documento, validar);

        Formulario f = new Formulario();
        f.campo("DNI / RUC", filaDoc)
         .campo("Nombre", nombre)
         .campo("Correo electronico", correo)
         .campo("Tipo de cliente", tipoCliente);

        g.add(f, BorderLayout.CENTER);
        return g;
    }

    private JComponent datosReclamo() {
        Formulario f = new Formulario();
        f.campo("Nro de compra", nroCompra)
         .campo("Producto", producto)
         .campo("Tipo de problema", tipoProblema)
         .campo("Problema", problema)
         .campo("Canal", canal)
         .campo("Detalle", Ui.scroll(detalle));

        confirmar.addActionListener(e -> confirmar());
        JButton limpiar = Ui.boton("Limpiar");
        limpiar.addActionListener(e -> limpiar());

        JPanel pie = Ui.panel(new BorderLayout());
        pie.setBorder(Ui.relleno(Tema.ESP_SM, 0, 0, 0));
        pie.add(Ui.filaDerecha(limpiar, confirmar), BorderLayout.EAST);

        grupoReclamo.add(f, BorderLayout.CENTER);
        grupoReclamo.add(pie, BorderLayout.SOUTH);
        return grupoReclamo;
    }

    private void recargarProblemas() {
        problema.setModel(new DefaultComboBoxModel<>(
                Datos.problemasDe((String) tipoProblema.getSelectedItem()).toArray(new String[0])));
        problema.setFont(Tema.cuerpo());
    }

    /** Verifica si el documento ingresado pertenece a un cliente de la empresa. */
    private void validar() {
        String doc = documento.getText().trim();
        cliente = Datos.CLIENTES.stream()
                .filter(c -> c.documento().equals(doc))
                .findFirst().orElse(null);

        if (cliente == null) {
            nombre.setText("");
            correo.setText("");
            tipoCliente.setText("");
            habilitarReclamo(false);
            avisar("El documento " + (doc.isEmpty() ? "(vacio)" : doc)
                    + " no pertenece a un cliente de la empresa.");
            return;
        }
        nombre.setText(cliente.nombre());
        correo.setText(cliente.correo());
        tipoCliente.setText(cliente.tipo().etiqueta());
        habilitarReclamo(true);
        avisar("Cliente identificado: " + cliente.nombre() + ".");
        nroCompra.requestFocusInWindow();
    }

    private void habilitarReclamo(boolean activo) {
        for (Component c : List.of(nroCompra, producto, tipoProblema, problema, canal, detalle, confirmar)) {
            c.setEnabled(activo);
        }
    }

    private void confirmar() {
        if (cliente == null) { avisar("Primero valide el documento del cliente."); return; }
        if (nroCompra.getText().isBlank()) {
            avisar("Indique el numero de compra.");
            nroCompra.requestFocusInWindow();
            return;
        }
        if (!confirmar("Confirma el registro del reclamo a nombre de " + cliente.nombre() + "?")) return;

        Reclamo r = new Reclamo(
                Estado.siguienteCodigo(), LocalDate.now(), cliente, nroCompra.getText().trim(),
                (Producto) producto.getSelectedItem(),
                new Problema((String) tipoProblema.getSelectedItem(), (String) problema.getSelectedItem()),
                (Canal) canal.getSelectedItem(), EstadoReclamo.EN_COLA,
                areaSegun((String) tipoProblema.getSelectedItem()), "Sin asignar",
                null, null, detalle.getText().trim());

        Estado.agregar(r);
        mostrarResumen(r);
        avisar("Reclamo " + r.id() + " registrado.");
    }

    /** Derivacion inicial al area, segun el tipo de problema. */
    private static String areaSegun(String tipoProblema) {
        return switch (tipoProblema) {
            case "Cobro de producto" -> "Ventas";
            case "Entrega de producto" -> "Logistica";
            case "Instalacion de producto" -> "Operaciones";
            case "Funcionamiento", "Parte de producto" -> "Area Tecnica de Reparacion e Inspeccion";
            default -> "PostVenta";
        };
    }

    private void mostrarResumen(Reclamo r) {
        resumen.limpiar();
        resumen.agregar("Nro de reclamo", r.id());
        resumen.agregar("Fecha de emision", r.fechaEmision().format(FECHA));
        resumen.agregar("Cliente", r.cliente().nombre());
        resumen.agregar("DNI / RUC", r.cliente().documento());
        resumen.agregar("Tipo de cliente", r.cliente().tipo().etiqueta());
        resumen.agregar("Nro de compra", r.nroCompra());
        resumen.agregar("Producto", r.producto().id() + " - " + r.producto().bien()
                + " " + r.producto().marca() + " " + r.producto().modelo());
        resumen.agregar("Tipo de problema", r.problema().tipo());
        resumen.agregar("Problema", r.problema().descripcion());
        resumen.agregar("Canal", r.canal().name());
        resumen.agregar("Area asignada", r.area());
        resumen.agregar("Estado", r.estado().etiqueta());
        grupoResumen.setVisible(true);
        grupoResumen.revalidate();
    }

    private void limpiar() {
        documento.setText("");
        nombre.setText("");
        correo.setText("");
        tipoCliente.setText("");
        nroCompra.setText("");
        detalle.setText("");
        producto.setSelectedIndex(0);
        tipoProblema.setSelectedIndex(0);
        canal.setSelectedIndex(0);
        cliente = null;
        habilitarReclamo(false);
        grupoResumen.setVisible(false);
        documento.requestFocusInWindow();
    }
}
