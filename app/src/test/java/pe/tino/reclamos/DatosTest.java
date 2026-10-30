package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Prototipo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coherencia de los catalogos sembrados desde los prototipos: si el .xlsm y
 * estas tablas se separan, estas pruebas lo delatan.
 */
class DatosTest {

    @Test
    void todoValorHabilitadoExisteEnSuCatalogo() {
        for (String clave : Datos.clavesCatalogo()) {
            Datos.Catalogo c = Datos.catalogo(clave);
            assertNotNull(c, "catálogo inexistente: " + clave);
            assertFalse(c.existentes().isEmpty(), "catálogo vacío: " + clave);
            assertTrue(c.existentes().containsAll(c.habilitados()),
                    "hay habilitados fuera del universo en: " + clave);
        }
    }

    @Test
    void cadaTipoDeProblemaTieneAlMenosUnProblema() {
        assertFalse(Prototipo.tiposDeProblema().isEmpty());
        for (String tipo : Prototipo.tiposDeProblema()) {
            assertFalse(Prototipo.problemasDe(tipo).isEmpty(), "sin problemas: " + tipo);
        }
    }

    @Test
    void losAccesosSembradosDeCadaPerfilPertenecenAlCatalogo() {
        Datos.perfiles().forEach(perfil ->
                assertTrue(Datos.ACCESOS.containsAll(perfil.accesos()),
                        "accesos desconocidos en el perfil " + perfil.nombre()));
    }

    @Test
    void cadaIndicadorDelPrototipoEstaListado() {
        assertEquals(11, Datos.INDICADORES.size());
        Datos.INDICADORES.forEach(i -> assertFalse(i.isBlank()));
    }

    @Test
    void hayUnSoloCatalogoDeTiposDeProblemaYCadaUnoTieneNaturaleza() {
        Prototipo.tipos().forEach(f -> {
            assertEquals(3, f.length);
            assertTrue(Prototipo.NATURALEZAS.contains(f[1]), "naturaleza desconocida: " + f[1]);
            assertTrue(f[2].equals("Habilitado") || f[2].equals("Deshabilitado"), "estado desconocido: " + f[2]);
        });
        // todo problema cuelga de un tipo del catalogo
        Prototipo.problemas().forEach(f -> assertTrue(
                Prototipo.tipos().stream().anyMatch(t -> t[0].equals(f[0])), "tipo inexistente: " + f[0]));
    }

    @Test
    void laJerarquiaDeProductoTieneCuatroNiveles() {
        assertFalse(Datos.JERARQUIA_PRODUCTO.isEmpty());
        Datos.JERARQUIA_PRODUCTO.forEach(ruta ->
                assertEquals(4, ruta.length, "ruta incompleta: " + String.join(" > ", ruta)));
    }

    @Test
    void elAreaDeCadaReclamoDemoEstaHabilitada() {
        var habilitadas = Datos.catalogo("areas").habilitados();
        Datos.reclamosDemo().forEach(r ->
                assertTrue(habilitadas.contains(r.area()),
                        "área no habilitada: " + r.area() + " en " + r.id()));
    }
}
