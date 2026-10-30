package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.ParqueTickets;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cambio 4 con el parque de tickets: la cita sale de turnos generados en
 * horario laboral, sin feriados y sin que dos citas tomen el mismo turno.
 */
class ParqueTicketsTest {

    private static LocalDateTime en(int mes, int dia, int hora, int minuto) {
        return LocalDateTime.of(2026, mes, dia, hora, minuto);
    }

    @Test
    void elBatchGeneraTurnosSoloEnDiasLaborablesYDentroDeLaJornada() {
        int generados = ParqueTickets.generar("PQ-0", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));
        // 1/11 es domingo y feriado: solo se genera el lunes, de 9:00 a 18:00 en turnos de 10 min
        assertEquals(54, generados);
        List<Ticket> lunes = ParqueTickets.de("PQ-0", LocalDate.of(2026, 11, 2));
        assertEquals(en(11, 2, 9, 0), lunes.get(0).inicio());
        assertEquals(en(11, 2, 18, 0), lunes.get(lunes.size() - 1).fin());
        assertTrue(lunes.stream().allMatch(Ticket::disponible));
        assertTrue(lunes.get(0).codigo().startsWith("TK-PQ-0-20261102-0900"));
    }

    @Test
    void laCitaEmpiezaEnElSiguienteTurno() {
        assertEquals(en(10, 27, 10, 10), ParqueTickets.primerHueco("PQ-1", en(10, 27, 10, 3), 20));
    }

    @Test
    void antesDeLaJornadaSeEsperaALasNueve() {
        assertEquals(en(10, 27, 9, 0), ParqueTickets.primerHueco("PQ-2", en(10, 27, 7, 30), 20));
    }

    @Test
    void unEventoNoSeParteEntreDosDias() {
        assertEquals(en(10, 28, 9, 0), ParqueTickets.primerHueco("PQ-3", en(10, 27, 17, 50), 20));
    }

    @Test
    void elDomingoYLosFeriadosNoHayTickets() {
        assertEquals(en(11, 2, 9, 0), ParqueTickets.primerHueco("PQ-4", en(10, 31, 17, 55), 20));
        assertEquals(en(12, 10, 9, 0), ParqueTickets.primerHueco("PQ-4", en(12, 8, 10, 0), 20));
        assertFalse(Catalogos.esLaborable(LocalDate.of(2026, 12, 8)));
    }

    @Test
    void unEventoTomaTantosTicketsComoDuraYOtroNoPuedeTomarlos() {
        List<String> tomados = ParqueTickets.tomar("PQ-5", en(10, 27, 10, 0), 20, "RC-X", "Inspección");
        assertEquals(2, tomados.size(), "20 minutos son dos turnos de 10");
        Ticket primero = ParqueTickets.porCodigo(tomados.get(0));
        assertFalse(primero.disponible());
        assertEquals("RC-X", primero.reclamo());

        assertEquals(en(10, 27, 10, 20), ParqueTickets.primerHueco("PQ-5", en(10, 27, 10, 0), 20));
        assertThrows(IllegalStateException.class, () ->
                ParqueTickets.tomar("PQ-5", en(10, 27, 10, 10), 20, "RC-Y", "Inspección"),
                "una cita nueva no desplaza a una ya tomada");
        // otro operario si esta libre a esa hora
        assertEquals(en(10, 27, 10, 0), ParqueTickets.primerHueco("PQ-6", en(10, 27, 10, 0), 20));
    }

    @Test
    void liberarDevuelveLosTicketsAlParque() {
        List<String> tomados = ParqueTickets.tomar("PQ-7", en(10, 27, 11, 0), 30, "RC-Z", "Solución");
        ParqueTickets.liberar(tomados);
        assertEquals(en(10, 27, 11, 0), ParqueTickets.primerHueco("PQ-7", en(10, 27, 11, 0), 30));
    }
}
