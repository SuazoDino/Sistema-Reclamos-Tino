package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.Reclamos;
import pe.tino.reclamos.repo.Validacion;
import pe.tino.reclamos.repo.Validacion.Campo;
import pe.tino.reclamos.repo.Validacion.Solicitud;
import pe.tino.reclamos.repo.Dominio.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Cambios 1 y 2: el formulario no deja pasar campos vacios ni combinaciones sin sentido, y el reclamo tiene que proceder. */
class ValidacionTest {

    private static final String DETALLE = "El producto falla desde la primera semana de uso.";

    private static final String ALEXANDER = "72119474";
    private static final String SAMSUNG = "Teléfono Móvil Samsung Galaxy A54";

    @Test
    void unFormularioVacioMarcaTodosLosCamposObligatorios() {
        Map<Campo, String> e = Validacion.formulario(new Solicitud("", null, null, null, null, null, null, DETALLE, "Reparación"));
        assertTrue(e.containsKey(Campo.DOCUMENTO));
        assertTrue(e.containsKey(Campo.CANAL));
        assertTrue(e.containsKey(Campo.TIPO_OBJETO));
    }

    @Test
    void sinElegirProductoTipoNiProblemaNoHayTicket() {
        Map<Campo, String> e = Validacion.formulario(new Solicitud(ALEXANDER, Canal.WEB, null,
                TipoObjeto.PRODUCTO, null, null, null, DETALLE, "Reparación"));
        assertEquals(List.of(Campo.OBJETO, Campo.TIPO_PROBLEMA, Campo.PROBLEMA), List.copyOf(e.keySet()));
    }

    @Test
    void elDocumentoDebeSerDniORucDeUnClienteRegistrado() {
        assertTrue(errorDe("72A19474").contains("dígitos"));
        assertTrue(errorDe("1234").contains("8 dígitos"));
        assertTrue(errorDe("12345678").contains("no corresponde"));
        assertNull(errorDe(ALEXANDER));
    }

    private static String errorDe(String documento) {
        return Validacion.formulario(new Solicitud(documento, Canal.WEB, null, TipoObjeto.PRODUCTO,
                SAMSUNG, "Funcionamiento", "No enciende", DETALLE, "Reparación")).get(Campo.DOCUMENTO);
    }

    @Test
    void elLocalSoloEsObligatorioEnLaAtencionPresencial() {
        assertTrue(Validacion.formulario(new Solicitud(ALEXANDER, Canal.PRESENCIAL, null,
                TipoObjeto.PRODUCTO, SAMSUNG, "Funcionamiento", "No enciende", DETALLE, "Reparación")).containsKey(Campo.LOCAL));
        assertFalse(Validacion.formulario(new Solicitud(ALEXANDER, Canal.WEB, null,
                TipoObjeto.PRODUCTO, SAMSUNG, "Funcionamiento", "No enciende", DETALLE, "Reparación")).containsKey(Campo.LOCAL));
    }

    @Test
    void unRecojoEnTiendaNoAdmiteQueLaCamaraNoFuncione() {
        assertFalse(Validacion.tiposPara(ALEXANDER, TipoObjeto.SERVICIO, "Recojo en tienda")
                .contains("Funcionamiento"));
        Map<Campo, String> e = Validacion.formulario(new Solicitud(ALEXANDER, Canal.WEB, null,
                TipoObjeto.SERVICIO, "Recojo en tienda", "Funcionamiento", "La cámara no funciona", DETALLE, "Reparación"));
        assertTrue(e.containsKey(Campo.TIPO_PROBLEMA));
    }

    @Test
    void losProblemasSeFiltranPorElBienReclamado() {
        List<String> celular = Validacion.problemasPara(ALEXANDER, TipoObjeto.PRODUCTO, SAMSUNG, "Funcionamiento");
        assertTrue(celular.contains("La cámara no funciona"));
        assertTrue(celular.contains("Teléfono Sobrecalentado"));
        assertFalse(celular.contains("Mala instalación"), "es un problema de servicio");
    }

    @Test
    void elClienteSoloVeLosProductosQueCompro() {
        assertEquals(List.of(SAMSUNG), Validacion.objetosPara(ALEXANDER, TipoObjeto.PRODUCTO));
        Map<Campo, String> e = Validacion.formulario(new Solicitud(ALEXANDER, Canal.WEB, null,
                TipoObjeto.PRODUCTO, "Computadores Notebook HP Omen 16", "Funcionamiento", "No enciende", DETALLE, "Reparación"));
        assertTrue(e.containsKey(Campo.OBJETO));
    }

    @Test
    void sobreUnServicioQueNoContratoSoloPuedeReclamarPorAtencion() {
        assertEquals(List.of("Atención"), Validacion.tiposPara(ALEXANDER, TipoObjeto.SERVICIO, "Cobro"));
        assertEquals(List.of("No registramos esa compra a su nombre."),
                Validacion.procedencia(new Solicitud(ALEXANDER, Canal.WEB, null,
                        TipoObjeto.SERVICIO, "Cobro", "Cobranza", "Facturación Doble", DETALLE, "Reparación")));
    }

    @Test
    void elComprobanteTieneFormatoYFechaNoFutura() {
        assertTrue(Validacion.comprobante("B001-00012345", "12/01/2026").isEmpty());
        assertTrue(Validacion.comprobante("X001-1", "12/01/2026").containsKey(Campo.COMPROBANTE));
        assertTrue(Validacion.comprobante("F001-1", "31/02/2026").containsKey(Campo.FECHA_COMPROBANTE));
        String manana = Validacion.fecha(LocalDate.now().plusDays(1));
        assertTrue(Validacion.comprobante("F001-1", manana).containsKey(Campo.FECHA_COMPROBANTE));
    }

    @Test
    void elMismoReclamoAbiertoNoSeDuplica() {
        Solicitud s = new Solicitud("70775031", Canal.WEB, null, TipoObjeto.PRODUCTO,
                "Teléfono Móvil Motorola Moto G84", "Atención", "Mal trato", DETALLE, "Reparación");
        Reclamos.Resultado primero = Reclamos.registrar(s);
        assertTrue(primero.generado(), "el primero debería generarse: " + primero.rechazos());

        Reclamos.Resultado segundo = Reclamos.registrar(s);
        assertFalse(segundo.generado());
        assertTrue(segundo.rechazos().get(0).contains(primero.reclamo().numero()));
    }

    @Test
    void unProductoFueraDeGarantiaSeAtiendePeroQuedaMarcado() {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud("20548712341", Canal.WEB, null,
                TipoObjeto.PRODUCTO, "Computadores Notebook HP Omen", "Funcionamiento", "No enciende", DETALLE, "Reparación"));
        assertTrue(r.generado());
        assertTrue(r.reclamo().fueraDeGarantia());
        assertEquals("Pago Adicional", r.reclamo().solucionSugerida(),
                "fuera de garantía el motor sugiere que el cliente asuma el costo");
        assertTrue(r.reclamo().bitacora().stream().anyMatch(b -> b.detalle().contains("Fuera de garantía")));
    }
}
