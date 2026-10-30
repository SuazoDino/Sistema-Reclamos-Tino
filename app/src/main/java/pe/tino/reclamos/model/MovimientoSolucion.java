package pe.tino.reclamos.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Un paso del protocolo ejecutado: el Movimiento de Soluciones.
 *
 * Guarda cuando se esperaba hacerlo (sale de la cita y de los tiempos del
 * protocolo) y cuando se hizo de verdad. Con eso se mide el avance del
 * reclamo (pasos completados sobre el total) y el desvio entre lo estimado y
 * lo real.
 */
public record MovimientoSolucion(int numero, String reclamo, String ticket, String operario,
                                 String protocolo, String evento, int paso, String accion,
                                 LocalDateTime fecPasoEstimado, LocalDateTime fecFinPasoEstimado,
                                 LocalDateTime fecPaso, LocalDateTime fecFinPaso) {

    /** Minutos de mas (positivo) o de menos (negativo) frente a lo estimado. */
    public long desvioMinutos() {
        return Duration.between(fecFinPasoEstimado, fecFinPaso).toMinutes();
    }
}
