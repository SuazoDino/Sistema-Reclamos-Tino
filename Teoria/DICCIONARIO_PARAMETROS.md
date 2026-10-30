# Diccionario de parámetros y catálogos

## Por qué hace falta

El libro define el parámetro como la *"variable que permanece fija, para definir escenarios posibles del
comportamiento del módulo"*. Para que un valor sea parámetro debe cumplir dos cosas:

1. **Cambiar el comportamiento.** Si se cambia el valor y el sistema hace lo mismo, no es parámetro: es decoración
   (por eso se quitaron Magnitud, Idioma, Nivel y los cruces de catálogos).
2. **Tener un tipo y un dominio cerrados.** El flujo automático hace cálculos con estos valores: suma minutos,
   compara días, elige por nivel. Si "Tiempo" aceptara texto libre o "Hora de inicio" aceptara `25:90`, el sistema
   fallaría en medio de una asignación. Por eso cada atributo tiene su tipo, su rango y su validación, y la
   pantalla rechaza lo que no cumple antes de guardarlo.

El tipo también es lo que pide el diseño de la base de datos: cada atributo de aquí será una columna con ese tipo,
longitud y restricción.

## Clases de atributo

| Clase | Qué es | Ejemplo |
|---|---|---|
| **Parámetro** | Valor único que fija el comportamiento de todo el sistema. | Inicio de jornada, plazo legal |
| **Catálogo** | Lista de valores válidos que otros datos referencian. Se habilita o deshabilita, no se borra si está en uso. | Tipos de problema, canales |
| **Regla (catálogo de relación)** | Fila que relaciona dos catálogos y decide algo. | Tipo de problema × tipo de cliente → prioridad |
| **Descriptivo** | Dato que describe un elemento del catálogo; no cambia el flujo, pero identifica o clasifica. | Dirección del local, marca |
| **Derivado** | No se ingresa: lo calcula el sistema. Se muestra, no se edita. | Duración de la cita, operario mínimo |

Convenciones de tipo: `VARCHAR(n)` texto de hasta n caracteres, `CHAR(n)` texto de largo fijo, `SMALLINT`
entero, `TIME` hora, `ESTADO` = `VARCHAR(13)` con dominio {Habilitado, Deshabilitado}. "FK →" indica que el
valor debe existir en otro catálogo.

---

## Parámetro General

### Generalidades del sistema (parámetros)

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Inicio de jornada | Parámetro | `TIME` (hh:mm) | 00:00–23:59, menor que fin de jornada | Hora desde la que se programan citas y corre el SLA |
| Fin de jornada | Parámetro | `TIME` (hh:mm) | mayor que inicio de jornada | Hora hasta la que se programan citas y corre el SLA |
| Plazo legal de respuesta | Parámetro | `SMALLINT` (días hábiles) | 1–60 (hoy 15) | Tope del límite de atención |
| Muestra de calidad | Parámetro | `SMALLINT` (1 de cada N) | 1–100 (hoy 10) | Cuántos aprobados revisa además el supervisor |
| Garantía por defecto | Parámetro | `SMALLINT` (días) | 0–3650 (hoy 365) | Garantía de un producto sin política propia |

### Áreas

| Atributo | Clase | Tipo | Rango / dominio | Regla |
|---|---|---|---|---|
| Área | Catálogo (clave) | `VARCHAR(60)` | 3–60 caracteres, único | — |
| Estado | Catálogo | `ESTADO` | Habilitado / Deshabilitado | No se deshabilita si la usa un tipo de problema, una instancia o un especialista |
| La usa | Derivado | texto | — | Calculado |

### Especialistas

| Atributo | Clase | Tipo | Rango / dominio | Regla |
|---|---|---|---|---|
| Código | Catálogo (clave) | `CHAR(3)` | formato `E01` | único |
| Tipo de operario | Catálogo | `VARCHAR(12)` | Asistente < Técnico < Especialista | Decide qué eventos puede atender |
| Área | Catálogo | `VARCHAR(60)` | FK → Áreas habilitadas | Decide qué reclamos le llegan |
| Estado | Catálogo | `ESTADO` | — | Deshabilitado = no recibe reclamos |

