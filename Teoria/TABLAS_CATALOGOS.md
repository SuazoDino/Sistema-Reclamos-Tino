# Diseño interno — Tablas de Catálogos

## Criterios

Del entregable 2, que es la base del proyecto:

- **Un catálogo por pantalla de catálogo.** El entregable 2 diseña el Catálogo de Reclamo, el de Protocolos y el de Problemas: cada uno corresponde a una pantalla de Mantenimiento de Parámetros. Aquí se sigue la misma idea, con las pantallas que tiene este prototipo.

Del libro del profesor:

- **Qué es un catálogo:** la *información tabulada / de parámetros (catálogo)* (p. 74): "variables que se mantienen fijas para definir escenarios del negocio", mantenidas en el módulo de **Mantenimiento de Parámetros** (p. 10).
- **Un dominio fijo no necesita tabla.** En la integridad de dominio (pp. 179–183), los valores fijos de una columna se controlan con `check` o `create domain`. Alta/Media/Baja, los estados del reclamo o las instancias no los configura la gerencia: son dominios.
- **Una FK necesita su entidad.** La clave foránea es "un atributo que siendo almacenado en una entidad, también es almacenado en otra, pero en esta última tiene la categoría de clave" (3.1.1.4, p. 37). Si una maestra o un catálogo guarda `CodArea`, `CodLocal` o `CodBien` como FK, tiene que existir la tabla donde ese código es clave.
- **Parámetro de módulo no es tabla.** El libro define parámetro como "variable que permanece fija, para definir escenarios posibles del comportamiento del módulo", junto a la Entrada, la Función y la Salida del módulo (p. 12). La jornada, los plazos, los SLA o los feriados son parámetros de los módulos que los usan, no entidades con clave. Residen en memoria (p. 170).
- **Relación 1 a n como arreglo** (p. 47): la relación se guarda como vector dentro del registro principal, con su número de elementos en el mismo registro. Así no hacen falta tablas de relación.
- **Memoria residente** (p. 170) y **organización indexada**, con el IC de la terna online elegido por menor residuo. Las tablas con vector de largo variable dan su longitud como `(MAX, MODA)` (pp. 79, 90–91).

## Resultado: 9 catálogos

| # | Catálogo | Pantalla del prototipo | Equivalente en el entregable 2 |
|---|---|---|---|
| 1 | Catálogo de Reclamo | 03 Catálogo de reclamos y eventos | Catálogo Reclamo |
| 2 | Catálogo de Problemas | 06 Catálogo de problemas | Catálogo Problemas |
| 3 | Catálogo de Protocolos | 08 Catálogo de protocolos | Catálogo Protocolos |
| 4 | Catálogo de Productos | 04 Catálogo de producto + 10 Catálogo de políticas | (el entregable usaba `CodProducto` como FK) |
| 5 | Catálogo de Servicios | 05 Catálogo de servicios | — (el entregable no tenía servicios) |
| 6 | Catálogo de Reglas | 09 Catálogo de reglas | — (tenía la pantalla, sin tabla) |
| 7 | Catálogo de Áreas | 02 Parámetro general | — (el entregable guardaba el área como texto en Operarios) |
| 8 | Catálogo de Locales | 11 Parámetros de atención | — |
| 9 | Catálogo de Clasificación | 04 Catálogo de producto (pestaña Clasificación) | — |

Las otras pantallas no necesitan tabla propia:

| Pantalla / lista | Dónde queda |
|---|---|
| 07 Catálogo de cliente (tipos y su plazo) | Tipo de cliente: dominio `check`. Plazos (SLA): parámetro de módulo |
| 10 Catálogo de políticas (garantías) | Columnas de garantía Legal y Comercial del Catálogo de Productos |
| Jornada, plazo legal, muestra de calidad, garantía por defecto (02) | Parámetros de módulo |
| SLA, feriados, días de impugnación y área de cada instancia (11) | Parámetros de módulo |
| Canales (11) | Dominio `check` |
| Área por tipo, regla de prioridad, regla por tipo (09 y 11) | Columnas del Catálogo de Reclamo |
| Estados, instancias, variables del motor (solo consulta) | Dominios `check` |

---

## Dominios con `check` (no son tablas)

