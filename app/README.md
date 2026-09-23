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
          ┌──────────────────────┼──────────────────────┐
     SEGURIDAD               APLICATIVO              TECNICO
                        ┌────────┴────────┐         ├ ACT-BD
                     ONLINE            BATCH        ├ MANT-BD
              ┌─────────┴─────────┐                 ├ ESTADISTICAS
          GERENCIAL           OPERATIVO             └ CONTINGENCIA
       ┌──────┴──────┐      ┌─────┴─────┐
  MANT-PARAM     CONSULTA  AREA      CLIENTE
  ├ Parámetros      └ Indicadores  ├ Data Entry  ├ Data Entry
  ├ Reclamos y Eventos            └ Reportes     └ Reportes
  ├ Productos
  ├ Problemas
  ├ Clientes
  ├ Protocolos
  ├ Reglas
  └ Políticas
```

Están **todos** los módulos de la arquitectura. Los que desarrolla el 1er
entregable son botones y llevan a su pantalla; SEGURIDAD, BATCH, ACT-BD,
MANT-BD, ESTADISTICAS y CONTINGENCIA se dibujan con **borde punteado**, porque
forman parte del diseño pero todavía no del prototipo.

El diagrama se calcula solo a partir del árbol declarado en `MapaPantalla`: una
rama reparte a sus hijos en horizontal y se centra sobre ellos; un nodo cuyos
hijos son todos hojas las cuelga en vertical con una espina a la izquierda. Cada
caja mide lo que mide su rótulo, que es lo que permite que el diagrama entero
entre a lo ancho sin scroll.

## De dónde sale cada pantalla

| Pantalla | Entrada / Función / Salida | Origen |
|---|---|---|
| Parámetros Generales | Registrar generalidades de la empresa y del sistema | hojas `*Exis` / `*Hab` |
| Cat. de Reclamos y Eventos | Registrar los reclamos con sus respectivos eventos | `ReclamosHab`, `ReclamosDes`, `Protocolo` |
| Cat. de Productos | Registrar productos por segmento, familia y clase | hoja `Producto` |
| Cat. de Problemas | Registrar problemas y sincronizarlos con tipo y producto | `CatProblemas1`, `CatProblemas2` |
| Cat. de Clientes | Sincronizar las condiciones con los tipos de cliente | `Clientes`, `Cons`, `MANT-PARAM` |
| Cat. de Protocolos | Registrar los parámetros del protocolo de solución | `Protocolo`, `MantProtocolo` |
| Cat. de Reglas | Establecer reglas y registrar sus parámetros | hoja `MantReglas` |
| Cat. de Políticas | Establecer políticas y sus tipos | garantías de la hoja `Hoja1` |
| Indicadores | Resultados estadísticos del sistema de reclamos | los 11 indicadores de `MANT-PARAM` |
| Área · Data Entry | Ingreso del usuario para atender los reclamos en cola | informe, 2.1.2.1.1 |
| Área · Reportes | Avances y detalles realizados por el usuario | informe, Operativo → Área |
| Cliente · Data Entry | Validar documento, registrar reclamo, cuadro resumen | informe, 2.1.2.2.1 |
| Cliente · Reportes | Seguimiento del estado y del tiempo restante | informe, 2.1.2.2.2 |

Cliente · Reportes implementa los tres casos que describe el informe: reclamo
rechazado dentro del plazo de impugnación (con formulario para enviar a otra
instancia), rechazado fuera del plazo (advertencia) y reclamo pendiente.

## Criterios que sigue el código

- **Nada inventado.** Todos los datos salen del Excel o del informe. Los
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
│                 Estado: lo que cambia durante la sesión, con oyentes
└── ui/
    ├── theme/    Tema: colores, tipografía y espaciado
    ├── components/  Grupo, Tabla, Formulario, PanelAncho y la fábrica Ui
    ├── screens/  Una clase por pantalla; MapaPantalla dibuja el árbol y
    │              TablaCatalogo se reutiliza en los catálogos tabulares
    ├── Navegacion      pila de navegación (ir / volver / inicio)
    └── VentanaPrincipal  ventana única con CardLayout
```
