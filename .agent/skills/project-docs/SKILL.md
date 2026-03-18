---
name: project-docs
description: >
  Generates ALL mandatory documentation for the Credit Risk Scoring Engine university project
  (CodeF@ctory advanced, Universidad de Antioquia). Covers C4 diagrams (Structurizr DSL),
  Architecture Decision Records (MADR), Architecture Document, Entity-Relationship Model
  (Mermaid), API Documentation Guide, Deployment Diagrams, Database Deployment Guide,
  and the Documentation Index.
  Trigger: "generá la documentación", "create docs", "diagrama C4", "ADR", "MER",
  "deployment diagram", "architecture document", "documentación del proyecto".
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

## When to Use

- User asks to generate or update project documentation
- User asks for a C4 diagram (Context, Container, or Component level)
- User needs an ADR created for any architectural decision
- User needs the MER/ERD for the database model
- User asks for deployment diagrams (local, production, CI/CD, Kubernetes)
- User needs the API documentation guide

## Output Structure

ALL documentation lives under `docs/`. Never place documentation outside this tree.

```
docs/
├── README.md                          # Documentation index
├── architecture/
│   ├── ARCHITECTURE.md                # Architecture document
│   ├── DEPLOYMENT.md                  # Deployment diagrams
│   ├── c4/
│   │   ├── workspace.dsl              # Structurizr DSL (all levels)
│   │   └── README.md                  # Render instructions
│   └── decisions/
│       ├── 0001-hexagonal-architecture.md
│       ├── 0002-spring-boot-stack.md
│       ├── 0003-postgresql-persistence.md
│       ├── 0004-api-versioning-url-path.md
│       ├── 0005-hateoas-rest.md
│       ├── 0006-jwt-authentication.md
│       ├── 0007-rbac-abac-authorization.md
│       ├── 0008-flyway-migrations.md
│       ├── 0009-testcontainers-integration.md
│       └── 0010-sonarcloud-quality-gates.md
├── database/
│   ├── MER.md                         # Entity-Relationship Model (Mermaid)
│   └── DEPLOYMENT.md                  # Database deployment guide
└── api/
    └── API-DOCS.md                    # API documentation guide
```

## Critical Patterns

### Diagram tools
- C4 diagrams → **Structurizr DSL** only. File: `docs/architecture/c4/workspace.dsl`.
- MER and deployment diagrams → **Mermaid** `erDiagram` / `flowchart` / `graph` inside Markdown.
- Every diagram block MUST be followed by a plain-text explanation paragraph.

### ADR format
- **MADR** (Markdown Architectural Decision Records) format. Never use other formats.
- File naming: `NNNN-kebab-case-title.md` with zero-padded 4-digit index.
- Status values: `Proposed` | `Accepted` | `Deprecated` | `Superseded by [NNNN]`.

### Documentation rules
- All cross-references use relative Markdown links.
- Every artifact identifies which sprint deliverable it covers (see `docs/README.md`).
- Package/class names MUST match the actual project packages under
  `com.udea.creditrisk.*`.

## Generation Steps

When asked to generate documentation, follow this order:

1. Create `docs/` directory tree (all folders).
2. Write `docs/architecture/c4/workspace.dsl` from the C4 template in
   `assets/c4/workspace.dsl`.
3. Write `docs/architecture/c4/README.md` with render instructions.
4. Write all 10 ADRs from `assets/adrs/` templates.
5. Write `docs/architecture/ARCHITECTURE.md` from `assets/architecture.md`.
6. Write `docs/database/MER.md` from `assets/database/MER.md`.
7. Write `docs/database/DEPLOYMENT.md` from `assets/database/DEPLOYMENT.md`.
8. Write `docs/api/API-DOCS.md` from `assets/api/API-DOCS.md`.
9. Write `docs/architecture/DEPLOYMENT.md` from `assets/deployment/DEPLOYMENT.md`.
10. Write `docs/README.md` from `assets/docs-index.md`.

## Commands

```bash
# Render C4 diagrams locally with Structurizr Lite
docker run -it --rm \
  -p 8080:8080 \
  -v $(pwd)/docs/architecture/c4:/usr/local/structurizr \
  structurizr/lite

# Open browser at http://localhost:8080

# Render C4 online (no Docker required)
# Upload docs/architecture/c4/workspace.dsl to https://structurizr.com/dsl

# Preview Mermaid diagrams (VS Code extension or GitHub PR)
# Install: ext install bierner.markdown-mermaid
```

## Resources

- **C4 DSL template**: See [assets/c4/workspace.dsl](assets/c4/workspace.dsl)
- **ADR templates**: See [assets/adrs/](assets/adrs/) — one file per decision
- **Architecture doc**: See [assets/architecture.md](assets/architecture.md)
- **MER model**: See [assets/database/MER.md](assets/database/MER.md)
- **Database deployment**: See [assets/database/DEPLOYMENT.md](assets/database/DEPLOYMENT.md)
- **API docs guide**: See [assets/api/API-DOCS.md](assets/api/API-DOCS.md)
- **Deployment diagrams**: See [assets/deployment/DEPLOYMENT.md](assets/deployment/DEPLOYMENT.md)
- **Docs index**: See [assets/docs-index.md](assets/docs-index.md)
