# Diseño de procesos, programas y rutinas — Online

## Criterios del libro

- **Módulo transaccional** (3.2.1, p. 112): está en el módulo Online, sub-módulo Operativo. Debe asegurar **agilidad** (tiempo mínimo por operación) e **integridad** (el saldo de la maestra debe poder verificarse con sus movimientos).
- **Modelo típico** (p. 115): "graba registro de movimiento, actualiza maestro", en tiempo real, con acceso aleatorio y seguro ante cualquier falla.
- **Requerimientos** (pp. 116, 123): transacción altamente utilizada → **procesar pocas tablas**; procesa saldos (proceso delicado); consultas de últimos movimientos.
- **Cadena simple** (3.2.1.2.1, pp. 115–122): la maestra es la **raíz** y guarda el puntero al último movimiento; cada movimiento guarda el puntero al anterior.
- **Notación** (p. 120): cada rutina se escribe como **EVENTO → OPERACIÓN → MÉTODO**:

```
EVENTO: GRABA MOVIMIENTO
  OPERACIÓN.- VALIDA(CTA, TMOV, MTO)
     MÉTODO.- LEE-MAE(CTA)
     MÉTODO.- CHK-SALDO(SALDO, TMOV, MTO)
  OPERACIÓN.- CONSIGUE(PRR)
     MÉTODO.- ADD-PRR(+1, PRR)
  OPERACIÓN.- GRABA-MOV(CTA, TMOV, MTO, MOV-ANT, PRR)     (1)
  OPERACIÓN.- ACT-MAE(CTA, TMOV, MTO, PRR)                (2)
```

- **El orden importa:** "la secuencia de pasos es importante de manera que si se realizan en sentido inverso se destruirá la cadena" (p. 121). Primero (1) se graba el movimiento apuntando al anterior y después (2) se actualiza la raíz.

## Las dos cadenas del sistema

```
Maestra Reclamos (raíz, indexada por CodReclamo)
 ├─ PtrUltMov ──► Movimiento de Reclamos   ─PtrMovAnterior─► … ─► 0
 └─ PtrUltPaso ─► Movimiento de Soluciones ─PtrMovAnterior─► … ─► 0
```

Es el mismo esquema `Mae-Ctas` / `Mov-diario` del libro: la Maestra Reclamos hace de `Mae-Ctas` (clave `CodReclamo`, puntero `Ult-mov`) y cada tabla de movimientos hace de `Mov-diario` (relativa, puntero `Mov-ant`).

**Ejemplo de la cadena del reclamo RC-2026-0004** (los PRR son los que tocaron al grabar; no son consecutivos porque entre medio se grabaron movimientos de otros reclamos):

| PRR | CodReclamo | CodTipoMov | CodEstado | Observacion / cita | PtrMovAnterior |
|---|---|---|---|---|---|
| 001 | RC-2026-0004 | Registro | Registrado | `SUG=No aplica; GAR=Dentro` | 0 |
| 100 | RC-2026-0004 | Programación | Asignado | Inspección 29/10 09:00–09:35, E02 | 001 |
| 500 | RC-2026-0004 | Cambio de estado | En revisión | Rechazo propuesto | 100 |
| 999 | RC-2026-0004 | Revisión | Rechazado | Rechazo aprobado | 500 |

Maestra Reclamos: `CodReclamo = RC-2026-0004`, `CodEstado = Rechazado`, `PtrUltMov = 999`.

Figuras de las dos cadenas: `procesos_png/01_Cadena_Movimiento_de_Reclamos.png` y `procesos_png/02_Cadena_Movimiento_de_Soluciones.png`.

---

## Rutina común: GRABA MOVIMIENTO DE RECLAMO

La usan todas las transacciones que cambian el caso. Es la rutina del libro aplicada al reclamo.

```
EVENTO: GRABA MOV-RECLAMO(RC, TMOV, DATOS)
  OPERACIÓN.- VALIDA(RC, TMOV)
     MÉTODO.- LEE-MAE-RECLAMO(RC)                       → reg. con CodEstado y PtrUltMov
     MÉTODO.- CHK-ESTADO(CodEstado, TMOV)               → la transición debe estar permitida
  OPERACIÓN.- CONSIGUE(PRR)
     MÉTODO.- ADD-PRR(+1, PRR)                          → correlativo del Movimiento de Reclamos
  OPERACIÓN.- GRABA-MOV(RC, TMOV, DATOS, PtrMovAnterior = PtrUltMov, PRR)   (1)
  OPERACIÓN.- ACT-MAE(RC, CodEstado, CodOperario…, PtrUltMov = PRR)        (2)
  OPERACIÓN.- ACT-CONSULTA(Documento, RC)               → elemento del vector en Consulta Reclamos Cliente
```

