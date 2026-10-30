# Cambios al Sistema de Reclamos (observaciones del profesor)

Instrucciones para Claude Code. Proyecto Java 21 + Swing + Maven, sin base de datos (todo en memoria).
Paquete base `pe.tino.reclamos`. Capas: `model` (Ticket, Cliente), `repo` (Tickets, Dominio, Catalogos,
MotorReglas, Prototipo, Datos, Estadisticas), `ui` (una clase *Pantalla por pantalla).

**Antes de cambiar nada:** explora el proyecto, confirma los nombres reales de clases y métodos, y corre
`mvn test` para ver que las 58 pruebas pasan. Haz los cambios en el orden de abajo, uno por uno, y vuelve a
correr `mvn test` después de cada uno. Agrega pruebas nuevas para cada cambio.

Principio que pidió el profesor: **el sistema debe ser automático**. Ninguna decisión del flujo
(asignación, fecha, validación) debe depender de que una persona la elija a mano.

---

## Cambio 1 · Validación de los campos del formulario

**Problema:** el Formulario Reclamo casi no valida. Los combos vienen preseleccionados (Presencial, Loc01,
Funcionamiento, "La cámara no funciona"), así que se puede generar un ticket sin elegir nada. Además permite
combinaciones sin sentido, por ejemplo Servicio "Recojo en tienda" + "La cámara no funciona".

**Qué hacer (en la pantalla del formulario del cliente):**

1. Todos los combos empiezan en `"— Seleccione —"` (sin valor elegido).
2. Listas dependientes:
   - Si reclama sobre **Producto**: los tipos de problema y problemas se filtran por la relación
     problema ↔ producto que ya existe en el Catálogo de Problemas (hoy no se usa en el formulario).
   - Si reclama sobre **Servicio**: crear la relación servicio → tipos de problema permitidos. Propuesta:
     - Delivery, Recojo en tienda → Entrega, Atención
     - Cobro, Facturación → Cobranza, Atención
     - Instalación, Mantenimiento → Funcionamiento, Atención
     - Limpieza, Soporte telefónico → Atención
   - Al cambiar el producto/servicio se recarga Tipo de problema y se limpia Problema.
3. Reglas por campo al pulsar **Generar Ticket**:

| Campo | Regla |
| --- | --- |
| DNI/RUC | Obligatorio, solo dígitos, 8 (DNI) u 11 (RUC), cliente existente y validado |
| Nombre, Apellido, Correo, Teléfono, Tipo de cliente | Solo lectura, los llena el sistema |
| Canal | Obligatorio, solo habilitados |
| Local | Obligatorio solo si el canal es Presencial; deshabilitado en otros canales |
| Reclama sobre | Obligatorio (Producto o Servicio) |
| Producto o servicio | Obligatorio |
| Tipo de problema | Obligatorio y permitido para el producto/servicio |
| Problema | Obligatorio y del tipo elegido |
| Nro de comprobante | Si se marca la casilla: obligatorio, formato `[BF]\d{3}-\d{1,8}` (ej. B001-00012345) |
| Fecha del comprobante | Si se marca la casilla: obligatoria, formato dd/MM/yyyy, no futura |

4. Si hay errores: mostrar todos juntos en un solo mensaje y marcar en rojo los campos con error.
   No se genera el ticket.

**Criterio de aceptación:** no es posible generar un ticket con un campo vacío, con formato inválido o con
una combinación producto/servicio–problema incoherente.

---

## Cambio 2 · Validación del reclamo (que proceda)

**Problema:** aunque el formulario esté bien llenado, nadie revisa si el reclamo tiene derecho a existir.
Cualquiera puede reclamar por un producto que no compró.

**Qué hacer:**

1. Crear el registro de **compras del cliente** (en `Datos`, como los clientes): documento del cliente,
   producto o servicio, nro de comprobante, fecha de compra, monto. Cargar datos de prueba coherentes con los
   tickets iniciales (Alexander compró el Samsung Galaxy A54, etc.).
