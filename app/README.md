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
     └ Perfiles y        ┌────────┴────────┐         ├ ACT-BD
       Accesos        ONLINE            BATCH        ├ MANT-BD
              ┌─────────┴─────────┐                  ├ ESTADISTICAS
          GERENCIAL           OPERATIVO              └ CONTINGENCIA
       ┌──────┴──────┐      ┌─────┴─────┐
  MANT-PARAM     CONSULTA  AREA      CLIENTE
  ├ Catálogo General  └ Indicadores  ├ Data Entry  ├ Data Entry
  ├ Catálogo de Reclamos             └ Reportes    └ Reportes
  ├ Catálogo de Productos
  ├ Catálogo de Bienes
  ├ Catálogo de Problemas
  ├ Catálogo de Clientes
  ├ Catálogo de Categorización
  ├ Catálogo de Protocolos
  ├ Catálogo de Reglas
  └ Catálogo de Políticas
```

Están **todos** los módulos de la arquitectura. Los que tienen pantalla son
botones; BATCH, ACT-BD, MANT-BD y CONTINGENCIA se dibujan con **borde
punteado**, porque forman parte del diseño pero ni el Excel ni el informe traen
contenido para ellos.

El diagrama se calcula solo a partir del árbol declarado en `MapaPantalla`: una
rama reparte a sus hijos en horizontal y se centra sobre ellos; un nodo cuyos
hijos son todos hojas las cuelga en vertical con una espina a la izquierda. Cada
caja mide lo que mide su rótulo, que es lo que permite que el diagrama entero
entre a lo ancho sin scroll.

## De dónde sale cada pantalla

| Pantalla | Qué hace | Origen |
|---|---|---|
| Seguridad · Perfiles y Accesos | Conceder accesos y permisos a cada perfil | hoja `SEGURIDAD` |
| Catálogo General | Áreas, ámbito, sectores, locales, idiomas y empleados | hojas `*Exis` / `*Hab`, `MantGeneral` |
| Catálogo de Reclamos | Tipos de reclamo con sus eventos de protocolo | `ReclamosHab`, `ReclamosDes`, `Protocolo` |
| Catálogo de Productos | Segmento › Familia › Clase › Bien | hoja `Producto` |
| Catálogo de Bienes | Tipos de bien y el problema que admiten | `MANT-PARAM`, `Bien`, `BienHab` |
| Catálogo de Problemas | Problemas por segmento, familia y tipo | `CatProblemas1`, `CatProblemas2` |
| Catálogo de Clientes | Tipos de cliente y sus consideraciones | `Clientes`, `Cons`, `Conshab` |
| Catálogo de Categorización | Condiciones, métodos y fórmulas del cálculo | `MANT-PARAM` |
| Catálogo de Protocolos | Parámetros de cada evento del protocolo | `Protocolo`, `MantProtocolo` |
| Catálogo de Reglas | Condiciones encadenadas del motor de decisión | hoja `MantReglas` |
| Catálogo de Políticas | Garantías legales, explícitas e implícitas | garantías de `Hoja1` |
| Indicadores | Resultados estadísticos del sistema | los 11 indicadores de `MANT-PARAM` |
| Área · Data Entry | Ingreso del usuario para atender la cola | informe, 2.1.2.1.1 |
| Área · Reportes | Avances y detalles realizados por el usuario | informe, Operativo → Área |
| Cliente · Data Entry | Validar documento, registrar, cuadro resumen | informe, 2.1.2.2.1 |
| Cliente · Reportes | Seguimiento del estado y del tiempo restante | informe, 2.1.2.2.2 |

Cliente · Reportes implementa los tres casos que describe el informe: reclamo
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
│                 Estado: lo que cambia durante la sesión, con oyentes
└── ui/
    ├── theme/    Tema: colores, tipografía y espaciado
    ├── components/  Grupo, Tabla, Formulario, PanelAncho y la fábrica Ui
    ├── screens/  Una clase por pantalla; MapaPantalla dibuja el árbol y
    │              TablaCatalogo se reutiliza en los catálogos tabulares
    ├── Navegacion      pila de navegación (ir / volver / inicio)
    └── VentanaPrincipal  ventana única con CardLayout
```
