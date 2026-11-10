# Sistema-Reclamos-Tino

Sistema de Reclamos: diseño arquitectónico, diseño externo (prototipos), diseño interno (base de datos)
y la aplicación Java que los implementa.

## Contenido del repositorio

| Archivo / carpeta | Qué es |
|---|---|
| `app/` | Aplicación en Java 21 (escritorio Swing y versión web). Ver [`app/README.md`](app/README.md) |
| `Teoria/diseno_arquitectonico.*` | Diseño arquitectónico (`.drawio` editable, `.png` y `.svg`) |
| `Teoria/prototipos_web/` | Diseño externo: capturas de cada pantalla y `PROTOTIPOS.md` con su Entrada, Función y Salida |
| `Teoria/TABLAS_MAESTRAS.md` | Diseño interno: tablas maestras |
| `Teoria/TABLAS_MOVIMIENTOS.md` | Diseño interno: tablas de movimientos y cadena simple |
| `Teoria/TABLAS_CATALOGOS.md` | Diseño interno: catálogos, dominios y parámetros de módulo |
| `Teoria/TABLAS_HISTORICAS.md` | Diseño interno: tablas históricas |
| `Teoria/PARQUE_DE_TICKET.md` | Diseño interno: parque de tickets |
| `Teoria/CHECKPOINT.md` | Diseño interno: checkpoint |
| `Teoria/tablas_png/` | Todas las tablas del diseño interno como imágenes, listas para el informe |
| `Teoria/LIBRO DISEÑO-BD.pdf` | Libro del curso (teoría de diseño de base de datos) |
| `Teoria/Entregable 2.docx.pdf`, `Teoria/Tablas.xlsx` | Entregable de referencia en el que se basa el proyecto |

## Arranque rápido

```bash
cd app
mvn compile exec:java                                                  # escritorio
mvn compile exec:java -Dmain.class=pe.tino.reclamos.web.ServidorWeb    # web, http://localhost:8080
```
