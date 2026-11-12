# Guía de defensa — Entregable 2 (Diseño interno)

Todo lo que el profe puede preguntar sobre las tablas, con la respuesta corta y de dónde sale en su libro.

---

## 1. Definiciones del libro (decirlas tal cual)

| Concepto | Definición corta | Libro |
|---|---|---|
| **Tabla maestra** | Guarda la entidad y sus **saldos**. Los eventos solo actualizan los saldos comprometidos con ellos. | p. 75 |
| **Tabla de movimientos** | Información de **eventos**: cada transacción "graba registro de movimiento y actualiza maestro". | pp. 74, 115 |
| **Catálogo** | Información tabulada o de parámetros: "variables que se mantienen fijas para definir escenarios del negocio". Se mantiene en Mantenimiento de Parámetros y reside en memoria. | pp. 10, 74, 170 |
| **Histórica** | Tabla "congelada", sin cambios; crece por lotes durante la noche. | p. 75 |
| **Lista invertida / entidad suplementaria** | Tabla redundante que deja **ya armada** una consulta crítica para no construirla en tiempo real. | pp. 44–48 |
| **Consulta crítica** | La que "teniendo un alto costo, es muy solicitada". | p. 31 |
| **IC (intervalo de control)** | Bloque físico de lectura/escritura, siempre múltiplo de 512 (un sector). | pp. 67–70 |
| **AC (área de control)** | Bloque de crecimiento de la tabla (factor de expansión), expresado en número de registros. | pp. 79, 92 |
| **ESPLIB (%IC, %AC)** | Espacio libre que se deja en cada IC y en cada AC para insertar o hacer crecer registros. `%IC + %AC = (N0 − N) / N0`. | pp. 92–97 |
| **CLAVE (LONG, UBICAC)** | Longitud de la clave y su desplazamiento dentro del registro. | p. 91 |
| **Clave alterna** | Índice por otro atributo distinto de la clave primaria. | 3.3.2 |
| **PRR** | Posición Relativa del Registro: el número de registro en una tabla relativa. | p. 107 |
| **Cadena simple** | La maestra guarda el PRR de su **último** movimiento y cada movimiento guarda el PRR del **anterior** (0 si es el primero). | pp. 115–121 |

### Las tres organizaciones

| Organización | Cómo se accede | Dónde la uso | IC |
|---|---|---|---|
| **Indexada** | Por clave, con índice | Maestras, catálogos, parque, compras, perfiles, lista invertida | Terna online, buscar el **mínimo** |
| **Relativa** | Directo por PRR, sin índice, longitud fija `(L0, L0)` | Movimientos (transaccional en tiempo real) | Terna online (`m512`, "mínimo para transaccional") |
| **Secuencial** | Se graba al final, se lee completo | Diario de Operaciones (bitácora), históricas | Terna batch, "lo más grande posible" |

---

## 2. Las tres fórmulas (saberlas de memoria)

**IC** (pp. 67–70):

```
#RL = (IC − 4) / (RL + 3)     (parte entera)
r   = (IC − 4) − #RL · (RL + 3)
Online: 512, 1024, 1536      Batch: 3072, 3584, 4096
Se elige el IC con menor r.  Si el registro es variable, se usa la MODA.
```

- El "−4" es el control del IC y el "+3" es el control de cada registro.
- Ejemplo: Maestra Cliente, RL = 147 → con 1536: `1532 / 150 = 10`, `r = 1532 − 1500 = 32`. Es el menor de la terna (58 / 120 / 32), así que el IC es 1536 con 10 registros por IC.

**AC** (pp. 92–93):

```
1 cilindro = 19 pistas;  1 pista = 3 × 4096 = 12288 bytes
AC = 19 × (IC por pista) × (RL por IC)
IC por pista = 12288 / IC  →  512: 24   1024: 12   1536: 8   3072: 4
```

- El ejemplo del libro es `19*12*5`: IC de 1024 (12 por pista) con 5 registros por IC. **No es una constante**: cambia con el IC y el RL de cada tabla.
- Todas las tablas crecen de a **un cilindro**, como el ejemplo del libro.
- Ejemplos:
  - Maestra Cliente: IC 1536 → 8 IC por pista; 10 RL por IC → `19*8*10`.
  - Movimiento de Reclamos: IC 512 → 24 IC por pista; 3 RL por IC → `19*24*3`.

**Longitud variable** (pp. 79, 90–91): `LONG.REG = (MAX, MODA)`, con `RL = parte fija + n × parte variable`.