- `CHK-ESTADO` es la integridad del flujo: por ejemplo, no se puede revisar un reclamo que no está Resuelto ni impugnar uno que no está Rechazado.
- `ACT-CONSULTA` mantiene al día la tabla suplementaria de la consulta crítica (ver `LISTAS_INVERTIDAS.md`).

## Rutina común: GRABA PASO

La misma rutina sobre la segunda cadena.

```
EVENTO: GRABA PASO(RC, NumPaso, FecPaso, FecFinPaso)
  OPERACIÓN.- VALIDA(RC, NumPaso)
     MÉTODO.- LEE-MAE-RECLAMO(RC)                       → reg. con PtrUltPaso, PasosCompletados
     MÉTODO.- CHK-PASO(NumPaso)                         → el paso es del evento en curso y no se repite
  OPERACIÓN.- CONSIGUE(PRR)
     MÉTODO.- ADD-PRR(+1, PRR)                          → correlativo del Movimiento de Soluciones
  OPERACIÓN.- GRABA-MOV-SOL(NroMovReclamo, CodTicket, NumPaso, estimado, real,
                            PtrMovAnterior = PtrUltPaso, PRR)                (1)
  OPERACIÓN.- ACT-MAE(RC, PasosCompletados + 1, PtrUltPaso = PRR)           (2)
  OPERACIÓN.- ACT-CONSULTA(Documento, RC)               → PasosCompletados del vector
```

---

## 1. Data Entry Cliente — Registrar Reclamo (pantalla 16)

Todo lo decide el sistema: el cliente solo llena el formulario.

```
EVENTO: REGISTRA RECLAMO(Documento, Canal, Local, CodCompra, Problema, Detalle, Pedido)
  OPERACIÓN.- VALIDA-COMPRA(Documento, CodCompra)
     MÉTODO.- LEE-COMPRAS(Documento)                    → Tabla de Compras, por índice alterno: lo que compró el cliente
     MÉTODO.- CHK-GARANTIA(FecComprobante, Plazo)       → plazo del Catálogo de Productos (memoria)
  OPERACIÓN.- VALIDA-DUPLICADO(Documento, CodCompra, Problema)
     MÉTODO.- LEE-CONSULTA(Documento)                   → un reclamo abierto igual no se repite
  OPERACIÓN.- LEE-CLIENTE(Documento)                    → por la clave alterna Documento; si no existe, alta en la Maestra Cliente
  OPERACIÓN.- EVALUA-REGLAS(Problema, Compra, TipoCliente)
     MÉTODO.- MOTOR(CondicionInicial)                   → solución sugerida (Catálogo de Reglas, memoria)
  OPERACIÓN.- CONSIGUE(CodReclamo)                      → RC-aaaa-nnnn
  OPERACIÓN.- GRABA-MAE-RECLAMO(RC, …, CodEstado = Registrado, PtrUltMov = 0, PtrUltPaso = 0)
  OPERACIÓN.- GRABA MOV-RECLAMO(RC, Registro, Observacion = "SUG=…; GAR=…")
  OPERACIÓN.- PROGRAMA EVENTO(RC, primer evento del protocolo)    → rutina 2
  OPERACIÓN.- ACT-MAE-CLIENTE(NumReclamos + 1, NumReclamosAbiertos + 1)
  OPERACIÓN.- ACT-CONSULTA(Documento, + elemento RC)
  SALIDA.- "Su reclamo será atendido el [día] a las [hora] por [área]"
```

- La prioridad (Catálogo de Reclamo × tipo de cliente) y el límite de atención (horas del Catálogo de Cliente según la prioridad) se calculan en memoria y se graban en la maestra.
- Si no hay turno disponible, la transacción se deshace entera y no queda un reclamo sin cita.

## 2. Programar Evento (rutina interna)

La usan el registro, el fin de un evento y la impugnación. Así la fecha y el responsable no los elige nadie.

