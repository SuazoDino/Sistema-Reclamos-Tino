// Genera el informe del Entregable 2 (módulo online) en Word.
// Uso: node informe.js <repo> <scratch> <salida.docx>
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, ImageRun, HeadingLevel, AlignmentType,
  Table, TableRow, TableCell, WidthType, BorderStyle, ShadingType, LevelFormat,
  TableOfContents, PageBreak, Footer, PageNumber,
} = require("docx");
const { imageSize } = require("./tam");

const [REPO, S, OUT] = process.argv.slice(2);
const T = (p) => path.join(REPO, "Teoria", p);
const IMG = (n) => path.join(S, "img", `i-${n}.png`);

const ANCHO_MAX = 600; // px a 96 dpi (≈ 6.25 in de ancho útil en A4)
const ALTO_MAX = 820;

// ---------- utilidades ----------
function runs(texto, base = {}) {
  // **negrita** y `código` -> TextRun
  const out = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let i = 0, m;
  while ((m = re.exec(texto))) {
    if (m.index > i) out.push(new TextRun({ text: texto.slice(i, m.index), ...base }));
    const s = m[0];
    if (s.startsWith("**")) out.push(new TextRun({ text: s.slice(2, -2), bold: true, ...base }));
    else out.push(new TextRun({ text: s.slice(1, -1), font: "Consolas", ...base }));
    i = m.index + s.length;
  }
  if (i < texto.length) out.push(new TextRun({ text: texto.slice(i), ...base }));
  return out;
}
const P = (texto, opt = {}) => new Paragraph({ children: runs(texto), spacing: { after: 120 }, ...opt });
const B = (texto, nivel = 0, keepNext = false) => new Paragraph({ children: runs(texto), numbering: { reference: "vinetas", level: nivel }, spacing: { after: 60 }, keepNext });
const H1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const H3 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(t)] });
const H4 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_4, children: [new TextRun(t)] });
const SALTO = () => new Paragraph({ children: [new PageBreak()] });

function imagen(ruta, anchoMax = ANCHO_MAX) {
  const { width, height } = imageSize(ruta);
  let w = Math.min(anchoMax, width), h = Math.round(height * w / width);
  if (h > ALTO_MAX) { h = ALTO_MAX; w = Math.round(width * h / height); }
  return new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 60, after: 160 },
    children: [new ImageRun({ type: "png", data: fs.readFileSync(ruta), transformation: { width: w, height: h } })],
  });
}
const rotulo = (t) => new Paragraph({ keepNext: true, spacing: { before: 200, after: 80 }, children: [new TextRun({ text: t, bold: true, font: "Times New Roman", size: 24 })] });

function ets(entrada, funcion, salida) {
  return [B(`**Entrada:** ${entrada}`, 0, true), B(`**Función:** ${funcion}`, 0, true), B(`**Salida:** ${salida}`, 0, true)];
}

const borde = { style: BorderStyle.SINGLE, size: 4, color: "7F7F7F" };
const bordes = { top: borde, bottom: borde, left: borde, right: borde };
function tabla(filas) {
  const n = filas[0].length, total = 9000;
  const w = Array(n).fill(Math.floor(total / n));
  w[n - 1] = total - w.slice(0, -1).reduce((a, b) => a + b, 0);
  return new Table({
    width: { size: total, type: WidthType.DXA }, columnWidths: w,
    rows: filas.map((f, r) => new TableRow({
      tableHeader: r === 0,
      children: f.map((c, k) => new TableCell({
        borders: bordes, width: { size: w[k], type: WidthType.DXA },
        shading: r === 0 ? { fill: "4A86E8", type: ShadingType.CLEAR, color: "auto" } : undefined,
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        children: [new Paragraph({ children: runs(c, r === 0 ? { bold: true, color: "FFFFFF", size: 18 } : { size: 18 }) })],
      })),
    })),
  });
}