- Ejemplo: Consulta Reclamos Cliente, con 63 bytes fijos y 75 por reclamo → MAX = 63 + 20×75 = **1563**; MODA = 63 + 2×75 = **213**.
- Otro: Maestra Reclamos, con 94 bytes fijos más `Detalle` y `Pedido` varchar → MAX = 94 + 150 + 100 = **344**; MODA = 94 + 60 + 30 = **184**.

---

## 3. La lógica del sistema en 30 segundos

> "El **reclamo** es el caso. Cada cosa que le pasa al caso (registro, cita, cambio de estado, revisión, conformidad, cierre) es un **movimiento de reclamo**. Cada paso del protocolo que ejecuta el operario es un **movimiento de solución**. La **Maestra Reclamos** guarda los saldos del caso (estado, operario, pasos completados) y los punteros al último movimiento de cada cadena. La fecha y el responsable los decide el sistema: el reclamo toma turnos libres del **Parque de Ticket**, que genera un batch. Nadie asigna nada a mano."

Las dos cadenas:

```
Maestra Reclamos
 ├─ PtrUltMov  ─► Movimiento de Reclamos   ─PtrMovAnterior─► … ─► 0
 └─ PtrUltPaso ─► Movimiento de Soluciones ─PtrMovAnterior─► … ─► 0
```

**El orden importa** (p. 121): primero se graba el movimiento apuntando al anterior y **después** se actualiza la maestra. Al revés, si el sistema cae en medio, la cadena se rompe.

---

## 4. Por qué cada tabla es lo que es

| Tabla | Tipo | Por qué |
|---|---|---|
| Maestra Cliente | Maestra | Tiene saldos: `NumReclamos`, `NumReclamosAbiertos`. Clave alterna `Documento` porque el cliente entra con su DNI/RUC. |
| Maestra Operarios | Maestra | Tiene saldos: `CargaActual`, atendidos, resueltos. La asignación por menor carga los usa. |
| Maestra Reclamos | Maestra | Tiene saldos del caso (estado, operario, instancia, pasos completados) y es la **raíz** de las dos cadenas. |
| Movimiento de Reclamos | Movimiento, relativa | Eventos del caso. Relativa porque es transaccional en tiempo real y se llega por PRR desde la maestra. |
| Movimiento de Soluciones | Movimiento, relativa | Cada paso ejecutado, con hora estimada y real. Es otra estructura y actualiza otro saldo (`PasosCompletados`). |
| 11 catálogos | Catálogo | Cada uno es una pantalla de Mantenimiento de Parámetros, o lo exige una FK (Áreas, Locales, Clasificación): "una FK necesita su entidad" (p. 37). |
| Parque de Ticket | Sección propia, indexada | Turnos de cada operario. Lo genera el batch y lo usa el online buscando "primer turno libre del operario X desde la hora Y". Por eso tiene la clave alterna `CodOperario + FecInicio`. |
| Tabla de Compras | Interfase, indexada | Las ventas nacen en otro sistema: es la "interfase de datos" (p. 19). El online solo la lee. Clave alterna `Documento` para listar lo que compró el cliente. |
| Tabla de Perfiles | Seguridad, indexada | Acceso según el perfil del usuario (p. 10). |
| Diario de Operaciones | Seguridad, secuencial | Bitácora: se graba al final y guarda la "foto instantánea" para la reversa (p. 9). |
| Consulta Reclamos Cliente | Lista invertida | La consulta crítica: todos los clientes revisan su reclamo muchas veces. Pasa de 11 accesos a 1. |

---

## 5. Preguntas probables y respuesta

**¿Por qué el producto no es maestra?**
No tiene saldos que el reclamo actualice. El stock es del almacén. Es un dato fijo que define la gerencia, así que es catálogo (p. 74).

**¿Por qué `CodTipoCliente` es FK y `CodTipoEmpleado` no?**
El tipo de cliente tiene su pantalla en Mantenimiento de Parámetros (07 "Catálogo de Cliente"), donde la gerencia lo mantiene junto con sus plazos de atención: es un catálogo, y por eso `CodTipoCliente` es FK al Catálogo de Cliente. El tipo de empleado no tiene pantalla: son 4 valores fijos del sistema (Asistente, Técnico, Especialista, Supervisor), así que es un dominio con `check`.

**¿Por qué `Documento` no es FK?**
En la Maestra Cliente es la **clave alterna** (el cliente entra con su DNI/RUC). En la Tabla de Compras no puede ser FK a la Maestra Cliente porque quien compra todavía no es cliente del sistema de reclamos: lo es recién cuando registra su primer reclamo. Y la Consulta Reclamos Cliente, donde `Documento` es clave, es una tabla redundante que se arma desde la maestra; nadie apunta a ella.

