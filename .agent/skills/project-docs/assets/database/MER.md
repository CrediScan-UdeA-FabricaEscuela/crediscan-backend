# Entity-Relationship Model — Credit Risk Scoring Engine

> Formato: Mermaid `erDiagram` (renderiza en GitHub, VS Code, Notion).
> Módulos afectados: `applicant`, `financialdata`, `scoring`, `evaluation`, `shared`.

---

## Modelo Lógico

El modelo lógico muestra entidades, atributos clave y relaciones sin tipos físicos.

```mermaid
erDiagram
    SOLICITANTE {
        uuid id PK
        string nombre_completo
        string numero_documento
        string tipo_documento
        string email
        string telefono
        date fecha_nacimiento
        string estado_civil
        string nivel_educativo
        string ocupacion
        timestamp created_at
        timestamp updated_at
        string created_by
    }

    DATOS_FINANCIEROS {
        uuid id PK
        uuid solicitante_id FK
        decimal ingresos_mensuales
        decimal egresos_mensuales
        decimal deuda_total
        decimal patrimonio_neto
        integer historial_pagos_score
        integer antiguedad_laboral_meses
        integer numero_obligaciones_activas
        boolean reporte_negativo_burea
        string fuente_datos
        timestamp fecha_consulta
        timestamp created_at
        string created_by
    }

    VARIABLE_SCORING {
        uuid id PK
        string codigo
        string nombre
        string descripcion
        string tipo_dato
        decimal peso_defecto
        boolean activa
    }

    VERSION_MODELO_SCORING {
        uuid id PK
        string version
        string descripcion
        boolean activa
        timestamp fecha_activacion
        string creado_por
        timestamp created_at
    }

    REGLA_CALCULO {
        uuid id PK
        uuid version_modelo_id FK
        uuid variable_id FK
        decimal peso
        string funcion_transformacion
        decimal valor_minimo
        decimal valor_maximo
        integer orden
    }

    EVALUACION_RIESGO {
        uuid id PK
        uuid solicitante_id FK
        uuid datos_financieros_id FK
        uuid version_modelo_id FK
        decimal score_calculado
        string estado
        string analista_id FK
        timestamp fecha_evaluacion
        timestamp created_at
    }

    CLASIFICACION_RIESGO {
        uuid id PK
        uuid evaluacion_id FK
        string categoria
        decimal score_minimo
        decimal score_maximo
        string descripcion
        string recomendacion
    }

    DECISION_CREDITO {
        uuid id PK
        uuid evaluacion_id FK
        string decision
        decimal monto_solicitado
        decimal monto_aprobado
        decimal tasa_interes
        integer plazo_meses
        string justificacion
        string decidido_por
        timestamp fecha_decision
    }

    USUARIO {
        uuid id PK
        string username
        string email
        string password_hash
        boolean activo
        timestamp ultimo_acceso
        timestamp created_at
    }

    ROL_USUARIO {
        uuid id PK
        uuid usuario_id FK
        string rol
        timestamp asignado_en
        string asignado_por
    }

    AUDIT_LOG {
        uuid id PK
        string tipo_entidad
        uuid entidad_id
        string accion
        string realizado_por
        timestamp realizado_en
        jsonb detalles
        string ip_origen
        string correlation_id
    }

    SOLICITANTE ||--o{ DATOS_FINANCIEROS : "tiene"
    SOLICITANTE ||--o{ EVALUACION_RIESGO : "es evaluado en"
    DATOS_FINANCIEROS ||--|| EVALUACION_RIESGO : "usados en"
    VERSION_MODELO_SCORING ||--o{ REGLA_CALCULO : "compuesta por"
    VARIABLE_SCORING ||--o{ REGLA_CALCULO : "referenciada en"
    VERSION_MODELO_SCORING ||--o{ EVALUACION_RIESGO : "aplicada en"
    EVALUACION_RIESGO ||--|| CLASIFICACION_RIESGO : "genera"
    EVALUACION_RIESGO ||--o| DECISION_CREDITO : "resulta en"
    USUARIO ||--o{ ROL_USUARIO : "tiene"
    USUARIO ||--o{ EVALUACION_RIESGO : "realiza"
```

---

## Modelo Físico

El modelo físico muestra tipos PostgreSQL, constraints e índices.

