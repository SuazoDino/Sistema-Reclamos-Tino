"""Genera las figuras de cadena simple (3.2.1 Online).

Uso: python3 cadena.py <carpeta salida>
"""
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageChops

SALIDA = sys.argv[1]

CSS = """
body { margin: 0; padding: 24px 24px 24px 70px; background: #fff; font-family: Arial, Helvetica, sans-serif; position: relative; }
h3 { font-size: 15px; margin: 0 0 8px; }
table { border-collapse: collapse; font-size: 13px; margin-bottom: 34px; }
td, th { border: 1px solid #7f7f7f; padding: 5px 10px; text-align: center; height: 20px; white-space: nowrap; }
th { background: #4a86e8; color: #fff; }
tr.mio td { background: #ffff00; }
tr.otro td { background: #f4cccc; color: #555; }
tr.otro2 td { background: #d9ead3; color: #555; }
td.ptr { font-weight: bold; }
td.raiz { background: #ffd966; font-weight: bold; }
.nota { font-size: 12px; color: #444; margin-top: -24px; }
svg { position: absolute; left: 0; top: 0; pointer-events: none; }
"""

JS = """
const svg = document.querySelector('svg');
const body = document.body.getBoundingClientRect();
svg.setAttribute('width', document.body.scrollWidth + 40);
svg.setAttribute('height', document.body.scrollHeight + 20);
function c(el) { const r = el.getBoundingClientRect(); return {l: r.left, r: r.right, t: r.top, b: r.bottom, y: (r.top + r.bottom) / 2, x: (r.left + r.right) / 2}; }
function path(d) {
  const p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  p.setAttribute('d', d); p.setAttribute('fill', 'none'); p.setAttribute('stroke', '#1f5fbf');
  p.setAttribute('stroke-width', '3'); p.setAttribute('marker-end', 'url(#m)'); svg.appendChild(p);
}
// raiz -> primer elemento de la cadena: sale por la derecha y entra por la derecha de la fila
document.querySelectorAll('[data-raiz]').forEach(src => {
  const dst = document.getElementById(src.dataset.raiz);
  const a = c(src), b = c(dst.lastElementChild);
  const x = Math.max(a.r, b.r) + 36;
  path(`M ${a.r} ${a.y} H ${x} V ${b.y} H ${b.r + 4}`);
});
// puntero al anterior: curva por la izquierda de la tabla
document.querySelectorAll('[data-ant]').forEach(row => {
  const dst = document.getElementById(row.dataset.ant);
  if (!dst) return;
  const a = c(row.firstElementChild), b = c(dst.firstElementChild);
  const x = a.l - 40;
  path(`M ${a.l} ${a.y} C ${x} ${a.y}, ${x} ${b.y}, ${b.l - 4} ${b.y}`);
});
"""

DEFS = ("<svg><defs><marker id='m' viewBox='0 0 10 10' refX='9' refY='5' markerWidth='5' "
        "markerHeight='5' orient='auto'><path d='M0,0 L10,5 L0,10 z' fill='#1f5fbf'/></marker></defs></svg>")


def fila(cls, celdas, rid=None, ant=None, ptr_col=None):
    attrs = f" class='{cls}'"
    if rid:
        attrs += f" id='{rid}'"
    if ant:
        attrs += f" data-ant='{ant}'"
    tds = "".join(
        f"<td class='ptr'>{v}</td>" if i == ptr_col else f"<td>{v}</td>" for i, v in enumerate(celdas))
    return f"<tr{attrs}>{tds}</tr>"


