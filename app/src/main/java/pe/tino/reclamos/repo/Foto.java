package pe.tino.reclamos.repo;

import pe.tino.reclamos.model.Modelo.Producto;
import pe.tino.reclamos.repo.MotorReglas.Condicion;
import pe.tino.reclamos.repo.Prototipo.Empleado;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Foto instantanea de toda la configuracion: catalogos, reglas, empleados y
 * parametros.
 *
 * Es lo que el libro pide para la Reversa de Operaciones: "contar con toda la
 * informacion a modo de foto instantanea antes de la actualizacion para
 * dejarla en el mismo estado despues de la reversa". Se toma antes de cada
 * cambio de mantenimiento y sirve para dos cosas: deshacer un cambio que deja
 * al sistema incoherente y revertir una operacion desde el Diario.
 *
 * No incluye los reclamos: una reversa devuelve la configuracion, no deshace
 * la atencion que ya se dio con ella.
 */
public final class Foto {

    private final Map<List<String[]>, List<String[]>> tablas = new IdentityHashMap<>();
    private final List<Producto> productos;
    private final List<String> areasExistentes;
    private final List<String> areasHabilitadas;
    private final Map<String, Empleado> empleados;
    private final Set<String> deshabilitados;
    private final List<Condicion> condiciones;
    private final Map<String, String> reglasAsignadas;
    private final LocalTime inicioJornada;
    private final LocalTime finJornada;
    private final int plazoLegal;
    private final int garantiaPorDefecto;
    private final int muestra;

    private Foto() {
        for (List<String[]> t : List.of(Catalogos.canales(), Catalogos.locales(), Catalogos.servicios(),
                Catalogos.areasPorTipo(), Catalogos.tiemposAtencion(), Catalogos.reglasPrioridad(),
                Prototipo.tipos(), Prototipo.problemas(), Prototipo.pasosDeProtocolo(), Prototipo.tiposDePolitica())) {
            tablas.put(t, copia(t));
        }
        productos = List.copyOf(Datos.PRODUCTOS);
        areasExistentes = List.copyOf(Datos.catalogo("areas").existentes());
        areasHabilitadas = List.copyOf(Datos.catalogo("areas").habilitados());
        empleados = new LinkedHashMap<>(Prototipo.EMPLEADOS);
        deshabilitados = Prototipo.deshabilitados();
        condiciones = List.copyOf(MotorReglas.condiciones());
        reglasAsignadas = new LinkedHashMap<>(MotorReglas.asignadas());
        inicioJornada = Parametros.inicioJornada();
        finJornada = Parametros.finJornada();
        plazoLegal = Parametros.plazoLegalDias();
        garantiaPorDefecto = Parametros.garantiaPorDefectoDias();
        muestra = Calidad.muestraCada();
    }

    /** Toma la foto de la configuracion tal como esta ahora. */
    public static Foto tomar() { return new Foto(); }

    /** Deja la configuracion exactamente como estaba al tomar la foto. */
    public void restaurar() {
        tablas.forEach((viva, guardada) -> {
            viva.clear();
            viva.addAll(copia(guardada));
        });
        reemplazar(Datos.PRODUCTOS, productos);
        reemplazar(Datos.catalogo("areas").existentes(), areasExistentes);
        reemplazar(Datos.catalogo("areas").habilitados(), areasHabilitadas);
        Prototipo.EMPLEADOS.clear();
        Prototipo.EMPLEADOS.putAll(empleados);
        Prototipo.restaurarDeshabilitados(deshabilitados);
        reemplazar(MotorReglas.condiciones(), condiciones);
        MotorReglas.asignadas().clear();
        MotorReglas.asignadas().putAll(reglasAsignadas);
        boolean otraJornada = !inicioJornada.equals(Parametros.inicioJornada())
                || !finJornada.equals(Parametros.finJornada());
        Parametros.restaurar(inicioJornada, finJornada, plazoLegal, garantiaPorDefecto, muestra);
        if (otraJornada) ParqueTickets.ajustarJornada();
    }

    private static List<String[]> copia(List<String[]> filas) {
        List<String[]> c = new ArrayList<>();
        filas.forEach(f -> c.add(f.clone()));
        return c;
    }

    private static <T> void reemplazar(List<T> viva, List<T> guardada) {
        viva.clear();
        viva.addAll(guardada);
    }
}
