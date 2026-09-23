package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Las reglas del ticket: prioridad, plazo, maquina de estados e impugnacion. */
class TicketTest {

    private static Ticket nuevo(String tipoProblema, String tipoCliente) {
        Prioridad p = Catalogos.prioridadDe(tipoProblema, tipoCliente);
        long horas = Catalogos.horasDeAtencion(tipoCliente, p);
        return new Ticket("TK-TEST-0001", LocalDateTime.now(), Canal.WEB, "Loc01",
                "72119474", "Cliente de prueba", tipoCliente,
                TipoObjeto.PRODUCTO, "Telefono Movil", tipoProblema, "No enciende",
                "", "", p, LocalDateTime.now().plusHours(horas));
    }

    @Test
    void laPrioridadSaleDelTipoDeProblemaYDelTipoDeCliente() {
        assertEquals(Prioridad.ALTA, Catalogos.prioridadDe("Funcionamiento", "Vip"));
        assertEquals(Prioridad.MEDIA, Catalogos.prioridadDe("Funcionamiento", "Ordinario"));
        assertEquals(Prioridad.BAJA, Catalogos.prioridadDe("Atencion", "Nuevo"));
    }

    @Test
    void unaCobranzaEsAltaParaCualquierCliente() {
        for (String cliente : new String[]{"SuperVip", "Vip", "Ordinario", "Nuevo"}) {
            assertEquals(Prioridad.ALTA, Catalogos.prioridadDe("Cobranza", cliente),
                    "la cobranza deberia ser alta para " + cliente);
        }
    }

    @Test
    void elPlazoDeAtencionMejoraConLaCategoriaDelCliente() {
        long superVip = Catalogos.horasDeAtencion("SuperVip", Prioridad.ALTA);
        long vip = Catalogos.horasDeAtencion("Vip", Prioridad.ALTA);
        long nuevo = Catalogos.horasDeAtencion("Nuevo", Prioridad.ALTA);
        assertTrue(superVip < vip, "SuperVip deberia tener menos plazo que Vip");
        assertTrue(vip < nuevo, "Vip deberia tener menos plazo que Nuevo");
    }

    @Test
    void unClienteSinCategoriaIgualRecibeUnPlazo() {
        assertTrue(Catalogos.horasDeAtencion("Desconocido", Prioridad.MEDIA) > 0);
    }

    @Test
    void elTicketNaceRegistradoYEnPrimeraInstancia() {
        Ticket t = nuevo("Funcionamiento", "Vip");
        assertEquals(Estado.REGISTRADO, t.estado());
        assertEquals(Instancia.PRIMERA, t.instancia());
        assertFalse(t.vencido());
        assertFalse(t.bitacora().isEmpty(), "el alta deberia quedar en la bitacora");
    }

    @Test
    void laMaquinaDeEstadosRechazaUnSaltoInvalido() {
        Ticket t = nuevo("Funcionamiento", "Vip");
        assertFalse(t.cambiarEstado(Estado.CERRADO, "E01", ""),
                "un ticket recien registrado no puede cerrarse de golpe");
        assertEquals(Estado.REGISTRADO, t.estado());
    }

    @Test
    void elCaminoNormalLlegaHastaCerrado() {
        Ticket t = nuevo("Funcionamiento", "Vip");
        assertTrue(t.cambiarEstado(Estado.ASIGNADO, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.EN_ATENCION, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.RESUELTO, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.CERRADO, "E01", ""));
        assertFalse(t.estado().abierto());
    }

    @Test
    void alRechazarSeAbreElPlazoDeImpugnacion() {
        Ticket t = nuevo("Funcionamiento", "Vip");
        t.cambiarEstado(Estado.RECHAZADO, "E01", "no cumple garantia");

        assertNotNull(t.limiteImpugnacion(), "el rechazo deberia fijar la fecha limite");
        assertTrue(t.limiteImpugnacion().isAfter(LocalDateTime.now()));
    }

    @Test
    void impugnarSubeDeInstanciaYCambiaElAreaQueResuelve() {
        Ticket t = nuevo("Funcionamiento", "Vip");
        t.cambiarEstado(Estado.RECHAZADO, "E01", "no cumple garantia");

        assertTrue(t.impugnar("No estoy de acuerdo", "Cliente"));
        assertEquals(Instancia.SEGUNDA, t.instancia());
        assertEquals(Instancia.SEGUNDA.resuelve(), t.area());
    }

    @Test
    void soloSePuedeImpugnarUnTicketRechazado() {
        Ticket t = nuevo("Funcionamiento", "Vip");
        assertFalse(t.impugnar("sin motivo", "Cliente"));
    }

    @Test
    void laTerceraInstanciaNoAdmiteImpugnacion() {
        assertFalse(Instancia.TERCERA.admiteImpugnacion());
        assertEquals(Instancia.TERCERA, Instancia.TERCERA.siguiente(),
                "la ultima instancia no deberia escalar a otra");
    }

    @Test
    void cadaEstadoDeclaraSusTransicionesYSoloCerradoEsFinal() {
        for (Estado e : Estado.values()) {
            if (e == Estado.CERRADO) assertTrue(e.siguientes().isEmpty());
            else assertFalse(e.siguientes().isEmpty(), e + " deberia tener salida");
        }
    }
}
