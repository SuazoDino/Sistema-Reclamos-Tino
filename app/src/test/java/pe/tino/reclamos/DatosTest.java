package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.Datos;

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
            assertNotNull(c, "catalogo inexistente: " + clave);
            assertFalse(c.existentes().isEmpty(), "catalogo vacio: " + clave);
            assertTrue(c.existentes().containsAll(c.habilitados()),
                    "hay habilitados fuera del universo en: " + clave);
        }
    }

    @Test
    void cadaTipoDeProblemaTieneAlMenosUnProblema() {
        assertFalse(Datos.tiposDeProblema().isEmpty());
        for (String tipo : Datos.tiposDeProblema()) {
            assertFalse(Datos.problemasDe(tipo).isEmpty(), "sin problemas: " + tipo);
        }
    }

    @Test
    void losAccesosSembradosDeCadaPerfilPertenecenAlCatalogo() {
        Datos.perfiles().forEach(perfil ->
                assertTrue(Datos.ACCESOS.containsAll(perfil.accesos()),
                        "accesos desconocidos en el perfil " + perfil.nombre()));
    }

    @Test
    void elCatalogoDeBienesUsaTiposConocidos() {
        Datos.catalogoBienes().forEach(f -> {
            assertEquals(3, f.length);
            assertTrue(Datos.TIPOS_BIEN.contains(f[0]), "tipo de bien desconocido: " + f[0]);
            assertTrue(Datos.TIPOS_PROBLEMA_BIEN.contains(f[1]),
                    "tipo de problema desconocido: " + f[1]);
        });
    }

    @Test
    void cadaIndicadorDelPrototipoEstaListado() {
        assertEquals(11, Datos.INDICADORES.size());
        Datos.INDICADORES.forEach(i -> assertFalse(i.isBlank()));
    }

    @Test
    void elCatalogoDeReclamosSoloUsaEstadosConocidos() {
        Datos.catalogoReclamos().forEach(f -> {
            assertEquals(3, f.length);
            assertTrue(f[2].equals("Habilitado") || f[2].equals("Deshabilitado"),
                    "estado desconocido: " + f[2]);
        });
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
                        "area no habilitada: " + r.area() + " en " + r.id()));
    }
}
