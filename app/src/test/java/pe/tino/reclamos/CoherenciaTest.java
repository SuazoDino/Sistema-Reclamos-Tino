package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.model.Modelo.Cliente;
import pe.tino.reclamos.model.Ticket;
import pe.tino.reclamos.repo.Catalogos;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.MotorReglas;
import pe.tino.reclamos.repo.Prototipo;
import pe.tino.reclamos.repo.Tickets;
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
        Prototipo.protocolos().forEach(f ->
                assertFalse(Prototipo.accionesDe(f[0]).isEmpty(),
                        "el protocolo " + f[0] + " no tiene acciones"));
    }

    @Test
    void lasAccionesDeclaranTiempoOperarioYCriticidad() {
        Prototipo.accionesDeProtocolo().forEach(f -> {
            assertEquals(5, f.length);
            assertDoesNotThrow(() -> Integer.parseInt(f[2]),
                    "el tiempo máximo de " + f[1] + " debería ser un número");
            assertTrue(Prototipo.TIPO_OPERARIO.contains(f[3]), "operario desconocido: " + f[3]);
            assertTrue(Prototipo.CRITICIDAD_ACCION.contains(f[4]),
                    "criticidad desconocida: " + f[4]);
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

        // 1. el cliente abre el ticket
        Ticket t = Tickets.abrir(Canal.PRESENCIAL, "Loc01", c.documento(), c.nombreCompleto(),
                c.tipo().etiqueta(), TipoObjeto.PRODUCTO, "Teléfono Móvil Samsung",
                "Funcionamiento", "La cámara no funciona", "25713", "12/01/2019");

        // 2. la prioridad y el plazo salieron de los catalogos
        assertEquals(Catalogos.prioridadDe("Funcionamiento", c.tipo().etiqueta()), t.prioridad());
        assertNotNull(t.limiteAtencion());

        // 3. el area lo toma con el protocolo que le corresponde
        String protocolo = Prototipo.protocoloDe(t.tipoProblema());
        t.asignar("Área Técnica de Reparación e Inspección", "E01", protocolo, "E01");
        assertEquals(Estado.ASIGNADO, t.estado());
        assertFalse(Prototipo.accionesDe(t.protocolo()).isEmpty());

        // 4. avanza por la maquina de estados hasta rechazarse
        assertTrue(t.cambiarEstado(Estado.EN_ATENCION, "E01", ""));
        assertTrue(t.cambiarEstado(Estado.RECHAZADO, "E01", "fuera de garantía"));
        assertNotNull(t.limiteImpugnacion(), "el rechazo abre el plazo de impugnación");

        // 5. el cliente impugna y sube de instancia
        assertTrue(t.impugnar("No estoy de acuerdo", "Cliente"));
        assertEquals(Instancia.SEGUNDA, t.instancia());
        assertEquals(Instancia.SEGUNDA.resuelve(), t.area());

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
                "Catálogo de Políticas", "Parámetros de Atención", "Consulta de Tickets",
                "Consulta de Indicadores", "Área - Data Entry", "Área - Reportes",
                "Cliente - Data Entry", "Cliente - Reportes", "Seguridad", "Módulo BATCH");
        assertEquals(pantallas.size(), Datos.ACCESOS.size());
        assertTrue(Datos.ACCESOS.containsAll(pantallas));
    }
}