2. En el formulario, cuando reclama sobre Producto/Servicio, la lista muestra **solo lo que ese cliente
   compró o contrató**. Al elegirlo, el nro y la fecha del comprobante se llenan solos (solo lectura).
   Excepción: tipo de problema **Atención** (mal trato, demora) no requiere compra.
3. Antes de abrir el ticket (`Tickets.abrir` o equivalente), revisar automáticamente:

| Revisión | Si falla |
| --- | --- |
| El cliente compró ese producto/servicio | No se genera: "No registramos esa compra a su nombre" |
| El problema corresponde al producto/servicio | No se genera |
| No hay otro ticket abierto del mismo cliente, mismo objeto y mismo problema | No se genera: "Ya tiene el reclamo TK-... en atención" |
| Está en garantía (fecha de compra + plazo de `Prototipo.plazoDeGarantia`) | Se genera, pero marcado "Fuera de garantía" y anotado en bitácora |

4. Conectar el **MotorReglas** al flujo real: hoy solo corre en el Simulador. Al abrir el ticket, evaluar la
   cadena con los datos reales (Tiene_Comprobante, Tiempo_Transcurrido, Tiempo_Garantia, Tipo_Cliente,
   Instancia, Precio desde el monto de la compra) y guardar en el ticket la **solución sugerida** y la traza.
   Las variables que solo se conocen tras la inspección (Reparable, Stock) se evalúan al resolver.

**Criterio de aceptación:** no se puede reclamar algo no comprado ni duplicar un reclamo abierto; el ticket
guarda si está en garantía y la solución sugerida por el motor.

---

## Cambio 3 · Asignación automática (quitar la autoasignación)

**Problema:** hoy el empleado elige qué ticket tomar con el botón **Asignarme** (modelo "pull"). El profesor
lo rechazó: el empleado puede escoger los fáciles y dejar los difíciles.

**Qué hacer:**

1. Crear la relación **Tipo de problema → Área responsable** (configurable, en Parámetros de Atención o en el
   Catálogo de Problemas). Propuesta: Funcionamiento → Área Técnica de Reparación e Inspección;
   Entrega → Logística; Cobranza → Ventas; Atención → PostVenta.
2. Al generar el ticket, el sistema asigna automáticamente: área según el tipo de problema y especialista de
   esa área con **menor carga** (menos tickets abiertos); en empate, por orden (round-robin).
   También asigna el protocolo (`Prototipo.protocoloDe`). El ticket nace en estado **Asignado**.
3. Al impugnar, la reasignación también es automática (área de la nueva instancia + especialista con menor carga).
4. En Atender Ticket: quitar **Asignarme**. La pantalla muestra solo **los tickets del empleado**, ordenados
   por fecha programada (ver Cambio 4) y prioridad. Inspeccionar, Resolver y Rechazar se mantienen.
5. Actualizar la máquina de estados si hace falta (Registrado → Asignado ocurre en el mismo momento de abrir).
   Mantener la bitácora: "Asignado automáticamente a E01 (menor carga)".

**Criterio de aceptación:** ningún ticket queda sin responsable; ningún empleado elige tickets.

---

## Cambio 4 · Fecha fija de atención (cita automática)

**Problema:** el profesor dijo que el "límite de atención" (apertura + horas) no es una fecha fija, es un
plazo máximo. Quiere que el cliente sepa **cuándo** se atiende su reclamo, y que esa fecha la fije el
sistema automáticamente (no un empleado).

**Qué hacer:**

1. Crear la **agenda del especialista**: bloques ocupados (inicio, fin, ticket).
2. Crear **horario laboral** (L-S 9:00–18:00, tomando el de los canales) y lista de **feriados**.
3. Duración de la atención = suma de los tiempos máximos de las acciones del protocolo asignado
   (PR-FUNC-01 = 10 + 5 + 5 = 20 min).
4. Al generar el ticket (después de la asignación del Cambio 3): buscar el **primer hueco libre** en la agenda
   del especialista, dentro del horario laboral, que no sea feriado y que termine antes del límite de
   atención según prioridad. Si el especialista elegido no tiene hueco a tiempo, probar con otro del área.
   Reservar ese bloque.
