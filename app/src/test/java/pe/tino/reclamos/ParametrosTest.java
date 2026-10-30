package pe.tino.reclamos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.Dominio.*;
import pe.tino.reclamos.repo.Validacion.Campo;
import pe.tino.reclamos.repo.Validacion.Solicitud;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cada parametro que queda en las pantallas cambia el comportamiento del
 * sistema: si se cambia su valor, el flujo hace otra cosa.
 */
class ParametrosTest {

    private static final String DETALLE = "El equipo falla desde la primera semana de uso.";

    /** Problemas propios de esta prueba: otras clases ya registraron los del catalogo. */
    private static final String[] FALLA_A = {"Funcionamiento", "Falla de prueba A", Prototipo.TODO_PRODUCTO};
    private static final String[] FALLA_B = {"Funcionamiento", "Falla de prueba B", Prototipo.TODO_PRODUCTO};
    private static final String[] FALLA_C = {"Funcionamiento", "Falla de prueba C", Prototipo.TODO_PRODUCTO};

    @BeforeAll
    static void agregarProblemas() {
        Prototipo.problemas().addAll(List.of(FALLA_A, FALLA_B, FALLA_C));
    }

    @AfterAll
    static void quitarProblemas() {
        Prototipo.problemas().removeAll(List.of(FALLA_A, FALLA_B, FALLA_C));
    }

    @AfterEach
    void restaurar() {
        Parametros.restablecer();
        Prototipo.habilitar("E01", true);
        MotorReglas.asignar("Funcionamiento", "CND1");
    }

    private static Reclamo registrar(String doc, TipoObjeto tipo, String objeto, String tipoProblema, String problema) {
        Reclamos.Resultado r = Reclamos.registrar(new Solicitud(doc, Canal.WEB, null, tipo, objeto,
                tipoProblema, problema, DETALLE, "Reparación"));
        assertTrue(r.generado(), "debería generarse: " + r.errores() + r.rechazos());
        return r.reclamo();
    }

    /* ---------------- el plazo corre en horario laboral ---------------- */

    @Test
    void elPlazoNoCorreDeNoche() {
        // martes 27/10/2026 a las 18:40: las 8 horas empiezan el miercoles a las 9:00
        LocalDateTime noche = LocalDateTime.of(2026, 10, 27, 18, 40);
        assertEquals(LocalDateTime.of(2026, 10, 28, 17, 0), Parametros.sumarMinutosLaborables(noche, 8 * 60));
    }

