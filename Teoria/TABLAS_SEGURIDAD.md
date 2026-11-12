# Diseño interno — Tablas de Seguridad

## Criterios del libro

- **Seguridad:** "tiene por función restringir acceso a los datos. Esta restricción es de acuerdo al perfil asignado al usuario" (Figura 1.3, p. 10). "Todo perfil de usuario resume el acceso a un tipo de dato y grado de actualización desde consultas y reportes hasta las operaciones de alta, baja y cambio" (p. 9).
- **Confidencialidad:** perfil de usuario e "identificar y autenticar" (control de confidencialidad, p. 177).
- **Auditoría:** supervisar el sistema "dejando huella en cada evento. Estos hechos quedan registrados en el archivo bitácora, desde el cual se emitirán una serie de reportes de supervisión y control conocido como **Diario de Operaciones**" (p. 9).
- **Reversa de operaciones:** "se debe de contar con toda la información a modo de **foto instantánea** antes de la actualización para dejarla en el mismo estado después de la reversa" (p. 9).
- **Registro bitácora:** "graba cronológicamente las transacciones" y "permite conocer el registro modificado, el atributo cambiado tanto su valor anterior y el nuevo" (p. 200).
- **Organización secuencial** para la bitácora: "todo registro nuevo se agrega al final del archivo. Usos: archivos temporales, archivos loging (bitácora)" (3.1.2.6, p. 78).

## Resultado: 2 tablas

| # | Tabla | Pantalla del prototipo | Organización |
|---|---|---|---|
| 1 | Tabla de Perfiles | 01 Perfiles y accesos | indexada |
| 2 | Diario de Operaciones | 01b Diario de operaciones | secuencial |

---

## 1. Tabla de Perfiles

Cada perfil con las pantallas que puede abrir y los permisos que tiene.

| Nombre | Tabla de Perfiles |
|---|---|
| Organización | indexada |
| Longitud Registro | (53, 53) |
| IC | 512 |
| Clave | (2, 0) |
| Capacidad | (10, 19*24*9) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodPerfil | int | 2 | X | |
| Perfil | char | 30 | | |
| Accesos (19) | char | 19 | | |
| Permisos (2) | char | 2 | | |
| | | **53** | | |

- `Accesos`: arreglo fijo de 19 elementos de 1 byte (libro, p. 47), uno por pantalla en el orden del prototipo: Parámetro General, Catálogo de Reclamo, Catálogo de Producto … Seguridad, Módulo BATCH. Cada uno vale `S` (concedido) o `N`.
- `Permisos`: arreglo fijo de 2 elementos con el mismo criterio: "Agregar líneas para los bienes" y "Agregar productos a las líneas".
- Las pantallas no necesitan tabla: son un dominio fijo, porque las define el sistema y no la gerencia. Al ser un arreglo de largo fijo, el registro es de longitud fija.
- Perfiles del prototipo: Administrador de BD, Administrador de Seguridad, Cliente, Especialista, Supervisor de Calidad, Gerente General.

## 2. Diario de Operaciones

La bitácora del módulo de Auditoría. Registra **todo cambio a la configuración** (catálogos, parámetros, especialistas y perfiles), también los intentos rechazados, y guarda la foto previa para la reversa.

| Nombre | Diario de Operaciones |
|---|---|
| Organización | secuencial |
| Longitud Registro | (548, 252) |
| IC | 3072 |
| Capacidad | (20000, 19*4*12) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| NroOperacion | int | 8 | X | |
| FecOperacion | int | 12 | | |
| Usuario | char | 10 | | |
| Modulo | char | 25 | | |
| Operacion | char | 30 | | |
| ClaveRegistro | char | 12 | | |
| CodResultado | int | 1 | | |
| Detalle | varchar | 150 | | |
| ValorAnterior | varchar | 150 | | |
| ValorNuevo | varchar | 150 | | |
| | | **548** | | |

- `NroOperacion`: correlativo; da el orden cronológico.
- `Usuario`: quién hizo la operación, como texto: el ID de empleado cuando la pantalla lo pide (E01, S01…), `GERENCIA` en las pantallas de Mantenimiento de Parámetros y `SISTEMA` en lo que hace el propio sistema. No es FK porque todavía no hay inicio de sesión (ver Pendiente).
- `Modulo` y `ClaveRegistro`: qué tabla y qué registro se cambió (por ejemplo, Catálogo de Locales, `5`).
- `CodResultado`: dominio `check`: 1 Aplicada, 2 Rechazada, 3 Revertida.
- `Detalle`: lo que se ve en la pantalla 01b; en una rechazada, el motivo (por ejemplo, "el cambio dejaría el sistema incoherente…").
- `ValorAnterior`: la **foto instantánea** del registro antes del cambio, que es lo que pide el libro para la reversa. `ValorNuevo`: el registro después del cambio. En una rechazada los dos van vacíos, porque no se cambió nada.
- **Reversa:** se vuelve a grabar `ValorAnterior` en el registro `ClaveRegistro` de la tabla `Modulo`, y se agrega al final del diario una operación nueva. La original pasa a Revertida. Solo se revierte la última operación aplicada, porque revertir una anterior desharía también las de después.
- Longitud variable (libro, pp. 79, 90–91): parte fija de 98 bytes.
  - MAX = 98 + 150 + 150 + 150 = **548**.
  - MODA = 98 + 70 + 42 + 42 = **252**, suponiendo un detalle típico de 70 caracteres y una foto de 42 (un registro de catálogo típico).
- Capacidad: unas 50 operaciones de configuración por día laborable, alrededor de un año.

### Por qué secuencial

- Se graba siempre al final y en orden de llegada: es exactamente una bitácora (p. 78).
- Nunca se modifica un registro grabado, salvo marcar como Revertida la operación que se revierte.
- Se lee completo y en orden, del más reciente al más antiguo, que es lo que muestra la pantalla 01b.
- Por eso el IC va en la terna batch (3072, 3584, 4096): en un secuencial "su IC debe ser lo más grande posible" (p. 78).

---

## Cálculo del IC

Terna online para las indexadas y terna batch para la secuencial. `#RL = (IC − 4) / (RL + 3)`, `r = (IC − 4) − #RL·(RL + 3)`; se elige el menor `r`. En la de longitud variable se usa la MODA.

| Tabla | RL | N | Terna | r | IC elegido |
|---|---|---|---|---|---|
| Perfiles | 53 | 10 | 512 / 1024 / 1536 | **4** / 12 / 20 | **512** (9 RL) |
| Diario de Operaciones | 252 (moda) | 20000 | 3072 / 3584 / 4096 | **8** / 10 / 12 | **3072** (12 RL) |

## Pendiente de confirmar con el profesor

- Si las tablas de Seguridad se evalúan en esta entrega o solo las del núcleo del negocio.
- **Inicio de sesión (identificar y autenticar, p. 177):** queda para una etapa posterior. Cuando se haga, se agregará una Tabla de Usuarios (código, perfil, clave cifrada) y `Usuario` pasará a ser FK a ella. Mientras tanto, la Tabla de Perfiles define qué pantallas corresponden a cada perfil.
- El AC se calcula como en el resto del diseño (ver «Cálculo del AC» en `TABLAS_MAESTRAS.md`).
