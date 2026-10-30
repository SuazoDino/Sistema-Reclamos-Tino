package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.model.Reclamo;
import pe.tino.reclamos.repo.Atencion;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.MotorReglas;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Reclamos;
import pe.tino.reclamos.repo.Dominio.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coherencia de punta a punta: que lo que una pantalla escribe sea lo que
 * otra lee, y que no queden datos que se muestran sin existir en un catalogo.
 */
class CoherenciaTest {

    /* ---------------- una sola fuente por dato ---------------- */

    @Test
    void elClienteTieneUnaSolaFuente() {
        Cliente c = Datos.clientePorDocumento("72119474");
        assertNotNull(c, "el cliente del prototipo debería existir en el catálogo");
        assertFalse(c.nombres().isBlank());
        assertFalse(c.apellidos().isBlank());
        assertNotNull(c.tipo(), "sin categoría no se puede calcular prioridad ni plazo");
    }

    @Test
    void cadaClienteTieneUnaCategoriaConTiempoDeAtencionDefinido() {
        Datos.CLIENTES.forEach(c -> {
            for (Prioridad p : Prioridad.values()) {
                assertTrue(Catalogos.horasDeAtencion(c.tipo().etiqueta(), p) > 0,
                        "falta el SLA de " + c.tipo().etiqueta() + " con prioridad " + p);
            }
        });
    }

    /* ---------------- el protocolo que se asigna existe ---------------- */

    @Test
    void elProtocoloDeCadaTipoDeProblemaEstaEnElCatalogo() {
        List<String> codigos = Prototipo.protocolos().stream().map(f -> f[0]).toList();
        for (String tipo : Prototipo.tiposDeProblema()) {
            String protocolo = Prototipo.protocoloDe(tipo);
            assertTrue(codigos.contains(protocolo),
                    "el protocolo " + protocolo + " de " + tipo + " no está en el catálogo");
        }
    }

    @Test
    void todoProtocoloDelCatalogoTieneAccionesDefinidas() {
        Prototipo.protocolos().forEach(f -> {
            assertFalse(Prototipo.eventosDelProtocolo(f[0]).isEmpty(), "el protocolo " + f[0] + " no tiene eventos");
            for (String evento : Prototipo.eventosDelProtocolo(f[0])) {
                assertFalse(Prototipo.pasosDe(f[0], evento).isEmpty(),
                        "el evento " + evento + " del protocolo " + f[0] + " no tiene pasos");
            }
        });
    }

    @Test
    void lasAccionesDeclaranTiempoOperarioYCriticidad() {
        Prototipo.pasosDeProtocolo().forEach(f -> {
            assertEquals(7, f.length);
            assertTrue(Prototipo.eventosDelProtocolo(f[0]).contains(f[1]),
                    "el paso " + f[3] + " es de un evento que no está en su protocolo");
            assertDoesNotThrow(() -> Integer.parseInt(f[4]),
                    "el tiempo máximo de " + f[3] + " debería ser un número");
            assertTrue(Prototipo.TIPO_OPERARIO.contains(f[5]), "operario desconocido: " + f[5]);
            assertTrue(Prototipo.CRITICIDAD_ACCION.contains(f[6]),
                    "criticidad desconocida: " + f[6]);
        });
    }

    /* ---------------- la garantia que lee el motor existe ---------------- */

    @Test
    void elCatalogoDePoliticasDaElPlazoQueLeeElMotor() {
        int plazo = Prototipo.plazoDeGarantia("PROD007");
        assertTrue(plazo > 0, "sin plazo, la condición CND2 no tiene contra que comparar");
    }

    @Test
    void unaPoliticaDeshabilitadaNoAportaPlazo() {
        // PROD012 Comercial esta deshabilitada: el plazo vigente es el de la Legal
        assertEquals(365, Prototipo.plazoDeGarantia("PROD012"));
    }

    /* ---------------- la cadena completa ---------------- */

    @Test
    void elCicloDelTicketCierraDePuntaAPunta() {
        Cliente c = Datos.clientePorDocumento("72119474");

        // 1. el cliente abre el reclamo
        Reclamo t = Reclamos.abrir(Canal.PRESENCIAL, "Loc01", c.documento(), c.nombreCompleto(),
                c.tipo().etiqueta(), TipoObjeto.PRODUCTO, "Teléfono Móvil Samsung",
                "Funcionamiento", "La cámara no funciona", "25713", "12/01/2019");

        // 2. la prioridad y el plazo salieron de los catalogos
        assertEquals(Catalogos.prioridadDe("Funcionamiento", c.tipo().etiqueta()), t.prioridad());
        assertNotNull(t.limiteAtencion());

        // 3. el sistema lo asigna solo, con el protocolo que le corresponde
        assertEquals(Estado.ASIGNADO, t.estado());
        assertEquals("Área Técnica de Reparación e Inspección", t.area());
        assertEquals(Prototipo.protocoloDe(t.tipoProblema()), t.protocolo());
        assertFalse(Prototipo.pasosDe(t.protocolo(), t.eventoActual().evento()).isEmpty());

        // 4. avanza por la maquina de estados hasta rechazarse
        assertTrue(t.cambiarEstado(Estado.EN_ATENCION, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.RECHAZADO, "E01", "fuera de garantía"));
        assertNotNull(t.limiteImpugnacion(), "el rechazo abre el plazo de impugnación");

        // 5. el cliente impugna y sube de instancia
        assertTrue(Atencion.impugnar(t, "No estoy de acuerdo"));
        assertEquals(Instancia.SEGUNDA, t.instancia());
        assertEquals(Instancia.SEGUNDA.resuelve(), t.area());
        assertEquals(Estado.ASIGNADO, t.estado(), "la reasignación también es automática");

        // 6. todo el recorrido quedo en la bitacora
        assertTrue(t.bitacora().size() >= 6, "la bitácora debería registrar cada paso");
    }

    @Test
    void elMotorDeReglasResuelveConLosDatosQueElTicketTiene() {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("Tiene_Comprobante", "true");
        valores.put("Tiempo_Transcurrido", "100");
        valores.put("Tiempo_Garantia", String.valueOf(Prototipo.plazoDeGarantia("PROD007")));
        valores.put("Reparable", "false");
        valores.put("Stock", "0");

        MotorReglas.Resultado r = MotorReglas.evaluar(valores);
        assertTrue(r.valido(), "la cadena debería resolver: " + r.error());
        assertTrue(MotorReglas.ACCIONES.contains(r.accion()),
                "la solución debería estar en el catálogo de acciones");
    }

    /* ---------------- seguridad al dia ---------------- */

    @Test
    void losAccesosDeSeguridadNombranPantallasQueExisten() {
        List<String> pantallas = List.of("Parámetro General", "Catálogo de Reclamo",
                "Catálogo de Producto", "Catálogo de Servicios", "Catálogo de Problemas",
                "Catálogo de Cliente", "Catálogo de Protocolos", "Catálogo de Reglas",
                "Catálogo de Políticas", "Parámetros de Atención", "Consulta de Reclamos",
                "Consulta de Indicadores", "Área - Data Entry", "Área - Reportes",
                "Área - Revisión de Calidad",
                "Cliente - Data Entry", "Cliente - Reportes", "Seguridad", "Módulo BATCH");
        assertEquals(pantallas.size(), Datos.ACCESOS.size());
        assertTrue(Datos.ACCESOS.containsAll(pantallas));
    }
}
