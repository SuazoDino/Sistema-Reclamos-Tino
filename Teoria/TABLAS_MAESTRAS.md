# Diseño interno — Tablas Maestras

Criterios tomados del libro del profesor (cap. 3.1.2, pp. 64–80):

- **Qué guarda una maestra:** la entidad y sus saldos. "Las tablas maestras mantienen los saldos. Se deben actualizar vía transaccional solo los saldos comprometidos con los eventos" (p. 75). Lo que son eventos va a Movimientos; lo de cierre, a Históricas.
- **IC:** se calcula con `(IC − 4) / (RL + 3) = #RL`, residuo `r = (IC − 4) − #RL·(RL + 3)`, y se elige el IC de menor residuo dentro de la terna de su modalidad (pp. 67–70):
  - Online (acceso unitario por clave): 512, 1024, 1536.
  - Batch (recorrido completo): 3072, 3584, 4096.
- **Capacidad = (N, AC):** N = cantidad estimada total de registros; AC = factor de expansión, el bloque en que crece la tabla, expresado en registros (pp. 79, 92). Ver «Cálculo del AC».
- **Clave = (LONG, UBICAC):** longitud de la clave y su desplazamiento dentro del registro (p. 91). La clave alterna es el índice por otro atributo (3.3.2).
- **Dominios:** `CodTipoEmpleado`, `CodInstancia`, `CodPrioridad`, `CodEstado` y `CodCanal` no son FK: son dominios fijos que se controlan con `check` (ver `TABLAS_CATALOGOS.md`).
- **Maestra o catálogo:** el libro distingue la *información maestra (cuentas)*, que mantiene saldos que los eventos actualizan, de la *información tabulada / de parámetros (catálogo)*, que son "variables que se mantienen fijas para definir escenarios del negocio" y se mantienen en el módulo gerencial de Mantenimiento de Parámetros (pp. 10, 74–75).

## Qué es maestra en este proyecto

Prueba aplicada a cada entidad: ¿tiene saldos que los eventos del reclamo actualizan, o es un dato fijo que define el negocio y se mantiene en Mantenimiento de Parámetros?

| Entidad | Saldos que actualizan los eventos | Dónde se mantiene en el prototipo | Resultado |
|---|---|---|---|
| Cliente | NumReclamos, NumReclamosAbiertos | Lo registra el Data Entry del cliente | **Maestra** |
| Operarios | CargaActual, NumReclamosAtendidos, NumReclamosResueltos, habilitado | Parámetro General (alta), y la asignación lo actualiza | **Maestra** |
| Reclamos | Estado, operario, PasosCompletados, instancia | Lo abre el Data Entry; lo mueven los movimientos | **Maestra** |
| Ticket | Flg_Disponible, NroMovReclamo (al tomarlo) | Lo genera el batch del parque | Sección propia: **Parque de Ticket**, como en el entregable 2 |
| Producto | Ninguno: el stock es del almacén | Mantenimiento de Parámetros → Catálogo de Producto (`04`) | **Catálogo** |
| Servicios | Ninguno | Mantenimiento de Parámetros → Catálogo de Servicios (`05`) | **Catálogo** |

Quedan **3 maestras: Cliente, Operarios y Reclamos**, las mismas del entregable 2. El ticket va en Parque de Ticket y lo demás en Tablas de Catálogos.

---

## 1. Maestra Cliente

| Nombre | Maestra Cliente |
|---|---|
| Organización | indexada |
| Longitud Registro | (147, 147) |
| IC | 1536 |
| Clave | (8, 0) |
| Clave alterna | Documento (11, 8) |
| Capacidad | (600000, 19*8*10) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodCliente | char | 8 | X | |
| Documento | char | 11 | | |
| Nombres | char | 25 | | |
| Apellidos | char | 25 | | |
| Correo | char | 30 | | |
| Telefono | char | 9 | | |
| Direccion | char | 30 | | |
| CodTipoCliente | int | 2 | | X |
| Flg_Activo | int | 1 | | |
| NumReclamos | int | 3 | | |
| NumReclamosAbiertos | int | 3 | | |
| | | **147** | | |

## 2. Maestra Operarios

| Nombre | Maestra Operarios |
|---|---|
| Organización | indexada |
| Longitud Registro | (107, 107) |
| IC | 1024 |
| Clave | (3, 0) |
| Capacidad | (60, 19*12*9) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodOperario | char | 3 | X | |
| Nombre | char | 25 | | |
| Apellido | char | 25 | | |
| Correo | char | 30 | | |
| Telefono | char | 9 | | |
| CodTipoEmpleado | int | 1 | | |
| CodArea | int | 2 | | X |
| Flg_Habilitado | int | 1 | | |
| NumReclamosAtendidos | int | 4 | | |
| NumReclamosResueltos | int | 4 | | |
| CargaActual | int | 3 | | |
| | | **107** | | |

