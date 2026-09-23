package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Dominio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Coherencia de los catalogos nuevos. */
class CatalogosTest {

    @Test
    void cadaServicioUsaUnTipoYUnaUnidadConocidos() {
        Catalogos.servicios().forEach(f -> {
            assertEquals(4, f.length);
            assertTrue(Catalogos.TIPOS_SERVICIO.contains(f[0]), "tipo desconocido: " + f[0]);
            assertTrue(Catalogos.UNIDADES_ATENCION.contains(f[2]), "unidad desconocida: " + f[2]);
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
}
