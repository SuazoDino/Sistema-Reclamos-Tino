package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Dominio;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Validacion;
import pe.tino.reclamos.repo.Validacion.Campo;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Coherencia de los catalogos nuevos. */
class CatalogosTest {

    private static final String DETALLE = "El producto falla desde la primera semana de uso.";

    @Test
    void cadaServicioUsaUnTipoYTiposDeProblemaConocidos() {
        Catalogos.servicios().forEach(f -> {
            assertEquals(4, f.length);
            assertTrue(Catalogos.TIPOS_SERVICIO.contains(f[0]), "tipo desconocido: " + f[0]);
            for (String t : Catalogos.tiposDeProblemaDeServicio(f[1])) {
                assertTrue(Prototipo.tiposDeProblema().contains(t), "tipo de problema desconocido: " + t);
            }
        });
    }

    @Test
    void hayUnTiempoDeAtencionParaCadaCombinacion() {
        List<String> clientes = List.of("SuperVip", "Vip", "Ordinario", "Nuevo");
        for (String c : clientes) {
            for (Dominio.Prioridad p : Dominio.Prioridad.values()) {
                assertTrue(Catalogos.horasDeAtencion(c, p) > 0,
                        "falta el tiempo de " + c + " con prioridad " + p);
            }
        }
    }

    @Test
    void laReglaDePrioridadCubreLosTiposDeProblemaDelSistema() {
        List<String> tipos = List.of("Funcionamiento", "Cobranza", "Entrega", "Atención");
        for (String tipo : tipos) {
            for (String cliente : List.of("SuperVip", "Vip", "Ordinario", "Nuevo")) {
                boolean hay = Catalogos.reglasPrioridad().stream()
                        .anyMatch(f -> f[0].equals(tipo) && f[1].equals(cliente));
                assertTrue(hay, "falta la regla de " + tipo + " para " + cliente);
            }
        }
    }

    @Test
    void elCatalogoDeEstadosRefleiaLaMaquinaDeEstados() {
        assertEquals(Dominio.Estado.values().length, Catalogos.estados().size());
    }

    @Test
    void cadaInstanciaDeclaraQuienResuelve() {
        Catalogos.instancias().forEach(f -> {
            assertEquals(4, f.length);
            assertFalse(f[1].isBlank(), "la instancia " + f[0] + " no dice quién resuelve");
        });
    }

    /* ---------------- cambio 6 ---------------- */

    @Test
    void todoTipoDeClienteHabilitadoTieneTiemposYPrioridad() {
        for (String cliente : Datos.catalogo("clientes").habilitados()) {
            for (Dominio.Prioridad p : Dominio.Prioridad.values()) {
                assertTrue(Catalogos.tiemposAtencion().stream()
                                .anyMatch(f -> f[0].equals(cliente) && f[1].equals(p.etiqueta())),
                        "falta el tiempo de " + cliente + " con prioridad " + p);
            }
            assertTrue(Catalogos.reglasPrioridad().stream().anyMatch(f -> f[1].equals(cliente)),
                    "falta la prioridad de " + cliente);
        }
    }

    @Test
    void deshabilitarUnServicioLoQuitaDelFormularioSinReiniciar() {
        String[] limpieza = Catalogos.servicios().stream()
                .filter(f -> f[1].equals("Limpieza")).findFirst().orElseThrow();
        assertTrue(Validacion.objetosPara("72119474", Dominio.TipoObjeto.SERVICIO).contains("Limpieza"));
        limpieza[3] = "Deshabilitado";
        try {
            assertFalse(Validacion.objetosPara("72119474", Dominio.TipoObjeto.SERVICIO).contains("Limpieza"));
        } finally {
            limpieza[3] = "Habilitado";
        }
    }

    @Test
    void unCanalDeshabilitadoNoSeAcepta() {
        String[] telefonico = Catalogos.canales().stream()
                .filter(f -> f[0].equals("Telefónico")).findFirst().orElseThrow();
        telefonico[3] = "Deshabilitado";
        try {
            assertTrue(Validacion.formulario(new Solicitud("72119474", Dominio.Canal.TELEFONICO, null,
                    null, null, null, null, DETALLE, "Reparación")).containsKey(Campo.CANAL));
        } finally {
            telefonico[3] = "Habilitado";
        }
    }

    @Test
    void losLocalesSonUnCatalogoConEstado() {
        assertTrue(Catalogos.localesHabilitados().contains("Loc01"));
        assertFalse(Catalogos.localesHabilitados().contains("Loc04"), "Loc04 está deshabilitado");
        Catalogos.locales().forEach(f -> assertEquals(3, f.length));
    }
}
