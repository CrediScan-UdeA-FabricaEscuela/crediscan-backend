---
applyTo: "Dockerfile,docker-compose*.yml,.dockerignore,.github/workflows/*.yml,render.yaml,k8s/**/*.yaml"
---
# Spring Docker Deploy Rules

## Mandatory rules
- Use multi-stage Docker builds.
- Run containers as non-root users whenever possible.
- Keep runtime images minimal and include health checks.
- Keep environment-specific values externalized via env vars.
- Add `.env.example`; keep real `.env` out of version control.

## CI/CD
- Build, test, and quality checks run before image push/deploy.
- Pin key actions and base images where feasible.
- Avoid leaking secrets in logs and workflow outputs.

## Anti-patterns
- No secrets baked in Dockerfile, compose, k8s manifests, or CI files.
- No production deploy from unverified branches.
