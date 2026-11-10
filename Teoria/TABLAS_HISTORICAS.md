# Diseño interno — Tablas Históricas

## Criterios del libro

- **Qué es un histórico:** "Todo hecho de la realidad… impacta en sus tablas maestras… El conjunto de estos hechos o movimientos configuran el historial de las cuentas" (3.2.2.3, p. 159). Sirve para demostrar el estado de cada caso y para estudiar patrones de comportamiento.
- **Congeladas y batch:** "Las tablas históricas deben estar sin cambios, es decir congeladas; su crecimiento será llevado a cabo en procesos por lotes, durante la noche" (p. 75). "Deben ser generados en modalidad batch por cada pase de Movimiento-diario al histórico" (p. 170).
- **Acumulación en cascada** (p. 160): Paso 1 UNLOAD (descarga del movimiento), Paso 2 adicionar el movimiento al acumulado. Todos los pasos son re-ejecutables.
- **Organización secuencial** (3.1.2.6): se graban en orden de llegada, todo registro nuevo va al final y se leen completos, que es justo lo que hace un batch. El IC va en la terna batch (3072, 3584, 4096) por menor residuo, porque "su IC debe ser lo más grande posible" (p. 78).

Igual que en el entregable 2, cada histórico tiene la **misma estructura que su tabla de origen**, con una diferencia: aquí se quitan los punteros de cadena. Un archivo secuencial no tiene posición relativa (PRR), así que esos punteros ya no apuntan a nada. El orden lo da la clave (`CodReclamo` + fecha).

## Cuándo pasa un reclamo al histórico

Lo hace el batch **BAT-05 Depuración de reclamos cerrados**: los reclamos cerrados hace más de un año salen de la Maestra Reclamos y sus movimientos salen de las tablas relativas. Se mueven **cuando el reclamo se cierra, no cada día**, porque mientras está abierto la cadena simple necesita todos sus movimientos en la tabla relativa. Al liberar esas posiciones, las tablas online no crecen sin límite.

Son tres históricos: uno por cada tabla que se depura.

---

## 1. Histórica de Reclamos

Los reclamos cerrados que salen de la Maestra Reclamos.

| Nombre | H_Reclamos |
|---|---|
| Organización | secuencial |
| Longitud Registro | (350, 190) |
| IC | 4096 |
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
| FecCierre | int | 12 | | |
| | | **350** | | |

- La misma estructura que la Maestra Reclamos, sin `PtrUltMov` ni `PtrUltPaso` y con `FecCierre`.
- MODA = 190, con el mismo supuesto de la maestra (detalle de ~60 caracteres y pedido de ~30).

## 2. Histórica de Movimientos de Reclamos

| Nombre | H_MovimientoReclamos |
|---|---|
| Organización | secuencial |
| Longitud Registro | 152 |
| IC | 3584 |
| Capacidad | (3600000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| NroMov | int | 8 | X | |
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
| | | **152** | | |

- La misma estructura que el Movimiento de Reclamos, sin `PtrMovAnterior`.

## 3. Histórica de Movimientos de Soluciones

| Nombre | H_MovimientoSoluciones |
|---|---|
| Organización | secuencial |
| Longitud Registro | 74 |
| IC | 4096 |
| Capacidad | (4500000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| NroMov | int | 8 | X | |
| NroMovReclamo | int | 8 | | X |
| CodTicket | int | 8 | | X |
| NumPaso | int | 2 | | |
| FecPasoEstimado | int | 12 | | |
| FecFinPasoEstimado | int | 12 | | |
| FecPaso | int | 12 | | |
| FecFinPaso | int | 12 | | |
| | | **74** | | |

- La misma estructura que el Movimiento de Soluciones, sin `PtrMovAnterior`. `NroMovReclamo` apunta a la Histórica de Movimientos de Reclamos.

---

## Cálculo del IC

Terna batch: `#RL = (IC − 4) / (RL + 3)`, `r = (IC − 4) − #RL·(RL + 3)`. Se elige el menor `r`. En la de longitud variable se usa la MODA.

| Tabla | RL | r con 3072 / 3584 / 4096 | IC elegido |
|---|---|---|---|
| H_Reclamos | 190 (moda) | 173 / 106 / **39** | **4096** (21 RL) |
| H_MovimientoReclamos | 152 | 123 / **15** / 62 | **3584** (23 RL) |
| H_MovimientoSoluciones | 74 | 65 / 38 / **11** | **4096** (53 RL) |

## Diferencias con el entregable 2

- El entregable 2 tenía dos históricos (de los dos movimientos). Aquí se agrega **H_Reclamos** porque el batch BAT-05 saca de la maestra los reclamos cerrados, y deben quedar guardados en algún lado.
- Sin punteros, porque en un archivo secuencial no significan nada.
- IC calculado en la terna batch (el entregable usaba 4096 sin cálculo; en dos de las tres tablas el cálculo también da 4096).

## Pendiente de confirmar con el profesor

- El plazo de un año para depurar es el que hoy usa BAT-05.
- Las N repiten las de las tablas de origen, como hizo el entregable 2.
