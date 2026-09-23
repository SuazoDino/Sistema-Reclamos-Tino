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
        Navegacion.ir("cat-politicas");
        Navegacion.ir("cat-reglas");

        assertEquals("cat-reglas", Navegacion.actual());
        assertTrue(Navegacion.hayDondeVolver());

        Navegacion.volver();
        assertEquals("cat-politicas", Navegacion.actual());

        Navegacion.volver();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
        assertFalse(Navegacion.hayDondeVolver());

        assertEquals(List.of("cat-politicas", "cat-reglas", "cat-politicas", Navegacion.MAPA), visitadas);
    }

    @Test
    void volverDesdeElMapaNoRompeNada() {
        Navegacion.volver();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
    }

    @Test
    void inicioLimpiaElHistorialCompleto() {
        Navegacion.ir("cat-productos");
        Navegacion.ir("cat-clientes");
        Navegacion.ir("cliente-dataentry");

        Navegacion.inicio();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
        assertFalse(Navegacion.hayDondeVolver());
    }

    @Test
    void repetirLaPantallaActualNoApilaHistorial() {
        Navegacion.ir("indicadores");
        Navegacion.ir("indicadores");
        Navegacion.volver();
        assertEquals(Navegacion.MAPA, Navegacion.actual());
    }
}
