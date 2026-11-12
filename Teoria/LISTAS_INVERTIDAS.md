# Diseño interno — Listas Invertidas (Consulta Crítica)

## Criterios del libro

- **Consulta crítica:** "son las que teniendo un alto costo son muy solicitadas" (2.4, p. 31). Muchos usuarios × muchos accesos.
- **Solución:** "crear tablas suplementarias a las originales (redundancia) de manera que se disponga de esa consulta previamente elaborada, sin sacrificar la información original… evita su construcción en tiempo real minimizando así el tiempo de respuesta" (p. 44). Es la **entidad suplementaria**: un registro por cliente con un **vector variable** de sus cuentas (pp. 45–48).
- **Redundancia controlada:** "la redundancia debe ser controlada a fin de economizar el espacio en disco" (p. 44). Solo se repite lo que la consulta muestra.
- **Parámetros de una indexada** (p. 89): `ORGANIZACIÓN = INDEXADA`, `LONG.REG = (MAX, MODA)`, `IC = m512 (se sugiere el mínimo)`, `ESPLIB = (%IC, %AC)`, `CLAVE = (LONG, UBICAC)`, `CAPACIDAD = (N, AC)`.
- **Espacio libre** (pp. 92–97): `%IC + %AC = (N0 − N) / N0`. Se fija primero el `%IC`, que es el más crítico, y con la ecuación se halla el `%AC`.

## Cuál es la consulta crítica del sistema

| Consulta | Pantalla | Quién la usa | Tablas que recorre sin tabla suplementaria | ¿Crítica? |
|---|---|---|---|---|
| **Estado de Reclamo** | 17 (Cliente · Reportes) | Todos los clientes, por la web, en cualquier momento | Maestra Cliente + Maestra Reclamos (n reclamos) + Movimiento de Reclamos (la cadena de cada uno, hasta la Programación) | **Sí** |
| Consulta de Reclamos | 12 (Gerencial · Consulta) | Pocos usuarios de gerencia, un reclamo a la vez | Maestra Reclamos + su cadena de movimientos | No |

**Estado de Reclamo es la consulta crítica.** Es la más usada del sistema: cada cliente vuelve a revisar su reclamo varias veces mientras espera. Además, para armar su lista hay que juntar datos de varias tablas:

- El cliente escribe su **DNI/RUC**, pero la Maestra Cliente está indexada por `CodCliente`.
- Por cada reclamo hay que leer la Maestra Reclamos.
- La **fecha programada** (la cita) no está en la maestra: está en el movimiento *Programación de evento*, así que hay que retroceder por la cadena simple de cada reclamo.
- El producto o servicio reclamado está en la Tabla de Compras (por `CodCompra`), y su nombre en otro catálogo.

**Accesos a disco para un cliente típico (2 reclamos):**

| | Sin tabla suplementaria | Con tabla suplementaria |
|---|---|---|
| Ubicar al cliente por DNI/RUC | 1 | 1 (es la clave) |
| Leer cada reclamo en la Maestra Reclamos | 2 | 0 |
| Leer la compra de cada reclamo en la Tabla de Compras | 2 | 0 |
| Recorrer la cadena hasta la Programación (unos 3 movimientos por reclamo) | 6 | 0 |
| **Total** | **11** | **1** |

La Consulta de Reclamos (pantalla 12) no es crítica: la usa poca gente y cada vez abre un solo reclamo. Se resuelve con la Maestra Reclamos y su cadena, sin redundancia.

## Entidad suplementaria: Consulta Reclamos Cliente

Un registro por cliente, con la clave que escribe el cliente (`Documento`) y un vector con sus reclamos. Lleva solo lo que muestra la lista de la pantalla 17: reclamo, objeto, estado y fecha programada, más la instancia, el límite y el avance. Al hacer clic en un reclamo, el detalle (eventos, resultado, impugnación) se lee de la Maestra Reclamos y su cadena, porque es **un** reclamo y no todos.

| Nombre | Consulta Reclamos Cliente |
|---|---|
| Organización | indexada |
| Longitud Registro | (1563, 213) |
| IC | 1536 |
| ESPLIB | (28.6%, 6.7%) |
| Clave | (11, 0) |
| Capacidad | (600000, 19*8*7) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| Documento | char | 11 | X | |
| Nombres | char | 25 | | |
| Apellidos | char | 25 | | |
| NroReclamos | int | 2 | | |
| Reclamos: vector de 1 a 20 (depende de NroReclamos) | | 75 c/u | | |
| · CodReclamo | char | 12 | | X |
| · Objeto | char | 30 | | |
| · CodEstado | int | 2 | | |
| · CodInstancia | int | 1 | | |
| · FecProgramada | int | 12 | | |
| · FecLimiteAtencion | int | 12 | | |
| · TotalPasos | int | 3 | | |
| · PasosCompletados | int | 3 | | |
| | | **(1563, 213)** | | |

- `Documento`: DNI o RUC; es la clave y está al inicio del registro: `CLAVE = (11, 0)`.
- `Objeto`: nombre del producto (bien + marca + modelo) o del servicio, ya armado. Es la redundancia que evita ir al catálogo.
- `FecProgramada`: la cita fija del evento en curso, copiada del movimiento *Programación de evento*. Es la redundancia que evita recorrer la cadena.
- `CodEstado`, `CodInstancia`, `FecLimiteAtencion`, `TotalPasos`, `PasosCompletados`: copia de los saldos de la Maestra Reclamos.
- `CodReclamo`: enlace a la Maestra Reclamos para abrir el detalle de un reclamo.

