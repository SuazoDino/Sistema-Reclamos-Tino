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
  ├ Catálogo de Producto               └ REPORTES
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
└── ui/
    ├── theme/    Tema: colores, tipografía y espaciado
    ├── components/  Grupo, Tabla, Formulario, PanelAncho, PanelBusqueda,
    │                PanelHabilitacion y la fábrica Ui
    ├── screens/  Una clase por pantalla; MapaPantalla dibuja el árbol y
    │              TablaCatalogo se reutiliza en los catálogos tabulares
    ├── Navegacion      pila de navegación (ir / volver / inicio)
    └── VentanaPrincipal  ventana única con CardLayout
```
