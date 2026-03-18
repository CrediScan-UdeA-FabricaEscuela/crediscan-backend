# Database Deployment Guide — Credit Risk Scoring Engine

> Cubre: convenciones Flyway, stored procedures y triggers, roles y seguridad del esquema, estimación de volumen de datos.

---

## 1. Flyway — Convenciones

Ver [ADR-0008](../../adrs/0008-flyway-migrations.md) para la decisión de adopción.

### Ubicación de scripts

```
src/main/resources/db/migration/
├── V1__create_schema_initial.sql      # Extensiones (uuid-ossp, pgcrypto)
├── V2__create_applicant_tables.sql    # solicitante, datos_financieros
├── V3__create_scoring_tables.sql      # variable_scoring, version_modelo_scoring, regla_calculo
├── V4__create_evaluation_tables.sql   # evaluacion_riesgo, clasificacion_riesgo, decision_credito
├── V5__create_security_tables.sql     # usuario, rol_usuario
├── V6__create_audit_table.sql         # audit_log
├── V7__create_indexes.sql             # Todos los índices
├── V8__seed_roles.sql                 # Datos iniciales: roles y modelo base
└── V9__create_triggers.sql            # Triggers de updated_at
```

### Reglas obligatorias

1. **Nunca modificar** un archivo `V*.sql` ya ejecutado. Crear `V{N+1}__fix_...sql`.
2. Un script = un cambio atómico. No combinar DDL + DML en el mismo archivo.
3. Scripts idempotentes: usar `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`.
4. Los scripts de seed de datos iniciales (`V8__seed_*.sql`) solo insertan con `INSERT ... ON CONFLICT DO NOTHING`.
5. Scripts `R__` (repeatable) solo para vistas o funciones que cambian frecuentemente.

### Ejemplo de migración correcta

```sql
-- V2__create_applicant_tables.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS solicitante (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_completo VARCHAR(100) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    tipo_documento  VARCHAR(20) NOT NULL
        CHECK (tipo_documento IN ('cedula', 'pasaporte', 'nit', 'extranjeria')),
    email           VARCHAR(150) NOT NULL,
    telefono        VARCHAR(20),
    fecha_nacimiento DATE,
    estado_civil    VARCHAR(30),
    nivel_educativo VARCHAR(50),
    ocupacion       VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL,
    CONSTRAINT uq_solicitante_documento UNIQUE (numero_documento),
    CONSTRAINT uq_solicitante_email     UNIQUE (email)
);
```

---

## 2. Stored Procedures y Triggers

### Trigger de `updated_at` (aplicar a todas las tablas)

```sql
-- V9__create_triggers.sql

-- Función reutilizable
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar a tablas que tienen updated_at
CREATE TRIGGER trg_solicitante_updated_at
    BEFORE UPDATE ON solicitante
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- Repetir para: datos_financieros (si se agrega updated_at), evaluacion_riesgo, etc.
```

### Stored Procedure: calcular score (referencia, lógica principal en Java)

> La lógica de scoring se implementa en Java (`com.udea.creditrisk.scoring.domain.service.ScoringCalculatorService`).
> El stored procedure se usa como validación/auditoría alternativa, no como implementación primaria.

```sql
-- R__fn_calculate_score.sql (repeatable — puede cambiar con el modelo)
CREATE OR REPLACE FUNCTION fn_calculate_score(p_evaluacion_id UUID)
RETURNS NUMERIC AS $$
DECLARE
    v_score NUMERIC := 0;
BEGIN
    SELECT SUM(rc.peso * fn_transform_variable(vd.valor, rc.funcion_transformacion))
    INTO v_score
    FROM regla_calculo rc
    JOIN variable_scoring vs ON vs.id = rc.variable_id
    -- JOIN con datos_financieros para obtener valores reales
    WHERE rc.version_modelo_id = (
        SELECT version_modelo_id FROM evaluacion_riesgo WHERE id = p_evaluacion_id
    );

    RETURN COALESCE(v_score, 0);
END;
$$ LANGUAGE plpgsql;
```