// ---------- markdown de procesos -> párrafos ----------
const PANTALLAS = [
  [/ \(pantalla 16\)/g, " (2.5.1)"], [/ \(pantalla 14\)/g, " (2.4.1)"], [/ \(pantalla 15b\)/g, " (2.4.3)"],
  [/ \(pantalla 17\)/g, " (2.5.2)"], [/ \(pantallas 01 y 01b\)/g, " (2.1.1 y 2.1.2)"],
  [/ \(ver `[A-Z_]+\.md`\)/g, ""],
];
function procesos() {
  let md = fs.readFileSync(T("PROCESOS_ONLINE.md"), "utf8");
  md = md.slice(md.indexOf("## Criterios del libro"), md.indexOf("## Diferencias con el entregable 2"));
  for (const [a, b] of PANTALLAS) md = md.replace(a, b);
  const L = md.split("\n");
  const out = [];
  let i = 0;
  while (i < L.length) {
    const l = L[i];
    if (l.startsWith("## ")) { out.push(H4(l.slice(3).replace(/`/g, ""))); i++; continue; }
    if (l.startsWith("```")) {
      i++;
      while (i < L.length && !L[i].startsWith("```")) {
        out.push(new Paragraph({ spacing: { after: 0 }, shading: { fill: "F2F2F2", type: ShadingType.CLEAR, color: "auto" },
          children: [new TextRun({ text: L[i] || " ", font: "Consolas", size: 13 })] }));
        i++;
      }
      out.push(new Paragraph({ spacing: { after: 120 }, children: [] }));
      i++; continue;
    }
    if (l.startsWith("|")) {
      const filas = [];
      while (i < L.length && L[i].startsWith("|")) {
        const c = L[i].trim().replace(/^\||\|$/g, "").split("|").map((x) => x.trim());
        if (!c.every((x) => /^[-: ]*$/.test(x))) filas.push(c);
        i++;
      }
      out.push(tabla(filas));
      out.push(new Paragraph({ spacing: { after: 120 }, children: [] }));
      continue;
    }
    if (l.startsWith("Figuras de las dos cadenas")) {
      out.push(rotulo("Cadena simple: Maestra Reclamos → Movimiento de Reclamos"));
      out.push(imagen(T("procesos_png/01_Cadena_Movimiento_de_Reclamos.png")));
      out.push(rotulo("Cadena simple: Maestra Reclamos → Movimiento de Soluciones"));
      out.push(imagen(T("procesos_png/02_Cadena_Movimiento_de_Soluciones.png")));
      i++; continue;
    }
    if (/^\s*- /.test(l)) { out.push(B(l.replace(/^\s*- /, ""), /^\s{2,}-/.test(l) ? 1 : 0)); i++; continue; }
    if (l.trim() === "" || l.trim() === "---") { i++; continue; }
    out.push(P(l)); i++;
  }
  return out;
}

// ---------- tablas del diseño interno ----------
const TP = (d, f) => T(`tablas_png/${d}/${f}`);
const grupo = (titulo, d, items) => [H3(titulo), ...items.flatMap(([nombre, f]) => [rotulo(nombre), imagen(TP(d, f))])];

// ---------- documento ----------
const portada = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 400, after: 120 }, children: [new TextRun({ text: "Universidad Nacional de Ingeniería", bold: true, size: 40, font: "Times New Roman" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 300 }, children: [new TextRun({ text: "Facultad de Ingeniería Industrial y de Sistemas", size: 28, font: "Times New Roman" })] }),
  imagen(path.join(S, "logo.png"), 200),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 300, after: 120 }, children: [new TextRun({ text: "Diseño de Base de Datos", size: 32, font: "Times New Roman" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 }, children: [new TextRun({ text: "2do Entregable - Sistema de Reclamos", bold: true, size: 36, font: "Times New Roman" })] }),
  new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "Integrantes:", bold: true, size: 24 })] }),
  new Paragraph({ spacing: { after: 60 }, children: [new TextRun({ text: "20231324K\tSuazo Zanabria Dino Paul Roel", size: 24 })] }),
  new Paragraph({ spacing: { after: 300 }, children: [new TextRun({ text: "20231201F\tNavarro Palacios Ivan Alejandro", size: 24 })] }),
  new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: "Docente:", bold: true, size: 24 })] }),
  new Paragraph({ spacing: { after: 600 }, children: [new TextRun({ text: "DR. REYNA MONTEVERDE TINO EDUARDO", size: 24 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "2026", bold: true, size: 28 })] }),
  SALTO(),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 }, children: [new TextRun({ text: "ÍNDICE", bold: true, size: 32, font: "Times New Roman" })] }),
  new TableOfContents("Índice", { hyperlink: true, headingStyleRange: "1-3" }),
  SALTO(),
];

