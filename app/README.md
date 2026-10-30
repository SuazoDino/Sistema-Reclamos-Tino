# Sistema de Reclamos — prototipo de diseño externo

Aplicación de escritorio en Java que lleva a pantallas reales el diseño externo
del **1er Entregable**, tal como lo documentan `Teoria/entregable 1 sistema de
reclamos.docx.pdf` y `Teoria/TinoPrototipos_menu_con_iconos.xlsm`.

No persiste datos: todo vive en memoria durante la sesión, igual que un
prototipo navegable.

## Requisitos

- JDK 21 o superior
- Maven 3.8 o superior
- Un entorno gráfico (X11 o Wayland con XWayland)

## Cómo ejecutarlo

```bash
cd app
mvn compile exec:java
```

O generando un JAR autocontenido:

```bash
mvn package
java -jar target/sistema-reclamos.jar
```

Las pruebas:

```bash
mvn test
```

## Versión web

La misma lógica (validación, asignación, cita y control de calidad) también
se sirve como página web, con HTML, CSS y JavaScript y un servidor Java sin
dependencias extra:

```bash
cd app
mvn compile exec:java -Dmain.class=pe.tino.reclamos.web.ServidorWeb
```

Luego abrir http://127.0.0.1:8080 (con `localhost` algunos navegadores van por IPv6 y no llegan). Las páginas están en
`src/main/resources/web/` y la API en `pe.tino.reclamos.web.ServidorWeb`.
Por ahora los datos viven en memoria, igual que en la versión de escritorio.

## Abrirlo en IntelliJ IDEA

`File → Open` y seleccionar la carpeta `app`. IntelliJ reconoce el `pom.xml` y
configura el módulo solo. La clase de arranque es `pe.tino.reclamos.app.Main`.

## Cómo se navega

La aplicación abre en el **mapa conceptual**: el diagrama de módulos típicos
del diseño arquitectónico, dibujado como organigrama con la raíz arriba y las
ramas abriéndose hacia abajo. No hay barra lateral ni menús: se entra por el
mapa y se vuelve con **Volver** o **Mapa**.

```
                   SISTEMA DE RECLAMOS
                            │
                       SEGURIDAD
              ┌─────────────┴─────────────┐
           ONLINE                       BATCH
      ┌───────┴───────┐           ┌───────┴───────┐
  GERENCIAL       OPERATIVO   APLICATIVO       TECNICO
      │          ┌────┴────┐      ├ ACT-BD       ├ MANT-BD
  MANT-PARAM   ÁREA     CLIENTE   └ ESTADISTICAS └ CONTINGENCIA
  ├ Parámetro General
  ├ Catálogo de Reclamo        ÁREA    ├ DATA ENTRY
  ├ Catálogo de Producto               ├ REPORTES
  │                                    └ REVISIÓN
  ├ Catálogo de Problemas
  ├ Catálogo de Cliente        CLIENTE ├ DATA ENTRY
  ├ Catálogo de Protocolos             └ REPORTES
  ├ Catálogo de Reglas
  └ Catálogo de Políticas
  CONSULTA
  └ Consulta de Indicadores
```

El mapa reproduce el diagrama de `Teoria/image.png`: la misma jerarquía, los
mismos rótulos y los mismos colores — azul para los módulos, amarillo para los
submódulos y rosa para las pantallas. Las cajas rosadas se pulsan para entrar;
SEGURIDAD también, porque la hoja `SEGURIDAD` del Excel sí tiene contenido.
ACT-BD, ESTADISTICAS, MANT-BD y CONTINGENCIA no se pulsan: están en el diagrama
pero ninguna de las dos fuentes trae contenido para ellos.

El diagrama se calcula solo a partir del árbol declarado en `MapaPantalla`: un
nodo en fila reparte a sus hijos en horizontal y se centra sobre ellos; un nodo
apilado los cuelga en vertical con una espina a la izquierda. Cada caja mide lo
que mide su rótulo, así el diagrama entero entra sin scroll.

## De dónde sale cada pantalla

| Pantalla | Cómo es en el prototipo | Captura |
|---|---|---|
| Seguridad · Perfiles y Accesos | Perfiles, sus 11 accesos y sus 2 permisos | hoja `SEGURIDAD` |
| General | Cuatro bloques: Áreas, Especialistas por Área, Magnitud e Idioma | p. 5 |
| Catálogo de Reclamos y Eventos | Pestañas Reclamo y Eventos, cada una con búsqueda, resultados y bloque Agregar | p. 6 |
| Producto | Búsqueda Segmento/Familia/Clase y pestañas Agregar Familia / Clase / Producto | p. 7 |
| CatalogoProblemas | Búsqueda por tipo, pestañas Agregar Problemas y Agregar Producto | p. 7–8 |
| Catalogo Clientes | Pestañas Agregar Tipo Cliente y Asignar Condiciones | p. 8 |
| Catálogo de Protocolos | Búsqueda, evento y la lista de acciones con tiempo y operario | p. 9 |
| Catalogo de reglas | Pestañas Regla, Condiciones y Asignar Regla, con la ventana Agregar/Modificar | p. 10 |
| Catálogo de Políticas | Una política cruza dos catálogos; debajo se habilitan sus tipos | p. 11–12 |
| Consulta de Indicadores | Lista de los 11 indicadores con Detalle y Salir; Detalle grafica el indicador (barras, o tendencia mensual con proyeccion) | p. 13 |
| Atender Reclamo | Pide el ID de empleado, lista sus reclamos y abre Inspeccionar Producto | p. 13–15 |
| Reporte de Atención | Avances del usuario sobre los reclamos registrados | p. 13 |
| Formulario Reclamo | Datos personales con Validar, luego compras y reclamos a registrar | p. 15–17 |
| Estado de Reclamo | Reclamos del cliente y ventana Detalle del Reclamo | p. 17–19 |