| Dominio | Valores |
|---|---|
| Tipo de cliente | 1 Nuevo, 2 Ordinario, 3 Vip, 4 SuperVip |
| Tipo de empleado | 1 Asistente, 2 Técnico, 3 Especialista, 4 Supervisor |
| Estado del reclamo | Registrado, Asignado, En atención, En revisión, Resuelto, Rechazado, Impugnado, Cerrado… |
| Instancia | 1 Primera, 2 Segunda, 3 Tercera |
| Prioridad | 1 Alta, 2 Media, 3 Baja |
| Naturaleza | 1 Reclamo, 2 Queja |
| Evento | 1 Inspección, 2 Solución, 3 Seguimiento |
| Tipo de movimiento | Registro, Programación de evento, Cambio de estado, Impugnación, Revisión, Conformidad, Cierre |
| Resultado | Aprobado, Observado, Muestra al azar, Rechazo propuesto, Devuelto, Conforme, No conforme |
| Tipo de objeto | 1 Producto, 2 Servicio |
| Tipo de servicio | Logístico, Comercial, Técnico, Atención |
| Variable del motor | Tiempo_Garantia, Reparable, Stock, Tiene_Comprobante, Tipo_Cliente… |
| Acción del motor | Reembolso Total, Intercambio Equipo, Reparación Equipo… |
| Operador | EQ, NE, GT, GE, LT, LE |
| Canal | 1 Presencial, 2 Telefónico, 3 Internet, 4 Web |
| Nivel de clasificación | 1 Segmento, 2 Familia, 3 Clase, 4 Bien |

Ejemplo, como en el libro: `CodPrioridad int(1) check (CodPrioridad in (1, 2, 3))`.

---

## 1. Catálogo de Reclamo

El tipo de problema (Funcionamiento, Entrega, Cobranza, Atención) con todo lo que va 1 a 1 con él.