const arquitectonico = [
  H1("1. DISEÑO ARQUITECTÓNICO"),
  imagen(IMG("003-002")),
  P("Para el segundo entregable se presenta todo el módulo online:"),
  H2("1.1. Seguridad"),
  B("**Perfiles y Accesos:** Permite definir qué pantallas puede abrir cada perfil de usuario y qué permisos tiene."),
  B("**Diario de Operaciones:** Registra cada cambio a la configuración (aplicado o rechazado) con la foto del registro antes del cambio, y permite revertir la última operación."),
  H2("1.2. Gerencial — Mantenimiento de Parámetros"),
  B("**Parámetro General:** Permite ingresar y habilitar datos generales de la empresa (áreas y especialistas por área) y del sistema (jornada, plazo legal, muestra de calidad y garantía por defecto)."),
  B("**Catálogo de Reclamos y Eventos:** Permite ingresar y habilitar los tipos de reclamo (tipo de problema y naturaleza: Reclamo o Queja) y muestra los eventos del protocolo (Inspección, Solución, Seguimiento)."),
  B("**Catálogo de Producto:** Permite ingresar y habilitar segmentos, familias, clases, bienes y productos."),
  B("**Catálogo de Servicios:** Permite registrar los servicios de la empresa sobre los que se puede reclamar (delivery, cobro, instalación, etc.) y los tipos de problema que admite cada uno."),
  B("**Catálogo de Problemas:** Permite ingresar y habilitar tipos de problema y problemas específicos, y relacionar cada problema con los productos o servicios a los que aplica. El formulario del cliente usa esta relación para filtrar sus listas."),
  B("**Catálogo de Cliente:** Permite registrar los tipos de cliente (Nuevo, Ordinario, Vip, SuperVip) con su plazo de atención según la prioridad, y las consideraciones para categorizarlos."),
  B("**Catálogo de Protocolos:** Permite definir un protocolo por tipo de reclamo, formado por eventos. Cada evento tiene sus pasos, con tiempo máximo, tipo de empleado y criticidad. La duración de la cita es la suma de los tiempos de sus pasos."),
  B("**Catálogo de Reglas:** Permite ingresar las condiciones de cada regla, las variables que lee y las acciones con que termina, y asignar a cada tipo de reclamo su regla inicial. Incluye un simulador para probar la cadena. El motor sugiere automáticamente la solución de cada reclamo con los datos reales."),
  B("**Catálogo de Políticas:** Permite registrar las garantías (Legal y Comercial) de cada producto, con su plazo en días, y habilitarlas o deshabilitarlas."),
  B("**Parámetros de Atención:** Permite configurar lo que necesita la atención automática: canales, locales, área responsable por tipo de problema, tipos de problema permitidos por servicio, horario laboral y feriados, instancias y plazo de impugnación, tiempos de atención, prioridad por tipo de cliente y estados del reclamo."),
  H2("1.3. Consulta"),
  B("**Consulta de Reclamos:** Permite rastrear cada reclamo: estado, área y especialista asignados, fecha programada de atención (fija), límite de atención, garantía, solución sugerida, eventos del protocolo y bitácora completa. Es solo de consulta, porque el estado lo cambia el propio flujo."),
  B("**Consulta de Indicadores:** Muestra los indicadores del sistema con gráfico de barras o una línea mes a mes con proyección, incluidos la satisfacción del cliente y el porcentaje de reclamos devueltos por calidad."),
  B("**Parque de Tickets:** Muestra los turnos de atención de cada operario, libres y ocupados."),
  H2("1.4. Operativo"),
  P("**Área**"),
  B("**Data Entry (Atender Reclamo):** El especialista ingresa con su ID y ve solo los eventos que el sistema le asignó (por menor carga), en el orden de su agenda. Desde ahí inspecciona, ejecuta los pasos del protocolo y resuelve o rechaza. El siguiente evento se programa solo."),
  B("**Reportes (Reporte de Atención):** Muestra el avance y el detalle de los reclamos asignados al usuario del área."),
  B("**Revisión (Revisión de Calidad):** El supervisor revisa los reclamos observados por la revisión automática, una muestra al azar de los aprobados y todos los rechazos antes de avisar al cliente. Puede aprobar o devolver con un motivo (vuelve a En atención). El reclamo se cierra cuando la revisión está aprobada y el cliente da su conformidad."),
  P("**Cliente**"),
  B("**Data Entry (Formulario de Reclamo):** El cliente se valida con su DNI/RUC y registra su reclamo. Solo ve lo que compró o contrató, y el comprobante se llena solo. El sistema valida los campos, la compra, los duplicados y la garantía, y luego asigna el área, el especialista y una fecha y hora fija de atención."),
  B("**Reportes (Estado de Reclamo):** El cliente consulta sus reclamos, la fecha programada y el tiempo restante. Puede impugnar un reclamo rechazado dentro del plazo y, cuando su reclamo está en revisión, confirma si quedó solucionado y lo califica del 1 al 5."),
  SALTO(),
];