## Catálogo de Reclamos y Eventos

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Tipo de problema | Catálogo (clave) | `VARCHAR(40)` | 3–40 caracteres, único | Único catálogo de tipos del sistema |
| Naturaleza | Catálogo | `VARCHAR(7)` | Reclamo / Queja | Cómo se registra y se muestra al cliente |
| Estado | Catálogo | `ESTADO` | — | Un tipo deshabilitado no se ofrece en el formulario |
| Evento / Lo usa | Derivado | texto | — | Sale de los protocolos |

## Catálogo de Producto

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Código | Catálogo (clave) | `CHAR(7)` | formato `PROD001`, único | Lo referencian compras y garantías |
| Bien | Catálogo | `VARCHAR(60)` | FK → Clasificación (nivel bien) | Decide qué problemas aplican |
| Marca, Modelo | Descriptivo | `VARCHAR(40)` | obligatorios | Identifican el producto |
| Clase, Familia, Segmento | Derivado | `VARCHAR(80)` | salen del bien | Clasificación UNSPSC |

## Catálogo de Servicios

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Servicio | Catálogo (clave) | `VARCHAR(60)` | 3–60 caracteres, único | Lo que el cliente puede reclamar |
| Tipo de servicio | Descriptivo | `VARCHAR(12)` | Logístico / Comercial / Técnico / Atención | Clasifica |
| Tipos de problema que admite | Regla | lista de `VARCHAR(40)` | cada uno FK → Tipos de problema | Filtra el formulario |
| Estado | Catálogo | `ESTADO` | — | Deshabilitado = no se ofrece |

## Catálogo de Problemas

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Tipo de problema | Catálogo | `VARCHAR(40)` | FK → Tipos de problema | — |
| Problema | Catálogo (clave con el tipo) | `VARCHAR(80)` | 3–80 caracteres | Lo que el cliente elige |
| Aplica a | Regla | lista de `VARCHAR(60)` | Todo producto / Todo servicio / un bien / un servicio | Filtra el formulario |

## Catálogo de Cliente

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Tipo de cliente | Catálogo | `VARCHAR(10)` | Nuevo / Ordinario / Vip / SuperVip | Prioridad y plazo |
| Plazo por prioridad | Derivado | texto | sale de Tiempos de atención | — |
| Consideraciones | Catálogo | `VARCHAR(40)` | — | Las usará el batch BAT-03; hoy no cambian el flujo |

## Catálogo de Protocolos

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Protocolo | Catálogo | `CHAR(7)` | PR-FUNC / PR-ENTR / PR-COBR / PR-ATEN | Uno por tipo de problema |
| Evento | Catálogo | `VARCHAR(40)` | FK → eventos del protocolo | — |
| Paso | Catálogo (clave con protocolo y evento) | `SMALLINT` | 1–20 | Orden de ejecución |
| Acción | Descriptivo | `VARCHAR(80)` | 3–80 caracteres | Lo que hace el operario |
| Tiempo máximo | Parámetro del paso | `SMALLINT` (minutos) | 1–480 | Duración de la cita |
| Tipo de operario | Regla | `VARCHAR(12)` | Asistente / Técnico / Especialista | Quién puede atender el evento |
| Criticidad | Regla | `VARCHAR(11)` | Crítico / No Crítico | Un paso crítico no se puede omitir |
| Duración de la cita | Derivado | `SMALLINT` (min) | suma de los tiempos | Reserva de tickets |
| Pide como mínimo | Derivado | `VARCHAR(12)` | el operario más alto de sus pasos | Filtro de la asignación |

