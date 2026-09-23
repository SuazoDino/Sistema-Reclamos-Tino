package pe.tino.reclamos.repo;

import java.util.List;

/**
 * Dominios unicos del sistema. Existe para que un mismo concepto no tenga dos
 * escalas segun la pantalla: antes "criticidad" era Alta/Media/Baja en el
 * catalogo de reclamos y Critico/No Critico en protocolos, y el estado del
 * reclamo tenia tres vocabularios distintos.
 */
public final class Dominio {

    private Dominio() {}

    /* ---------------- estados del ticket ---------------- */

    /**
     * Estado del ticket. Es el unico dominio de estado del sistema: lo que
     * antes se llamaba "Pendiente", "En cola" o "Aceptado" en cada pantalla.
     */
    public enum Estado {
        REGISTRADO("Registrado", "El ticket entró al sistema y espera asignación"),
        ASIGNADO("Asignado", "Tiene área y especialista responsables"),
        EN_ATENCION("En atención", "El especialista está ejecutando el protocolo"),
        RESUELTO("Resuelto", "El protocolo terminó con una solución"),
        RECHAZADO("Rechazado", "No procede; admite impugnación dentro del plazo"),
        IMPUGNADO("Impugnado", "El cliente impugnó; pasa a la siguiente instancia"),
        CERRADO("Cerrado", "Entregado y conforme; no admite más movimientos");

        private final String etiqueta;
        private final String descripcion;

        Estado(String etiqueta, String descripcion) {
            this.etiqueta = etiqueta;
            this.descripcion = descripcion;
        }

        public String etiqueta()    { return etiqueta; }
        public String descripcion() { return descripcion; }
        @Override public String toString() { return etiqueta; }

        /** Los estados a los que puede pasar. Es la maquina de estados del ticket. */
        public List<Estado> siguientes() {
            return switch (this) {
                case REGISTRADO -> List.of(ASIGNADO, RECHAZADO);
                case ASIGNADO -> List.of(EN_ATENCION, RECHAZADO);
                case EN_ATENCION -> List.of(RESUELTO, RECHAZADO);
                case RESUELTO -> List.of(CERRADO);
                case RECHAZADO -> List.of(IMPUGNADO, CERRADO);
                case IMPUGNADO -> List.of(ASIGNADO);
                case CERRADO -> List.of();
            };
        }

        public boolean puedePasarA(Estado destino) { return siguientes().contains(destino); }

        /** Un ticket esta abierto mientras no este cerrado. */
        public boolean abierto() { return this != CERRADO; }
    }

    /* ---------------- prioridad del ticket ---------------- */

    /**
     * Prioridad del ticket: cuanto corre el reloj. Antes se llamaba
     * "criticidad" y se confundia con la criticidad de una accion.
     */
    public enum Prioridad {
        ALTA("Alta"), MEDIA("Media"), BAJA("Baja");

        private final String etiqueta;
        Prioridad(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    /** Criticidad de una accion del protocolo: si puede omitirse o no. */
    public enum CriticidadAccion {
        CRITICA("Crítica"), NO_CRITICA("No crítica");

        private final String etiqueta;
        CriticidadAccion(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    /* ---------------- instancias ---------------- */

    /**
     * Instancia de atencion. Cada una tiene su plazo de impugnacion, que es de
     * donde sale la "Fecha Limite de Impugnacion" que antes aparecia en
     * pantalla sin tener origen.
     */
    public enum Instancia {
        PRIMERA("Primera", 15, "Área responsable"),
        SEGUNDA("Segunda", 10, "Área especializada"),
        TERCERA("Tercera", 0, "Gerencia de PostVenta");

        private final String etiqueta;
        private final int diasImpugnacion;
        private final String resuelve;

        Instancia(String etiqueta, int diasImpugnacion, String resuelve) {
            this.etiqueta = etiqueta;
            this.diasImpugnacion = diasImpugnacion;
            this.resuelve = resuelve;
        }

        public String etiqueta()      { return etiqueta; }
        public int diasImpugnacion()  { return diasImpugnacion; }
        public String resuelve()      { return resuelve; }
        @Override public String toString() { return etiqueta; }

        /** La ultima instancia no admite impugnacion. */
        public Instancia siguiente() {
            return switch (this) {
                case PRIMERA -> SEGUNDA;
                case SEGUNDA -> TERCERA;
                case TERCERA -> TERCERA;
            };
        }

        public boolean admiteImpugnacion() { return diasImpugnacion > 0; }
    }

    /* ---------------- objeto del reclamo ---------------- */

    /** Sobre que se reclama. El sistema atiende productos y servicios por igual. */
    public enum TipoObjeto {
        PRODUCTO("Producto"), SERVICIO("Servicio");

        private final String etiqueta;
        TipoObjeto(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    /* ---------------- canal ---------------- */

    public enum Canal {
        PRESENCIAL("Presencial"), TELEFONICO("Telefónico"),
        INTERNET("Internet"), WEB("Web");

        private final String etiqueta;
        Canal(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }

    public static final List<String> UNIDADES_TIEMPO =
            List.of("Minutos", "Horas", "Días", "Semanas", "Meses");
}
