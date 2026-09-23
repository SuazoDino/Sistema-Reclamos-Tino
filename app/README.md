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

## Qué hay en cada pantalla

El menú lateral reproduce los módulos típicos del diseño arquitectónico:

| Módulo | Pantalla | Origen en el prototipo |
|---|---|---|
| Aplicativo · Gerencial | Tablero de control | indicadores de la hoja `MANT-PARAM` |
| Aplicativo · Data Entry | Registro de reclamo | hoja `DATA_ENTRY_CLIENTE` |
| Aplicativo · On-Line | Atención de reclamos | cola de trabajo y asignación de área |
| Aplicativo · Consulta | Consulta de reclamos | seguimiento por estado y área |
| Mant-Param | Catálogos | hojas `*Exis` / `*Hab` (patrón existentes ↔ habilitados) |
| Mant-Param | Catálogo de productos | hoja `Producto` (Segmento › Familia › Clase › Bien) |
| Mant-Param | Protocolos | hojas `Protocolo` y `MantProtocolo` |
| Mant-Param | Reglas de negocio | hoja `MantReglas` |
| Gerencial | Indicadores | los 11 indicadores del prototipo |
| Seguridad | Perfiles y accesos | hoja `SEGURIDAD` |

## Organización del código

```
pe.tino.reclamos
├── app/          Main: instala el tema y abre la ventana
├── model/        Modelo: entidades del dominio (records inmutables)
├── repo/         Datos: catálogos sembrados desde el .xlsm
│                 Estado: lo que cambia durante la sesión, con oyentes
└── ui/
    ├── theme/    Tema (colores, tipografía, espaciado) e Iconos (vectoriales)
    ├── components/  Piezas reutilizables: Tarjeta, Tabla, Formulario, Ficha,
    │                GraficoBarras, PanelAncho, Ui (fábrica de controles)
    ├── screens/  Una clase por pantalla, todas sobre la base Pantalla
    ├── BarraLateral    menú de módulos
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
