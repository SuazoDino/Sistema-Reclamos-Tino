package pe.tino.reclamos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.tino.reclamos.ui.Navegacion;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** La navegacion clasica: entrar por el mapa, bajar a un catalogo y volver. */
class NavegacionTest {

    private final List<String> visitadas = new ArrayList<>();

    @BeforeEach
    void reiniciar() {
        visitadas.clear();
        Navegacion.instalarParaPruebas(visitadas::add);
    }

    @Test
    void arrancaEnElMapaYSinHistorial() {
        assertEquals(Navegacion.MAPA, Navegacion.actual());
        assertFalse(Navegacion.hayDondeVolver());
    }

    @Test
    void bajarYVolverRecorreElCaminoInverso() {
        Navegacion.ir("m-mantparam");
        Navegacion.ir("politicas");

        assertEquals("politicas", Navegacion.actual());
        assertTrue(Navegacion.hayDondeVolver());

        Navegacion.volver();
        assertEquals("m-mantparam", Navegacion.actual());

        Navegacion.volver();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
        assertFalse(Navegacion.hayDondeVolver());

        assertEquals(List.of("m-mantparam", "politicas", "m-mantparam", Navegacion.MAPA), visitadas);
    }

    @Test
    void volverDesdeElMapaNoRompeNada() {
        Navegacion.volver();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
    }

    @Test
    void inicioLimpiaElHistorialCompleto() {
        Navegacion.ir("m-aplicativo");
        Navegacion.ir("m-operativo");
        Navegacion.ir("registro");

        Navegacion.inicio();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
        assertFalse(Navegacion.hayDondeVolver());
    }

    @Test
    void repetirLaPantallaActualNoApilaHistorial() {
        Navegacion.ir("consulta");
        Navegacion.ir("consulta");
        Navegacion.volver();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
    }
}