## Catálogo de Reglas

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Condición | Catálogo (clave) | `VARCHAR(6)` | formato `CND1` | — |
| Variable | Regla | `VARCHAR(30)` | FK → Variables del motor | Qué dato del reclamo se compara |
| Operador | Regla | `CHAR(2)` | EQ / NE / GT / GE / LT / LE | Cómo se compara |
| Valor | Regla | `VARCHAR(30)` | número, true/false o nombre de otra variable | Contra qué se compara |
| Si es verdadero / Si es falso | Regla | `VARCHAR(40)` | "Ir a CNDx" o "Aplicar <solución>" | Camino de la cadena |
| Descripción | Descriptivo | `VARCHAR(200)` | — | Documenta |
| Tipo de reclamo → Condición inicial | Regla | `VARCHAR(40)` → `VARCHAR(6)` | FK → Tipos de problema, FK → Condiciones | Qué tipos pasan por el motor y desde dónde |

## Catálogo de Políticas

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Producto | Catálogo | `CHAR(7)` | FK → Productos | — |
| Garantía | Catálogo | `VARCHAR(9)` | Legal / Comercial | — |
| Plazo | Parámetro | `SMALLINT` (días) | 0–3650 | Vale el mayor habilitado; lo lee el motor |
| Estado | Catálogo | `ESTADO` | — | Deshabilitada = no cuenta |

## Parámetros de Atención

| Atributo | Clase | Tipo | Rango / dominio | Qué cambia |
|---|---|---|---|---|
| Canal | Catálogo (clave) | `VARCHAR(10)` | Presencial / Telefónico / Internet / Web | — |
| Horario del canal | Descriptivo | `VARCHAR(40)` | — | Informa al cliente |
| Requiere local | Regla | `CHAR(2)` | Sí / No | Obliga a elegir local |
| Estado del canal | Catálogo | `ESTADO` | — | Deshabilitado = no se ofrece |
| Local | Catálogo (clave) | `CHAR(5)` | formato `Loc01` | — |
| Dirección | Descriptivo | `VARCHAR(120)` | 5–120 caracteres | — |
| Estado del local | Catálogo | `ESTADO` | — | Deshabilitado = no se ofrece |
| Tipo de problema → Área responsable | Regla | `VARCHAR(40)` → `VARCHAR(60)` | FK → Tipos, FK → Áreas habilitadas | Área que recibe el reclamo |
| Tiempo de atención | Parámetro | `SMALLINT` + unidad (Horas / Días) | 1–90 | Límite de atención (en tiempo laborable) |
| Tipo de cliente × Prioridad (clave del SLA) | Regla | `VARCHAR(10)` × `VARCHAR(5)` | Alta / Media / Baja | — |
| Tipo de problema × Tipo de cliente → Prioridad | Regla | → `VARCHAR(5)` | Alta / Media / Baja | Con qué prioridad nace el reclamo |
| Instancia, Días de impugnación | Parámetro | `VARCHAR(7)`, `SMALLINT` | Primera 15 / Segunda 10 / Tercera 0 | Fecha límite de impugnación |
| Feriados | Catálogo | `DATE` (o día/mes si es fijo) | — | Días sin citas ni plazo |
| Estados | Diseño (solo consulta) | — | máquina de estados | No es parámetro: es el diseño del flujo |

## Formulario del reclamo (datos que ingresa el cliente)

| Atributo | Clase | Tipo | Regla |
|---|---|---|---|
| DNI/RUC | Dato | `VARCHAR(11)` | solo dígitos, 8 (DNI) u 11 (RUC), cliente registrado |
| Canal, Local | Dato | FK → Canales, Locales | Local obligatorio solo si el canal es Presencial |
| Reclama sobre | Dato | `VARCHAR(8)` | Producto / Servicio |
| Producto o servicio | Dato | FK → Productos o Servicios | Solo lo que el cliente compró (salvo Atención) |
| Tipo de problema, Problema | Dato | FK → Tipos, Problemas | Deben aplicar a lo reclamado |
| Detalle | Dato | `VARCHAR(500)` | 10–500 caracteres, obligatorio |
| Pedido del consumidor | Dato | `VARCHAR(40)` | Reparación / Cambio del producto / Devolución del dinero / Nueva prestación del servicio / Disculpa y corrección de la atención |
| Nro y fecha de comprobante | Derivado | `VARCHAR(13)`, `DATE` | Salen de la compra; formato `B001-00012345`, fecha no futura |
| Naturaleza | Derivado | `VARCHAR(7)` | Sale del tipo de problema |
