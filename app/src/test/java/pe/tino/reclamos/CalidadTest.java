package pe.tino.reclamos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cambios 2 a 5 juntos: el recorrido de la demo y el control de calidad.
 */
class CalidadTest {

    private static final String DETALLE = "El producto falla desde la primera semana de uso.";

    /** Un azar que nunca saca a un reclamo para la muestra. */
    private static final Random NUNCA = new Random() {
        @Override public int nextInt(int bound) { return bound > 1 ? 1 : 0; }
    };

    @BeforeEach
    void sinMuestra() {
        Calidad.azar(NUNCA);
    }

    @AfterEach
    void restaurar() {
        Calidad.muestraCada(10);
        Calidad.azar(new Random());
    }

    private static List<String> todas(Reclamo t) {
        return Prototipo.pasosDe(t.eventoActual().protocolo(), t.eventoActual().evento()).stream().map(a -> a[0]).toList();
    }

    /** Un reclamo nuevo de Funcionamiento sobre el Samsung de Alexander. */
    private static Reclamo reclamoDeCamara(String problema) {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("72119474", Canal.PRESENCIAL, "Loc01",
                TipoObjeto.PRODUCTO, "Teléfono Móvil Samsung Galaxy A54", "Funcionamiento", problema, DETALLE, "Reparación"));
        assertTrue(r.generado(), "debería generarse: " + r.errores() + r.rechazos());
        return r.reclamo();
    }

    @Test
    void elRecorridoDeLaDemoLlegaACerrado() {
        Reclamo t = reclamoDeCamara("La cámara no funciona");

        // se valida y se asigna solo, con fecha fija en horario laboral
        assertEquals(Estado.ASIGNADO, t.estado());
        assertEquals("Área Técnica de Reparación e Inspección", t.area());
        assertFalse(t.fueraDeGarantia());
        assertNotNull(t.fechaProgramada());
        assertNotEquals(DayOfWeek.SUNDAY, t.fechaProgramada().getDayOfWeek());
        assertFalse(t.fechaProgramada().toLocalTime().isBefore(Catalogos.inicioJornada()));
        assertEquals(Atencion.PENDIENTE_INSPECCION, t.solucionSugerida(),
                "Reparable solo se sabe al inspeccionar");

        // el especialista inspecciona: el motor ya puede decidir y se programa la solucion
        String especialista = t.especialista();
        EventoReclamo inspeccion = t.eventoActual();
        Atencion.completar(t, especialista, todas(t), true, null);
        assertEquals("Reparación Equipo", t.solucionSugerida());
        assertEquals("Solución del Reclamo", t.eventoActual().evento());
        assertTrue(t.eventoActual().inicio().compareTo(inspeccion.inicio()) >= 0);

        // resuelve con la solucion sugerida: pasa la revision automatica
        Atencion.completar(t, t.especialista(), todas(t), null, "Reparación Equipo");
        assertEquals(Estado.EN_REVISION, t.estado());
        assertEquals(Revision.APROBADO, t.revision());
        assertEquals(Prototipo.EVENTO_SEGUIMIENTO, t.eventoActual().evento());

        // el cliente confirma y el reclamo se cierra
        assertTrue(Calidad.esperaConformidad(t));
        Calidad.conformidad(t, true, 5);
        assertEquals(Estado.CERRADO, t.estado());
        assertTrue(Calidad.satisfaccion() > 0, "con notas, la satisfacción ya se puede calcular");
    }

    @Test
    void sinLasAccionesCriticasNoSeCompletaElEvento() {
        Reclamo t = reclamoDeCamara("Teléfono Sobrecalentado");
        assertThrows(IllegalArgumentException.class,
                () -> Atencion.completar(t, t.especialista(), List.of(), true, null));
        assertEquals(Estado.ASIGNADO, t.estado());
    }

    @Test
    void otraSolucionQueLaSugeridaQuedaObservadaParaElSupervisor() {
        Reclamo t = reclamoDeCamara("El Teléfono No Responde");
        Atencion.completar(t, t.especialista(), todas(t), true, null);
        Atencion.completar(t, t.especialista(), todas(t), null, "Reembolso Total");

        assertEquals(Revision.OBSERVADO, t.revision());
        assertTrue(t.observaciones().get(0).contains("no es la sugerida"));
        assertTrue(Calidad.paraSupervisor().contains(t));

        // aunque el cliente este conforme, no se cierra sin el supervisor
        Calidad.conformidad(t, true, 4);
        assertEquals(Estado.EN_REVISION, t.estado());
        Calidad.aprobar(t, "S01");
        assertEquals(Estado.CERRADO, t.estado());
    }

    @Test
    void elSupervisorDevuelveConMotivoYSeProgramaOtraCita() {
        Reclamo t = reclamoDeCamara("No enciende");
        Atencion.completar(t, t.especialista(), todas(t), true, null);
        Atencion.completar(t, t.especialista(), todas(t), null, "Pago Adicional");
        int eventos = t.eventos().size();

        assertThrows(IllegalArgumentException.class, () -> Calidad.devolverPorSupervisor(t, "S01", " "));
        Calidad.devolverPorSupervisor(t, "S01", "No se probó el equipo");
        assertEquals(Estado.EN_ATENCION, t.estado());
        assertEquals(1, t.devoluciones());
        assertEquals(eventos + 1, t.eventos().size(), "se agenda de nuevo el último evento");
        assertTrue(t.eventoActual().agendado());
        assertTrue(Calidad.porcentajeDevueltos() > 0);
    }

    @Test
    void unaMuestraAlAzarTambienPasaPorElSupervisor() {
        Calidad.muestraCada(1);
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("10112387203", Canal.WEB, null,
                TipoObjeto.SERVICIO, "Instalación", "Funcionamiento", "Mala instalación", DETALLE, "Reparación"));
        Reclamo t = r.reclamo();
        Atencion.completar(t, t.especialista(), todas(t), null, null);
        Atencion.completar(t, t.especialista(), todas(t), null, "Corrección");
        assertEquals(Revision.MUESTRA, t.revision());
        assertTrue(Calidad.paraSupervisor().contains(t));
    }

    @Test
    void unRechazoNoLlegaAlClienteHastaQueLoApruebaElSupervisor() {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("72119474", Canal.WEB, null,
                TipoObjeto.SERVICIO, "Delivery", "Entrega", "Producto equivocado", DETALLE, "Reparación"));
        Reclamo t = r.reclamo();
        Atencion.proponerRechazo(t, t.especialista(), "El producto entregado es el del pedido");

        assertEquals(Estado.EN_REVISION, t.estado());
        assertEquals(Revision.RECHAZO_PROPUESTO, t.revision());
        assertNull(t.limiteImpugnacion(), "el cliente todavía no fue notificado");
        assertFalse(Calidad.esperaConformidad(t));

        Calidad.aprobar(t, "S01");
        assertEquals(Estado.RECHAZADO, t.estado());
        assertNotNull(t.limiteImpugnacion());

        // al impugnar se reasigna solo al area de la segunda instancia
        assertTrue(Atencion.impugnar(t, "No estoy de acuerdo"));
        assertEquals(Estado.ASIGNADO, t.estado());
        assertEquals("Área Especializada", t.area());
        assertEquals("E06", t.especialista());
    }

    @Test
    void siElClienteNoQuedaConformeVuelveAAtencion() {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("10112387203", Canal.WEB, null,
                TipoObjeto.SERVICIO, "Instalación", "Funcionamiento", "El equipo falla después del servicio", DETALLE, "Reparación"));
        Reclamo t = r.reclamo();
        Atencion.completar(t, t.especialista(), todas(t), null, null);
        Atencion.completar(t, t.especialista(), todas(t), null, "Corrección");
        Calidad.conformidad(t, false, 2);
        assertEquals(Estado.EN_ATENCION, t.estado());
        assertTrue(t.eventoActual().agendado());
    }
}