const externo = [
  H1("2. DISEÑO EXTERNO (PROTOTIPO)"),
  H2("2.1. Seguridad"),
  H3("2.1.1. Perfiles y Accesos"), ...ets("perfil, accesos y permisos.", "definir qué pantallas puede abrir cada perfil.", "perfiles con sus accesos asignados."), imagen(IMG("005-004")),
  H3("2.1.2. Diario de Operaciones"), ...ets("cambios hechos a la configuración.", "registrar cada operación (aplicada o rechazada) y revertir la última.", "historial de operaciones con su resultado."), imagen(IMG("005-003")),
  H2("2.2. Mantenimiento de Parámetros"),
  H3("2.2.1. Parámetro General"), ...ets("áreas, especialistas y parámetros del sistema (jornada, plazo legal, muestra de calidad, garantía por defecto).", "registrar las generalidades de la empresa y del sistema.", "áreas, especialistas y parámetros configurados."), imagen(IMG("006-005")), imagen(IMG("006-006")),
  H3("2.2.2. Catálogo de Reclamos y Eventos"), ...ets("tipo de problema, naturaleza (Reclamo/Queja) y estado.", "registrar los tipos de reclamo y mostrar sus eventos.", "tipos de reclamo y eventos sincronizados."), imagen(IMG("007-007")), imagen(IMG("007-008")),
  H3("2.2.3. Catálogo de Producto"), ...ets("código, bien, marca y modelo.", "registrar los productos clasificados en segmento, familia, clase y bien.", "productos clasificados."), imagen(IMG("008-010")), imagen(IMG("008-009")),
  H3("2.2.4. Catálogo de Servicios"), ...ets("tipo de servicio, servicio y tipos de problema que admite.", "registrar los servicios y los problemas que se pueden reclamar sobre cada uno.", "servicios registrados y habilitados."), imagen(IMG("009-011")),
  H3("2.2.5. Catálogo de Problemas"), ...ets("tipo de problema, problema y a qué producto o servicio aplica.", "registrar los problemas y sincronizarlos con productos y servicios.", "problemas registrados y sincronizados."), imagen(IMG("010-012")),
  H3("2.2.6. Catálogo de Cliente"), ...ets("tipos de cliente (Nuevo, Ordinario, Vip, SuperVip) y su plazo de atención por prioridad.", "asociar cada tipo de cliente con su plazo de atención.", "tipos de cliente sincronizados con su prioridad y plazo."), imagen(IMG("010-013")),
  H3("2.2.7. Catálogo de Protocolos"), ...ets("protocolo, evento, paso, acción, tiempo, tipo de empleado y criticidad.", "registrar los pasos de cada evento del protocolo de solución.", "protocolos registrados con la duración de cada cita."), imagen(T("prototipos_web/08_catalogo_protocolos.png")),
  H3("2.2.8. Catálogo de Reglas"), ...ets("condiciones, variables y la regla de cada tipo de reclamo.", "armar la cadena de condiciones que decide la solución sugerida y probarla en el simulador.", "reglas registradas y solución sugerida."), imagen(IMG("012-015")), imagen(IMG("012-016")),
  H3("2.2.9. Catálogo de Políticas"), ...ets("producto, tipo de garantía (Legal o Comercial) y plazo en días.", "registrar las garantías de cada producto.", "políticas de garantía registradas."), imagen(IMG("013-017")),
  H3("2.2.10. Parámetros de Atención"), ...ets("canales, locales, áreas por tipo de problema, tiempos de atención, prioridad, instancias, feriados y estados.", "configurar los parámetros que usa la atención automática.", "parámetros de atención registrados."), imagen(IMG("014-018")), imagen(IMG("014-019")),
  H2("2.3. Consulta"),
  H3("2.3.1. Consulta de Reclamos"), ...ets("reclamo seleccionado.", "consultar el estado, responsable, fecha programada y eventos del reclamo.", "ficha del reclamo."), imagen(IMG("015-021")), imagen(IMG("015-020")),
  H3("2.3.2. Consulta de Indicadores"), ...ets("reclamos y movimientos registrados.", "calcular los indicadores de gestión.", "indicadores de satisfacción, carga por operario y reclamos por estado y área."), imagen(IMG("016-022")),
  H3("2.3.3. Parque de Tickets"), ...ets("operario y día.", "mostrar los turnos de atención de cada operario.", "tickets libres y ocupados."), imagen(IMG("016-023")),
  H2("2.4. Área"),
  H3("2.4.1. Data Entry — Atender Reclamo"), ...ets("ID de empleado y pasos ejecutados.", "atender los eventos asignados siguiendo los pasos del protocolo.", "pasos registrados y siguiente evento programado."), imagen(IMG("017-024")),
  H3("2.4.2. Reporte de Atención"), ...ets("ID de empleado.", "resumir el avance de los reclamos asignados.", "reclamos asignados, abiertos, cerrados y fuera de plazo."), imagen(IMG("018-025")),
  H3("2.4.3. Revisión de Calidad"), ...ets("ID de supervisor y decisión.", "revisar los reclamos observados, una muestra al azar y los rechazados.", "reclamo aprobado o devuelto."), imagen(IMG("018-026")),
  H2("2.5. Cliente"),
  H3("2.5.1. Data Entry — Formulario de Reclamo"), ...ets("datos personales del cliente y datos sobre el reclamo.", "validar y registrar el reclamo, y asignarle área, especialista y fecha de atención.", "reclamo registrado con su fecha de atención."), imagen(IMG("019-027")),
  H3("2.5.2. Reporte — Estado de Reclamo"), ...ets("DNI/RUC del cliente.", "consultar el estado del reclamo e impugnar o confirmar la solución.", "estado del reclamo y conformidad del cliente."), imagen(IMG("020-028")),
  SALTO(),
];