**¿Y por qué Locales es tabla y Canales no, si los dos están en Parámetros de Atención?**
El local lo crea la gerencia (abre una tienda nueva) y es FK en la Maestra Reclamos (`CodLocal`): necesita su tabla. Los canales (Presencial, Telefónico, Internet, Web) son las vías por las que el sistema recibe reclamos: son fijos, y la gerencia solo cambia su horario, que es un parámetro de módulo.

**¿Por qué el estado, la prioridad o la instancia no tienen tabla?**
Son dominios fijos que la gerencia no configura: se controlan con `check` (integridad de dominio, pp. 179–183). Ejemplo: `CodPrioridad int(1) check (CodPrioridad in (1,2,3))`.

**¿Por qué el AC no es `19*12*5` en todas, como en el libro?**
Porque `19*12*5` es el ejemplo del libro para un IC de 1024 con 5 registros por IC. El AC es un cilindro expresado en registros: 19 pistas × IC por pista × RL por IC. Cada tabla tiene su IC y su RL, así que cada una tiene su AC. Por ejemplo, la Maestra Reclamos tiene IC 1536 (8 IC por pista) con 8 RL por IC: `19*8*8`.

**¿Por qué no pusiste ESPLIB en todas?**
Lo calculé en la tabla que realmente lo necesita: la lista invertida, donde los registros **crecen** (+75 bytes por cada reclamo nuevo del cliente). El cálculo: `N = 400000`, `N0 = 600000` → 33.3 % libre, repartido en `%IC = 28.6 %` (2 RL libres por IC) y `%AC = 6.7 %`.
- Los catálogos los mantiene la gerencia y residen en memoria.
- Los movimientos son relativos, y el libro pone espacio libre cero en la organización relativa.

**¿Por qué los movimientos son relativos y no indexados?**
El libro dice que la relativa es la preferida para módulos transaccionales en tiempo real: acceso directo por PRR, sin índice que mantener. Se llega a ellos desde la maestra con la cadena simple.

**¿Cómo encuentras los movimientos de un reclamo si la relativa no tiene índice?**
Leo la Maestra Reclamos por clave, voy al PRR de `PtrUltMov` y retrocedo con `PtrMovAnterior` hasta llegar a 0.

**¿Cómo se obtiene un PRR nuevo?**
Con un correlativo (`ADD-PRR(+1)`). Nunca se reutiliza, por eso `NroMov` es único y sirve de clave.

**¿Por qué el Movimiento de Soluciones tiene IC 1024 y no 512?**
Por el método del menor residuo: con RL 82 da r = 83 / **0** / 2. Con 1024 no se desperdicia ningún byte. Sigue siendo de la terna online.

**¿Por qué dos tablas de movimientos y no una?**
Son dos clases de transacción: sobre el **caso** (pocas por reclamo, cambian el estado) y sobre la **ejecución** (muchas por reclamo, con hora estimada y real). Tienen otra estructura y actualizan otro saldo. Si fueran una sola tabla, cada registro cargaría columnas vacías.

**¿Por qué la Maestra Reclamos guarda `CodProtocolo` y `TotalPasos` si salen del catálogo?**
Es una foto tomada al registrar. Si la gerencia cambia el protocolo mientras el reclamo está abierto, el reclamo sigue con el protocolo con el que empezó. Por la misma razón, la solución sugerida por el motor se guarda en el movimiento Registro.

**¿Por qué `Detalle` y `Pedido` están en la maestra si son largos?**
Son atributos descriptivos fijos del reclamo, como `NOMBRE` o `DIRECC` en el `MAESTRO-CLIENTES` del libro. Por eso son `varchar` y la longitud es `(MAX, MODA) = (344, 184)`. El IC se calcula con la moda.

**El libro dice que las cuentas nuevas se insertan en batch. ¿Por qué das de alta reclamos online?**
El registro del reclamo **es** la transacción del negocio: el cliente necesita su fecha de atención en ese momento. El cliente se crea junto con su primer reclamo. Lo que sí va en batch es la carga de compras y la generación del parque de tickets.

**¿Por qué la lista invertida se actualiza en línea si el libro dice que se genera en lotes?**
El libro dice "generalmente". Aquí el cliente tiene que ver su cita real apenas la recibe. Cuesta 1 escritura más por transacción y ahorra 10 lecturas por consulta, y la consulta se hace muchas más veces. Igual queda un batch de regeneración como control.

**¿Por qué `Flg_Disponible` en el ticket si basta con `NroMovReclamo = 0`?**
Es la condición de búsqueda del turno libre. Con el flag se filtra sin interpretar un 0 en un campo FK, y cuesta 1 byte.

