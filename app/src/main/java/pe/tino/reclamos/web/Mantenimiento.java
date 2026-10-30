package pe.tino.reclamos.web;

import pe.tino.reclamos.model.Modelo.Perfil;
import pe.tino.reclamos.model.Modelo.Producto;
import pe.tino.reclamos.repo.*;
import pe.tino.reclamos.repo.MotorReglas.Condicion;
import pe.tino.reclamos.repo.MotorReglas.Salida;
import pe.tino.reclamos.repo.MotorReglas.TipoSalida;
import pe.tino.reclamos.repo.Prototipo.Empleado;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Las pantallas de Seguridad y de Mantenimiento de Parametros en la version
 * web. Cada pantalla se describe como secciones con bloques (tablas, listas,
 * simulador) y el navegador solo los dibuja.
 *
 * Regla de la pantalla: todo lo que se puede editar aqui lo lee el flujo
 * automatico. Cada tabla editable es la misma lista que usa el servidor, asi
 * que lo que se agrega, habilita o quita cambia el comportamiento al
 * instante. Lo que no cambia nada (magnitud, idioma, nivel, cruces de
 * catalogos) ya no esta.
 */
final class Mantenimiento {

    private Mantenimiento() {}

    private static final List<String> ESTADOS = List.of("Habilitado", "Deshabilitado");
    private static final List<String> PRIORIDADES = List.of("Alta", "Media", "Baja");
    private static final List<String> UNIDADES = List.of("Horas", "Días");

    /* ---------------- catalogos editables ---------------- */

    /** Un catalogo que el flujo lee: sus filas y como se edita. */
    private interface Editor {
        List<String[]> filas();
        default void agregar(List<String> v) { throw new IllegalArgumentException("Este catálogo no admite agregar."); }
        default void estado(int fila, String estado) { throw new IllegalArgumentException("Este catálogo no tiene estado."); }
        default void quitar(int fila) { throw new IllegalArgumentException("Este catálogo no admite quitar."); }
    }

    /**
     * Una lista de filas de texto: las primeras columnas de {@code claves} la
     * identifican (agregar una clave que ya existe la reemplaza).
     */
    private record Lista(List<String[]> lista, int columnas, int[] claves, int colEstado,
                         boolean quitable, Consumer<String[]> validar) implements Editor {
        public List<String[]> filas() { return lista; }

        public void agregar(List<String> v) {
            if (v.size() != columnas || v.stream().anyMatch(x -> x == null || x.isBlank())) {
                throw new IllegalArgumentException("Complete todos los campos.");
            }
            String[] fila = v.stream().map(String::trim).toArray(String[]::new);
            validar.accept(fila);
            for (int i = 0; i < lista.size(); i++) {
                String[] f = lista.get(i);
                if (Arrays.stream(claves).allMatch(c -> f[c].equalsIgnoreCase(fila[c]))) {
                    lista.set(i, fila);
                    return;
                }
            }
            lista.add(fila);
        }

        public void estado(int fila, String estado) {
            if (colEstado < 0) Editor.super.estado(fila, estado);
            lista.get(fila)[colEstado] = estado;
        }

        public void quitar(int fila) {
            if (!quitable) Editor.super.quitar(fila);
            lista.remove(fila);
        }
    }

