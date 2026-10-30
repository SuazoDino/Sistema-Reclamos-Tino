package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Las reglas del reclamo: prioridad, plazo, maquina de estados e impugnacion. */
class ReclamoTest {

    private static Reclamo nuevo(String tipoProblema, String tipoCliente) {
        Prioridad p = Catalogos.prioridadDe(tipoProblema, tipoCliente);
        long horas = Catalogos.horasDeAtencion(tipoCliente, p);
        return new Reclamo("RC-TEST-0001", LocalDateTime.now(), Canal.WEB, "Loc01",
                "72119474", "Cliente de prueba", tipoCliente,
                TipoObjeto.PRODUCTO, "Teléfono Móvil", tipoProblema, "No enciende",
                "", "", p, LocalDateTime.now().plusHours(horas));
    }

    @Test
    void laPrioridadSaleDelTipoDeProblemaYDelTipoDeCliente() {
        assertEquals(Prioridad.ALTA, Catalogos.prioridadDe("Funcionamiento", "Vip"));
        assertEquals(Prioridad.MEDIA, Catalogos.prioridadDe("Funcionamiento", "Ordinario"));
        assertEquals(Prioridad.BAJA, Catalogos.prioridadDe("Atención", "Nuevo"));
    }

    @Test
    void unaCobranzaEsAltaParaCualquierCliente() {
        for (String cliente : new String[]{"SuperVip", "Vip", "Ordinario", "Nuevo"}) {
            assertEquals(Prioridad.ALTA, Catalogos.prioridadDe("Cobranza", cliente),
                    "la cobranza debería ser alta para " + cliente);
        }
    }

    @Test
    void elPlazoDeAtencionMejoraConLaCategoriaDelCliente() {
        long superVip = Catalogos.horasDeAtencion("SuperVip", Prioridad.ALTA);
        long vip = Catalogos.horasDeAtencion("Vip", Prioridad.ALTA);
        long nuevo = Catalogos.horasDeAtencion("Nuevo", Prioridad.ALTA);
        assertTrue(superVip < vip, "SuperVip debería tener menos plazo que Vip");
        assertTrue(vip < nuevo, "Vip debería tener menos plazo que Nuevo");
    }

    @Test
    void unClienteSinCategoriaIgualRecibeUnPlazo() {
        assertTrue(Catalogos.horasDeAtencion("Desconocido", Prioridad.MEDIA) > 0);
    }

    @Test
    void elTicketNaceRegistradoYEnPrimeraInstancia() {
        Reclamo t = nuevo("Funcionamiento", "Vip");
        assertEquals(Estado.REGISTRADO, t.estado());
        assertEquals(Instancia.PRIMERA, t.instancia());
        assertFalse(t.vencido());
        assertFalse(t.bitacora().isEmpty(), "el alta debería quedar en la bitácora");
    }

    @Test
    void laMaquinaDeEstadosRechazaUnSaltoInvalido() {
        Reclamo t = nuevo("Funcionamiento", "Vip");
        assertFalse(t.cambiarEstado(Estado.CERRADO, "E01", ""),
                "un reclamo recién registrado no puede cerrarse de golpe");
        assertEquals(Estado.REGISTRADO, t.estado());
    }

    @Test
    void elCaminoNormalLlegaHastaCerrado() {
        Reclamo t = nuevo("Funcionamiento", "Vip");
        assertTrue(t.cambiarEstado(Estado.ASIGNADO, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.EN_ATENCION, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.RESUELTO, "E01", ""));
        assertFalse(t.cambiarEstado(Estado.CERRADO, "E01", ""),
                "un resuelto no se cierra sin pasar por control de calidad");
        assertTrue(t.cambiarEstado(Estado.EN_REVISION, "Control de calidad", ""));
        assertTrue(t.cambiarEstado(Estado.CERRADO, "S01", ""));
        assertFalse(t.estado().abierto());
    }

    @Test
    void alRechazarSeAbreElPlazoDeImpugnacion() {
        Reclamo t = nuevo("Funcionamiento", "Vip");
        t.cambiarEstado(Estado.RECHAZADO, "E01", "no cumple garantía");

        assertNotNull(t.limiteImpugnacion(), "el rechazo debería fijar la fecha límite");
        assertTrue(t.limiteImpugnacion().isAfter(LocalDateTime.now()));
    }

    @Test
    void impugnarSubeDeInstanciaYCambiaElAreaQueResuelve() {
        Reclamo t = nuevo("Funcionamiento", "Vip");
        t.cambiarEstado(Estado.RECHAZADO, "E01", "no cumple garantía");

        assertTrue(t.impugnar("No estoy de acuerdo", "Cliente"));
        assertEquals(Instancia.SEGUNDA, t.instancia());
        assertEquals(Instancia.SEGUNDA.resuelve(), t.area());
    }

    @Test
    void soloSePuedeImpugnarUnTicketRechazado() {
        Reclamo t = nuevo("Funcionamiento", "Vip");
        assertFalse(t.impugnar("sin motivo", "Cliente"));
    }

    @Test
    void laTerceraInstanciaNoAdmiteImpugnacion() {
        assertFalse(Instancia.TERCERA.admiteImpugnacion());
        assertEquals(Instancia.TERCERA, Instancia.TERCERA.siguiente(),
                "la última instancia no debería escalar a otra");
    }

    @Test
    void cadaEstadoDeclaraSusTransicionesYSoloCerradoEsFinal() {
        for (Estado e : Estado.values()) {
            if (e == Estado.CERRADO) assertTrue(e.siguientes().isEmpty());
            else assertFalse(e.siguientes().isEmpty(), e + " debería tener salida");
        }
    }
}
