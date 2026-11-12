"""Genera una imagen PNG por tabla a partir de los md de diseño interno.

Uso: python3 tablas_png.py <carpeta Teoria> <carpeta salida>
"""
import html
import os
import re
import subprocess
import sys
import tempfile
import unicodedata

from PIL import Image, ImageChops

TEORIA, SALIDA = sys.argv[1], sys.argv[2]
ARCHIVOS_X = [

    ("TABLAS_SEGURIDAD.md", "8_Seguridad"),

]
ARCHIVOS = [
    ("TABLA_COMPRAS.md", "7_Tabla_de_Compras"),
    ("TABLAS_SEGURIDAD.md", "8_Seguridad"),
    ("LISTAS_INVERTIDAS.md", "9_Listas_Invertidas"),
    ("TABLAS_MAESTRAS.md", "1_Maestras"),
    ("TABLAS_MOVIMIENTOS.md", "2_Movimientos"),
    ("TABLAS_CATALOGOS.md", "3_Catalogos"),
    ("TABLAS_HISTORICAS.md", "4_Historicas"),
    ("PARQUE_DE_TICKET.md", "5_Parque_de_Ticket"),
    ("CHECKPOINT.md", "6_Checkpoint"),
]

CSS = """
body { margin: 0; padding: 16px; background: #fff; font-family: Arial, Helvetica, sans-serif; }
.fila { display: flex; gap: 40px; align-items: flex-start; }
table { border-collapse: collapse; font-size: 13px; }
td, th { border: 1px solid #7f7f7f; padding: 4px 10px; text-align: center; }
.info td:first-child { text-align: left; }
.cols th { background: #4a86e8; color: #fff; font-weight: bold; }
.cols tr.pk td { background: #ffff00; }
.cols tr.total td { border: none; font-weight: bold; }
.cols tr.vec td { background: #e8eef9; font-style: italic; }
.cols tr.el td:first-child { text-align: left; padding-left: 28px; }
"""


def celdas(linea):
    return [limpiar(c) for c in linea.strip().strip("|").split("|")]


def limpiar(t):
    return t.strip().replace("**", "").replace("`", "")


def tabla_desde(lineas, i):
    filas = []
    while i < len(lineas) and lineas[i].startswith("|"):
        if not re.match(r"^\|[\s\-|:]+\|$", lineas[i].strip()):
            filas.append(celdas(lineas[i]))
        i += 1
    return filas, i


def extraer(md):
    lineas = open(md, encoding="utf-8").read().splitlines()
    tablas, i = [], 0
    while i < len(lineas):
        if lineas[i].startswith("| Nombre |"):
            info, i = tabla_desde(lineas, i)
            while i < len(lineas) and not lineas[i].startswith("| Columnas |"):
                i += 1
            cols, i = tabla_desde(lineas, i)
            tablas.append((info, cols))
        else:
            i += 1
    return tablas


def html_de(info, cols):
    filas_info = "".join(
        f"<tr><td>{html.escape(a)}</td><td>{html.escape(b)}</td></tr>" for a, b in info)
    cab = "".join(f"<th>{html.escape(c)}</th>" for c in cols[0])
    cuerpo = ""
    for f in cols[1:]:
        if not f[0] and not f[1]:
            clase = "total"
        elif "vector" in f[0]:
            clase = "vec"
        elif f[0].startswith("·"):
            clase = "el"
        elif len(f) > 3 and f[3].upper() == "X":
            clase = "pk"
        else:
            clase = ""
        cuerpo += f"<tr class='{clase}'>" + "".join(f"<td>{html.escape(c)}</td>" for c in f) + "</tr>"
    return (f"<!doctype html><meta charset='utf-8'><style>{CSS}</style>"
            f"<div class='fila'><table class='info'>{filas_info}</table>"
            f"<table class='cols'><tr>{cab}</tr>{cuerpo}</table></div>")


def recortar(ruta):
    img = Image.open(ruta).convert("RGB")
    fondo = Image.new("RGB", img.size, (255, 255, 255))
    caja = ImageChops.difference(img, fondo).getbbox()
    if caja:
        m = 20
        caja = (max(caja[0] - m, 0), max(caja[1] - m, 0),
                min(caja[2] + m, img.width), min(caja[3] + m, img.height))
        img.crop(caja).save(ruta)


os.makedirs(SALIDA, exist_ok=True)
tmp = tempfile.mkdtemp()
total = 0
for archivo, carpeta in ARCHIVOS:
    destino = os.path.join(SALIDA, carpeta)
    os.makedirs(destino, exist_ok=True)
    for n, (info, cols) in enumerate(extraer(os.path.join(TEORIA, archivo)), 1):
        nombre = dict((a, b) for a, b in info).get("Nombre", f"tabla{n}")
        ascii_ = unicodedata.normalize("NFKD", nombre).encode("ascii", "ignore").decode()
        base = f"{n:02d}_" + re.sub(r"[^A-Za-z0-9_]+", "_", ascii_.replace(" ", "_")).strip("_")
        pagina = os.path.join(tmp, base + ".html")
        open(pagina, "w", encoding="utf-8").write(html_de(info, cols))
        png = os.path.join(destino, base + ".png")
        subprocess.run(["google-chrome", "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        "--force-device-scale-factor=2", "--window-size=1400,1400",
                        f"--screenshot={png}", "file://" + pagina],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        recortar(png)
        total += 1
        print(png)
print("total", total)
