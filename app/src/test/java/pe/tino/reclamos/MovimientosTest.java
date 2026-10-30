package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.MovimientoSolucion;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El modelo del entregable 2 unido al flujo: el protocolo esta formado por
 * eventos, cada evento toma tickets del parque y cada paso queda como
 * Movimiento de Soluciones.
 */
class MovimientosTest {

    private static final String DETALLE = "El producto falla desde la primera semana de uso.";

    private static Reclamo nuevo(String problema) {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("70775031", Canal.WEB, null,
                TipoObjeto.PRODUCTO, "Teléfono Móvil Motorola Moto G84", "Funcionamiento",
                problema, DETALLE, "Reparación"));
        assertTrue(r.generado(), "debería generarse: " + r.errores() + r.rechazos());
        return r.reclamo();
    }

    @Test
    void elProtocoloDeUnTipoDeReclamoEstaFormadoPorEventos() {
        assertEquals("PR-FUNC", Prototipo.protocoloDe("Funcionamiento"));
        assertEquals(List.of("Inspección", "Solución del Reclamo"), Prototipo.eventosDelProtocolo("PR-FUNC"));
        assertEquals(3, Prototipo.pasosDe("PR-FUNC", "Inspección").size());
        assertEquals(20, Prototipo.duracionDe("PR-FUNC", "Inspección"));
    }

    @Test
    void cadaEventoTomaSusTicketsDelParque() {
        Reclamo t = nuevo("La cámara no funciona");
        EventoReclamo e = t.eventoActual();
        assertEquals(2, e.tickets().size(), "Inspección dura 20 min: dos tickets de 10");
        Ticket primero = ParqueTickets.porCodigo(e.tickets().get(0));
        assertFalse(primero.disponible());
        assertEquals(t.numero(), primero.reclamo());
        assertEquals(e.inicio(), primero.inicio());
        assertEquals(e.especialista(), primero.operario());
    }

    @Test
    void cadaPasoQuedaComoMovimientoConHoraEstimadaYReal() {
        Reclamo t = nuevo("El Teléfono No Responde");
        EventoReclamo e = t.eventoActual();
        assertEquals(0, t.pasosCompletados());
        assertEquals(4, t.totalPasos(), "3 de Inspección + 1 de Solución");

        MovimientoSolucion m = Atencion.ejecutarPaso(t, e.especialista(), "Reproducir la falla reportada");
        assertEquals(2, m.paso());
        assertEquals(e.inicio().plusMinutes(10), m.fecPasoEstimado(), "va después de revisar el estado físico (10 min)");
        assertEquals(e.inicio().plusMinutes(15), m.fecFinPasoEstimado());
        assertNotNull(m.fecFinPaso());
        assertEquals(e.tickets().get(1), m.ticket(), "a los 10 minutos ya corre el segundo ticket");
        assertEquals(Estado.EN_ATENCION, t.estado());
        assertEquals(1, t.pasosCompletados());

        assertThrows(IllegalStateException.class,
                () -> Atencion.ejecutarPaso(t, e.especialista(), "Reproducir la falla reportada"));
        assertThrows(IllegalStateException.class, () -> Atencion.ejecutarPaso(t, "E99", "Revisar estado físico del equipo"));
    }

    @Test
    void completarRegistraLosPasosQueFaltanYAvanzaAlSiguienteEvento() {
        Reclamo t = nuevo("No enciende");
        String esp = t.especialista();
        Atencion.ejecutarPaso(t, esp, "Registrar diagnóstico");
        Atencion.completar(t, esp, List.of("Revisar estado físico del equipo"), true, null);

        assertEquals(2, t.pasosCompletados(), "diagnóstico + revisión; la reproducción no se marcó");
        assertEquals("Solución del Reclamo", t.eventoActual().evento());
        assertEquals(12, t.eventoActual().tickets().size(), "Solución dura 120 min: doce tickets");
    }
}
