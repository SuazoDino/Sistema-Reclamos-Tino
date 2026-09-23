# Sistema de Reclamos — prototipo de diseño externo

Aplicación de escritorio en Java que lleva a pantallas reales el diseño externo
documentado en `Teoria/TinoPrototipos_menu_con_iconos.xlsm`. No persiste datos: todo
vive en memoria durante la sesión, igual que un prototipo navegable.

## Requisitos

- JDK 21 o superior
- Maven 3.8 o superior
- Un entorno gráfico (X11 o Wayland con XWayland)

## Cómo ejecutarlo

```bash
cd app
mvn compile exec:java          # arranca en modo claro
mvn compile exec:java -Dexec.args="--oscuro"
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

La aplicación abre en el **mapa de módulos**: el mismo diagrama de *módulos
típicos* del diseño arquitectónico, con las cajas en las posiciones que tienen
en la hoja de Excel. Cada caja es un botón.

```
Mapa de módulos
   └─ menú del módulo        (Aplicativo, Técnico, Operativo, Gerencial, Área,
                              Cliente, Mant-Param, Seguridad)
         └─ catálogo o pantalla de trabajo
```

Toda pantalla que no sea el mapa lleva arriba **Volver** (deshace un paso) y
**Mapa de módulos** (vuelve al inicio). El menú lateral queda como atajo: hace
lo mismo, en un solo clic.

## Qué hay en cada pantalla

| Módulo | Pantalla | Origen en el prototipo |
|---|---|---|
| — | Mapa de módulos | hoja `DISEÑO ARQUITECTONICO` (`xl/drawings/drawing2.xml`) |
| Aplicativo · Gerencial | Tablero de control | indicadores de la hoja `MANT-PARAM` |
| Aplicativo · Data Entry | Registro de reclamo | hoja `DATA_ENTRY_CLIENTE` |
| Aplicativo · On-Line | Atención de reclamos | cola de trabajo y asignación de área |
| Aplicativo · Consulta | Consulta de reclamos | seguimiento por estado y área |
| Aplicativo · Batch | Procesos batch | procesos programados |
| Mant-Param | Catálogo de productos | hoja `Producto` (Segmento › Familia › Clase › Bien) |
| Mant-Param | Catálogo de bienes | hoja `MANT-PARAM`, bloque tipo de bien |
| Mant-Param | Catálogo de problemas | hojas `CatProblemas1` / `CatProblemas2` |
| Mant-Param | Catálogo de protocolos | hojas `Protocolo` y `MantProtocolo` |
| Mant-Param | Catálogo de reglas | hoja `MantReglas` |
| Mant-Param | Catálogo de políticas | garantías de la hoja `Hoja1` |
| Mant-Param | Categorización de cliente | condiciones, métodos y fórmulas de `MANT-PARAM` |
| Mant-Param | Catálogos generales | hojas `*Exis` / `*Hab` (existentes ↔ habilitados) |
| Gerencial | Indicadores | los 11 indicadores del prototipo |
| Gerencial | Reportes | se arman con los reclamos de la sesión |
| Técnico | Act-BD, Mant-BD, Contingencia | procesos de plataforma |
| Seguridad | Perfiles y accesos | hoja `SEGURIDAD` |

## Organización del código

```
pe.tino.reclamos
├── app/          Main: instala el tema y abre el mapa de módulos
├── model/        Modelo: entidades del dominio (records inmutables)
├── repo/         Datos: catálogos sembrados desde el .xlsm
│                 Estado: lo que cambia durante la sesión, con oyentes
└── ui/
    ├── theme/    Tema (colores, tipografía, espaciado) e Iconos (vectoriales)
    ├── components/  Piezas reutilizables: Tarjeta, Tabla, Formulario, Ficha,
    │                GraficoBarras, PanelAncho, Ui (fábrica de controles)
    ├── screens/  Una clase por pantalla, todas sobre la base Pantalla.
    │              MapaPantalla dibuja el diagrama; MenuPantalla arma los
    │              menús de módulo; CatalogoTablaPantalla y ProcesosPantalla
    │              se reutilizan para varios catálogos
    ├── Navegacion      pila de navegación (ir / volver / inicio)
    ├── BarraLateral    atajo lateral a las pantallas
    └── VentanaPrincipal  armazón con CardLayout
```

Reglas que sigue el código:

- **Ningún color suelto.** Todo sale de `Tema`; cambiar la paleta es tocar un
  solo archivo. El botón de la esquina inferior derecha alterna claro/oscuro.
- **Ningún archivo de imagen.** Los iconos son trazos Java2D sobre una grilla
  de 24×24 (`Iconos`), así escalan y toman el color del contexto.
- **Los gráficos usan una sola serie y un solo tono**, con el valor rotulado al
  final de cada barra; el color nunca carga solo el significado. Los distintivos
  de estado siempre llevan punto **y** texto.