const interno = [
  H1("3. DISEÑO INTERNO"),
  H2("3.1. Base de datos"),
  P("Cálculos que se aplican a todas las tablas (libro del curso):"),
  B("**IC:** `#RL = (IC − 4) / (RL + 3)`, `r = (IC − 4) − #RL·(RL + 3)`; se elige el IC de menor residuo en la terna online (512, 1024, 1536) o batch (3072, 3584, 4096). En longitud variable se usa la MODA. Si toda la tabla cabe en un IC de 512, se usa 512."),
  B("**Capacidad = (N, AC):** N es la cantidad estimada de registros y AC es un cilindro expresado en registros: `AC = 19 × (IC por pista) × (RL por IC)`, con una pista de 3 × 4096 bytes."),
  B("**Clave = (LONG, UBICAC):** longitud de la clave y su posición dentro del registro. La clave alterna es el índice por otro atributo."),
  ...grupo("3.1.1. Tablas Maestras", "1_Maestras", [["Maestra Cliente", "01_Maestra_Cliente.png"], ["Maestra Operarios", "02_Maestra_Operarios.png"], ["Maestra Reclamos", "03_Maestra_Reclamos.png"]]),
  ...grupo("3.1.2. Tablas de Movimientos", "2_Movimientos", [["Movimiento de Reclamos", "01_Movimiento_de_Reclamos.png"], ["Movimiento de Soluciones", "02_Movimiento_de_Soluciones.png"]]),
  ...grupo("3.1.3. Tablas de Catálogos", "3_Catalogos", [
    ["Catálogo de Reclamo", "01_Catalogo_de_Reclamo.png"], ["Catálogo de Problemas", "02_Catalogo_de_Problemas.png"],
    ["Catálogo de Protocolos", "03_Catalogo_de_Protocolos.png"], ["Catálogo de Pasos", "04_Catalogo_de_Pasos.png"],
    ["Catálogo de Productos", "05_Catalogo_de_Productos.png"], ["Catálogo de Servicios", "06_Catalogo_de_Servicios.png"],
    ["Catálogo de Reglas", "07_Catalogo_de_Reglas.png"], ["Catálogo de Áreas", "08_Catalogo_de_Areas.png"],
    ["Catálogo de Locales", "09_Catalogo_de_Locales.png"], ["Catálogo de Clasificación", "10_Catalogo_de_Clasificacion.png"],
    ["Catálogo de Cliente", "11_Catalogo_de_Cliente.png"]]),
  ...grupo("3.1.4. Parque de Ticket", "5_Parque_de_Ticket", [["Parque de Ticket", "01_Parque_de_Ticket.png"]]),
  ...grupo("3.1.5. Tabla de Compras", "7_Tabla_de_Compras", [["Tabla de Compras", "01_Tabla_de_Compras.png"]]),
  ...grupo("3.1.6. Tabla de Perfiles", "8_Seguridad", [["Tabla de Perfiles", "01_Tabla_de_Perfiles.png"]]),
  ...grupo("3.1.7. Diario de Operaciones", "8_Seguridad", [["Diario de Operaciones", "02_Diario_de_Operaciones.png"]]),
  ...grupo("3.1.8. Listas Invertidas", "9_Listas_Invertidas", [["Consulta Reclamos Cliente", "01_Consulta_Reclamos_Cliente.png"]]),
  SALTO(),
  H2("3.2. Diseño de Procesos, Programas y Rutinas"),
  H3("3.2.1. Online"),
  ...procesos(),
];

