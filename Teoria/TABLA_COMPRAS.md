# Diseño interno — Tabla de Compras

## Qué es

Las compras y contrataciones de los clientes: el sustento de cada reclamo. El formulario del cliente (pantalla 16) la lee al ingresar el DNI/RUC para mostrar qué compró, y la validación (16.4) la usa para comprobar la compra, el monto y la garantía.

## Por qué hace falta

- La compra nace en el **sistema de ventas**, no en el de reclamos. El libro llama **interfase de datos** al acoplamiento entre módulos mediante datos, y dice que es "el tipo de interface más natural que debería usarse entre módulos" (1.3, p. 19).
- Esa interfase es el proceso batch **Carga de Compras** (rama BATCH → ACT-BD del diseño arquitectónico). La carga deja las ventas en esta tabla, y el online solo la **lee**. El batch se detalla en la entrega del módulo batch.
- Si la compra se consultara en línea al sistema de ventas, cada reclamo dependería de que ese sistema esté disponible. Con la tabla cargada, el formulario sigue funcionando aunque ventas no responda.

## Por qué no es maestra ni catálogo

- **No es maestra:** no tiene saldos que los eventos del reclamo actualicen (libro p. 75). El reclamo no modifica la compra.
- **No es catálogo:** no la mantiene la gerencia en Mantenimiento de Parámetros (p. 10). Tampoco es pequeña ni reside en memoria.
- Es una tabla de interfase **de solo lectura para el online**, que se renueva por lotes.

## Organización

- **Indexada.** La validación busca un comprobante concreto (acceso unitario). El libro dice que la indexada graba "en secuencia de clave… el tiempo de búsqueda será el mínimo posible" (p. 76).
- Clave primaria: `Comprobante`. Clave alterna (índice): `Documento`. Es la que usa el formulario para listar las compras del cliente que escribió su DNI/RUC (libro 3.3.2: "son muy buenos para acceso por llaves alternativas").
- **No lleva FK a la Maestra Cliente.** Quien compra todavía no es cliente del sistema de reclamos: se vuelve cliente cuando registra su primer reclamo. Por eso se enlaza por `Documento`, que es el dato que ya trae la venta.
- Como el acceso es online, el IC va en la terna online (512, 1024, 1536).

| Nombre | Tabla de Compras |
|---|---|
| Organización | indexada |
| Longitud Registro | (53, 53) |
| IC | 512 |
| Capacidad | (1200000, 19*12*5) |

| Columnas | Data Type | Length | PK | FK |
|---|---|---|---|---|
| Comprobante | char | 13 | X | |
| Documento | char | 11 | | |
| TipoObjeto | int | 1 | | |
| CodObjeto | char | 7 | | X |
| FecComprobante | int | 12 | | |
| Monto | decimal | 9 | | |
| | | **53** | | |

- `Comprobante`: serie y número de la boleta o factura (por ejemplo, B001-00012345). Es el mismo `Comprobante` que guarda la Maestra Reclamos.
- `Documento`: DNI (8) o RUC (11) del comprador.
- `TipoObjeto` + `CodObjeto`: producto (Catálogo de Productos) o servicio (Catálogo de Servicios), con el mismo dominio que la Maestra Reclamos.
- `FecComprobante`: fecha de la compra (`yyyyMMddHHmm`). Con ella y el plazo del Catálogo de Productos se calcula si el reclamo está dentro de la garantía.
- `Monto`: importe pagado, `decimal(9,2)`. Lo usa el motor de reglas (por ejemplo, para el reembolso).
- Capacidad: unos 2 comprobantes por cliente × 600000 clientes. La carga solo guarda las compras que todavía pueden reclamarse (dentro de la garantía más larga).

## Cálculo del IC

| RL | r con 512 / 1024 / 1536 | IC elegido |
|---|---|---|
| 53 | **4** / 12 / 20 | **512** (9 RL) |

## Qué cambia en los demás documentos

No cambia ninguna tabla ya definida. Solo se corrige el texto de `TABLAS_MAESTRAS.md`: `FecComprobante` y `Monto` ya no se buscan en el "sistema de ventas", sino en esta tabla.

## Pendiente de confirmar con el profesor

- La N (2 comprobantes por cliente) es una estimación.
- Se mantuvo la notación del AC `19*12*5`.