    @Test
    void elPlazoSaltaDomingosYFeriados() {
        // sabado 31/10 a las 17:00 con 2 horas: queda 1 hora el sabado; el domingo 1/11 es feriado y
        // domingo, asi que la otra hora es el lunes 2/11
        LocalDateTime sabado = LocalDateTime.of(2026, 10, 31, 17, 0);
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 0), Parametros.sumarMinutosLaborables(sabado, 120));
    }

    @Test
    void cambiarLaJornadaCambiaElPlazo() {
        Parametros.cambiar("Fin de jornada", "13:00");
        LocalDateTime manana = LocalDateTime.of(2026, 10, 27, 9, 0);
        assertEquals(LocalDateTime.of(2026, 10, 28, 13, 0), Parametros.sumarMinutosLaborables(manana, 8 * 60),
                "con jornada de 4 horas, 8 horas laborables son dos dias");
    }

    @Test
    void elLimiteNuncaPasaElPlazoLegal() {
        Parametros.cambiar("Plazo legal de respuesta", "1");
        LocalDateTime t = LocalDateTime.of(2026, 10, 27, 10, 0);
        LocalDateTime limite = Catalogos.limiteAtencion(t, "Nuevo", Prioridad.BAJA);
        assertEquals(LocalDateTime.of(2026, 10, 27, 18, 0), limite, "10 dias de SLA topados por 1 dia legal");
    }

    @Test
    void unParametroInvalidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> Parametros.cambiar("Inicio de jornada", "19:00"));
        assertThrows(IllegalArgumentException.class, () -> Parametros.cambiar("Muestra de calidad", "0"));
        assertThrows(IllegalArgumentException.class, () -> Parametros.cambiar("Idioma", "Español"));
    }

    @Test
    void laGarantiaPorDefectoDecideAUnProductoSinPolitica() {
        assertEquals(365, Prototipo.plazoDeGarantia("PROD029"), "la camisa no tiene politica propia");
        Parametros.cambiar("Garantía por defecto", "30");
        assertEquals(30, Prototipo.plazoDeGarantia("PROD029"));
        assertEquals(365, Prototipo.plazoDeGarantia("PROD031"), "la politica propia manda");
    }

    /* ---------------- asignacion ---------------- */

    @Test
    void unEspecialistaDeshabilitadoNoRecibeReclamos() {
        Prototipo.habilitar("E01", false);
        Reclamo t = registrar("10112387203", TipoObjeto.PRODUCTO, "Computadores Notebook HP Omen 16",
                "Funcionamiento", FALLA_A[1]);
        assertEquals("E04", t.especialista());
    }

    @Test
    void laSolucionLaHaceQuienTieneElPerfilQuePideElProtocolo() {
        assertEquals("Técnico", Prototipo.operarioRequerido("PR-FUNC", "Inspección"));
        assertEquals("Especialista", Prototipo.operarioRequerido("PR-FUNC", "Solución del Reclamo"));

        Reclamo t = registrar("70775031", TipoObjeto.PRODUCTO, "Teléfono Móvil Motorola Moto G84",
                "Funcionamiento", FALLA_B[1]);
        List<String> pasos = Prototipo.pasosDe(t.eventoActual().protocolo(), t.eventoActual().evento())
                .stream().map(a -> a[0]).toList();
        Atencion.completar(t, t.especialista(), pasos, true, null);

        assertEquals("Solución del Reclamo", t.eventoActual().evento());
        assertEquals("Especialista", Prototipo.EMPLEADOS.get(t.eventoActual().especialista()).tipo());
    }

    @Test
    void unAreaEnUsoNoSeDeshabilita() {
        assertThrows(IllegalArgumentException.class, () -> Catalogos.habilitarArea("Logística", false));
        Catalogos.habilitarArea("Producción", false);
        assertFalse(Datos.catalogo("areas").habilitados().contains("Producción"));
        Catalogos.habilitarArea("Producción", true);
    }

    /* ---------------- motor de reglas ---------------- */

    @Test
    void unTipoSinReglaNoPasaPorElMotor() {
        MotorReglas.asignar("Funcionamiento", null);
        Reclamo t = registrar("70775031", TipoObjeto.PRODUCTO, "Teléfono Móvil Motorola Moto G84",
                "Funcionamiento", FALLA_C[1]);
        assertEquals(Atencion.NO_APLICA, t.solucionSugerida());
    }

    @Test
    void laCadenaArrancaEnLaCondicionAsignada() {
        MotorReglas.Resultado r = MotorReglas.evaluar(MotorReglas.valoresDeEjemplo(), "CND3");
        assertEquals("CND3", r.traza().get(0).codigo());
        assertThrows(IllegalArgumentException.class, () -> MotorReglas.asignar("Funcionamiento", "CND99"));
    }

    /* ---------------- la hoja del reclamo ---------------- */

    @Test
    void elDetalleYElPedidoSonObligatorios() {
        Map<Campo, String> e = Validacion.formulario(new Solicitud("72119474", Canal.WEB, null,
                TipoObjeto.PRODUCTO, "Teléfono Móvil Samsung Galaxy A54", "Funcionamiento",
                "Teléfono Sobrecalentado", "corto", null));
        assertTrue(e.containsKey(Campo.DETALLE));
        assertTrue(e.containsKey(Campo.PEDIDO));
    }

    @Test
    void laAtencionEsUnaQuejaYLoDemasUnReclamo() {
        assertEquals("Queja", Prototipo.naturalezaDe("Atención"));
        assertEquals("Reclamo", Prototipo.naturalezaDe("Cobranza"));
        Reclamo t = registrar("10112387203", TipoObjeto.SERVICIO, "Instalación", "Atención", "Mal trato");
        assertEquals("Queja", t.naturaleza());
        assertEquals(DETALLE, t.detalle());
    }
}
