package pe.tino.reclamos.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidades del Sistema de Reclamos. Son records inmutables: el prototipo de
 * diseno externo no persiste nada, solo alimenta las pantallas.
 */
public final class Modelo {

    private Modelo() {}

    /** Estado del ciclo de vida de un reclamo (hoja DATA_ENTRY / MANT-PARAM). */
    public enum EstadoReclamo {
        EN_COLA("En cola"), EN_ATENCION("En atención"), ATENDIDO("Atendido"),
        ENTREGADO("Entregado"), RECHAZADO("Rechazado");

        private final String etiqueta;
        EstadoReclamo(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    /** Categorizacion del cliente (hojas Clientes / ClientesHab). */
    public enum TipoCliente {
        NUEVO("Nuevo"), ORDINARIO("Ordinario"), VIP("Vip"), SUPER_VIP("SuperVip");

        private final String etiqueta;
        TipoCliente(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    /** Canal de ingreso del reclamo (hoja Hoja1 - Tipo_Canal). */
    public enum Canal { PRESENCIAL, TELEFONICO, INTERNET, WEB }

    /**
     * Cliente del sistema. Es la unica fuente de datos del cliente: antes
     * habia una lista de clientes con su categoria y otra de personas con sus
     * datos de contacto, y una pantalla usaba una y otra la otra.
     */
    public record Cliente(String id, String documento, String nombres, String apellidos,
                          String correo, String direccion, String telefono,
                          TipoCliente tipo, String sector) {

        public String nombreCompleto() { return nombres + " " + apellidos; }

        @Override public String toString() { return nombreCompleto() + "  (" + documento + ")"; }
    }

    /** Jerarquia del catalogo: Segmento > Familia > Clase > Bien > Producto. */
    public record Producto(String id, String segmento, String familia, String clase,
                           String bien, String marca, String modelo) {
        @Override public String toString() { return id + " - " + bien + " " + marca; }
    }

    public record Problema(String tipo, String descripcion) {
        @Override public String toString() { return descripcion; }
    }

    public record Reclamo(String id, LocalDate fechaEmision, Cliente cliente, String nroCompra,
                          Producto producto, Problema problema, Canal canal,
                          EstadoReclamo estado, String area, String especialista,
                          LocalDate fechaAtencion, LocalTime horaAtencion, String detalle) {}

    /** Fila del protocolo de atencion (hojas Protocolo / MantProtocolo). */
    public record Protocolo(String tipoReclamo, String inspeccionProblema, String solucionProblema,
                            String reasignacionArea, String entregaProducto,
                            String seguimientoSolucion, boolean habilitado) {}

    /** Regla de negocio parametrizable (hoja MantReglas). */
    public record Regla(String condicion, String parametro, String operador, String variable,
                        String accionVerdadero, String accionFalso, String descripcion) {}

    /** Perfil de seguridad con los modulos a los que accede (hoja SEGURIDAD). */
    public record Perfil(String nombre, java.util.List<String> accesos) {
        @Override public String toString() { return nombre; }
    }



}