| Nombre | Catálogo de Reclamo |
|---|---|
| Organización | indexada |
| Longitud Registro | (37, 37) |
| IC | 512 |
| Capacidad | (10, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodTipoProblema | int | 2 | X | |
| TipoProblema | char | 20 | | |
| CodNaturaleza | int | 1 | | |
| CodArea | int | 2 | | X |
| CodProtocolo | int | 3 | | X |
| CodCondicionInicial | char | 4 | | X |
| PrioridadNuevo | int | 1 | | |
| PrioridadOrdinario | int | 1 | | |
| PrioridadVip | int | 1 | | |
| PrioridadSuperVip | int | 1 | | |
| Flg_Activo | int | 1 | | |
| | | **37** | | |

- `CodArea`: área responsable (Catálogo de Áreas).
- `CodCondicionInicial`: condición donde arranca el motor de reglas. Vacía si el tipo no pasa por el motor.
- `PrioridadNuevo` … `PrioridadSuperVip`: la prioridad (Alta, Media o Baja) que corresponde a cada tipo de cliente. Es la regla de prioridad de la pantalla 11, guardada como un arreglo de 4 elementos dentro del registro (libro, p. 47).

## 2. Catálogo de Problemas

| Nombre | Catálogo de Problemas |
|---|---|
| Organización | indexada |
| Longitud Registro | (71, 51) |
| IC | 1536 |
| Capacidad | (60, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodProblema | int | 3 | X | |
| CodTipoProblema | int | 2 | | X |
| Problema | char | 40 | | |
| NroAplica | int | 1 | | |
| AplicaA (hasta 5) | int | 25 | | X |
| | | **71** | | |

- `AplicaA`: arreglo de hasta 5 elementos (libro, p. 47); `NroAplica` dice cuántos se usan. Cada elemento mide 5 bytes: tipo de objeto (1) y código (4) del bien (Catálogo de Clasificación) o del servicio al que aplica el problema. Un código 0 significa "todo producto" o "todo servicio".
- Longitud de registro (MAX, MODA) = (71, 51): lo típico es que aplique a un solo bien o servicio.

## 3. Catálogo de Protocolos

Un registro por protocolo, con sus pasos en un arreglo (libro, p. 47). Así `CodProtocolo` es la clave y las tablas que lo usan como FK (Catálogo de Reclamo, Maestra Reclamos, Movimiento de Reclamos) apuntan a una clave de verdad: "en esta última tiene la categoría de clave" (3.1.1.4, p. 37).

| Nombre | Catálogo de Protocolos |
|---|---|
| Organización | indexada |
| Longitud Registro | (532, 124) |
| IC | 512 |
| Capacidad | (10, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodProtocolo | int | 3 | X | |
| Protocolo | char | 17 | | |
| NroPasos | int | 2 | | |
| Pasos: vector de 1 a 10 (depende de NroPasos) | | 51 c/u | | |
| · CodEvento | int | 2 | | |
| · OrdenEvento | int | 1 | | |
| · NumPaso | int | 2 | | |
| · Accion | char | 40 | | |
| · TiempoMaxMin | int | 4 | | |
| · CodTipoOperario | int | 1 | | |
| · Flg_Critico | int | 1 | | |
| | | **(532, 124)** | | |

- `Protocolo`: nombre del protocolo; hay uno por tipo de problema (Funcionamiento, Entrega, Cobranza, Atención).
- Cada elemento del vector es un paso: a qué evento pertenece (`CodEvento`, dominio), en qué orden va ese evento dentro del protocolo (`OrdenEvento`), el número de paso dentro del evento (`NumPaso`), la acción, su tiempo máximo en minutos, el tipo de operario mínimo y si es crítico.
- Un paso se identifica por protocolo + evento + `NumPaso`, que es justo lo que guarda el Movimiento de Soluciones (el protocolo y el evento los saca de su `NroMovReclamo`).
- Longitud de registro (MAX, MODA) = (532, 124): parte fija de 22 bytes y 51 por paso. MAX = 22 + 10 × 51; MODA = 22 + 2 × 51, porque lo típico son 2 pasos (hoy: Funcionamiento 4, Entrega 2, Cobranza 2, Atención 1). Como en el ejemplo del libro (p. 91), el registro máximo puede ser mayor que el IC; el IC se elige con la moda.
- La duración de cada cita (suma de los tiempos de los pasos de un evento) y el operario mínimo de cada evento se calculan, no se guardan.

## 4. Catálogo de Productos

| Nombre | Catálogo de Productos |
|---|---|
| Organización | indexada |
| Longitud Registro | (51, 51) |
| IC | 1536 |
| Capacidad | (5000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodProducto | char | 7 | X | |
| CodBien | int | 4 | | X |
| Marca | char | 15 | | |
| Modelo | char | 15 | | |
| PlazoGarantiaLegal | int | 4 | | |
| Flg_GarantiaLegal | int | 1 | | |
| PlazoGarantiaComercial | int | 4 | | |
| Flg_GarantiaComercial | int | 1 | | |
| | | **51** | | |

- `CodBien`: el bien en el Catálogo de Clasificación. De él salen clase, familia y segmento.
- Garantías (pantalla 10, Catálogo de Políticas): plazo en días y si está habilitada, para la Legal y la Comercial. Vale la mayor habilitada; si ninguna lo está, la garantía por defecto.
- Sin `Stock` (lo lleva el almacén) y sin `Estado` (quien ya compró debe poder reclamar mientras tenga garantía).

## 5. Catálogo de Servicios

| Nombre | Catálogo de Servicios |
|---|---|
| Organización | indexada |
| Longitud Registro | (40, 36) |
| IC | 512 |
| Capacidad | (20, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodServicio | int | 3 | X | |
| CodTipoServicio | int | 2 | | |
| Servicio | char | 25 | | |
| Flg_Activo | int | 1 | | |
| NroTipos | int | 1 | | |
| TiposAdmitidos (hasta 4) | int | 8 | | X |
| | | **40** | | |

- `TiposAdmitidos`: arreglo de hasta 4 códigos de tipo de problema (2 bytes cada uno); `NroTipos` dice cuántos se usan.
- Longitud de registro (MAX, MODA) = (40, 36): lo típico son 2 tipos admitidos.
- El servicio sí lleva estado: uno que ya no se ofrece deja de aparecer en el formulario.

## 6. Catálogo de Reglas

La cadena de condiciones del motor de reglas.

| Nombre | Catálogo de Reglas |
|---|---|
| Organización | indexada |
| Longitud Registro | (94, 94) |
| IC | 512 |
| Capacidad | (30, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodCondicion | char | 4 | X | |
| CodVariable | int | 3 | | |
| Operador | char | 2 | | |
| Valor | char | 15 | | |
| TipoSalidaV | int | 1 | | |
| DestinoV | char | 4 | | X |
| TipoSalidaF | int | 1 | | |
| DestinoF | char | 4 | | X |
| Descripcion | char | 60 | | |
| | | **94** | | |

- `TipoSalidaV` / `TipoSalidaF`: 1 = ir a otra condición, 2 = aplicar una acción. El destino es el código de otra condición (FK a esta misma tabla) o el de una acción (dominio).

## 7. Catálogo de Áreas

| Nombre | Catálogo de Áreas |
|---|---|
| Organización | indexada |
| Longitud Registro | (43, 43) |
| IC | 512 |
| Capacidad | (15, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodArea | int | 2 | X | |
| Area | char | 40 | | |
| Flg_Activo | int | 1 | | |
| | | **43** | | |

- La usan como FK la Maestra Operarios y el Catálogo de Reclamo. Un área en uso no se puede deshabilitar. "Control de Calidad" es el área de los supervisores.

## 8. Catálogo de Locales

| Nombre | Catálogo de Locales |
|---|---|
| Organización | indexada |
| Longitud Registro | (74, 74) |
| IC | 1024 |
| Capacidad | (20, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodLocal | int | 3 | X | |
| Local | char | 30 | | |
| Direccion | char | 40 | | |
| Flg_Activo | int | 1 | | |
| | | **74** | | |

- La usa como FK la Maestra Reclamos (local donde se registró un reclamo presencial).

## 9. Catálogo de Clasificación

Los cuatro niveles (Segmento › Familia › Clase › Bien) en una sola tabla: cada fila apunta a su nivel superior.

| Nombre | Catálogo de Clasificación |
|---|---|
| Organización | indexada |
| Longitud Registro | (69, 69) |
| IC | 512 |
| Capacidad | (200, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodClasif | int | 4 | X | |
| CodNivel | int | 1 | | |
| Descripcion | char | 60 | | |
| CodPadre | int | 4 | | X |
| | | **69** | | |

- `CodPadre`: el nivel superior (FK a esta misma tabla; 0 para un segmento). Un bien apunta a su clase, la clase a su familia y la familia a su segmento.
- La usan como FK el Catálogo de Productos (`CodBien`) y el Catálogo de Problemas (`AplicaA`).

---

## Parámetros de módulo (no son tablas)

Según el libro (p. 12), cada módulo tiene Entrada, Función, Salida y **Parámetro**. Estos valores son parámetros de los módulos que los usan: la gerencia los cambia en Parámetro General (02) y Parámetros de Atención (11), y residen en memoria (p. 170).

| Parámetro | Valor actual | Módulo que lo usa |
|---|---|---|
| Inicio y fin de jornada | 09:00 – 18:00 | Asignación (citas), generación del parque de tickets |
| Plazo legal de respuesta | 15 días hábiles | Cálculo del límite de atención |
| Muestra de calidad | 1 de cada 10 | Revisión de calidad |
| Garantía por defecto | 365 días | Validación de garantía |
| SLA por tipo de cliente y prioridad | 12 valores en horas | Cálculo del límite de atención |
| Días de impugnación y área de cada instancia | por instancia | Impugnación |
| Feriados | lista de fechas | Asignación, generación del parque de tickets |
| Horario de cada canal | por canal | Registro del reclamo |

---

## Cálculo del IC

Si la tabla entera cabe en un IC de 512 (`N × (RL + 3) + 4 ≤ 512`), se usa 512. Si no, se elige el menor residuo de la terna online: `#RL = (IC − 4) / (RL + 3)`, `r = (IC − 4) − #RL·(RL + 3)`. En las de longitud variable se usa la MODA.

| Catálogo | RL | N | ¿Cabe en 512? | r con 512 / 1024 / 1536 | IC elegido |
|---|---|---|---|---|---|
| Reclamo | 37 | 10 | Sí (404) | — | **512** |
| Problemas | 51 (moda) | 60 | No | 22 / 48 / **20** | **1536** (28 RL) |
| Protocolos | 124 (moda) | 10 | No | **0** / 4 / 8 | **512** (4 RL) |
| Productos | 51 | 5000 | No | 22 / 48 / **20** | **1536** (28 RL) |
| Servicios | 36 (moda) | 20 | No | **1** / 6 / 11 | **512** (13 RL) |
| Reglas | 94 | 30 | No | **23** / 50 / 77 | **512** (5 RL) |
| Áreas | 43 | 15 | No (694) | **2** / 8 / 14 | **512** (11 RL) |
| Locales | 74 | 20 | No | 46 / **19** / 69 | **1024** (13 RL) |
| Clasificación | 69 | 200 | No | **4** / 12 / 20 | **512** (7 RL) |

---

## Diferencias con el entregable 2

- Se mantienen sus tres catálogos: Reclamo, Problemas y Protocolos.
- El Catálogo de Protocolos pasa de una fila por paso a **una fila por protocolo con sus pasos en un arreglo**. Con una fila por paso, `CodProtocolo` no era clave de ninguna tabla y no podía ser FK en las demás.
- Se agregan Productos, Servicios y Reglas porque este prototipo tiene esas pantallas y sus datos necesitan dónde guardarse.
- Se agregan Áreas, Locales y Clasificación porque otras tablas las usan como FK (libro 3.1.1.4). El entregable 2 guardaba el área como texto en Operarios.
- Lo demás de las pantallas 02 y 11 son parámetros de módulo (libro p. 12), no tablas.
- IC calculado: el entregable 2 usaba 2560 en todos sin cálculo.

## Pendiente de confirmar con el profesor

- La regla "si toda la tabla cabe en un IC de 512, se usa 512" sale del criterio de economía del libro, no está escrita en él.
- Las N y las MODAS son estimaciones.
- Se mantuvo la notación del AC `19*12*5`.
