package pe.tino.reclamos;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import pe.tino.reclamos.model.Modelo.*;
import pe.tino.reclamos.repo.Datos;
import pe.tino.reclamos.repo.Estado;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El estado es estatico y compartido por toda la aplicacion, asi que las
 * pruebas se ejecutan en orden y cada una parte de lo que dejo la anterior.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EstadoTest {

    private static Reclamo nuevoReclamo(String id) {
        return new Reclamo(id, LocalDate.now(), Datos.CLIENTES.get(0), "COMP099",
                Datos.PRODUCTOS.get(0), new Problema("Funcionamiento", "No enciende"),
                Canal.WEB, EstadoReclamo.EN_COLA, "PostVenta", "Sin asignar",
                null, null, "reclamo de prueba");
    }

    @Test @Order(1)
    void elCodigoSigueAlUltimoReclamoSembrado() {
        assertEquals("R008", Estado.siguienteCodigo());
    }

    @Test @Order(2)
    void agregarPonePrimeroElReclamoYAvisaALosOyentes() {
        AtomicInteger avisos = new AtomicInteger();
        Estado.alCambiarReclamos(r -> avisos.incrementAndGet());

        int antes = Estado.reclamos().size();
        Estado.agregar(nuevoReclamo("R008"));

        assertEquals(antes + 1, Estado.reclamos().size());
        assertEquals("R008", Estado.reclamos().get(0).id());
        assertEquals(1, avisos.get());
        assertEquals("R009", Estado.siguienteCodigo());
    }

    @Test @Order(3)
    void reemplazarActualizaSinDuplicar() {
        Reclamo original = Estado.reclamos().get(0);
        int antes = Estado.reclamos().size();

        Estado.reemplazar(original, new Reclamo(original.id(), original.fechaEmision(),
                original.cliente(), original.nroCompra(), original.producto(), original.problema(),
                original.canal(), EstadoReclamo.ATENDIDO, "Ventas", "E02",
                LocalDate.now(), null, "atendido"));

        assertEquals(antes, Estado.reclamos().size());
        assertEquals(EstadoReclamo.ATENDIDO, Estado.reclamos().get(0).estado());
    }

    @Test @Order(4)
    void unReclamoDesconocidoSeAgregaEnLugarDePerderse() {
        int antes = Estado.reclamos().size();
        Estado.reemplazar(nuevoReclamo("R500"), nuevoReclamo("R500"));
        assertEquals(antes + 1, Estado.reclamos().size());
    }
}