    private static final Map<String, Supplier<Editor>> EDITORES = new LinkedHashMap<>();
    static {
        EDITORES.put("canales", () -> new Lista(Catalogos.canales(), 4, new int[]{0}, 3, false, f -> {
            if (Arrays.stream(Dominio.Canal.values()).noneMatch(c -> c.etiqueta().equals(f[0])))
                throw new IllegalArgumentException("Solo se pueden configurar los canales que el sistema atiende.");
            dominio(f[2], List.of("Sí", "No"), "Requiere local");
            dominio(f[3], ESTADOS, "Estado");
        }));
        EDITORES.put("locales", () -> new Lista(Catalogos.locales(), 3, new int[]{0}, 2, false, f -> {
            if (!f[0].matches("Loc\\d{2}")) throw new IllegalArgumentException("El código de local tiene el formato Loc01.");
            longitud(f[1], 5, 120, "Dirección");
            dominio(f[2], ESTADOS, "Estado");
        }));
        EDITORES.put("servicios", () -> new Lista(Catalogos.servicios(), 4, new int[]{1}, 3, false, f -> {
            dominio(f[0], Catalogos.TIPOS_SERVICIO, "Tipo de servicio");
            longitud(f[1], 3, 60, "Servicio");
            for (String t : f[2].split(",\\s*")) dominio(t, nombresDeTipos(), "Tipo de problema");
            dominio(f[3], ESTADOS, "Estado");
        }));
        EDITORES.put("tipos", () -> new Lista(Prototipo.tipos(), 3, new int[]{0}, 2, false, f -> {
            longitud(f[0], 3, 40, "Tipo de problema");
            dominio(f[1], Prototipo.NATURALEZAS, "Naturaleza");
            dominio(f[2], ESTADOS, "Estado");
        }));
        EDITORES.put("problemas", () -> new Lista(Prototipo.problemas(), 3, new int[]{0, 1}, -1, true, f -> {
            dominio(f[0], nombresDeTipos(), "Tipo de problema");
            longitud(f[1], 3, 80, "Problema");
        }));
        EDITORES.put("area-por-tipo", () -> new Lista(Catalogos.areasPorTipo(), 2, new int[]{0}, -1, false, f -> {
            dominio(f[0], nombresDeTipos(), "Tipo de problema");
            dominio(f[1], Datos.catalogo("areas").habilitados(), "Área responsable");
        }));
        EDITORES.put("pasos", () -> new Lista(Prototipo.pasosDeProtocolo(), 7, new int[]{0, 1, 2}, -1, true, f -> {
            List<String> codigos = Prototipo.protocolos().stream().map(p -> p[0]).toList();
            dominio(f[0], codigos, "Protocolo");
            dominio(f[1], Prototipo.eventosDelProtocolo(f[0]), "Evento de " + f[0]);
            entero(f[2], 1, 20, "Paso");
            longitud(f[3], 3, 80, "Acción");
            entero(f[4], 1, 480, "Tiempo máximo (min)");
            dominio(f[5], Prototipo.TIPO_OPERARIO, "Tipo de operario");
            dominio(f[6], Prototipo.CRITICIDAD_ACCION, "Criticidad");
        }));
        EDITORES.put("garantias", () -> new Lista(Prototipo.tiposDePolitica(), 4, new int[]{0, 1}, 3, true, f -> {
            dominio(f[0], Datos.PRODUCTOS.stream().map(Producto::id).toList(), "Producto");
            dominio(f[1], Prototipo.TIPOS_GARANTIA, "Garantía");
            entero(f[2], 0, 3650, "Plazo (días)");
            dominio(f[3], ESTADOS, "Estado");
        }));
        EDITORES.put("tiempos", () -> new Lista(Catalogos.tiemposAtencion(), 4, new int[]{0, 1}, -1, false, f -> {
            dominio(f[0], Datos.catalogo("clientes").habilitados(), "Tipo de cliente");
            dominio(f[1], PRIORIDADES, "Prioridad");
            entero(f[2], 1, 90, "Tiempo");
            dominio(f[3], UNIDADES, "Unidad");
        }));
        EDITORES.put("prioridad", () -> new Lista(Catalogos.reglasPrioridad(), 3, new int[]{0, 1}, -1, false, f -> {
            dominio(f[0], nombresDeTipos(), "Tipo de problema");
            dominio(f[1], Datos.catalogo("clientes").habilitados(), "Tipo de cliente");
            dominio(f[2], PRIORIDADES, "Prioridad");
        }));
        EDITORES.put("productos", () -> new Editor() {
            public List<String[]> filas() {
                return Datos.PRODUCTOS.stream().map(p -> new String[]{p.id(), p.bien(), p.marca(), p.modelo(),
                        p.clase(), p.familia()}).toList();
            }
            public void agregar(List<String> v) {
                if (v.size() != 4 || v.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Complete todos los campos.");
                String id = v.get(0).trim().toUpperCase();
                if (!id.matches("PROD\\d{3}")) throw new IllegalArgumentException("El código tiene el formato PROD001.");
                if (Datos.PRODUCTOS.stream().anyMatch(p -> p.id().equals(id))) throw new IllegalArgumentException("El código " + id + " ya existe.");
                String[] j = Datos.JERARQUIA_PRODUCTO.stream().filter(r -> r[3].equals(v.get(1))).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("El bien no está en la clasificación."));
                Datos.PRODUCTOS.add(new Producto(id, j[0], j[1], j[2], j[3], v.get(2).trim(), v.get(3).trim()));
            }
        });
        EDITORES.put("areas", () -> new Editor() {
            public List<String[]> filas() {
                var cat = Datos.catalogo("areas");
                return cat.existentes().stream().map(a -> new String[]{a,
                        cat.habilitados().contains(a) ? "Habilitado" : "Deshabilitado", Catalogos.usoDeArea(a)}).toList();
            }
            public void agregar(List<String> v) {
                String a = v.isEmpty() ? "" : v.get(0).trim();
                longitud(a, 3, 60, "Área");
                var cat = Datos.catalogo("areas");
                if (cat.existentes().stream().anyMatch(x -> x.equalsIgnoreCase(a))) throw new IllegalArgumentException("El área ya existe.");
                cat.existentes().add(a);
                cat.habilitados().add(a);
            }
            public void estado(int fila, String estado) {
                Catalogos.habilitarArea(Datos.catalogo("areas").existentes().get(fila), "Habilitado".equals(estado));
            }
        });
        EDITORES.put("empleados", () -> new Editor() {
            public List<String[]> filas() {
                return Prototipo.EMPLEADOS.values().stream().filter(e -> !e.esSupervisor())
                        .map(e -> new String[]{e.id(), e.tipo(), e.area(),
                                Prototipo.habilitado(e.id()) ? "Habilitado" : "Deshabilitado"}).toList();
            }
            public void agregar(List<String> v) {
                if (v.size() != 4 || v.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Complete todos los campos.");
                String id = v.get(0).trim().toUpperCase();
                if (!id.matches("E\\d{2}")) throw new IllegalArgumentException("El código tiene el formato E01.");
                dominio(v.get(1), Prototipo.TIPO_OPERARIO, "Tipo de operario");
                dominio(v.get(2), Datos.catalogo("areas").habilitados(), "Área");
                dominio(v.get(3), ESTADOS, "Estado");
                if (Prototipo.EMPLEADOS.containsKey(id)) throw new IllegalArgumentException("El código " + id + " ya existe.");
                Prototipo.EMPLEADOS.put(id, new Empleado(id, v.get(1), v.get(2)));
                Prototipo.habilitar(id, "Habilitado".equals(v.get(3)));
            }
            public void estado(int fila, String estado) {
                Prototipo.habilitar(filas().get(fila)[0], "Habilitado".equals(estado));
            }
        });
        EDITORES.put("reglas", () -> new Editor() {
            public List<String[]> filas() {
                return MotorReglas.asignadas().entrySet().stream()
                        .map(e -> new String[]{e.getKey(), e.getValue()}).toList();
            }
            public void agregar(List<String> v) {
                if (v.size() != 2) throw new IllegalArgumentException("Complete todos los campos.");
                dominio(v.get(0), nombresDeTipos(), "Tipo de reclamo");
                MotorReglas.asignar(v.get(0), v.get(1));
            }
            public void quitar(int fila) { MotorReglas.asignar(filas().get(fila)[0], null); }
        });
        EDITORES.put("condiciones", () -> new Editor() {
            public List<String[]> filas() {
                return MotorReglas.condiciones().stream().map(c -> new String[]{c.codigo(), c.variable(), c.operador(),
                        c.valor(), c.siVerdadero().toString(), c.siFalso().toString(), c.descripcion()}).toList();
            }
            public void agregar(List<String> v) {
                if (v.size() != 7 || v.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Complete todos los campos.");
                String codigo = v.get(0).trim().toUpperCase();
                if (!codigo.matches("CND\\d{1,3}")) throw new IllegalArgumentException("El código tiene el formato CND5.");
                dominio(v.get(1), MotorReglas.nombresDeVariable(), "Variable");
                dominio(v.get(2), MotorReglas.OPERADORES, "Operador");
                Condicion nueva = new Condicion(codigo, v.get(1), v.get(2), v.get(3).trim(),
                        salida(v.get(4)), salida(v.get(5)), v.get(6).trim());
                List<Condicion> lista = MotorReglas.condiciones();
                for (int i = 0; i < lista.size(); i++) {
                    if (lista.get(i).codigo().equals(codigo)) { lista.set(i, nueva); return; }
                }
                lista.add(nueva);
            }
        });
    }

    /** Nombre del catalogo para el Diario de Operaciones. */
    private static final Map<String, String> NOMBRES = Map.ofEntries(
            Map.entry("canales", "Canales"), Map.entry("locales", "Locales"), Map.entry("servicios", "Servicios"),
            Map.entry("tipos", "Tipos de reclamo"), Map.entry("problemas", "Problemas"),
            Map.entry("area-por-tipo", "Área por tipo de problema"), Map.entry("pasos", "Pasos de protocolo"),
            Map.entry("garantias", "Garantías"), Map.entry("tiempos", "Tiempos de atención"),
            Map.entry("prioridad", "Regla de prioridad"), Map.entry("productos", "Productos"),
            Map.entry("areas", "Áreas"), Map.entry("empleados", "Especialistas"),
            Map.entry("reglas", "Asignación de reglas"), Map.entry("condiciones", "Condiciones del motor"));

    /**
     * Agrega, cambia el estado o quita una fila. Corre como transaccion de
     * {@link Integridad}: si deja el sistema incoherente se deshace, y en
     * cualquier caso queda en el Diario de Operaciones.
     */
    static Object editar(String catalogo, String accion, Map<String, List<String>> f) {
        Supplier<Editor> s = EDITORES.get(catalogo);
        if (s == null || !List.of("agregar", "estado", "quitar").contains(accion)) return null;
        Editor e = s.get();
        String nombre = NOMBRES.getOrDefault(catalogo, catalogo);
        String mensaje;
        if (accion.equals("agregar")) {
            List<String> valores = f.getOrDefault("valor", List.of());
            mensaje = Integridad.aplicar(Auditoria.SIN_SESION, nombre, "Agregar o reemplazar", () -> {
                e.agregar(valores);
                return String.join(" | ", valores);
            });
        } else {
            int i = fila(f, e);
            String[] fila = e.filas().get(i);
            String clave = fila[0] + (fila.length > 1 ? " · " + fila[1] : "");
            if (accion.equals("estado")) {
                String estado = primero(f, "estado");
                dominio(estado, ESTADOS, "Estado");
                boolean habilitar = "Habilitado".equals(estado);
                if (catalogo.equals("empleados")) {
                    // ademas de cambiar el estado, reparte sus citas pendientes
                    mensaje = Asignacion.cambiarEstado(fila[0], habilitar, Auditoria.SIN_SESION);
                } else {
                    mensaje = Integridad.aplicar(Auditoria.SIN_SESION, nombre, (habilitar ? "Habilitar " : "Deshabilitar ") + clave,
                            () -> { e.estado(i, estado); return clave + ": " + estado.toLowerCase() + "."; });
                }
            } else {
                if (catalogo.equals("pasos") && Prototipo.pasosDe(fila[0], fila[1]).size() == 1) {
                    throw new IllegalArgumentException("No se puede quitar el único paso de " + fila[1]
                            + ": el evento quedaría sin duración.");
                }
                mensaje = Integridad.aplicar(Auditoria.SIN_SESION, nombre, "Quitar " + clave,
                        () -> { e.quitar(i); return clave + " quitado."; });
            }
        }
        return mapa("mensaje", mensaje, "filas", filas(e.filas()));
    }

    /**
     * La fila sobre la que se actua. El navegador manda tambien como la veia
     * ({@code actual}): si otro usuario cambio el catalogo entretanto, la fila
     * de ese numero ya es otra y la operacion se rechaza en vez de tocar la
     * fila equivocada.
     */
    private static int fila(Map<String, List<String>> f, Editor e) {
        String v = primero(f, "fila");
        if (v == null) throw new IllegalArgumentException("Seleccione una fila de la tabla.");
        int i = Integer.parseInt(v);
        String actual = primero(f, "actual");
        if (i < 0 || i >= e.filas().size()
                || (actual != null && !actual.equals(String.join("|", e.filas().get(i))))) {
            throw new CatalogoCambiado();
        }
        return i;
    }

    /** El catalogo cambio mientras el usuario lo veia: el navegador recarga las filas. */
    static final class CatalogoCambiado extends IllegalStateException {
        CatalogoCambiado() { super("Otro usuario cambió este catálogo mientras lo veía. Se recargó: revise y vuelva a intentarlo."); }
    }

    /** Las filas actuales de un catalogo, para que el navegador se ponga al dia. */
    static List<List<String>> filasDe(String catalogo) {
        Supplier<Editor> s = EDITORES.get(catalogo);
        return s == null ? List.of() : filas(s.get().filas());
    }

    /* ---------------- validaciones de dominio ---------------- */

    private static void dominio(String v, List<String> valores, String campo) {
        if (v == null || !valores.contains(v.trim())) {
            throw new IllegalArgumentException(campo + ": \"" + v + "\" no es un valor válido.");
        }
    }

    private static void longitud(String v, int min, int max, String campo) {
        int n = v == null ? 0 : v.trim().length();
        if (n < min || n > max) throw new IllegalArgumentException(campo + " debe tener entre " + min + " y " + max + " caracteres.");
    }

    private static void entero(String v, int min, int max, String campo) {
        try {
            int n = Integer.parseInt(v.trim());
            if (n < min || n > max) throw new IllegalArgumentException(campo + " debe estar entre " + min + " y " + max + ".");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un número entero.");
        }
    }

    /** "Ir a CND2" o "Aplicar Reembolso Total". */
    private static Salida salida(String texto) {
        String t = texto.trim();
        if (t.startsWith("Ir a ")) {
            String c = t.substring(5).trim();
            if (MotorReglas.condicion(c) == null) throw new IllegalArgumentException("No existe la condición " + c + ".");
            return new Salida(TipoSalida.CONDICION, c);
        }
        if (t.startsWith("Aplicar ")) {
            String a = t.substring(8).trim();
            dominio(a, MotorReglas.ACCIONES, "Solución");
            return new Salida(TipoSalida.ACCION, a);
        }
        throw new IllegalArgumentException("La salida debe ser \"Ir a CNDx\" o \"Aplicar <solución>\".");
    }

    private static List<String> nombresDeTipos() {
        return Prototipo.tipos().stream().map(f -> f[0]).toList();
    }

    /* ---------------- generalidades del sistema ---------------- */

    static Object cambiarParametro(String parametro, String valor) {
        String mensaje = Integridad.aplicar(Auditoria.SIN_SESION, "Generalidades del sistema",
                "Cambiar " + parametro, () -> {
                    Parametros.cambiar(parametro, valor);
                    return parametro + " = " + (valor == null ? "" : valor.trim()) + ".";
                });
        return mapa("mensaje", mensaje, "filas", filas(Parametros.lista()));
    }

    /* ---------------- diario de operaciones ---------------- */

    static Object diario() {
        Auditoria.Operacion ultima = Auditoria.ultimaRevertible();
        List<Auditoria.Operacion> ops = new ArrayList<>(Auditoria.diario());
        Collections.reverse(ops);
        return mapa("revertible", ultima == null ? null : ultima.numero(),
                "operaciones", ops.stream().map(o -> mapa("numero", o.numero(), "momento", o.momento(),
                        "usuario", o.usuario(), "modulo", o.modulo(), "operacion", o.operacion(),
                        "detalle", o.detalle(), "resultado", o.resultado())).toList());
    }

    static Object revertir(int numero) {
        String mensaje = Auditoria.revertir(numero, Auditoria.SIN_SESION);
        return mapa("mensaje", mensaje, "diario", diario());
    }

    /* ---------------- seguridad ---------------- */

    static Object seguridad() {
        return mapa("accesos", Datos.ACCESOS, "permisos", Datos.PERMISOS,
                "perfiles", Estado.perfiles().stream()
                        .map(p -> mapa("nombre", p.nombre(), "accesos", p.accesos())).toList());
    }

    static Object agregarPerfil(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Escriba el nombre del perfil.");
        if (Estado.perfiles().stream().anyMatch(p -> p.nombre().equalsIgnoreCase(nombre.trim()))) {
            throw new IllegalArgumentException("El perfil ya existe.");
        }
        Estado.perfiles().add(new Perfil(nombre.trim(), List.of()));
        Auditoria.anotar(Auditoria.SIN_SESION, "Seguridad", "Crear perfil", nombre.trim());
        return seguridad();
    }

    static Object guardarAccesos(String perfil, List<String> accesos) {
        List<Perfil> lista = Estado.perfiles();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).nombre().equals(perfil)) {
                List<String> validos = accesos.stream().filter(Datos.ACCESOS::contains).toList();
                lista.set(i, new Perfil(perfil, validos));
                Auditoria.anotar(Auditoria.SIN_SESION, "Seguridad", "Accesos de " + perfil,
                        validos.size() + " accesos: " + String.join(", ", validos));
                return mapa("mensaje", "Accesos del perfil \"" + perfil + "\" actualizados.",
                        "seguridad", seguridad());
            }
        }
        throw new IllegalArgumentException("No existe el perfil " + perfil + ".");
    }

    /* ---------------- motor de reglas ---------------- */

    static Object evaluar(Map<String, List<String>> f) {
        Map<String, String> valores = new LinkedHashMap<>();
        MotorReglas.valoresDeEjemplo().keySet().forEach(k -> valores.put(k, Optional.ofNullable(primero(f, k)).orElse("")));
        String inicio = Optional.ofNullable(primero(f, "inicio")).orElse(MotorReglas.codigos().get(0));
        MotorReglas.Resultado r = MotorReglas.evaluar(valores, inicio);
        return mapa("valido", r.valido(), "accion", r.accion(), "error", r.error(),
                "traza", r.traza().stream().map(p -> mapa("codigo", p.codigo(),
                        "comparacion", p.comparacion(), "resultado", p.resultado(), "salida", p.salida())).toList());
    }

    /* ---------------- pantallas ---------------- */

    static Object pantalla(String clave) {
        return switch (clave) {
            case "general" -> general();
            case "reclamos-eventos" -> reclamosEventos();
            case "productos" -> productos();
            case "servicios" -> servicios();
            case "problemas" -> problemas();
            case "clientes" -> clientes();
            case "protocolos" -> protocolos();
            case "reglas" -> reglas();
            case "politicas" -> politicas();
            case "atencion" -> atencion();
            default -> null;
        };
    }

    private static Object general() {
        List<String> areas = Datos.catalogo("areas").habilitados();
        return pantalla("Parámetro General", "Generalidades de la empresa y del sistema.", false,
                seccion("Generalidades de la empresa",
                        nota(vivo(alta(estado(tabla("Áreas", List.of("Área", "Estado", "La usa"), null), 1),
                                campo("Área")), "areas"),
                                "Un área que está en uso no se puede deshabilitar: dejaría reclamos sin quién los atienda."),
                        nota(vivo(alta(filtros(estado(tabla("Especialistas por área",
                                List.of("Código", "Tipo de operario", "Área", "Estado"), null), 3), 2),
                                campo("Código"), campo("Tipo de operario", Prototipo.TIPO_OPERARIO),
                                campo("Área", areas), campo("Estado", ESTADOS)), "empleados"),
                                "Un especialista deshabilitado (vacaciones, descanso médico) deja de recibir reclamos. "
                                        + "El tipo de operario decide qué eventos puede atender: "
                                        + "Asistente < Técnico < Especialista.")),
                seccion("Generalidades del sistema",
                        mapa("tipo", "parametros", "titulo", "Parámetros del sistema",
                                "columnas", List.of("Parámetro", "Valor", "Unidad", "Qué cambia en el sistema"),
                                "filas", filas(Parametros.lista()))));
    }

    private static Object reclamosEventos() {
        List<String[]> eventos = new ArrayList<>();
        Map<String, List<String>> usos = new LinkedHashMap<>();
        for (String[] e : Prototipo.eventosDeProtocolo()) usos.computeIfAbsent(e[2], k -> new ArrayList<>()).add(e[0]);
        usos.computeIfAbsent(Prototipo.EVENTO_SEGUIMIENTO, k -> new ArrayList<>()).add("Control de calidad");
        usos.forEach((evento, protocolos) -> eventos.add(new String[]{evento, String.join(", ", protocolos)}));
        return pantalla("Catálogo de Reclamos y Eventos",
                "Los tipos de reclamo que acepta el sistema y los eventos por los que pasan.", true,
                seccion("Tipos de reclamo",
                        nota(vivo(alta(estado(tabla("Tipos de reclamo",
                                List.of("Tipo de problema", "Naturaleza", "Estado"), null), 2),
                                campo("Tipo de problema"), campo("Naturaleza", Prototipo.NATURALEZAS),
                                campo("Estado", ESTADOS)), "tipos"),
                                "Es el único catálogo de tipos de problema del sistema. Reclamo: disconformidad con el "
                                        + "producto o servicio. Queja: malestar con la atención. Un tipo deshabilitado "
                                        + "deja de ofrecerse en el formulario.")),
                seccion("Eventos",
                        nota(tabla("Eventos y protocolos que los usan", List.of("Evento", "Lo usa"), eventos),
                                "Los eventos se ordenan y se definen paso a paso en el Catálogo de Protocolos.")));
    }

    private static Object productos() {
        List<String> bienes = Datos.JERARQUIA_PRODUCTO.stream().map(r -> r[3]).toList();
        return pantalla("Catálogo de Producto",
                "Productos clasificados en segmento, familia, clase y bien.", true,
                seccion("Productos",
                        nota(vivo(alta(filtros(tabla("Productos que vende la empresa",
                                List.of("Código", "Bien", "Marca", "Modelo", "Clase", "Familia"), null), 1, 2),
                                campo("Código"), campo("Bien", bienes), campo("Marca"), campo("Modelo")), "productos"),
                                "El bien decide qué problemas aplican al producto (Catálogo de Problemas); "
                                        + "la clase y la familia salen de la clasificación.")),
                seccion("Clasificación",
                        filtros(tabla("Segmento › Familia › Clase › Bien",
                                List.of("Segmento", "Familia", "Clase", "Bien"), Datos.JERARQUIA_PRODUCTO), 0, 1, 2)));
    }

    private static Object servicios() {
        return pantalla("Catálogo de Servicios",
                "Servicios sobre los que se puede reclamar y qué tipos de problema admite cada uno.", false,
                seccion(null,
                        nota(vivo(alta(estado(tabla("Servicios",
                                List.of("Tipo de servicio", "Servicio", "Tipos de problema que admite", "Estado"), null), 3),
                                campo("Tipo de servicio", Catalogos.TIPOS_SERVICIO), campo("Servicio"),
                                campo("Tipos de problema (separados por coma)"), campo("Estado", ESTADOS)), "servicios"),
                                "Un servicio deshabilitado deja de ofrecerse en el formulario al instante. "
                                        + "Los tipos que admite evitan, por ejemplo, \"La cámara no funciona\" sobre un recojo en tienda.")));
    }

    private static Object problemas() {
        List<String> aplica = new ArrayList<>(List.of(Prototipo.TODO_PRODUCTO, Prototipo.TODO_SERVICIO));
        aplica.addAll(new LinkedHashSet<>(Datos.JERARQUIA_PRODUCTO.stream().map(r -> r[3]).toList()));
        Catalogos.servicios().forEach(s -> aplica.add(s[1]));
        return pantalla("Catálogo de Problemas",
                "Problemas específicos de cada tipo y a qué producto o servicio aplica cada uno.", false,
                seccion(null,
                        nota(vivo(quitable(alta(filtros(tabla("Problemas registrados",
                                List.of("Tipo de problema", "Problema", "Aplica a"), null), 0),
                                campo("Tipo de problema", nombresDeTipos()), campo("Problema"), campo("Aplica a", aplica))),
                                "problemas"),
                                "El formulario del cliente solo ofrece los problemas que aplican a lo que compró. "
                                        + "\"Aplica a\" admite varios valores separados por coma.")));
    }

    private static Object clientes() {
        List<String[]> plazos = new ArrayList<>();
        for (String tipo : Datos.catalogo("clientes").habilitados()) {
            plazos.add(new String[]{tipo, plazo(tipo, "Alta"), plazo(tipo, "Media"), plazo(tipo, "Baja")});
        }
        return pantalla("Catálogo de Cliente",
                "Tipos de cliente y lo que cambia para cada uno.", false,
                seccion(null,
                        nota(tabla("Tipos de cliente y su plazo de atención",
                                List.of("Tipo de cliente", "Prioridad alta", "Prioridad media", "Prioridad baja"), plazos),
                                "El tipo de cliente cambia la prioridad y el plazo del reclamo. Se editan en "
                                        + "Parámetros de Atención › Tiempos de atención y › Prioridad."),
                        nota(lista("Consideraciones para categorizar al cliente",
                                Datos.catalogo("consideraciones").habilitados()),
                                "Las usará el proceso batch de categorización (BAT-03) para recalcular el tipo de "
                                        + "cada cliente. Hoy el tipo viene registrado con el cliente.")));
    }

    private static String plazo(String tipo, String prioridad) {
        return Catalogos.tiemposAtencion().stream().filter(f -> f[0].equals(tipo) && f[1].equals(prioridad))
                .map(f -> f[2] + " " + f[3].toLowerCase()).findFirst().orElse("-");
    }

    private static Object protocolos() {
        List<String> codigos = Prototipo.protocolos().stream().map(f -> f[0]).toList();
        List<String[]> eventos = new ArrayList<>();
        for (String[] e : Prototipo.eventosDeProtocolo()) {
            eventos.add(new String[]{e[0], e[1], e[2], Prototipo.duracionDe(e[0], e[2]) + " min",
                    Prototipo.operarioRequerido(e[0], e[2])});
        }
        List<String> nombresEventos = new ArrayList<>(new LinkedHashSet<>(
                Prototipo.eventosDeProtocolo().stream().map(e -> e[2]).toList()));
        return pantalla("Catálogo de Protocolos",
                "Un protocolo por tipo de reclamo; cada protocolo tiene eventos y cada evento sus pasos.", false,
                seccion(null,
                        tabla("Protocolos", List.of("Protocolo", "Tipo de problema"), Prototipo.protocolos()),
                        nota(filtros(tabla("Eventos del protocolo",
                                List.of("Protocolo", "Orden", "Evento", "Duración de la cita", "Pide como mínimo"), eventos), 0),
                                "La duración es la suma de los tiempos de los pasos; el operario mínimo es el más alto "
                                        + "que piden sus pasos. Las dos cosas las usa la asignación automática."),
                        nota(vivo(quitable(alta(filtros(tabla("Pasos del evento",
                                List.of("Protocolo", "Evento", "Paso", "Acción", "Tiempo máximo (min)",
                                        "Tipo de operario", "Criticidad"), null), 0, 1),
                                campo("Protocolo", codigos), campo("Evento", nombresEventos),
                                campo("Paso"), campo("Acción"), campo("Tiempo máximo (min)"),
                                campo("Tipo de operario", Prototipo.TIPO_OPERARIO),
                                campo("Criticidad", Prototipo.CRITICIDAD_ACCION))), "pasos"),
                                "Un paso crítico no se puede omitir para completar el evento.")));
    }

    private static Object reglas() {
        List<String[]> variables = MotorReglas.variables().stream().map(v -> new String[]{
                v.nombre(), v.tipo().name(), v.origen(), v.unidad()}).toList();
        List<Map<String, Object>> simulador = new ArrayList<>();
        MotorReglas.valoresDeEjemplo().forEach((nombre, valor) -> {
            MotorReglas.Variable v = MotorReglas.variable(nombre);
            simulador.add(mapa("nombre", nombre, "valor", valor, "origen", v == null ? "" : v.origen()));
        });
        List<String> salidas = new ArrayList<>(MotorReglas.codigos().stream().map(c -> "Ir a " + c).toList());
        MotorReglas.ACCIONES.forEach(a -> salidas.add("Aplicar " + a));
        return pantalla("Catálogo de Reglas",
                "Condiciones encadenadas que deciden la solución sugerida de cada reclamo.", true,
                seccion("Condiciones",
                        nota(vivo(alta(tabla("Cadena de condiciones",
                                List.of("Condición", "Variable", "Operador", "Valor", "Si es verdadero",
                                        "Si es falso", "Descripción"), null),
                                campo("Condición"), campo("Variable", MotorReglas.nombresDeVariable()),
                                campo("Operador", MotorReglas.OPERADORES), campo("Valor"),
                                campo("Si es verdadero", salidas), campo("Si es falso", salidas), campo("Descripción")),
                                "condiciones"),
                                "Cada salida va a otra condición (Ir a) o termina en una solución (Aplicar). "
                                        + "Agregar una condición con un código existente la reemplaza.")),
                seccion("Asignar regla",
                        nota(vivo(quitable(alta(tabla("Regla de cada tipo de reclamo",
                                List.of("Tipo de reclamo", "Condición inicial"), null),
                                campo("Tipo de reclamo", nombresDeTipos()), campo("Condición inicial", MotorReglas.codigos()))),
                                "reglas"),
                                "Solo los tipos de esta tabla pasan por el motor, y la cadena arranca en su condición "
                                        + "inicial. Un tipo sin regla no recibe solución sugerida: la decide el área.")),
                seccion("Variables",
                        nota(tabla("Variables que el motor puede leer",
                                List.of("Variable", "Tipo", "De dónde sale", "Unidad"), variables),
                                "Una condición solo puede comparar variables de esta lista.")),
                seccion("Acciones",
                        lista("Soluciones con las que puede terminar la cadena", MotorReglas.ACCIONES)),
                seccion("Simulador", mapa("tipo", "simulador", "titulo", "Datos del caso", "variables", simulador,
                        "inicios", MotorReglas.codigos())));
    }

    private static Object politicas() {
        List<Map<String, Object>> productos = Datos.PRODUCTOS.stream()
                .map(p -> mapa("valor", p.id(), "texto", p.id() + " - " + Datos.nombreProducto(p.id()))).toList();
        return pantalla("Catálogo de Políticas",
                "Las políticas de garantía de cada producto.", false,
                seccion(null,
                        nota(vivo(quitable(alta(filtros(estado(tabla("Garantía por producto",
                                List.of("Producto", "Garantía", "Plazo (días)", "Estado"), null), 3), 0, 3),
                                campo("Producto", productos), campo("Garantía", Prototipo.TIPOS_GARANTIA),
                                campo("Plazo (días)"), campo("Estado", ESTADOS))), "garantias"),
                                "Vale la mayor garantía habilitada del producto; si no tiene ninguna, la garantía por "
                                        + "defecto de Parámetro General. El motor la lee como Tiempo_Garantia y decide "
                                        + "si el reclamo está en garantía.")));
    }

    private static Object atencion() {
        List<String> canales = Arrays.stream(Dominio.Canal.values()).map(Dominio.Canal::etiqueta).toList();
        List<String> clientes = Datos.catalogo("clientes").habilitados();
        return pantalla("Parámetros de Atención",
                "Lo que usa la atención automática: canales, locales, áreas, plazos, prioridad y estados.", true,
                seccion("Canales",
                        vivo(alta(estado(tabla("Canales de ingreso",
                                List.of("Canal", "Horario", "Requiere local", "Estado"), null), 3),
                                campo("Canal", canales), campo("Horario"), campo("Requiere local", List.of("Sí", "No")),
                                campo("Estado", ESTADOS)), "canales")),
                seccion("Locales",
                        vivo(alta(estado(tabla("Locales", List.of("Local", "Dirección", "Estado"), null), 2),
                                campo("Local"), campo("Dirección"), campo("Estado", ESTADOS)), "locales")),
                seccion("Área por tipo de problema",
                        nota(vivo(alta(tabla("Área responsable por tipo de problema",
                                List.of("Tipo de problema", "Área responsable"), null),
                                campo("Tipo de problema", nombresDeTipos()),
                                campo("Área responsable", Datos.catalogo("areas").habilitados())), "area-por-tipo"),
                                "El tipo de problema decide el área; dentro del área el sistema elige al "
                                        + "especialista habilitado con menor carga.")),
                seccion("Tiempos de atención",
                        nota(vivo(alta(tabla("Tiempos de atención (SLA)",
                                List.of("Tipo de cliente", "Prioridad", "Tiempo", "Unidad"), null),
                                campo("Tipo de cliente", clientes), campo("Prioridad", PRIORIDADES),
                                campo("Tiempo"), campo("Unidad", UNIDADES)), "tiempos"),
                                "Se cuenta en tiempo laborable: un día es una jornada y el plazo no corre de noche "
                                        + "ni en feriados. Nunca pasa el plazo legal de Parámetro General.")),
                seccion("Prioridad",
                        nota(vivo(alta(tabla("Regla de prioridad",
                                List.of("Tipo de problema", "Tipo de cliente", "Prioridad"), null),
                                campo("Tipo de problema", nombresDeTipos()), campo("Tipo de cliente", clientes),
                                campo("Prioridad", PRIORIDADES)), "prioridad"),
                                "Con qué prioridad nace el reclamo, sin que nadie la elija.")),
                seccion("Instancias",
                        nota(tabla("Instancias de atención",
                                List.of("Instancia", "Quién resuelve", "Días de impugnación", "Admite impugnación"),
                                Catalogos.instancias()),
                                "De aquí sale la fecha límite de impugnación. La última instancia no admite impugnación.")),
                seccion("Feriados",
                        nota(tabla("Feriados", List.of("Fecha", "Feriado"), Catalogos.feriados()),
                                "Lunes a sábado, sin feriados. La hora de inicio y fin de la jornada está en "
                                        + "Parámetro General › Generalidades del sistema.")),
                seccion("Estados",
                        nota(tabla("Estados del reclamo",
                                List.of("Estado", "Qué significa", "Puede pasar a"), Catalogos.estados()),
                                "Es la máquina de estados del reclamo: es parte del diseño, por eso solo se consulta.")));
    }

    /* ---------------- piezas ---------------- */

    private static Map<String, Object> pantalla(String titulo, String subtitulo, boolean pestanias,
                                                Map<?, ?>... secciones) {
        return mapa("titulo", titulo, "subtitulo", subtitulo, "pestanias", pestanias,
                "secciones", List.of(secciones));
    }

    private static Map<String, Object> seccion(String titulo, Map<?, ?>... bloques) {
        return mapa("titulo", titulo, "bloques", List.of(bloques));
    }

    /** Una tabla; si {@code filas} es null, sus filas las pone {@link #vivo}. */
    private static Map<String, Object> tabla(String titulo, List<String> columnas, List<String[]> filas) {
        return mapa("tipo", "tabla", "titulo", titulo, "columnas", columnas, "filas", filas == null ? null : filas(filas));
    }

    /** Columnas por las que se puede filtrar la tabla. */
    private static Map<String, Object> filtros(Map<String, Object> tabla, int... columnas) {
        tabla.put("filtros", Arrays.stream(columnas).boxed().toList());
        return tabla;
    }

    /** Columna de estado: la tabla ofrece Habilitar y Deshabilitar. */
    private static Map<String, Object> estado(Map<String, Object> tabla, int columna) {
        tabla.put("estado", columna);
        return tabla;
    }

    /** Formulario para agregar una fila. */
    private static Map<String, Object> alta(Map<String, Object> tabla, Map<?, ?>... campos) {
        tabla.put("alta", List.of(campos));
        return tabla;
    }

    private static Map<String, Object> quitable(Map<String, Object> tabla) {
        tabla.put("quitar", true);
        return tabla;
    }

    /** La tabla se guarda en el servidor y la lee el flujo automatico. */
    private static Map<String, Object> vivo(Map<String, Object> tabla, String catalogo) {
        tabla.put("catalogo", catalogo);
        tabla.put("filas", filas(EDITORES.get(catalogo).get().filas()));
        return tabla;
    }

    private static Map<String, Object> nota(Map<String, Object> bloque, String nota) {
        bloque.put("nota", nota);
        return bloque;
    }

    private static Map<String, Object> campo(String etiqueta) {
        return mapa("etiqueta", etiqueta);
    }

    private static Map<String, Object> campo(String etiqueta, List<?> opciones) {
        return mapa("etiqueta", etiqueta, "opciones", opciones);
    }

    private static Map<String, Object> lista(String titulo, List<String> valores) {
        return mapa("tipo", "lista", "titulo", titulo, "valores", valores);
    }

    private static List<List<String>> filas(List<String[]> filas) {
        return filas.stream().map(f -> List.of(f)).toList();
    }

    private static String primero(Map<String, List<String>> m, String clave) {
        List<String> v = m.get(clave);
        return v == null || v.isEmpty() || v.get(0).isBlank() ? null : v.get(0);
    }

    private static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