```mermaid
erDiagram
    solicitante {
        uuid id PK "DEFAULT gen_random_uuid()"
        varchar_100 nombre_completo "NOT NULL"
        varchar_20 numero_documento "NOT NULL UNIQUE"
        varchar_20 tipo_documento "NOT NULL CHECK(cedula|pasaporte|nit|extranjeria)"
        varchar_150 email "NOT NULL UNIQUE"
        varchar_20 telefono
        date fecha_nacimiento
        varchar_30 estado_civil
        varchar_50 nivel_educativo
        varchar_100 ocupacion
        timestamptz created_at "NOT NULL DEFAULT now()"
        timestamptz updated_at "NOT NULL DEFAULT now()"
        varchar_100 created_by "NOT NULL"
    }

    datos_financieros {
        uuid id PK "DEFAULT gen_random_uuid()"
        uuid solicitante_id FK "NOT NULL REFERENCES solicitante(id)"
        numeric_15_2 ingresos_mensuales "NOT NULL CHECK(> 0)"
        numeric_15_2 egresos_mensuales "NOT NULL CHECK(>= 0)"
        numeric_15_2 deuda_total "NOT NULL CHECK(>= 0)"
        numeric_15_2 patrimonio_neto
        smallint historial_pagos_score "CHECK(0..999)"
        smallint antiguedad_laboral_meses "CHECK(>= 0)"
        smallint numero_obligaciones_activas "CHECK(>= 0)"
        boolean reporte_negativo_burea "NOT NULL DEFAULT false"
        varchar_50 fuente_datos "NOT NULL CHECK(manual|bureaux|api)"
        timestamptz fecha_consulta "NOT NULL DEFAULT now()"
        timestamptz created_at "NOT NULL DEFAULT now()"
        varchar_100 created_by "NOT NULL"
    }

    variable_scoring {
        uuid id PK "DEFAULT gen_random_uuid()"
        varchar_50 codigo "NOT NULL UNIQUE"
        varchar_100 nombre "NOT NULL"
        text descripcion
        varchar_20 tipo_dato "NOT NULL CHECK(numerico|booleano|categorico)"
        numeric_5_4 peso_defecto "NOT NULL CHECK(0..1)"
        boolean activa "NOT NULL DEFAULT true"
    }

    version_modelo_scoring {
        uuid id PK "DEFAULT gen_random_uuid()"
        varchar_20 version "NOT NULL UNIQUE"
        text descripcion
        boolean activa "NOT NULL DEFAULT false"
        timestamptz fecha_activacion
        varchar_100 creado_por "NOT NULL"
        timestamptz created_at "NOT NULL DEFAULT now()"
    }

    regla_calculo {
        uuid id PK "DEFAULT gen_random_uuid()"
        uuid version_modelo_id FK "NOT NULL REFERENCES version_modelo_scoring(id)"
        uuid variable_id FK "NOT NULL REFERENCES variable_scoring(id)"
        numeric_5_4 peso "NOT NULL CHECK(0..1)"
        varchar_50 funcion_transformacion "NOT NULL CHECK(linear|log|sigmoid|step)"
        numeric_15_4 valor_minimo
        numeric_15_4 valor_maximo
        smallint orden "NOT NULL"
    }

    evaluacion_riesgo {
        uuid id PK "DEFAULT gen_random_uuid()"
        uuid solicitante_id FK "NOT NULL REFERENCES solicitante(id)"
        uuid datos_financieros_id FK "NOT NULL REFERENCES datos_financieros(id)"
        uuid version_modelo_id FK "NOT NULL REFERENCES version_modelo_scoring(id)"
        numeric_5_2 score_calculado "NOT NULL CHECK(0..1000)"
        varchar_20 estado "NOT NULL DEFAULT 'PENDIENTE' CHECK(PENDIENTE|EN_REVISION|APROBADO|RECHAZADO)"
        uuid analista_id FK "NOT NULL REFERENCES usuario(id)"
        timestamptz fecha_evaluacion "NOT NULL DEFAULT now()"
        timestamptz created_at "NOT NULL DEFAULT now()"
    }

    clasificacion_riesgo {
        uuid id PK "DEFAULT gen_random_uuid()"
        uuid evaluacion_id FK "NOT NULL REFERENCES evaluacion_riesgo(id) UNIQUE"
        varchar_5 categoria "NOT NULL CHECK(AAA|AA|A|BBB|BB|B|CCC|D)"
        numeric_5_2 score_minimo "NOT NULL"
        numeric_5_2 score_maximo "NOT NULL"
        text descripcion
        text recomendacion
    }

    decision_credito {
        uuid id PK "DEFAULT gen_random_uuid()"
        uuid evaluacion_id FK "NOT NULL REFERENCES evaluacion_riesgo(id) UNIQUE"
        varchar_20 decision "NOT NULL CHECK(APROBADO|RECHAZADO|REVISION_MANUAL)"
        numeric_15_2 monto_solicitado "NOT NULL CHECK(> 0)"
        numeric_15_2 monto_aprobado "CHECK(>= 0)"
        numeric_5_4 tasa_interes "CHECK(>= 0)"
        smallint plazo_meses "CHECK(> 0)"
        text justificacion "NOT NULL"
        varchar_100 decidido_por "NOT NULL"
        timestamptz fecha_decision "NOT NULL DEFAULT now()"
    }

    usuario {
        uuid id PK "DEFAULT gen_random_uuid()"
        varchar_50 username "NOT NULL UNIQUE"
        varchar_150 email "NOT NULL UNIQUE"
        varchar_255 password_hash "NOT NULL"
        boolean activo "NOT NULL DEFAULT true"
        timestamptz ultimo_acceso
        timestamptz created_at "NOT NULL DEFAULT now()"
    }

    rol_usuario {
        uuid id PK "DEFAULT gen_random_uuid()"
        uuid usuario_id FK "NOT NULL REFERENCES usuario(id)"
        varchar_30 rol "NOT NULL CHECK(ROLE_ADMIN|ROLE_ANALYST|ROLE_VIEWER)"
        timestamptz asignado_en "NOT NULL DEFAULT now()"
        varchar_100 asignado_por "NOT NULL"
    }

    audit_log {
        uuid id PK "DEFAULT gen_random_uuid()"
        varchar_100 tipo_entidad "NOT NULL"
        uuid entidad_id "NOT NULL"
        varchar_50 accion "NOT NULL CHECK(CREATE|READ|UPDATE|DELETE|LOGIN|LOGOUT)"
        varchar_100 realizado_por "NOT NULL"
        timestamptz realizado_en "NOT NULL DEFAULT now()"
        jsonb detalles
        inet ip_origen
        varchar_50 correlation_id
    }

    solicitante ||--o{ datos_financieros : "solicitante_id"
    solicitante ||--o{ evaluacion_riesgo : "solicitante_id"
    datos_financieros ||--|| evaluacion_riesgo : "datos_financieros_id"
    version_modelo_scoring ||--o{ regla_calculo : "version_modelo_id"
    variable_scoring ||--o{ regla_calculo : "variable_id"
    version_modelo_scoring ||--o{ evaluacion_riesgo : "version_modelo_id"
    evaluacion_riesgo ||--|| clasificacion_riesgo : "evaluacion_id"
    evaluacion_riesgo ||--o| decision_credito : "evaluacion_id"
    usuario ||--o{ rol_usuario : "usuario_id"
    usuario ||--o{ evaluacion_riesgo : "analista_id"
```

