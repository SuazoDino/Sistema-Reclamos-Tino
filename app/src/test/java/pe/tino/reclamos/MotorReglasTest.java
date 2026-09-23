package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.MotorReglas;
import pe.tino.reclamos.repo.MotorReglas.Resultado;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El motor de reglas tiene que poder ejecutarse, no solo documentarse: si la
 * cadena no resuelve un caso concreto, la regla no sirve.
 */
class MotorReglasTest {

    private static Map<String, String> caso(String comprobante, String transcurrido,
                                            String garantia, String reparable, String stock) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Tiene_Comprobante", comprobante);
        m.put("Tiempo_Transcurrido", transcurrido);
        m.put("Tiempo_Garantia", garantia);
        m.put("Reparable", reparable);
        m.put("Stock", stock);
        return m;
    }

    @Test
    void laCadenaEstaBienArmada() {
        assertTrue(MotorReglas.problemas().isEmpty(),
                "la cadena tiene problemas: " + MotorReglas.problemas());
    }

    @Test
    void sinComprobanteElReclamoSeRechaza() {
        Resultado r = MotorReglas.evaluar(caso("false", "10", "365", "true", "5"));
        assertTrue(r.valido());
        assertEquals("Rechazo", r.accion());
        assertEquals(1, r.traza().size(), "debería cortar en la primera condición");
    }

    @Test
    void fueraDeGarantiaElClientePagaLaDiferencia() {
        Resultado r = MotorReglas.evaluar(caso("true", "400", "365", "true", "5"));
        assertTrue(r.valido());
        assertEquals("Pago Adicional", r.accion());
    }

    @Test
    void dentroDeGarantiaYReparableSeRepara() {
        Resultado r = MotorReglas.evaluar(caso("true", "120", "365", "true", "0"));
        assertTrue(r.valido());
        assertEquals("Reparación Equipo", r.accion());
    }

    @Test
    void noReparableConStockSeIntercambia() {
        Resultado r = MotorReglas.evaluar(caso("true", "120", "365", "false", "3"));
        assertTrue(r.valido());
        assertEquals("Intercambio Equipo", r.accion());
    }

    @Test
    void noReparableSinStockSeReembolsa() {
        Resultado r = MotorReglas.evaluar(caso("true", "120", "365", "false", "0"));
        assertTrue(r.valido());
        assertEquals("Reembolso Total", r.accion());
    }

    @Test
    void laTrazaExplicaCadaPasoQueSeEvaluo() {
        Resultado r = MotorReglas.evaluar(caso("true", "120", "365", "false", "0"));
        assertEquals(4, r.traza().size(), "debería pasar por las cuatro condiciones");
        assertEquals("CND1", r.traza().get(0).codigo());
        assertEquals("CND4", r.traza().get(3).codigo());
        r.traza().forEach(p -> assertFalse(p.comparacion().isBlank()));
    }

    @Test
    void unaVariableSinValorDetieneLaEvaluacionConUnMensaje() {
        Map<String, String> incompleto = caso("true", "", "365", "true", "5");
        Resultado r = MotorReglas.evaluar(incompleto);

        assertFalse(r.valido());
        assertTrue(r.error().contains("Tiempo_Transcurrido"),
                "el error debería nombrar la variable que falta: " + r.error());
    }

    @Test
    void elValorDeComparacionPuedeSerOtraVariable() {
        // CND2 compara Tiempo_Transcurrido contra Tiempo_Garantia, no contra un literal
        Resultado dentro = MotorReglas.evaluar(caso("true", "100", "365", "true", "1"));
        Resultado fuera = MotorReglas.evaluar(caso("true", "100", "50", "true", "1"));

        assertEquals("Reparación Equipo", dentro.accion());
        assertEquals("Pago Adicional", fuera.accion());
    }

    @Test
    void cadaSalidaDeclaraSiVaAOtraCondicionOAUnaAccion() {
        MotorReglas.condiciones().forEach(c -> {
            assertNotNull(c.siVerdadero().tipo());
            assertNotNull(c.siFalso().tipo());
            assertFalse(c.siVerdadero().destino().isBlank());
            assertFalse(c.siFalso().destino().isBlank());
        });
    }

    @Test
    void todaVariableUsadaEstaEnElCatalogo() {
        MotorReglas.condiciones().forEach(c ->
                assertNotNull(MotorReglas.variable(c.variable()),
                        "variable fuera del catálogo: " + c.variable()));
    }
}
