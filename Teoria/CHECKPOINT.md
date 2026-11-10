# Diseño interno — Checkpoint

## Criterios del libro (Punto de control de proceso, pp. 143–146)

- **Para qué sirve:** "permite conocer el estado de un proceso respecto a su base de datos, para que dado el caso de final abrupto del proceso, retomar el proceso desde el punto donde se produjo el problema. Proceso conocido como **re-arranque**". Para re-iniciar, la base debe estar igual que en el **último commit** correcto.
- **Eventos de un proceso:** INICIO (normal o forzado), COMMIT (cada actualización física; si falla, rollback) y FIN (normal o con error). Desde el inicio hasta el fin el proceso queda en estado de re-arranque.
- **Estado del checkpoint al iniciar:** si no existe, se crea; si está en N (normal), se actualiza; si está en R (re-arranque), se re-arranca desde el último commit.
- **Usa dos tablas** que se actualizan juntas:
  - **Tabla Checkpoint:** el estado del proceso hasta la última interacción correcta.
  - **Tabla Archivos:** cuántos archivos planos escribe el proceso y cuántos registros de cada uno ya se grabaron. Con eso, en el re-arranque, cada archivo se regenera hasta el último commit.
- Debe activarse "cada vez que se ejecuta una cadena de procesos".

## Dónde se usa en este sistema

En los procesos batch: generación del parque de tickets, BAT-01 a BAT-06. Por ejemplo, si BAT-05 se cae a la mitad de la depuración, al re-arrancar sabe cuántos reclamos ya pasó al histórico (`NumCommits`, `UltimaClave`) y sigue desde el siguiente, sin duplicar ni perder ninguno.

## Organización

Son tablas pequeñas (una fila por job y paso) que el proceso **lee y actualiza por su clave** en cada commit. Por eso van **indexadas** y no secuenciales: en una secuencial todo registro nuevo va al final y no se puede ir directo a uno (p. 76). El entregable 2 la puso secuencial.

---

## 1. Checkpoint

| Nombre | Checkpoint |
|---|---|
| Organización | indexada |
| Longitud Registro | 73 |
| IC | 1536 |
| Capacidad | (20, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodPaso | char | 8 | X | |
| CodJob | char | 8 | | |
| NumSecuencia | int | 1 | | |
| IndRearranque | char | 1 | | |
| FecUltCommit | int | 12 | | |
| NumCommits | int | 9 | | |
| TotActualizados | int | 9 | | |
| FrecCommit | int | 5 | | |
| UltimaClave | char | 20 | | |
| | | **73** | | |

- `CodPaso`: clave primaria. Es único en todo el sistema porque incluye el job (por ejemplo `B05UNLD` y `B05LOAD` para los pasos UNLOAD y LOAD de BAT-05). `CodJob`: el proceso al que pertenece (GEN-TKT, BAT-01 … BAT-06).
- `NumSecuencia`: qué ejecución del día es, porque un proceso puede correr más de una vez al día (libro, p. 143). La fila guarda siempre el estado de la última.
- `IndRearranque`: N (normal) o R (re-arranque).
- `FrecCommit`: cada cuántos registros se hace commit.
- `UltimaClave`: el último registro procesado con éxito (por ejemplo un `CodReclamo`). Es lo que el libro llama datos de reposicionamiento.
- Basada en la tabla `DCLASCHKPMD` del libro, sin los campos que este sistema no necesita (empresa, tiempos de ejecución).

## 2. Archivos del Checkpoint

| Nombre | Archivos_Checkpoint |
|---|---|
| Organización | indexada |
| Longitud Registro | 25 |
| IC | 512 |
| Capacidad | (10, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| NomArchivo | char | 8 | X | |
| CodPaso | char | 8 | | X |
| NumRegistros | int | 9 | | |
| | | **25** | | |

- Una fila por archivo plano que escribe un paso: la descarga (UNLOAD) de BAT-05, el resultado de indicadores de BAT-04 o la lista de alertas de BAT-06.
- `NomArchivo`: clave primaria; cada archivo plano tiene un nombre único. `CodPaso`: el paso que lo escribe (FK a Checkpoint); de ahí se sabe el job.
- Basada en la tabla `DCLASFICHMD` del libro.

---

## Cálculo del IC

Si la tabla entera cabe en un IC de 512, se usa 512. Si no, el menor residuo de la terna online.

| Tabla | RL | N | ¿Cabe en 512? | r con 512 / 1024 / 1536 | IC elegido |
|---|---|---|---|---|---|
| Checkpoint | 73 | 20 | No (1524) | 52 / 32 / **12** | **1536** (20 RL) |
| Archivos_Checkpoint | 25 | 10 | Sí (284) | — | **512** |

## Diferencias con el entregable 2

- El entregable 2 tenía una sola tabla. El libro usa dos (Checkpoint y Archivos), así que aquí van las dos.
- Se agregan `IndRearranque` y `UltimaClave`, que son los que hacen posible el re-arranque según el libro.
- Indexada en lugar de secuencial, porque se actualiza por clave en cada commit.
