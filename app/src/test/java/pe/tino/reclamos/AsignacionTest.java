package pe.tino.reclamos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Prototipo.Empleado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cambio 3: nadie elige reclamos. Se usa un area de prueba aislada para que
 * los reclamos de ejemplo no alteren la carga.
 */
class AsignacionTest {

    private static final String AREA = "Área de Prueba";
    private static final String AREA_LLENA = "Área de Prueba Llena";

    @BeforeAll
    static void crearAreas() {
        Catalogos.areasPorTipo().add(new String[]{"Prueba", AREA});
        Catalogos.areasPorTipo().add(new String[]{"PruebaLlena", AREA_LLENA});
        Prototipo.EMPLEADOS.put("T1", new Empleado("T1", "Técnico", AREA));
        Prototipo.EMPLEADOS.put("T2", new Empleado("T2", "Técnico", AREA));
        Prototipo.EMPLEADOS.put("P1", new Empleado("P1", "Técnico", AREA_LLENA));
        Prototipo.EMPLEADOS.put("P2", new Empleado("P2", "Técnico", AREA_LLENA));
    }

    @AfterAll
    static void quitarAreas() {
        Catalogos.areasPorTipo().removeIf(f -> f[0].startsWith("Prueba"));
        List.of("T1", "T2", "P1", "P2").forEach(Prototipo.EMPLEADOS::remove);
    }

    private static Reclamo abrir(String tipo) {
        return Reclamos.abrir(Canal.WEB, "-", "72119474", "Cliente de prueba", "Vip",
                TipoObjeto.SERVICIO, "Soporte telefónico", tipo, "Demora", "", "");
    }

    @Test
    void elAreaSaleDelTipoDeProblema() {
        assertEquals("Área Técnica de Reparación e Inspección",
                Catalogos.areaDe("Funcionamiento", Instancia.PRIMERA));
        assertEquals("Logística", Catalogos.areaDe("Entrega", Instancia.PRIMERA));
        assertEquals("Ventas", Catalogos.areaDe("Cobranza", Instancia.PRIMERA));
        assertEquals("PostVenta", Catalogos.areaDe("Atención", Instancia.PRIMERA));
        assertEquals("Área Especializada", Catalogos.areaDe("Funcionamiento", Instancia.SEGUNDA));
    }

    @Test
    void cadaAreaQueAtiendeTieneEspecialistas() {
        Catalogos.areasPorTipo().stream().filter(f -> !f[0].startsWith("Prueba"))
                .forEach(f -> assertFalse(Prototipo.empleadosDe(f[1]).isEmpty(), "sin especialistas: " + f[1]));
        for (Instancia i : Instancia.values()) {
            if (i != Instancia.PRIMERA) assertFalse(Prototipo.empleadosDe(i.resuelve()).isEmpty());
        }
    }

    @Test
    void elTicketNaceAsignadoAlDeMenorCargaYLosEmpatesVanPorTurno() {
        Reclamo a = abrir("Prueba");
        Reclamo b = abrir("Prueba");
        Reclamo c = abrir("Prueba");

        assertEquals(Estado.ASIGNADO, a.estado());
        assertEquals(AREA, a.area());
        assertEquals("T1", a.especialista());
        assertEquals("T2", b.especialista(), "T1 ya tiene uno: va al de menor carga");
        assertEquals("T1", c.especialista(), "empate: le toca al siguiente en el turno");
        assertTrue(a.bitacora().stream().anyMatch(r -> r.accion().equals("Asignación automática")));
    }

    @Test
    void ningunTicketQuedaSinResponsable() {
        Reclamos.todos().forEach(t -> {
            assertNotEquals(Estado.REGISTRADO, t.estado(), t.numero() + " quedó sin asignar");
            assertNotEquals("Sin asignar", t.especialista(), t.numero() + " no tiene responsable");
        });
    }

    @Test
    void siElElegidoNoTieneHuecoATiempoSePruebaConOtroDelArea() {
        // P1 tiene la agenda llena los proximos dias; P2 esta libre
        for (LocalDate d = LocalDate.now(); d.isBefore(LocalDate.now().plusDays(10)); d = d.plusDays(1)) {
            if (Catalogos.esLaborable(d)) ParqueTickets.tomar("P1", d.atTime(9, 0), 540, "ocupado", "-");
        }
        Reclamo t = new Reclamo("RC-P-0001", LocalDateTime.now(), Canal.WEB, "-", "72119474",
                "Cliente", "Vip", TipoObjeto.SERVICIO, "Soporte telefónico", "PruebaLlena", "Demora",
                "", "", Prioridad.MEDIA, LocalDateTime.now().plusDays(5));

        EventoReclamo e = Asignacion.programar(t, "Seguimiento de Reclamo", "PR-ATEN-01", null,
                LocalDateTime.now());
        assertEquals("P2", e.especialista());
        assertTrue(e.dentroDelPlazo());
    }

    @Test
    void sinHuecoAntesDelLimiteIgualSeAgendaYQuedaMarcado() {
        Reclamo t = new Reclamo("RC-P-0002", LocalDateTime.now(), Canal.WEB, "-", "72119474",
                "Cliente", "Vip", TipoObjeto.SERVICIO, "Soporte telefónico", "Prueba", "Demora",
                "", "", Prioridad.ALTA, LocalDateTime.now().plusMinutes(1));

        EventoReclamo e = Asignacion.programar(t, "Seguimiento de Reclamo", "PR-ATEN-01", null,
                LocalDateTime.now());
        assertNotNull(e.inicio(), "igual recibe una cita");
        assertFalse(e.dentroDelPlazo());
        assertTrue(t.sinCapacidad());
        assertTrue(t.bitacora().stream().anyMatch(r -> r.detalle().contains("Sin capacidad dentro del plazo")));
    }
}