```
EVENTO: PROGRAMA EVENTO(RC, Evento, Protocolo)
  OPERACIÓN.- ELIGE-OPERARIO(Area, TipoEmpleado)
     MÉTODO.- LEE-OPERARIOS(Area)                       → habilitados, menor CargaActual
  OPERACIÓN.- BUSCA-TICKETS(CodOperario, Desde, Duracion)
     MÉTODO.- LEE-PARQUE(CodOperario + FecInicio)       → primeros tickets libres, por índice alterno
  OPERACIÓN.- CONSIGUE(PRR)                             → del Movimiento de Reclamos
  OPERACIÓN.- TOMA-TICKETS(Flg_Disponible = 0, NroMovReclamo = PRR)
  OPERACIÓN.- GRABA MOV-RECLAMO(RC, Programación, CodEvento, CodProtocolo,
                                FecInicioEstimado, FecFinEstimado)   → la cita fija
  OPERACIÓN.- ACT-MAE-OPERARIO(CargaActual + 1)
```

- La duración de la cita es la suma de los tiempos de los pasos del evento (Catálogo de Pasos, memoria).
- El PRR se consigue antes de tomar los tickets porque el ticket guarda `NroMovReclamo`: así el ticket apunta al movimiento que lo tomó.

## 3. Data Entry Área — Atender Reclamo (pantalla 14)

```
EVENTO: ATIENDE EVENTO(CodOperario, RC, Pasos ejecutados, [Reparable], [Solución])
  OPERACIÓN.- VALIDA-OPERARIO(CodOperario, RC)          → es el responsable del evento en curso
  OPERACIÓN.- POR CADA PASO: GRABA PASO(RC, NumPaso, …)
  OPERACIÓN.- CHK-CRITICOS(Protocolo, Evento)           → no puede faltar ningún paso crítico
  SI hay evento siguiente en el protocolo:
     OPERACIÓN.- [si estaba pendiente] EVALUA-REGLAS(…, Reparable, Stock)  → SUG en la Observacion
     OPERACIÓN.- PROGRAMA EVENTO(RC, siguiente evento)
  SI era el último:
     OPERACIÓN.- GRABA MOV-RECLAMO(RC, Cambio de estado → Resuelto, Observacion = solución aplicada)
     OPERACIÓN.- REVISION-AUTOMATICA(RC)                → rutina 4
  OPERACIÓN.- ACT-MAE-OPERARIO(CargaActual − 1, NumReclamosAtendidos + 1)
```

## 4. Revisión de Calidad (pantalla 15b)

Primero el sistema revisa solo; el supervisor ve únicamente lo que el sistema le manda.

```
EVENTO: REVISION-AUTOMATICA(RC)
  OPERACIÓN.- RECORRE-CADENA(PtrUltMov)                 → hasta encontrar la última "SUG="
  OPERACIÓN.- RECORRE-CADENA(PtrUltPaso)                → todos los pasos críticos ejecutados
  OPERACIÓN.- CHK-PLAZO(FecMov de Resuelto, FecLimiteAtencion)
  OPERACIÓN.- CHK-SUGERIDA(SUG, solución aplicada)
  OPERACIÓN.- DECIDE(Resultado)                         → Observado si algo falla;
                                                          Muestra al azar 1 de cada N (parámetro);
                                                          si no, Aprobado
  OPERACIÓN.- GRABA MOV-RECLAMO(RC, Revisión, CodEstado = En revisión, CodResultado)

EVENTO: DECISION-SUPERVISOR(CodSupervisor, RC, Aprueba | Devuelve, Motivo)
  OPERACIÓN.- GRABA MOV-RECLAMO(RC, Revisión, CodOperario = S01…, CodResultado, Observacion = Motivo)
  SI devuelve:  OPERACIÓN.- PROGRAMA EVENTO(RC, último evento)   → se repite con nueva cita; vuelve a En atención
```

## 5. Cliente — Estado de Reclamo, Conformidad e Impugnación (pantalla 17)