---

## Índices Recomendados

```sql
-- Búsquedas frecuentes de solicitantes
CREATE INDEX idx_solicitante_numero_documento ON solicitante(numero_documento);
CREATE INDEX idx_solicitante_email ON solicitante(email);

-- Historial de datos financieros por solicitante
CREATE INDEX idx_datos_financieros_solicitante ON datos_financieros(solicitante_id);

-- Evaluaciones por solicitante y estado
CREATE INDEX idx_evaluacion_solicitante ON evaluacion_riesgo(solicitante_id);
CREATE INDEX idx_evaluacion_estado ON evaluacion_riesgo(estado);
CREATE INDEX idx_evaluacion_analista ON evaluacion_riesgo(analista_id);

-- Auditoría: búsqueda por entidad y acción
CREATE INDEX idx_audit_entidad ON audit_log(tipo_entidad, entidad_id);
CREATE INDEX idx_audit_realizado_por ON audit_log(realizado_por);
CREATE INDEX idx_audit_fecha ON audit_log(realizado_en DESC);

-- Modelo de scoring activo
CREATE UNIQUE INDEX idx_version_modelo_activa ON version_modelo_scoring(activa)
    WHERE activa = true;
```

---

## Notas de Diseño

- Todas las PKs son `UUID` generados por PostgreSQL (`gen_random_uuid()`), no secuencias enteras.
- `TIMESTAMPTZ` en lugar de `TIMESTAMP` para correcta gestión de zonas horarias.
- `audit_log.detalles` es `JSONB` para almacenar el estado anterior/posterior del recurso sin esquema fijo.
- La columna `version_modelo_scoring.activa` tiene índice único parcial para garantizar que solo un modelo esté activo a la vez.
- `decision_credito` y `clasificacion_riesgo` tienen FK con `UNIQUE` constraint sobre `evaluacion_id` → relación 1:1.
