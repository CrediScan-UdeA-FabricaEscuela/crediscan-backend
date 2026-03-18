# C4 Diagrams — Render Instructions

Los diagramas C4 están definidos en Structurizr DSL (`workspace.dsl`).
Existen dos formas de renderizarlos: local con Docker o en línea.

## Opción A — Structurizr Lite (Docker, recomendado)

```bash
# Desde la raíz del repositorio
docker run -it --rm \
  -p 8080:8080 \
  -v $(pwd)/docs/architecture/c4:/usr/local/structurizr \
  structurizr/lite
```

Abrí http://localhost:8080 en el browser.
El archivo `workspace.dsl` se recarga automáticamente al guardar.

## Opción B — Structurizr Online (sin Docker)

1. Entrá a https://structurizr.com/dsl
2. Pegá el contenido de `workspace.dsl`
3. Hacé clic en **Render**

## Vistas disponibles

| Vista | ID | Descripción |
|-------|----|-------------|
| System Context | `SystemContext` | Level 1: actores y sistemas externos |
| Container | `Containers` | Level 2: Spring Boot app, PostgreSQL, Redis, Prometheus, Grafana |
| Component | `Components` | Level 3: módulos internos (ports, adapters, use cases) |

## Exportar imágenes

En Structurizr Lite: **Export → PNG/SVG** desde el menú de cada vista.
Guardá las imágenes en `docs/architecture/c4/exports/` y referencialas desde `ARCHITECTURE.md`.