`CodOperario`: E01, E02… para operarios; S01, S02… para supervisores de calidad. `CodTipoEmpleado`: Asistente, Técnico, Especialista o Supervisor.

## 3. Maestra Reclamos

| Nombre | Maestra Reclamos |
|---|---|
| Organización | indexada |
| Longitud Registro | (344, 184) |
| IC | 1536 |
| Clave | (12, 0) |
| Capacidad | (600000, 19*8*8) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodReclamo | char | 12 | X | |
| FecApertura | int | 12 | | |
| CodCliente | char | 8 | | X |
| CodCompra | int | 7 | | X |
| CodProblema | int | 3 | | X |
| CodCanal | int | 1 | | |
| CodLocal | int | 3 | | X |
| CodInstancia | int | 1 | | |
| CodPrioridad | int | 1 | | |
| CodEstado | int | 2 | | |
| CodOperario | char | 3 | | X |
| CodProtocolo | char | 7 | | X |
| FecLimiteAtencion | int | 12 | | |
| TotalPasos | int | 3 | | |
| PasosCompletados | int | 3 | | |
| Detalle | varchar | 150 | | |
| Pedido | varchar | 100 | | |
| PtrUltMov | int | 8 | | |
| PtrUltPaso | int | 8 | | |
| | | **344** | | |

`CodReclamo` con formato RC-2026-0001. `CodCompra` es FK a la Tabla de Compras: apunta a la línea de compra (el producto o servicio comprado) que se reclama. De ahí salen el comprobante, el producto o servicio, la fecha de compra y el monto, así que no se repiten en la maestra. Las fechas van como `yyyyMMddHHmm`.

`Detalle` y `Pedido` son lo que el cliente cuenta y pide al registrar. Son atributos descriptivos fijos del reclamo, como `NOMBRE` o `DIRECC` en el `MAESTRO-CLIENTES` del libro, así que van en la maestra. Como son texto de largo variable, se declaran `varchar` y la longitud de registro se da como `(MAX, MODA)`, igual que en el ejemplo de longitud variable del libro (pp. 79, 90–91):

- MAX = 94 de parte fija + 150 + 100 = **344**.
- MODA = 94 + 60 + 30 = **184**, suponiendo que un detalle típico tiene unos 60 caracteres y un pedido unos 30.
- El IC se calcula con la moda, que es el registro típico.

`PtrUltMov` y `PtrUltPaso` son los punteros de **cadena simple** (libro, 3.2.1.2.1). Guardan la posición relativa (PRR) del último movimiento del reclamo en el Movimiento de Reclamos y en el Movimiento de Soluciones. Como esas tablas tienen organización relativa y no tienen índice, es la forma de llegar a ellas desde el reclamo. Ver `TABLAS_MOVIMIENTOS.md`.

`Documento` es **clave alterna** (índice) de la Maestra Cliente: el cliente se identifica con su DNI/RUC en el formulario (pantalla 16), así que el registro del reclamo busca al cliente por `Documento`, no por `CodCliente`. `CLAVE = (11, 8)`: 11 bytes a partir del byte 8, justo después de `CodCliente`.

## Ticket

Va en su propia sección, **Parque de Ticket**, como en el entregable 2. Ver `PARQUE_DE_TICKET.md`.

---

## Producto y Servicios

No son maestras (ver la prueba al inicio). Están definidos en `TABLAS_CATALOGOS.md`.

---

## Cálculo del IC

Fórmula: `#RL = (IC − 4) / (RL + 3)` (parte entera), `r = (IC − 4) − #RL·(RL + 3)`. Se elige el menor `r` de la terna.

| Tabla | RL | Modalidad | IC = 512 / 1024 / 1536 | IC = 3072 / 3584 / 4096 | IC elegido |
|---|---|---|---|---|---|
| Cliente | 147 | Online | r = 58 / 120 / **32** | — | **1536** (10 RL) |
| Operarios | 107 | Online | r = 68 / **30** / 102 | — | **1024** (9 RL) |
| Reclamos | 184 (moda) | Online | r = 134 / 85 / **36** | — | **1536** (8 RL) |

Comparación: la Maestra Reclamos anterior (420 bytes de largo fijo, con datos de revisión, cierre y compra) cabía 1 registro por IC de 512 con 85 bytes perdidos; con la moda de 184 bytes caben 8 por IC de 1536 con 36 perdidos.

---

## Cálculo del AC (todas las tablas)

El libro (pp. 92–93) define el AC como el bloque físico de crecimiento ("factor de expansión"), recomienda **un cilindro** y lo expresa **en número de registros**:

```
1 cilindro = 19 pistas;   1 pista = 3 × 4096 = 12288 bytes
AC = 19 × (IC por pista) × (RL por IC)
Ejemplo del libro (IC 1024, 5 RL por IC):  1 cil = 19 × 12 × 5 RL
```

- `IC por pista = 12288 / IC`: 24 con 512, 12 con 1024, 8 con 1536, 4 con 3072, 3 con 3584 o 4096.
- `RL por IC` es el `#RL` que ya sale del cálculo del IC.
- Todas las tablas usan un cilindro, como el ejemplo del libro. Por eso el AC no es siempre `19*12*5`: ese valor es solo el de una tabla con IC 1024 y 5 registros por IC (el ejemplo MAESTRO-CLIENTES del libro).

| Tabla | IC | IC por pista | RL por IC | AC |
|---|---|---|---|---|
| Maestra Cliente | 1536 | 8 | 10 | 19*8*10 |
| Maestra Operarios | 1024 | 12 | 9 | 19*12*9 |
| Maestra Reclamos | 1536 | 8 | 8 | 19*8*8 |
| Movimiento de Reclamos | 512 | 24 | 3 | 19*24*3 |
| Movimiento de Soluciones | 1024 | 12 | 12 | 19*12*12 |
| Catálogo de Reclamo | 512 | 24 | 11 | 19*24*11 |
| Catálogo de Problemas | 1536 | 8 | 28 | 19*8*28 |
| Catálogo de Protocolos | 512 | 24 | 12 | 19*24*12 |
| Catálogo de Pasos | 1024 | 12 | 15 | 19*12*15 |
| Catálogo de Productos | 1536 | 8 | 28 | 19*8*28 |
| Catálogo de Servicios | 512 | 24 | 13 | 19*24*13 |
| Catálogo de Reglas | 512 | 24 | 5 | 19*24*5 |
| Catálogo de Áreas | 512 | 24 | 11 | 19*24*11 |
| Catálogo de Locales | 1024 | 12 | 15 | 19*12*15 |
| Catálogo de Clasificación | 512 | 24 | 7 | 19*24*7 |
| Catálogo de Cliente | 512 | 24 | 15 | 19*24*15 |
| Parque de Ticket | 1024 | 12 | 29 | 19*12*29 |
| Tabla de Compras | 512 | 24 | 8 | 19*24*8 |
| Tabla de Perfiles | 512 | 24 | 9 | 19*24*9 |
| Diario de Operaciones | 3072 | 4 | 12 | 19*4*12 |
| Consulta Reclamos Cliente | 1536 | 8 | 7 | 19*8*7 |
| H_Reclamos | 4096 | 3 | 22 | 19*3*22 |
| H_MovimientoReclamos | 3072 | 4 | 19 | 19*4*19 |
| H_MovimientoSoluciones | 4096 | 3 | 53 | 19*3*53 |
| Checkpoint | 1536 | 8 | 20 | 19*8*20 |
| Archivos_Checkpoint | 512 | 24 | 18 | 19*24*18 |

Con IC 3584 caben 3 IC por pista (3 × 3584 = 10752) y sobran 1536 bytes.

---

## Columnas que salieron de la Maestra Reclamos

| Columnas | Va a | Motivo |
|---|---|---|
| Naturaleza | Se elimina | Sale del tipo de problema (Catálogo de Reclamos: cada tipo tiene su naturaleza, Reclamo o Queja) |
| Revision, Nota, MotivoRechazo | Movimiento de Reclamos (movimientos Revisión y Conformidad) | Son transacciones de control de calidad |
| NumDevoluciones | Se elimina | Se cuenta en el Movimiento de Reclamos (revisiones con resultado Devuelto) |
| FecResolucion, SolucionAplicada | Movimiento de Reclamos (cambio de estado a Resuelto: `FecMov` y `Observacion`) | Es una transacción del caso |
| FecLimiteImpugnacion | Se elimina | Fecha del rechazo + días de impugnación de la instancia (parámetro del módulo) |
| Comprobante, TipoObjeto, CodObjeto, FecComprobante, Monto | Tabla de Compras (`TABLA_COMPRAS.md`) | Se llega por `CodCompra`. La tabla la llena el batch Carga de Compras desde el sistema de ventas |
| CodTipoProblema, Flg_FueraGarantia | Se eliminan | El tipo sale del problema (Catálogo de Problemas). La garantía sale de `FecComprobante` (Tabla de Compras) y del plazo del Catálogo de Productos; el resultado que vio el motor al registrar queda en el movimiento Registro (ver `TABLAS_MOVIMIENTOS.md`) |

---

## Pendiente de confirmar con el profesor

- La N de Operarios (60) es una estimación.