```
EVENTO: CONSULTA ESTADO(Documento)
  OPERACIÓN.- LEE-CONSULTA(Documento)                   → 1 acceso: lista con estado, cita y avance
  [al elegir un reclamo]
  OPERACIÓN.- LEE-MAE-RECLAMO(RC) y RECORRE-CADENA(PtrUltMov)  → eventos y resultados de ese reclamo

EVENTO: CONFORMIDAD(RC, Conforme | No conforme, Nota)
  OPERACIÓN.- GRABA MOV-RECLAMO(RC, Conformidad, CodResultado, Nota)
  SI conforme y la revisión está aprobada:
                   OPERACIÓN.- GRABA MOV-RECLAMO(RC, Cierre, CodEstado = Cerrado)
                   OPERACIÓN.- ACT-MAE-CLIENTE(NumReclamosAbiertos − 1)
  SI no conforme:  OPERACIÓN.- PROGRAMA EVENTO(RC, último evento)   → se repite con nueva cita

EVENTO: IMPUGNA(RC, Motivo)
  OPERACIÓN.- CHK-PLAZO(fecha del rechazo + días de impugnación de la instancia)
  OPERACIÓN.- LIBERA-TICKETS(citas no empezadas)       → vuelven al Parque de Ticket
  OPERACIÓN.- GRABA MOV-RECLAMO(RC, Impugnación, CodInstancia + 1, Observacion = Motivo)
  OPERACIÓN.- ACT-MAE(FecLimiteAtencion = nuevo plazo)
  OPERACIÓN.- PROGRAMA EVENTO(RC, primer evento del protocolo)   → en el área de la nueva instancia
```

## 6. Seguridad — Operación de configuración y Reversa (pantallas 01 y 01b)

Toda pantalla de Mantenimiento de Parámetros y de Seguridad graba en el Diario de Operaciones (ver `TABLAS_SEGURIDAD.md`).

```
EVENTO: GRABA OPERACION(Usuario, Modulo, ClaveRegistro, ValorNuevo)
  OPERACIÓN.- LEE-REGISTRO(Modulo, ClaveRegistro)       → ValorAnterior (la foto instantánea)
  OPERACIÓN.- CHK-COHERENCIA(Modulo, ValorNuevo)        → p. ej., un área no se queda sin especialistas
  SI no es coherente:
     OPERACIÓN.- GRABA-DIARIO(…, CodResultado = Rechazada)   → al final del archivo
  SI es coherente:
     OPERACIÓN.- GRABA-DIARIO(…, ValorAnterior, ValorNuevo, CodResultado = Aplicada)   (1)
     OPERACIÓN.- ACT-REGISTRO(Modulo, ClaveRegistro, ValorNuevo)                      (2)

EVENTO: REVERSA(Usuario, NroOperacion)
  OPERACIÓN.- CHK-ULTIMA(NroOperacion)                  → solo la última operación aplicada
  OPERACIÓN.- ACT-REGISTRO(Modulo, ClaveRegistro, ValorAnterior)
  OPERACIÓN.- GRABA-DIARIO(Reversa de NroOperacion)  y  marca la original como Revertida
```

- Como en la cadena simple, el diario se graba **antes** de cambiar el registro: si el sistema cae entre (1) y (2), el diario tiene la foto para dejar todo como estaba.

---

## Tablas que procesa cada transacción

El libro pide "procesar pocas tablas" en las transacciones muy usadas. Los catálogos y parámetros residen en memoria (p. 170), así que no cuentan como accesos a disco.

| Transacción | Tablas en disco que lee o graba |
|---|---|
| Registrar Reclamo | Compras, Maestra Cliente, Maestra Reclamos, Mov. Reclamos, Parque de Ticket, Maestra Operarios, Consulta Reclamos Cliente |
| Graba paso | Maestra Reclamos, Mov. Soluciones, Maestra Cliente (para el Documento), Consulta Reclamos Cliente |
| Cambio de estado / revisión | Maestra Reclamos, Mov. Reclamos, Maestra Cliente (para el Documento), Consulta Reclamos Cliente |
| Consulta Estado de Reclamo | Consulta Reclamos Cliente (1 acceso) |
| Operación de configuración | El catálogo que cambia, Diario de Operaciones |

La transacción más usada (graba paso) toca 4 tablas, y de la Maestra Cliente solo lee (por `CodCliente`, 1 acceso) el `Documento`, que es la clave de la Consulta Reclamos Cliente. El registro es la que más tablas toca, pero ocurre una sola vez por reclamo.

## Diferencias con el entregable 2

- El entregable 2 encadenaba la Maestra **Operario** con el Movimiento de Reclamos. Aquí la raíz es la Maestra **Reclamos**: lo que se consulta y se audita es la historia de cada reclamo, no la de cada operario. Las citas de un operario se encuentran en el Parque de Ticket por su índice `CodOperario + FecInicio`.
- Se agrega la segunda cadena (Movimiento de Soluciones con `PtrUltPaso`).
- Cada transacción se escribe como EVENTO → OPERACIÓN → MÉTODO, como en el libro (p. 120).
