# Diseño externo (prototipo) — versión web

Capturas de la versión web (`mvn compile exec:java -Dmain.class=pe.tino.reclamos.web.ServidorWeb`, http://localhost:8080).
Cada pantalla con su Entrada, Función y Salida, en el mismo formato del entregable anterior.

## Seguridad

### Perfiles y accesos — `01_seguridad_perfiles_y_accesos.png`
- **Entrada:** perfil de usuario, accesos (pantallas) y permisos.
- **Función:** crear perfiles y definir qué pantallas puede abrir cada uno.
- **Salida:** perfil con sus accesos concedidos.

### Diario de operaciones — `01b_seguridad_diario_de_operaciones.png`
- **Entrada:** cada cambio a la configuración (catálogos, parámetros, especialistas, perfiles), aplicado o rechazado.
- **Función:** dejar huella de todo lo que se hace (módulo de Auditoría del libro) y revertir la última operación vigente con la foto previa al cambio (Reversa de Operaciones).
- **Salida:** el diario con momento, usuario, módulo, operación, detalle y resultado (Aplicada, Rechazada o Revertida).

## Online · Gerencial · Mantenimiento de parámetros

### Parámetro General — `02_parametro_general.png`, `02_parametro_general_b_parametro_elegido.png`
- **Entrada:** áreas, especialistas (código, tipo de operario, área, estado) y los parámetros del sistema: inicio y fin de jornada, plazo legal de respuesta, muestra de calidad y garantía por defecto.
- **Función:** mantener las generalidades de la empresa y del sistema. Un área en uso no se puede deshabilitar; un especialista deshabilitado deja de recibir reclamos.
- **Salida:** la estructura que usa la asignación automática y los valores con los que el sistema calcula citas y plazos.

### Catálogo de Reclamos y Eventos — `03_..._1_tipos_de_reclamo.png`, `03_..._2_eventos.png`
- **Entrada:** tipo de problema, naturaleza (Reclamo o Queja) y estado.
- **Función:** mantener el único catálogo de tipos de problema del sistema y mostrar qué protocolos usan cada evento.
- **Salida:** los tipos habilitados que ofrece el formulario y su naturaleza.

### Catálogo de Producto — `04_..._1_productos.png`, `04_..._2_clasificacion.png`
- **Entrada:** código, bien, marca y modelo; la clase, familia y segmento salen de la clasificación.
- **Función:** registrar los productos que vende la empresa dentro de su jerarquía.
- **Salida:** catálogo de productos; el bien decide qué problemas aplican.

### Catálogo de Servicios — `05_catalogo_servicios.png`
- **Entrada:** tipo de servicio, servicio, tipos de problema que admite y estado.
- **Función:** mantener los servicios de la empresa y qué tipos de problema se pueden reclamar sobre cada uno.
- **Salida:** servicios habilitados; el formulario deja de ofrecer los deshabilitados al instante.

### Catálogo de Problemas — `06_catalogo_problemas.png`
- **Entrada:** tipo de problema, problema y a qué producto o servicio aplica.
- **Función:** registrar, reemplazar o quitar problemas y su relación con productos y servicios.
- **Salida:** relación problema ↔ producto/servicio que filtra las listas del formulario.

### Catálogo de Cliente — `07_catalogo_cliente.png`
- **Entrada:** tipos de cliente (Nuevo, Ordinario, Vip, SuperVip).
- **Función:** mostrar qué cambia para cada tipo: su plazo de atención según la prioridad. Las consideraciones de categorización quedan para el proceso batch BAT-03.
- **Salida:** tipo de cliente que decide prioridad y plazo.

### Catálogo de Protocolos — `08_catalogo_protocolos.png`
- **Entrada:** protocolo, evento, paso, acción, tiempo máximo, tipo de operario y criticidad.
- **Función:** definir los pasos de cada evento del protocolo.
- **Salida:** duración de cada cita (suma de los tiempos) y operario mínimo que pide cada evento; los usa la asignación.

### Catálogo de Reglas — `09_..._1` a `09_..._5`, `09_..._5b_simulador_evaluado.png`
- **Entrada:** condiciones (variable, operador, valor, salidas), regla de cada tipo de reclamo y datos de un caso.
- **Función:** armar la cadena de condiciones, decidir qué tipos pasan por el motor y desde qué condición, y probarla en el simulador.
- **Salida:** solución sugerida y la traza de cómo se llegó a ella.

### Catálogo de Políticas — `10_catalogo_politicas.png`
- **Entrada:** producto, tipo de garantía (Legal o Comercial), plazo en días y estado.
- **Función:** registrar las garantías de cada producto.
- **Salida:** plazo de garantía vigente (la mayor habilitada) que lee el motor de reglas.

### Parámetros de Atención — `11_..._1` a `11_..._8`
- **Entrada:** canales, locales, área por tipo de problema, tiempos de atención (SLA), regla de prioridad, instancias, feriados y estados.
- **Función:** configurar lo que usa la atención automática. El SLA se cuenta en tiempo laborable y nunca pasa el plazo legal.
- **Salida:** parámetros con los que el sistema asigna área y especialista, fija la cita y calcula el límite de atención.

## Online · Gerencial · Consulta

### Consulta de Reclamos — `12_consulta_de_reclamos.png`
- **Entrada:** selección de un reclamo.
- **Función:** rastrear estado, responsable, fecha programada, límite de atención, eventos y bitácora.
- **Salida:** ficha completa del reclamo (solo consulta).

### Consulta de Indicadores — `13_consulta_de_indicadores.png`
- **Entrada:** reclamos y movimientos registrados.
- **Función:** calcular los indicadores de gestión.
- **Salida:** satisfacción, % devueltos por calidad, carga por operario, reclamos por estado y por área.

### Parque de Tickets — `13b_parque_de_tickets.png`
- **Entrada:** operario y día.
- **Función:** mostrar los turnos de atención de cada operario y cuáles están tomados.
- **Salida:** agenda de tickets libres y ocupados.

## Online · Operativo · Área

### Data Entry · Atender Reclamo — `14_area_atender_reclamo.png`
- **Entrada:** ID de empleado y pasos ejecutados.
- **Función:** mostrar solo los eventos que el sistema le asignó, en el orden de su agenda, y registrar cada paso del protocolo.
- **Salida:** movimientos de solución con hora real; el siguiente evento se programa solo.

### Reportes · Reporte de Atención — `15_area_reporte_de_atencion.png`
- **Entrada:** ID de empleado.
- **Función:** resumir el avance de los reclamos asignados al usuario.
- **Salida:** reclamos asignados, abiertos, cerrados y fuera de plazo.

### Revisión de Calidad — `15b_area_revision_de_calidad.png`
- **Entrada:** ID de supervisor y decisión (aprobar o devolver con motivo).
- **Función:** revisar los observados por la revisión automática, una muestra al azar y los rechazos.
- **Salida:** reclamo aprobado (Cerrado) o devuelto (En atención).

## Online · Operativo · Cliente

### Data Entry · Formulario de Reclamo — `16_..._1_inicio`, `16_..._2_lleno`, `16_..._3_fecha_fija_de_atencion`, `16_..._4_validacion`
- **Entrada:** DNI/RUC, canal, local, producto o servicio comprado, tipo de problema, problema, detalle (qué pasó) y pedido del consumidor.
- **Función:** validar campos, compra, duplicados y garantía; clasificar como reclamo o queja; asignar área, especialista y fecha fija de atención.
- **Salida:** número de reclamo y el mensaje "Su reclamo será atendido el [día] a las [hora] por [área]".

### Reportes · Estado de Reclamo — `17_cliente_estado_de_reclamo.png`
- **Entrada:** DNI/RUC; impugnación o conformidad.
- **Función:** consultar los reclamos, su fecha fija de atención y su avance; impugnar o confirmar la solución.
- **Salida:** estado del reclamo, fecha programada y, al final, la conformidad con nota de 1 a 5.