**¿Por qué el Parque de Ticket es indexado y no secuencial, si lo carga un batch?**
Lo carga el batch, pero lo **usa** el online: hay que ir directo al primer turno libre de un operario. La secuencial no permite acceso aleatorio (p. 76).

**¿Por qué `CodCompra` es FK en la Maestra Reclamos?**
`CodCompra` es la clave primaria de la Tabla de Compras. Según el libro, una FK es un atributo guardado en una entidad que en otra tiene la categoría de clave y sirve de apuntador (p. 37). El reclamo lo guarda para **apuntar al producto o servicio comprado que se reclama**. Con ese apuntador valida la compra y lee el comprobante, la fecha y el monto (garantía y motor de reglas) sin repetirlos en la maestra.

**¿Por qué Compras tiene `CodCompra` y no usa el comprobante como clave?**
Porque una boleta puede tener varios productos. Cada producto comprado es una fila con su propio `CodCompra`; el `Comprobante` se repite en las filas de la misma boleta. Así el reclamo apunta exactamente al producto que se reclama.

**¿Cómo enlaza `CodObjeto char(7)` con `CodServicio int(3)`?**
En la Tabla de Compras, `CodObjeto` apunta a dos catálogos según `TipoObjeto`: 1 = producto, 2 = servicio. Mide 7 porque tiene que caber el código más largo, que es el de producto (`char(7)`). Si es un servicio, guarda su código con ceros a la izquierda: el servicio 12 queda `0000012`.

**¿Por qué Protocolos y Pasos son dos catálogos?**
Porque son dos cosas distintas, igual que en la pantalla 08: la lista de **protocolos** (uno por tipo de problema) y la lista de **pasos** de cada evento. Así cada tabla tiene **una sola PK**: `CodProtocolo` en Protocolos y `CodPasoProtocolo` en Pasos. Y `CodProtocolo` es FK en Pasos, Catálogo de Reclamo, Maestra Reclamos y Movimiento de Reclamos apuntando a una clave de verdad: "en esta última tiene la categoría de clave" (p. 37).

**¿Por qué los pasos son filas y no un vector dentro del protocolo?**
Cada paso tiene sus propios datos (acción, tiempo, tipo de empleado, criticidad) y otra tabla lo referencia: el Movimiento de Soluciones guarda el `NumPaso` del protocolo y evento en curso. Un elemento al que otra tabla apunta tiene que ser una fila. Además, así las dos tablas son de longitud fija.

**Entonces, ¿cuándo usas vector?**
Solo para **listas de códigos** que ninguna otra tabla referencia, como el libro guarda la relación 1 a n como atributo múltiple (pp. 40, 47): los bienes a los que aplica un problema (`AplicaA`), los tipos que admite un servicio (`TiposAdmitidos`), las pantallas de un perfil (`Accesos`). Y en la lista invertida, que es justamente para lo que el libro creó el vector variable: armar la consulta crítica en un solo acceso.

**¿Para qué la clave alterna `CodProtocolo + CodEvento + NumPaso` en Pasos?**
La PK es `CodPasoProtocolo`, pero los procesos buscan los pasos por protocolo y evento: para sumar la duración de una cita o para saber qué acción es el paso que registró el operario. Esa búsqueda es la clave alterna (3.3.2). Como el índice está en ese orden, los pasos de un protocolo salen seguidos.

**¿Por qué `CodProtocolo` es `char(7)`?**
Porque los códigos son `PR-FUNC`, `PR-ENTR`, `PR-COBR` y `PR-ATEN`, como se ven en el prototipo. Es el mismo tipo en las 5 tablas donde aparece.

**¿Por qué `Accion` mide 43?**
La acción más larga de hoy mide 38 caracteres. Con 43 el registro mide 65 y en un IC de 1024 caben 15 registros con residuo **0** (criterio de economía del libro).

**¿Qué significa `Clave = (8, 0)`?**
La clave mide 8 bytes y empieza en el byte 0 del registro. En la clave alterna `Documento (11, 8)` son 11 bytes que empiezan en el byte 8, justo después de `CodCliente`.

**¿Por qué el Diario de Operaciones usa IC 3072 si es online?**
Es secuencial: se graba siempre al final y se lee completo. Para la secuencial el libro dice "su IC debe ser lo más grande posible" (p. 78). Con la moda de 252 bytes da r = **8** / 10 / 12, así que 3072.

---

## 6. Si te preguntan algo que no sabes

Volver siempre a la prueba de los tres tipos de información: **¿tiene saldos que los eventos actualizan?** → maestra. **¿Es un evento?** → movimiento. **¿Es un dato fijo que define la gerencia?** → catálogo. Y a los dos criterios del IC: **economía** (menor residuo) y **performance** (online chico, batch grande).
