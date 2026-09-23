package pe.tino.reclamos;

import org.junit.jupiter.api.Test;
import pe.tino.reclamos.repo.Estadisticas;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadisticasTest {

    @Test
    void elHistoricoTerminaElMesAnteriorAlActual() {
        List<Estadisticas.Mes> meses = Estadisticas.historico();
        assertEquals(YearMonth.now().minusMonths(1), meses.get(meses.size() - 1).mes());
        for (int i = 1; i < meses.size(); i++) {
            assertEquals(meses.get(i - 1).mes().plusMonths(1), meses.get(i).mes());
        }
    }

    @Test
    void laProyeccionExtiendeLaRecta() {
        assertEquals(50.0, Estadisticas.proyectar(List.of(10.0, 20.0, 30.0, 40.0)), 1e-9);
        assertEquals(7.0, Estadisticas.proyectar(List.of(7.0)), 1e-9);
    }

    @Test
    void laProyeccionNoDaNegativos() {
        assertEquals(0.0, Estadisticas.proyectar(List.of(30.0, 20.0, 10.0, 0.0)), 1e-9);
    }
}
