# Diseño interno — Tablas Maestras

Criterios tomados del libro del profesor (cap. 3.1.2, pp. 64–80):

- **Qué guarda una maestra:** la entidad y sus saldos. "Las tablas maestras mantienen los saldos. Se deben actualizar vía transaccional solo los saldos comprometidos con los eventos" (p. 75). Lo que son eventos va a Movimientos; lo de cierre, a Históricas.
- **IC:** se calcula con `(IC − 4) / (RL + 3) = #RL`, residuo `r = (IC − 4) − #RL·(RL + 3)`, y se elige el IC de menor residuo dentro de la terna de su modalidad (pp. 67–70):
  - Online (acceso unitario por clave): 512, 1024, 1536.
  - Batch (recorrido completo): 3072, 3584, 4096.
- **Capacidad = (N, AC):** N = cantidad estimada total de registros; AC = factor de expansión (p. 79).
- **Dominios:** `CodTipoCliente`, `CodTipoEmpleado`, `CodInstancia`, `CodPrioridad`, `CodEstado`, `CodCanal` y `TipoObjeto` no son FK: son dominios fijos que se controlan con `check` (ver `TABLAS_CATALOGOS.md`).
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
| Capacidad | (600000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodCliente | char | 8 | X | |
| Documento | char | 11 | | |
| Nombres | char | 25 | | |
| Apellidos | char | 25 | | |
| Correo | char | 30 | | |
| Telefono | char | 9 | | |
| Direccion | char | 30 | | |
| CodTipoCliente | int | 2 | | |
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
| Capacidad | (60, 19*12*5) |

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
| Longitud Registro | (354, 194) |
| IC | 1024 |
| Capacidad | (600000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodReclamo | char | 12 | X | |
| FecApertura | int | 12 | | |
| CodCliente | char | 8 | | X |
| TipoObjeto | int | 1 | | |
| CodObjeto | char | 7 | | X |
| CodProblema | int | 3 | | X |
| CodCanal | int | 1 | | |
| CodLocal | int | 3 | | X |
| Comprobante | char | 13 | | |
| CodInstancia | int | 1 | | |
| CodPrioridad | int | 1 | | |
| CodEstado | int | 2 | | |
| CodOperario | char | 3 | | X |
| CodProtocolo | int | 3 | | X |
| FecLimiteAtencion | int | 12 | | |
| TotalPasos | int | 3 | | |
| PasosCompletados | int | 3 | | |
| Detalle | varchar | 150 | | |
| Pedido | varchar | 100 | | |
| PtrUltMov | int | 8 | | |
| PtrUltPaso | int | 8 | | |
| | | **354** | | |

`CodReclamo` con formato RC-2026-0001. `TipoObjeto` indica si `CodObjeto` es un producto (FK al Catálogo de Producto) o un servicio (FK al Catálogo de Servicios). Las fechas van como `yyyyMMddHHmm`.

`Detalle` y `Pedido` son lo que el cliente cuenta y pide al registrar. Son atributos descriptivos fijos del reclamo, como `NOMBRE` o `DIRECC` en el `MAESTRO-CLIENTES` del libro, así que van en la maestra. Como son texto de largo variable, se declaran `varchar` y la longitud de registro se da como `(MAX, MODA)`, igual que en el ejemplo de longitud variable del libro (pp. 79, 90–91):

- MAX = 104 de parte fija + 150 + 100 = **354**.
- MODA = 104 + 60 + 30 = **194**, suponiendo que un detalle típico tiene unos 60 caracteres y un pedido unos 30.
- El IC se calcula con la moda, que es el registro típico.

`PtrUltMov` y `PtrUltPaso` son los punteros de **cadena simple** (libro, 3.2.1.2.1). Guardan la posición relativa (PRR) del último movimiento del reclamo en el Movimiento de Reclamos y en el Movimiento de Soluciones. Como esas tablas tienen organización relativa y no tienen índice, es la forma de llegar a ellas desde el reclamo. Ver `TABLAS_MOVIMIENTOS.md`.

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
| Reclamos | 194 (moda) | Online | r = 114 / **35** / 153 | — | **1024** (5 RL) |

Comparación: la Maestra Reclamos anterior (420 bytes de largo fijo, con datos de revisión, cierre y compra) cabía 1 registro por IC de 512 con 85 bytes perdidos; con la moda de 194 bytes caben 5 por IC de 1024 con 35 perdidos.

---

## Columnas que salieron de la Maestra Reclamos

| Columnas | Va a | Motivo |
|---|---|---|
| Naturaleza | Se elimina | Sale del tipo de problema (Catálogo de Reclamos: cada tipo tiene su naturaleza, Reclamo o Queja) |
| Revision, Nota, MotivoRechazo | Movimiento de Reclamos (movimientos Revisión y Conformidad) | Son transacciones de control de calidad |
| NumDevoluciones | Se elimina | Se cuenta en el Movimiento de Reclamos (revisiones con resultado Devuelto) |
| FecResolucion, SolucionAplicada | Movimiento de Reclamos (cambio de estado a Resuelto: `FecMov` y `Observacion`) | Es una transacción del caso |
| FecLimiteImpugnacion | Se elimina | Fecha del rechazo + días de impugnación de la instancia (parámetro del módulo) |
| FecComprobante, Monto | Tabla de Compras (`TABLA_COMPRAS.md`) | Se llega por `Comprobante`. La tabla la llena el batch Carga de Compras desde el sistema de ventas |
| CodTipoProblema, Flg_FueraGarantia | Se eliminan | El tipo sale del problema (Catálogo de Problemas). La garantía sale de `FecComprobante` (Tabla de Compras) y del plazo del Catálogo de Productos; el resultado que vio el motor al registrar queda en el movimiento Registro (ver `TABLAS_MOVIMIENTOS.md`) |

---

## Pendiente de confirmar con el profesor

- Significado exacto del AC `19*12*5` (se mantuvo la notación del entregable 2; 19 coincide con las pistas de un cilindro de 10 discos, 2A − 1).
- La N de Operarios (60) es una estimación.