5. Guardar en el ticket `fechaProgramada` (inicio de la cita). Es **fija**: no se recalcula. Una cita nueva
   no desplaza a las ya reservadas.
6. Mostrarla:
   - Ventana Ticket Generado: "Su reclamo será atendido el jueves 29/10/2026 a las 10:00 por Área Técnica".
   - Estado de Reclamo: columna "Fecha programada" (además del tiempo restante).
   - Consulta de Tickets y Atender Ticket: columna "Fecha programada".
7. Si no hay ningún hueco antes del límite, igual reservar el primer hueco disponible y marcar el ticket
   "Sin capacidad dentro del plazo" (y anotarlo en bitácora) para que la gerencia lo vea.
8. Mantener el límite de atención (SLA) como control interno: sirve para medir cumplimiento e indicadores.

**Criterio de aceptación:** todo ticket nace con fecha y hora programada, calculada sin intervención humana,
en horario laboral, sin solaparse con otras citas del mismo especialista.

---

## Cambio 5 · Control de calidad

**Problema:** al pulsar Resolver, el ticket queda resuelto sin que nadie revise la solución.

**Qué hacer:**

1. Nuevo estado **En revisión** entre Resuelto y Cerrado. Transiciones:
   Resuelto → En revisión → Cerrado, y En revisión → En atención (si no pasa la revisión).
   Actualizar `Dominio.Estado`, `Estado.siguientes()`, la pestaña Estados y los mensajes de Estado de Reclamo.
2. **Revisión automática** de todo ticket resuelto:
   - Todas las acciones críticas ejecutadas.
   - Resuelto dentro del límite de atención.
   - La solución aplicada coincide con la solución sugerida por el motor de reglas (Cambio 2).
   Si algo falla, el ticket queda "Observado" para supervisor.
3. **Revisión del supervisor** (nuevo perfil o rol en Seguridad): ve los tickets observados, una muestra al
   azar (configurable, por defecto 1 de cada 10) y todos los rechazos antes de notificar al cliente.
   Puede Aprobar (→ Cerrado) o Devolver con motivo (→ En atención).
4. **Conformidad del cliente:** en Estado de Reclamo, un ticket En revisión muestra "¿Su reclamo quedó
   solucionado?" Sí/No y nota del 1 al 5. Si responde No, vuelve a En atención.
5. Usar el evento **Seguimiento** (ya existe en el Catálogo de Reclamos) para esta etapa.
6. Indicadores: con las notas de conformidad, **Satisfacción del cliente** deja de ser "No calculable".
   Agregar "% devueltos por calidad".
7. Todo queda en bitácora.

**Criterio de aceptación:** ningún ticket pasa a Cerrado sin pasar por la revisión automática; los
observados, la muestra y los rechazos pasan por supervisor.

---

## Cambio 6 · Ajustes menores pendientes

1. Tipo de cliente **Estándar**: agregarlo a las tablas de Prioridad y Tiempos, o deshabilitarlo.
2. Catálogo de Reclamos: renombrar la columna **"Criticidad"** a **"Nivel"** para no confundirla con la
   prioridad del ticket ni con la criticidad de las acciones.
3. **Locales** (Loc01–Loc03): hoy vienen fijos del código. Agregarlos como catálogo editable en Parámetros
   de Atención (nombre, dirección, estado).
4. Catálogos compartidos en vivo: si se deshabilita un servicio o canal, el formulario debe verlo sin
   reiniciar (que todas las pantallas lean del mismo repositorio).

---

## Al terminar

- `mvn test` en verde, con pruebas nuevas para: validaciones del formulario, validación del reclamo,
  asignación por menor carga, cálculo de la cita (horario, feriados, sin solapes) y control de calidad.
- Actualizar los datos de prueba para que el recorrido de demo funcione: Alexander (DNI 72119474, Vip)
  reclama "La cámara no funciona" del Samsung Galaxy A54 que compró → se valida → se asigna solo a E01 →
  recibe fecha programada → E01 inspecciona y resuelve → revisión de calidad → conformidad del cliente → Cerrado.
- Resumen corto de qué archivos cambiaron y qué decisiones tomaste donde algo no estaba especificado.