Las pantallas siguen las capturas del informe: mismos títulos de ventana,
mismos grupos, mismas pestañas y mismos botones. Los formularios que en el
prototipo son ventanas modales (Ingresar Datos, Inspeccionar Producto,
Validación de Usuario, Agregar/Modificar Regla, Detalle del Reclamo) aquí
también son diálogos.

Estado de Reclamo implementa los tres casos que describe el informe: reclamo
rechazado dentro del plazo de impugnación (con formulario para enviar a otra
instancia), rechazado fuera del plazo (advertencia) y reclamo pendiente.

## Flujo del reclamo

El sistema es automático: ninguna decisión del flujo depende de que una
persona la elija a mano. Une el modelo del entregable 2 (maestra de
reclamos, parque de tickets, movimientos) con las reglas de este proyecto
(validación, prioridad, motor de reglas, control de calidad).

```
Reclamo ──► Protocolo ──► Eventos ──► Pasos
el caso      uno por tipo   Inspección,   acción, tiempo,
             de reclamo     Solución...   operario, crítico
                               │
                               ├─ toma Tickets del parque (turnos de 10 min del operario)
                               └─ cada paso ejecutado = Movimiento de Solución
                                  (hora estimada y hora real)
```

1. **Registro y validación.** Nada viene preseleccionado; el cliente solo ve
   lo que compró. Se revisa la compra (salvo Atención), los duplicados y la
   garantía. El motor de reglas sugiere la solución con los datos reales.
2. **Protocolo.** El tipo de problema da el protocolo (PR-FUNC, PR-ENTR...),
   el área y la prioridad según el tipo de cliente.
3. **Ticket.** El batch genera el parque de tickets de cada operario (lunes a
   sábado de 9:00 a 18:00, sin feriados). Cada evento toma los tickets libres
   y seguidos que necesita, del operario de menor carga, antes del límite de
   atención. Si nadie llega a tiempo, igual se agenda y queda marcado
   *Sin capacidad dentro del plazo*.
4. **Atención.** El operario ejecuta los pasos uno por uno; cada paso queda
   como Movimiento de Solución. Al completar un evento, el siguiente toma sus
   tickets solo. El reclamo lleva pasos completados sobre el total.
5. **Control de calidad.** Revisión automática (pasos críticos, plazo,
   solución sugerida); el supervisor (S01) revisa observados, una muestra al
   azar y los rechazos; el cliente confirma si quedó solucionado.

Recorrido de demo: Alexander (DNI 72119474) reclama "La cámara no funciona"
del Samsung Galaxy A54 → se asigna solo a E01 → E01 inspecciona y resuelve →
S01 o la revisión automática aprueba → Alexander confirma en Estado de
Reclamo → Cerrado.

## Criterios que sigue el código

- **Nada inventado.** Todos los datos salen del Excel o del informe. En
  Seguridad solo viene sembrada la asignación del perfil gerencial, que es la
  única que la hoja documenta; el resto se asigna desde la pantalla. Los
  indicadores que el prototipo no puede calcular con los reclamos registrados
  se marcan como *no calculables* en vez de mostrar un número inventado. El
  plazo de impugnación es un parámetro visible en pantalla, no una constante
  escondida.
- **Aspecto de escritorio.** Paneles con borde y título, tablas con rejilla,
  esquinas rectas, sin iconos ni colores de marca. Todo el aspecto está en
  `Tema`.
- **Sin persistencia.** `Estado` guarda lo que cambia durante la sesión y avisa
  a las pantallas cuando un reclamo se agrega o se modifica.

## Organización del código

```
pe.tino.reclamos
├── app/          Main: instala el tema y abre el mapa
├── model/        Modelo: entidades del dominio (records inmutables)
├── repo/         Datos: catálogos sembrados desde el Excel y el informe
│                 Prototipo: los datos que muestran las capturas del informe
│                 Estado: lo que cambia durante la sesión, con oyentes
│                 Reclamos, Validacion, Asignacion, ParqueTickets, Atencion,
│                 Calidad: el flujo del reclamo, sin depender de la interfaz
└── ui/
    ├── theme/    Tema: colores, tipografía y espaciado
    ├── components/  Grupo, Tabla, Formulario, PanelAncho, PanelBusqueda,
    │                PanelHabilitacion y la fábrica Ui
    ├── screens/  Una clase por pantalla; MapaPantalla dibuja el árbol y
    │              TablaCatalogo se reutiliza en los catálogos tabulares
    ├── Navegacion      pila de navegación (ir / volver / inicio)
    └── VentanaPrincipal  ventana única con CardLayout
```