**Longitud de registro** (formato del libro `RL = PF + n·PV`, p. 91):

| Parte fija | 63 | Máximo | 20 reclamos |
|---|---|---|---|
| Parte variable | 75 | Moda | 2 reclamos |

- MAX = 63 + 20 × 75 = **1563**.
- MODA = 63 + 2 × 75 = **213**. Lo típico es que un cliente tenga 1 o 2 reclamos vigentes. Los que pasan al histórico salen del vector.

### Cómo se mantiene al día

- La consulta debe mostrar el estado **real**: un cliente que acaba de recibir su cita no puede ver un dato de ayer. Por eso, a diferencia de lo que el libro recomienda en general ("generalmente son generadas en procesos nocturnos de lotes", p. 49), el elemento del vector **se actualiza en línea**, en la misma transacción que actualiza la Maestra Reclamos (registro, programación de evento, paso completado, cambio de estado).
- Le cuesta 1 escritura más a esas transacciones, pero ahorra 10 lecturas a cada consulta, y la consulta se hace muchas más veces que las transacciones.
- La regeneración completa desde la Maestra Reclamos queda como proceso batch de control (se verá en la entrega del módulo batch).

## Cálculo del IC

Acceso online por clave, así que se busca el mínimo de la terna online (p. 91: "para acceso aleatorio buscar mínimo, tomar como referencia la moda del RL"):

| RL (moda) | r con 512 / 1024 / 1536 | IC elegido |
|---|---|---|
| 213 | 76 / 156 / **20** | **1536** (7 RL) |

## Cálculo de la capacidad

- **N0** = 600000: el tope de clientes, igual que la Maestra Cliente.
- **AC** = 1 cilindro, como en el libro (p. 92): `1 cil = 19 × 3 × 4096 bytes = 19 × 8 IC de 1536 = 19 × 8 × 7 RL`.
- `CAPACIDAD = (600000, 19*8*7)`.

## Cálculo del espacio libre (ESPLIB)

Se sigue el ejemplo del libro (pp. 96–97).

**1. Espacio libre total.** Hoy hay N = 400000 clientes con reclamos y se planifica crecer hasta N0 = 600000 en la vida útil de la tabla:

```
%IC + %AC = (N0 − N) / N0 = (600000 − 400000) / 600000 = 33.3 %
```

**2. Fijar el %IC.** Un IC de 1536 guarda 7 RL de moda, así que cada RL vale 1/7 = 14.3 % del IC:

| %IC | RL libres por IC | Qué permite |
|---|---|---|
| 0 % | 0 | Se descarta por teoría |
| 14.3 % | 1 | Una sola inserción antes de romper el IC |
| **28.6 %** | **2** | **Dos inserciones antes de romper el IC** |
| 42.9 % | 3 | Pasa el 33.3 % total: no es posible |

Se fija **%IC = 28.6 % (2 RL libres en cada IC)**. Aquí importa más que en una tabla de longitud fija: el registro **crece** cada vez que el cliente presenta un reclamo nuevo (+75 bytes), así que los clientes que ya están en el IC también consumen espacio libre, no solo los nuevos.

**3. Hallar el %AC.** En un AC de 152 IC (1064 RL), el espacio libre total es 33.3 % × 1064 = 354.7 RL. Si `x` es el número de IC que quedan vacíos en el AC:

```
RL libres en los IC usados + RL de los IC vacíos = 33.3 % del AC
2 × (152 − x) + 7 × x = 354.7
304 + 5x = 354.7
x = 10.1 IC  →  %AC = 10.1 / 152 = 6.7 %
```

Comprobación con la fórmula general:

```
%AC = (%ESPLIB − %IC) × 100 / (100 − %IC) = (33.3 − 28.6) × 100 / (100 − 28.6) = 6.7 %
```

**Resultado: `ESPLIB = (28.6 %, 6.7 %)`.** El %IC permite agregar 2 registros o hacer crecer los vectores dentro de cada IC sin romperlo. El %AC deja unos 10 IC vacíos por cilindro como reserva para cuando un IC se llene.

## Diferencias con el entregable 2

- El entregable 2 tenía la clave en `CodCliente`. Aquí es el `Documento`, que es lo que escribe el cliente en la pantalla 17: la consulta se resuelve con 1 acceso, sin buscar antes su código.
- El entregable guardaba nombre del producto, tipo de reclamo y 3 fechas por reclamo. Aquí el vector lleva solo lo que muestra la lista (75 bytes por reclamo en lugar de 93).
- IC: el entregable usaba 3584 (terna batch). Esta es una consulta online por clave, así que va en la terna online, con el mínimo residuo.
- ESPLIB: se calcula con el método del libro a partir de N y N0, en lugar de fijarse.

## Pendiente de confirmar con el profesor

- Si acepta que la tabla suplementaria se actualice en línea (para que el cliente vea su estado real) en lugar de solo en el batch nocturno.
- N = 400000 clientes con reclamos es una estimación.
- El AC se calcula igual en todas las tablas (ver «Cálculo del AC» en `TABLAS_MAESTRAS.md`).