### Convenciones de naming

| Objeto | Prefijo | Ejemplo |
|--------|---------|---------|
| Función | `fn_` | `fn_set_updated_at` |
| Trigger | `trg_` | `trg_solicitante_updated_at` |
| Índice | `idx_` | `idx_evaluacion_estado` |
| Constraint unique | `uq_` | `uq_solicitante_email` |
| Constraint check | `chk_` | `chk_score_rango` |
| Constraint FK | `fk_` | `fk_evaluacion_solicitante` |

---

## 3. Roles y Seguridad del Esquema

### Roles de PostgreSQL

```sql
-- Crear roles de base de datos (ejecutar manualmente en el DB del entorno)

-- Rol de aplicación (mínimo privilegio)
CREATE ROLE creditrisk_app LOGIN PASSWORD 'CHANGE_ME_IN_ENV';
GRANT CONNECT ON DATABASE creditrisk TO creditrisk_app;
GRANT USAGE ON SCHEMA public TO creditrisk_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO creditrisk_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO creditrisk_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO creditrisk_app;

-- Rol de solo lectura (para reporting / BI)
CREATE ROLE creditrisk_readonly LOGIN PASSWORD 'CHANGE_ME_IN_ENV';
GRANT CONNECT ON DATABASE creditrisk TO creditrisk_readonly;
GRANT USAGE ON SCHEMA public TO creditrisk_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO creditrisk_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO creditrisk_readonly;

-- Rol de Flyway (necesita DDL)
CREATE ROLE creditrisk_flyway LOGIN PASSWORD 'CHANGE_ME_IN_ENV';
GRANT ALL PRIVILEGES ON DATABASE creditrisk TO creditrisk_flyway;
```

### Variables de entorno requeridas

```
SPRING_DATASOURCE_URL=jdbc:postgresql://{host}:{port}/{db}
SPRING_DATASOURCE_USERNAME=creditrisk_app
SPRING_DATASOURCE_PASSWORD={secret}
SPRING_FLYWAY_USER=creditrisk_flyway
SPRING_FLYWAY_PASSWORD={secret}
```

Nunca commitear credenciales. Usar `.env` local (gitignored) y secrets de GitHub/Render en CI/CD.

---

## 4. Estimación de Volumen de Datos

Parámetros del proyecto universitario (carga simulada):

| Tabla | Registros estimados (1 año) | Tamaño estimado |
|-------|----------------------------|-----------------|
| `solicitante` | 10,000 | ~5 MB |
| `datos_financieros` | 15,000 | ~10 MB |
| `evaluacion_riesgo` | 12,000 | ~8 MB |
| `clasificacion_riesgo` | 12,000 | ~4 MB |
| `decision_credito` | 12,000 | ~6 MB |
| `audit_log` | 200,000 | ~100 MB |
| `variable_scoring` | 50 | < 1 MB |
| `version_modelo_scoring` | 10 | < 1 MB |
| `regla_calculo` | 200 | < 1 MB |
| `usuario` | 50 | < 1 MB |
| **Total estimado** | | **~135 MB** |

Dentro del límite del free tier de Render (1 GB storage).

### Estrategia de retención de `audit_log`

```sql
-- Job mensual: archivar logs mayores a 1 año
-- Ejecutar manualmente o con pg_cron (Sprint 4)
DELETE FROM audit_log
WHERE realizado_en < now() - INTERVAL '1 year';
```

---

## 5. Backup y Restauración (Render)

```bash
# Backup manual desde Render Dashboard o CLI
pg_dump -h {host} -U {user} -d creditrisk -F c -f backup_$(date +%Y%m%d).dump

# Restaurar
pg_restore -h {host} -U {user} -d creditrisk -F c backup_20260318.dump
```

Render realiza backups automáticos diarios en el plan gratuito (retención 7 días).
