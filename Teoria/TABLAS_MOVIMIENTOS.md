# Diseño interno — Tablas de Movimientos

Criterios tomados del libro del profesor:

- **Qué es un movimiento:** la *información de movimiento (eventos)* (p. 74). El modelo del módulo transaccional es **una maestra y su movimiento**: CUENTA ↔ MOVIMIENTO. La transacción "graba registro de movimiento y actualiza maestro" (pp. 115–116).
- **Procesar pocas tablas:** entre los requerimientos del diseño transaccional, el libro pone "Transacción altamente utilizada (procesar pocas tablas)" (pp. 116, 123). Cada tabla de movimientos de más es otra tabla que cada transacción tiene que grabar.
- **Organización relativa** (3.1.2.8, pp. 107–110): registros de longitud fija, sin índice, acceso directo por la Posición Relativa del Registro (PRR). El nuevo PRR se obtiene con un correlativo. Es la preferida "para módulos transaccionales en tiempo real".
  - Parámetros: `ORGANIZACIÓN = RELATIVA`, `LONG.REG = (L0, L0)`, `IC = m512 (mínimo para transaccional)`, `CAPACIDAD = (N, AC)`.
  - Por eso el IC se elige en la terna online (512, 1024, 1536) con `(IC − 4) / (RL + 3)` y el menor residuo.
- **Cadena simple** (3.2.1.2.1, pp. 115–120): la maestra guarda el PRR de su **último** movimiento y cada movimiento guarda el PRR del **anterior** (0 si es el primero). Como la tabla relativa no tiene índice, es así como se llega a los movimientos de un reclamo.
- **Sin redundancia** (Listas invertidas, pp. 40–41): el movimiento lleva la clave de su maestra como FK. Lo que ya se sabe por esa clave no se repite.
- **Dominios:** `CodTipoMov`, `CodEstado`, `CodInstancia`, `CodEvento` y `CodResultado` no son FK: son dominios fijos que se controlan con `check` (ver `TABLAS_CATALOGOS.md`).

## Por qué son dos tablas

En el sistema, las transacciones sobre un reclamo son de dos clases:

1. **Sobre el caso:** registro, programación de un evento (Inspección, Solución, Seguimiento) con su cita y responsable, cambio de estado, impugnación, revisión de calidad, conformidad del cliente y cierre. Todas afectan a la Maestra Reclamos (estado, operario, instancia), así que son un solo tipo de movimiento con un código de transacción (`CodTipoMov`), igual que el `Trx` del ejemplo `Mov-diario` del libro.
2. **Sobre la ejecución:** cada paso del protocolo que hace el operario, con hora estimada y real. Son muchos por reclamo (varios por evento), tienen otra estructura y actualizan otro saldo (`PasosCompletados`).

Es la misma división del entregable 2: *Movimiento de Reclamos* y *Movimiento de Soluciones*.

Lo que **no** es una tabla de movimientos:

- **El estado de un evento** (programado, en curso, completado) no se guarda: se deriva. Está en curso si ya tiene pasos en el Movimiento de Soluciones y completado si tiene todos los de su protocolo.
- **El Diario de Operaciones** (pantalla `01b`) registra cambios de configuración, no transacciones de un reclamo. Es una bitácora del módulo de Seguridad.

---

## Mapa de cadenas

```
Maestra Reclamos
 ├─ PtrUltMov ──► Movimiento de Reclamos  ──PtrMovAnterior──► … ──► 0
 └─ PtrUltPaso ─► Movimiento de Soluciones ─PtrMovAnterior──► … ──► 0
                      │ NroMovReclamo (FK)
                      └──► el movimiento "Programación" del evento al que pertenece el paso

Parque de Ticket ── NroMovReclamo ──► el movimiento "Programación" del evento que tomó el ticket
```

**Transacción típica (cadena simple), por ejemplo, un cambio de estado:**

1. Leer el reclamo en la Maestra Reclamos por su clave.
2. Obtener el nuevo PRR (correlativo de la tabla).
3. Grabar el movimiento con `PtrMovAnterior = PtrUltMov` del reclamo.
4. Actualizar la maestra: `PtrUltMov = nuevo PRR` y sus saldos (`CodEstado`, `CodOperario`…).

**Cómo se obtiene la cita de un reclamo.** La cita es fija: se guarda una sola vez, en `FecInicioEstimado` y `FecFinEstimado` del movimiento *Programación de evento*, y no se recalcula. Para leerla no se recorre toda la tabla:

1. Leer el reclamo en la Maestra Reclamos (1 acceso por clave).
2. Ir al PRR de `PtrUltMov` y retroceder por `PtrMovAnterior` hasta el primer movimiento con `CodTipoMov = Programación`.

`PtrUltMov` apunta al **último** movimiento, que puede ser un cambio de estado posterior a la programación. Por eso se retrocede, pero son pocos pasos: un reclamo tiene unos 6 movimientos en total. El cliente no hace ni siquiera esto: su consulta (pantalla 17) lee la cita ya armada en la Consulta de Reclamos (ver `LISTAS_INVERTIDAS.md`).

---

## 1. Movimiento de Reclamos

