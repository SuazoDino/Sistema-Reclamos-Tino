# Diseño interno — Parque de Ticket

Va en su propia sección, como en el entregable 2 (3.1.5). Antes estaba como "Maestra Ticket" en `TABLAS_MAESTRAS.md`.

## Qué es

Los turnos de atención de cada operario. Un ticket es un turno, no un reclamo. El batch genera el parque de antemano a partir de la jornada, los feriados y los operarios habilitados. Cuando un reclamo necesita una cita, el evento toma los tickets libres que le hacen falta. Así la fecha y el responsable salen del parque y no los elige nadie.

## Por qué indexada (y no secuencial)

- El parque lo **carga** un batch, pero lo **usa** el online: al programar una cita hay que encontrar "el primer ticket libre del operario X desde la hora Y" y marcarlo tomado.
- El libro dice que la organización secuencial "no permite acceder en forma aleatoria" (p. 76) y que la indexada graba "en secuencia de clave… el tiempo de búsqueda será el mínimo posible" (p. 76). Por eso es **indexada**, como en el entregable 2.
- Clave primaria: `CodTicket`. Clave alterna (índice): `CodOperario` + `FecInicio`, que es la que usa la búsqueda del primer turno libre (libro 3.3.2: "son muy buenos para acceso por llaves alternativas").
- Como el acceso es online, el IC va en la terna online (512, 1024, 1536).

| Nombre | Parque de Ticket |
|---|---|
| Organización | indexada |
| Longitud Registro | (32, 32) |
| IC | 1024 |
| Capacidad | (500000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| CodTicket | int | 8 | X | |
| CodOperario | char | 3 | | X |
| FecInicio | int | 12 | | |
| Flg_Disponible | int | 1 | | |
| NroMovReclamo | int | 8 | | X |
| | | **32** | | |

- La hora de fin sale de `FecInicio` + la duración del turno.
- `NroMovReclamo`: el movimiento *Programación de evento* que tomó el ticket (Movimiento de Reclamos). De ahí se sabe el reclamo y el evento. Vale 0 si el ticket está libre.
- Capacidad: operarios × turnos por día × días generados por adelantado.

## Cálculo del IC

| RL | r con 512 / 1024 / 1536 | IC elegido |
|---|---|---|
| 32 | 18 / **5** / 27 | **1024** (29 RL) |

## Diferencias con el entregable 2

- El entregable 2 repetía en el ticket el tipo de reclamo y el producto. Aquí se llega a ellos por `NroMovReclamo`, sin redundancia.
- El entregable 2 guardaba hora de inicio y de fin. Aquí la de fin se calcula.
