import re, sys
from pathlib import Path
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Border, Side, Alignment

T = Path(sys.argv[1])
OUT = Path(sys.argv[2])

HOJAS = [
    ("Maestras", "TABLAS MAESTRAS", ["TABLAS_MAESTRAS.md"]),
    ("Movimientos", "TABLAS DE MOVIMIENTOS", ["TABLAS_MOVIMIENTOS.md"]),
    ("Catalogos", "TABLAS DE CATÁLOGOS", ["TABLAS_CATALOGOS.md"]),
    ("Parque de Ticket", "PARQUE DE TICKET", ["PARQUE_DE_TICKET.md"]),
    ("Compras", "TABLA DE COMPRAS", ["TABLA_COMPRAS.md"]),
    ("Seguridad", "SEGURIDAD", ["TABLAS_SEGURIDAD.md"]),
    ("Listas invertidas", "LISTAS INVERTIDAS", ["LISTAS_INVERTIDAS.md"]),
]

def clean(s):
    s = s.strip().replace("**", "").replace("`", "")
    return s

def num(s):
    try:
        f = float(s)
        return int(f) if f.is_integer() else f
    except ValueError:
        return s

def rows_of(lines, i):
    out = []
    while i < len(lines) and lines[i].startswith("|"):
        cells = [clean(c) for c in lines[i].strip().strip("|").split("|")]
        if not all(set(c) <= set("-: ") for c in cells):
            out.append(cells)
        i += 1
    return out, i

def tablas(md):
    lines = md.read_text(encoding="utf-8").splitlines()
    res, ics, i = [], [], 0
    while i < len(lines):
        l = lines[i]
        if l.startswith("| Nombre |") and "Data Type" not in l and not l.startswith("| Nombre | char"):
            head, i = rows_of(lines, i)
            while i < len(lines) and not lines[i].startswith("| Columnas | Data Type"):
                i += 1
            cols, i = rows_of(lines, i)
            res.append((head, cols))
            continue
        if l.startswith("## Cálculo del IC"):
            j = i + 1
            while j < len(lines) and not lines[j].startswith("## "):
                if lines[j].startswith("|"):
                    t, j = rows_of(lines, j)
                    ics.append(t)
                else:
                    j += 1
            i = j
            continue
        i += 1
    return res, ics

thin = Side(style="thin", color="808080")
B = Border(left=thin, right=thin, top=thin, bottom=thin)
HFILL = PatternFill("solid", fgColor="1F4E78")
KFILL = PatternFill("solid", fgColor="DDEBF7")
HFONT = Font(bold=True, color="FFFFFF")
BOLD = Font(bold=True)

def put(ws, r, c, v, font=None, fill=None, border=True):
    cell = ws.cell(r, c, num(v) if isinstance(v, str) else v)
    if font: cell.font = font
    if fill: cell.fill = fill
    if border: cell.border = B
    cell.alignment = Alignment(vertical="center", wrap_text=False)
    return cell

wb = Workbook()
wb.remove(wb.active)
resumen = []
for hoja, titulo, files in HOJAS:
    ws = wb.create_sheet(hoja)
    ws["B1"] = titulo
    ws["B1"].font = Font(bold=True, size=14)
    r = 4
    ic_tables = []
    for f in files:
        tabs, ics = tablas(T / f)
        ic_tables += ics
        for head, cols in tabs:
            # bloque izquierdo
            for k, (campo, valor, *_) in enumerate(head):
                put(ws, r + k, 2, campo, font=BOLD, fill=KFILL)
                put(ws, r + k, 3, valor)
            nombre = head[0][1]
            # columnas
            hdr, body = cols[0], cols[1:]
            for c, h in enumerate(hdr):
                put(ws, r, 5 + c, h, font=HFONT, fill=HFILL)
            total = None
            if body and not body[-1][0] and not body[-1][1]:
                total = body[-1][2]; body = body[:-1]
            for k, row in enumerate(body, start=1):
                for c, v in enumerate(row):
                    put(ws, r + k, 5 + c, v)
            tr = r + len(body) + 1
            put(ws, tr, 6, "Total", font=BOLD)
            nums = [num(x[2]) for x in body]
            if total is not None:
                tval = num(total)
                if all(isinstance(n, int) for n in nums) and sum(nums) == tval:
                    put(ws, tr, 7, f"=SUM(G{r+1}:G{tr-1})", font=BOLD)
                else:
                    put(ws, tr, 7, tval, font=BOLD)
            resumen.append((hoja, nombre, dict((h[0], h[1]) for h in head)))
            r = max(r + len(head), tr) + 3
    if ic_tables:
        put(ws, r, 2, "Cálculo del IC", font=Font(bold=True, size=12), border=False)
        put(ws, r + 1, 2, "#RL = (IC − 4) / (RL + 3) (entero);  r = (IC − 4) − #RL·(RL + 3);  se elige el menor r de la terna", border=False)
        r += 3
        for t in ic_tables:
            for c, h in enumerate(t[0]):
                put(ws, r, 2 + c, h, font=HFONT, fill=HFILL)
            for k, row in enumerate(t[1:], start=1):
                for c, v in enumerate(row):
                    put(ws, r + k, 2 + c, v)
            r += len(t) + 2
    for col, w in {"A": 3, "B": 20, "C": 22, "D": 3, "E": 34, "F": 12, "G": 10, "H": 6, "I": 6}.items():
        ws.column_dimensions[col].width = w

# hoja resumen al inicio
ws = wb.create_sheet("Resumen", 0)
ws["B1"] = "RESUMEN DE TABLAS — MÓDULO ONLINE"
ws["B1"].font = Font(bold=True, size=14)
cols = ["Grupo", "Tabla", "Organización", "Longitud Registro", "IC", "Capacidad"]
for c, h in enumerate(cols):
    put(ws, 3, 2 + c, h, font=HFONT, fill=HFILL)
for k, (g, n, d) in enumerate(resumen, start=4):
    for c, v in enumerate([g, n, d.get("Organización", ""), d.get("Longitud Registro", ""), d.get("IC", ""), d.get("Capacidad", "")]):
        put(ws, k, 2 + c, v)
for col, w in {"A": 3, "B": 18, "C": 28, "D": 14, "E": 18, "F": 8, "G": 20}.items():
    ws.column_dimensions[col].width = w

wb.save(OUT)
for g, n, d in resumen:
    print(g, "|", n, "|", d.get("Longitud Registro"), "|", d.get("IC"))