| Nombre | Movimiento de Reclamos |
|---|---|
| Organización | relativa |
| Longitud Registro | (160, 160) |
| IC | 512 |
| Capacidad | (3600000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| NroMov (PRR) | int | 8 | X | |
| CodReclamo | char | 12 | | X |
| CodTipoMov | int | 2 | | |
| CodEstado | int | 2 | | |
| CodInstancia | int | 1 | | |
| CodOperario | char | 3 | | X |
| CodEvento | int | 2 | | |
| CodProtocolo | int | 3 | | X |
| FecMov | int | 12 | | |
| FecInicioEstimado | int | 12 | | |
| FecFinEstimado | int | 12 | | |
| CodResultado | int | 1 | | |
| Nota | int | 2 | | |
| Observacion | char | 80 | | |
| PtrMovAnterior | int | 8 | | |
| | | **160** | | |

- `CodTipoMov`: Registro, Programación de evento, Cambio de estado, Impugnación, Revisión, Conformidad, Cierre.
- `CodEstado`: estado en que queda el reclamo después del movimiento.
- `CodOperario`: responsable asignado; en una revisión, el supervisor (S01…).
- `CodEvento`, `CodProtocolo`, `FecInicioEstimado`, `FecFinEstimado`: se llenan en la **Programación de evento**. Son la cita fija que se le da al cliente.
- `CodResultado`: en una Revisión (Aprobado, Observado, Muestra al azar, Rechazo propuesto, Devuelto) o en la Conformidad (Conforme, No conforme).
- `Nota`: la que pone el cliente en la Conformidad.
- `Observacion`: en el **Registro**, la solución que sugirió el motor de reglas y si el reclamo estaba dentro o fuera de garantía (por ejemplo, `SUG=Reparación Equipo; GAR=Dentro`). En los demás: solución aplicada (en el cambio de estado a Resuelto), motivo de la impugnación, observaciones de la revisión, motivo de rechazo o de devolución.
- **Por qué se guarda la sugerida.** La Revisión de Calidad compara la solución aplicada con la que sugirió el motor al registrar. No basta con volver a correr el motor en ese momento: la gerencia puede haber cambiado el Catálogo de Reglas o el plazo de garantía entre el registro y la revisión, y el resultado ya no sería el mismo. Guardarla en el movimiento Registro no cambia la tabla, porque `Observacion` ya existe y en el Registro estaba libre.
- **Si el motor necesita la inspección.** Para algunos tipos, el motor necesita datos que solo se conocen tras inspeccionar (`Reparable`, `Stock`). Entonces el Registro queda con `SUG=Pendiente inspección`, y al completar la Inspección la sugerida se guarda en la `Observacion` de la *Programación* del evento siguiente, que es el movimiento que se graba en ese momento. La Revisión de Calidad recorre la cadena y toma la última `SUG=` que encuentra.
- El número de devoluciones se obtiene contando las revisiones con resultado Devuelto: no se guarda.
- Capacidad: unos 6 movimientos por reclamo × 600000 reclamos.

## 2. Movimiento de Soluciones

Cada paso del protocolo ejecutado, con la hora estimada y la real. Con él se miden el avance (`PasosCompletados` en la maestra) y el desvío entre lo estimado y lo real.

| Nombre | Movimiento de Soluciones |
|---|---|
| Organización | relativa |
| Longitud Registro | (82, 82) |
| IC | 1024 |
| Capacidad | (4500000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| NroMov (PRR) | int | 8 | X | |
| NroMovReclamo | int | 8 | | X |
| CodTicket | int | 8 | | X |
| NumPaso | int | 2 | | |
| FecPasoEstimado | int | 12 | | |
| FecFinPasoEstimado | int | 12 | | |
| FecPaso | int | 12 | | |
| FecFinPaso | int | 12 | | |
| PtrMovAnterior | int | 8 | | |
| | | **82** | | |

- `NroMovReclamo`: la Programación del evento al que pertenece el paso. De ahí salen el reclamo, el evento y el protocolo, así que no se repiten.
- El operario sale del ticket. La acción sale del Catálogo de Protocolos (protocolo + evento + `NumPaso`).
- Capacidad: unos 3 pasos por evento × 2.5 eventos por reclamo × 600000 reclamos.

---

## Cálculo del IC

Fórmula: `#RL = (IC − 4) / (RL + 3)` (parte entera), `r = (IC − 4) − #RL·(RL + 3)`. Se elige el menor `r` de la terna online.

| Tabla | RL | IC = 512 / 1024 / 1536 | IC elegido |
|---|---|---|---|
| Mov. Reclamos | 160 | r = **19** / 42 / 65 | **512** (3 RL) |
| Mov. Soluciones | 82 | r = 83 / **0** / 2 | **1024** (12 RL) |

`Observacion` es de 80 y no de 100 por este cálculo: con 100 el registro mide 180 bytes y el menor residuo de la terna sube a 68 bytes por IC.

---

## Cambios que esto produjo en las maestras

- **Maestra Reclamos:** lleva dos punteros de cadena, `PtrUltMov` y `PtrUltPaso`. `Detalle` y `Pedido` vuelven a la maestra (ver `TABLAS_MAESTRAS.md`).
- **Parque de Ticket:** apunta a la Programación del evento con `NroMovReclamo`. No basta con reclamo + evento, porque un mismo evento (por ejemplo, Inspección) puede repetirse en otra instancia.

## Diferencias con el entregable 2

- **Movimientos sin FK redundantes.** El entregable 2 repetía en cada movimiento `codCliente`, `codProducto` y `codOperario`, que ya se conocen por el reclamo o por el ticket.
- **Cadena simple completa.** La maestra guarda el puntero al último movimiento y cada movimiento el puntero al anterior, como en el ejemplo `Mae-Ctas` / `Mov-diario` del libro.
- **IC calculado.** El entregable 2 usaba un IC de 2560 sin cálculo. Aquí se elige por menor residuo dentro de la terna online.

## Pendiente de confirmar con el profesor

- Las N de capacidad son estimaciones: unos 6 movimientos y 7.5 pasos por reclamo, sobre 600000 reclamos.
- Se mantuvo la notación del AC `19*12*5`.