def figura_reclamos():
    mae = ("<h3>Maestra Reclamos (raíz, indexada por CodReclamo)</h3><table>"
           "<tr><th>CodReclamo</th><th>CodCliente</th><th>CodCompra</th><th>CodEstado</th>"
           "<th>CodOperario</th><th>…</th><th>PtrUltMov</th></tr>"
           "<tr><td>RC-2026-0004</td><td>C0000431</td><td>0004521</td><td>Rechazado</td>"
           "<td>S01</td><td>…</td><td class='raiz' data-raiz='m999'>999</td></tr></table>")
    filas = [
        ("mio", ["001", "RC-2026-0004", "Registro", "Registrado", "SUG=No aplica; GAR=Dentro", "0"], "m001", None),
        ("otro", ["002", "RC-2026-0005", "Registro", "Registrado", "SUG=Reparación Equipo; GAR=Dentro", "0"], None, None),
        ("mio", ["100", "RC-2026-0004", "Programación", "Asignado", "Inspección 29/10 09:00–09:35 · E02", "001"], "m100", "m001"),
        ("otro2", ["101", "RC-2026-0007", "Programación", "Asignado", "Solución 29/10 09:20–10:40 · E05", "087"], None, None),
        ("mio", ["500", "RC-2026-0004", "Cambio de estado", "En revisión", "Rechazo propuesto: empaque abierto", "100"], "m500", "m100"),
        ("otro", ["501", "RC-2026-0005", "Programación", "Asignado", "Inspección 29/10 10:00–10:45 · E03", "002"], None, None),
        ("mio", ["999", "RC-2026-0004", "Revisión", "Rechazado", "Rechazo aprobado", "500"], "m999", "m500"),
    ]
    mov = ("<h3>Movimiento de Reclamos (relativa, acceso por PRR)</h3><table>"
           "<tr><th>NroMov (PRR)</th><th>CodReclamo</th><th>CodTipoMov</th><th>CodEstado</th>"
           "<th>Observacion / cita</th><th>PtrMovAnterior</th></tr>"
           + "".join(fila(c, v, r, a, 5) for c, v, r, a in filas) + "</table>")
    nota = ("<p class='nota'>Amarillo: los movimientos del reclamo RC-2026-0004. Rojo y verde: movimientos de "
            "otros reclamos, grabados entre medio. Las flechas siguen la cadena: PtrUltMov → 999 → 500 → 100 → 001 → 0.</p>")
    return mae + mov + nota


def figura_soluciones():
    mae = ("<h3>Maestra Reclamos (raíz, indexada por CodReclamo)</h3><table>"
           "<tr><th>CodReclamo</th><th>CodEstado</th><th>TotalPasos</th><th>PasosCompletados</th>"
           "<th>…</th><th>PtrUltPaso</th></tr>"
           "<tr><td>RC-2026-0007</td><td>En atención</td><td>4</td><td>3</td><td>…</td>"
           "<td class='raiz' data-raiz='s310'>310</td></tr></table>")
    filas = [
        ("mio", ["040", "101", "00012050", "1", "09:20", "09:22", "0"], "s040", None),
        ("otro", ["041", "100", "00012011", "1", "09:00", "09:03", "0"], None, None),
        ("mio", ["205", "101", "00012051", "2", "09:40", "09:51", "040"], "s205", "s040"),
        ("otro2", ["206", "501", "00012034", "1", "10:00", "10:02", "0"], None, None),
        ("mio", ["310", "101", "00012052", "3", "10:00", "10:08", "205"], "s310", "s205"),
    ]
    mov = ("<h3>Movimiento de Soluciones (relativa, acceso por PRR)</h3><table>"
           "<tr><th>NroMov (PRR)</th><th>NroMovReclamo</th><th>CodTicket</th><th>NumPaso</th>"
           "<th>FecPasoEstimado</th><th>FecPaso (real)</th><th>PtrMovAnterior</th></tr>"
           + "".join(fila(c, v, r, a, 6) for c, v, r, a in filas) + "</table>")
    nota = ("<p class='nota'>Amarillo: los pasos del reclamo RC-2026-0007 (su evento Solución es el movimiento 101). "
            "Las flechas siguen la cadena: PtrUltPaso → 310 → 205 → 040 → 0.</p>")
    return mae + mov + nota


def recortar(ruta):
    img = Image.open(ruta).convert("RGB")
    caja = ImageChops.difference(img, Image.new("RGB", img.size, (255, 255, 255))).getbbox()
    if caja:
        m = 20
        img.crop((max(caja[0] - m, 0), max(caja[1] - m, 0),
                  min(caja[2] + m, img.width), min(caja[3] + m, img.height))).save(ruta)


os.makedirs(SALIDA, exist_ok=True)
tmp = tempfile.mkdtemp()
for nombre, cuerpo in [("01_Cadena_Movimiento_de_Reclamos", figura_reclamos()),
                       ("02_Cadena_Movimiento_de_Soluciones", figura_soluciones())]:
    pagina = os.path.join(tmp, nombre + ".html")
    open(pagina, "w", encoding="utf-8").write(
        f"<!doctype html><meta charset='utf-8'><style>{CSS}</style>{DEFS}{cuerpo}<script>{JS}</script>")
    png = os.path.join(SALIDA, nombre + ".png")
    subprocess.run(["google-chrome", "--headless=new", "--disable-gpu", "--hide-scrollbars",
                    "--force-device-scale-factor=2", "--window-size=1300,900",
                    f"--screenshot={png}", "file://" + pagina],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    recortar(png)
    print(png)