const doc = new Document({
  features: { updateFields: true },
  styles: {
    default: { document: { run: { font: "Arial", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: "Times New Roman", size: 32, bold: true }, paragraph: { spacing: { before: 240, after: 160 }, keepNext: true, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: "Times New Roman", size: 28, bold: true }, paragraph: { spacing: { before: 240, after: 120 }, keepNext: true, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: "Times New Roman", size: 26, bold: true }, paragraph: { spacing: { before: 200, after: 100 }, keepNext: true, outlineLevel: 2 } },
      { id: "Heading4", name: "Heading 4", basedOn: "Normal", next: "Normal", quickFormat: true, run: { font: "Times New Roman", size: 24, bold: true }, paragraph: { spacing: { before: 200, after: 100 }, keepNext: true, outlineLevel: 3 } },
    ],
  },
  numbering: { config: [{ reference: "vinetas", levels: [
    { level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
    { level: 1, format: LevelFormat.BULLET, text: "◦", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1440, hanging: 360 } } } },
  ] }] },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1300, bottom: 1300, left: 1300, right: 1300 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.RIGHT, children: [new TextRun({ children: [PageNumber.CURRENT], size: 18 })] })] }) },
    children: [...portada, ...arquitectonico, ...externo, ...interno],
  }],
});

Packer.toBuffer(doc).then((buf) => { fs.writeFileSync(OUT, buf); console.log("ok", OUT); });
