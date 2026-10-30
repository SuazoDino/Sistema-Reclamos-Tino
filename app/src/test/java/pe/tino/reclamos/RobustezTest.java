package pe.tino.reclamos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.EventoReclamo;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Dominio.Estado;
import pe.tino.reclamos.repo.MotorReglas.Condicion;
import pe.tino.reclamos.repo.MotorReglas.Salida;
import pe.tino.reclamos.repo.MotorReglas.TipoSalida;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Robustez: ningun cambio deja al sistema incoherente, todo queda en el
 * Diario de Operaciones y lo que se cancela devuelve lo que tenia tomado.
 */
class RobustezTest {

    private static final String DETALLE = "El equipo falla desde la primera semana de uso.";
    private static final String U = Auditoria.SIN_SESION;

    private static final String[] FALLA_D = {"Funcionamiento", "Falla de robustez D", Prototipo.TODO_PRODUCTO};
    private static final String[] FALLA_E = {"Funcionamiento", "Falla de robustez E", Prototipo.TODO_PRODUCTO};
    private static final String[] QUEJA_F = {"Atención", "Queja de robustez F", Prototipo.TODO_SERVICIO};

    @BeforeAll
    static void agregarProblemas() {
        Prototipo.problemas().addAll(List.of(FALLA_D, FALLA_E, QUEJA_F));
    }

    @AfterAll
    static void quitarProblemas() {
        Prototipo.problemas().removeAll(List.of(FALLA_D, FALLA_E, QUEJA_F));
    }

    @AfterEach
    void restaurar() {
        Parametros.restablecer();
        for (String id : List.of("E01", "E04", "E05")) Prototipo.habilitar(id, true);
    }

    private static Reclamo registrar(String problema) {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("70775031", Canal.WEB, null, TipoObjeto.PRODUCTO,
                "Teléfono Móvil Motorola Moto G84", "Funcionamiento", problema, DETALLE, "Reparación"));
        assertTrue(r.generado(), "debería generarse: " + r.errores() + r.rechazos());
        return r.reclamo();
    }

    private static boolean libres(List<String> tickets) {
        return tickets.stream().map(ParqueTickets::porCodigo).allMatch(Ticket::disponible);
    }

    /* ---------------- integridad ---------------- */

    @Test
    void laConfiguracionInicialEsCoherente() {
        assertEquals(List.of(), Integridad.verificar());
    }

    @Test
    void noSeDejaUnAreaSinEspecialistas() {
        int antes = Auditoria.diario().size();
        // PostVenta atiende las quejas y E05 es su unico especialista
        assertThrows(IllegalArgumentException.class, () -> Asignacion.cambiarEstado("E05", false, U));
        assertTrue(Prototipo.habilitado("E05"), "el cambio se deshizo");
        Auditoria.Operacion ultima = Auditoria.diario().get(Auditoria.diario().size() - 1);
        assertEquals(antes + 1, ultima.numero());
        assertEquals(Auditoria.RECHAZADA, ultima.resultado(), "el intento queda en el diario");
    }

    @Test
    void laJornadaNoPuedeSerMasCortaQueUnEvento() {
        // la Solucion de PR-FUNC dura 120 minutos
        assertThrows(IllegalArgumentException.class, () -> Integridad.aplicar(U, "Prueba", "Jornada corta",
                () -> { Parametros.cambiar("Fin de jornada", "10:00"); return ""; }));
        assertEquals("18:00", Parametros.finJornada().toString(), "volvio a la foto");
    }

    @Test
    void unValorQueNoEsNumeroSeRechazaAntesDeRomperElCalculo() {
        int pasos = Prototipo.pasosDeProtocolo().size();
        assertThrows(IllegalArgumentException.class, () -> Integridad.aplicar(U, "Prueba", "Paso mal escrito", () -> {
            Prototipo.pasosDeProtocolo().add(new String[]{"PR-FUNC", "Inspección", "4", "Limpiar", "diez", "Técnico", "Crítico"});
            return "";
        }));
        assertEquals(pasos, Prototipo.pasosDeProtocolo().size());
    }

    @Test
    void unaCondicionQueCierraUnCicloSeRechaza() {
        List<Condicion> cadena = MotorReglas.condiciones();
        Condicion cnd4 = MotorReglas.condicion("CND4");
        assertThrows(IllegalArgumentException.class, () -> Integridad.aplicar(U, "Prueba", "Ciclo", () -> {
            cadena.set(cadena.indexOf(cnd4), new Condicion("CND4", "Stock", "GT", "0",
                    new Salida(TipoSalida.CONDICION, "CND1"), new Salida(TipoSalida.ACCION, "Reembolso Total"), "ciclo"));
            return "";
        }));
        assertSame(cnd4, MotorReglas.condicion("CND4"), "la cadena quedo como estaba");
    }

    /* ---------------- reversa de operaciones ---------------- */

    @Test
    void unaOperacionSeRevierte() {
        Integridad.aplicar(U, "Generalidades del sistema", "Cambiar plazo",
                () -> { Parametros.cambiar("Plazo legal de respuesta", "20"); return "20"; });
        assertEquals(20, Parametros.plazoLegalDias());

        Auditoria.Operacion op = Auditoria.ultimaRevertible();
        assertNotNull(op);
        Auditoria.revertir(op.numero(), U);
        assertEquals(15, Parametros.plazoLegalDias());
        assertThrows(IllegalStateException.class, () -> Auditoria.revertir(op.numero(), U),
                "lo revertido no se revierte dos veces");
    }

    /* ---------------- citas y tickets ---------------- */

    @Test
    void alDeshabilitarAUnEspecialistaSusCitasPasanAOtro() {
        Reclamo t = registrar(FALLA_D[1]);
        String antes = t.especialista();
        EventoReclamo cita = t.eventoActual();
        List<String> tickets = cita.tickets();

        Asignacion.cambiarEstado(antes, false, U);

        assertNotEquals(antes, t.especialista(), "la cita paso a otro del area");
        assertEquals(EstadoEvento.ANULADO, cita.estado());
        assertTrue(libres(tickets), "los tickets del que se fue quedaron libres");
        assertEquals(Estado.ASIGNADO, t.estado());
    }

    @Test
    void alDevolverUnReclamoSuCitaAnteriorLiberaLosTickets() {
        Reclamo t = registrar(FALLA_E[1]);
        Atencion.iniciar(t, t.especialista());
        List<String> tickets = t.eventoActual().tickets();
        Atencion.devolver(t, "S01", "prueba de devolución");
        // la nueva cita puede volver a tomar esos turnos, pero ninguno queda en manos de la anulada
        List<String> nuevos = t.eventoActual().tickets();
        assertTrue(tickets.stream().allMatch(c -> ParqueTickets.porCodigo(c).disponible() || nuevos.contains(c)),
                "la cita anulada no deja tickets tomados");
    }

    @Test
    void siNoSePuedeProgramarElReclamoNoQuedaAMedias() {
        // se fuerza el caso que la integridad impide: PostVenta sin nadie (sin pasar por el diario)
        Prototipo.habilitar("E05", false);
        int total = Reclamos.todos().size();
        String siguiente = Reclamos.siguienteNumero();

        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("10112387203", Canal.WEB, null, TipoObjeto.SERVICIO,
                "Instalación", "Atención", QUEJA_F[1], DETALLE, "Disculpa y corrección de la atención"));

        assertFalse(r.generado());
        assertFalse(r.rechazos().isEmpty(), "el cliente recibe un mensaje claro");
        assertEquals(total, Reclamos.todos().size());
        assertEquals(siguiente, Reclamos.siguienteNumero(), "no se consumio el numero");
    }
}
