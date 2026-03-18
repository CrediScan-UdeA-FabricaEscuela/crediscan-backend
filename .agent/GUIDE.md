# Guía de Agent Skills para el Proyecto

> **Audiencia**: Estudiantes que nunca usaron agent skills.
> **Herramientas cubiertas**: Claude Code (CLI) y GitHub Copilot.

---

## 1. ¿Qué son las Skills?

Las skills son **archivos Markdown con instrucciones** que le enseñan a un asistente de IA (Claude, Copilot) **cómo escribir código siguiendo patrones específicos**.

Pensalo así: es como tener el playbook de un arquitecto senior que el asistente lee ANTES de escribir una sola línea de código. No genera código mágicamente — aplica patrones, convenciones y reglas que el equipo definió.

**Lo que SÍ son:**
- Guías de patrones arquitectónicos y convenciones del proyecto
- Reglas concretas: qué hacer, qué NO hacer, con ejemplos

**Lo que NO son:**
- Generadores de código automáticos
- Reemplazos para entender los conceptos

> **Importante**: Las skills no te sacan de estudiar. Si no entendés qué es Hexagonal Architecture, la skill no te va a salvar. Primero aprendé el concepto, después usá la skill para aplicarlo consistentemente.

---

## 2. Estructura del Proyecto

```
.agent/
├── AGENTS.md              ← Registro: lista todas las skills disponibles
└── skills/
    ├── spring-hexagonal/
    │   └── SKILL.md       ← Instrucciones de arquitectura hexagonal
    ├── spring-security/
    │   └── SKILL.md
    ├── spring-persistence/
    │   └── SKILL.md
    ├── spring-api-design/
    │   └── SKILL.md
    ├── spring-testing/
    │   └── SKILL.md
    ├── spring-observability/
    │   └── SKILL.md
    ├── spring-docker-deploy/
    │   └── SKILL.md
    └── sonarcloud-gates/
        └── SKILL.md
```

### ¿Qué es `AGENTS.md`?

Es el **índice/registro** de skills. Claude Code lo lee automáticamente para saber qué skills existen, dónde están, y cuándo activar cada una. Cada entrada tiene nombre, ruta y descripción.

### ¿Qué contiene cada `SKILL.md`?

Cada archivo tiene esta estructura:

1. **Frontmatter** (metadata): nombre, descripción, condiciones de activación
2. **When to Use**: situaciones concretas donde aplica la skill
3. **Critical Patterns**: las reglas mandatorias con ejemplos de código
4. **Anti-patterns**: lo que NO hay que hacer (y por qué)
5. **Examples**: código completo que muestra los patrones correctos

---

## 3. Cómo usar las Skills con Claude Code (CLI)

### Detección automática

Claude Code lee el directorio `.agent/` automáticamente. Cuando le pedís algo que coincide con las condiciones de una skill, la aplica sin que le digas nada.

Por ejemplo, si le pedís "creá un repositorio JPA para órdenes", va a detectar que aplica `spring-persistence` y va a seguir esos patrones.

### Referencia explícita

Cuando el contexto no es obvio, **nombrá la skill directamente**:

```
Seguí la skill spring-hexagonal para crear la estructura de paquetes del módulo de usuarios.
```

```
Usando la skill spring-testing, escribí tests unitarios para CreateOrderUseCase.
```

### Combinación de skills

Podés combinar varias skills en un solo pedido:

```
Siguiendo spring-hexagonal y spring-persistence, creá el módulo de productos
con la entidad, el repositorio y la migración Flyway.
```

---

## 4. Cómo usar las Skills con GitHub Copilot

Copilot no lee `.agent/skills/` directamente. Usa su propio sistema de instrucciones.

### Instrucciones a nivel de proyecto

Copilot lee el archivo `.github/copilot-instructions.md` para instrucciones generales del proyecto.

Ejemplo de contenido:

```markdown
# Instrucciones del Proyecto

Este proyecto usa Spring Boot 3.x con Arquitectura Hexagonal.

## Arquitectura
- Domain NUNCA depende de infrastructure
- Dependencias fluyen: Infrastructure → Application → Domain
- Paquetes: domain/model, domain/port, application/usecase, infrastructure/adapter

## Testing
- Tests unitarios con JUnit 5 + Mockito, patrón AAA
- Naming: should_expectedBehavior_when_condition()
- Coverage mínimo: 40%
```

### Instrucciones por contexto (globs)

Copilot también soporta archivos en `.github/instructions/` que se activan según **qué archivo estás editando** (glob patterns). Esto es lo más parecido a nuestras skills.

Estructura que necesitarían crear:

```
.github/
├── copilot-instructions.md           ← Instrucciones generales
└── instructions/
    ├── security.instructions.md      ← Se activa en **/security/**, **/auth/**
    ├── persistence.instructions.md   ← Se activa en **/repository/**, **/entity/**
    ├── api.instructions.md           ← Se activa en **/controller/**, **/dto/**
    ├── testing.instructions.md       ← Se activa en **/*Test.java, **/features/**
    ├── observability.instructions.md ← Se activa en **/config/**, **/logging/**
    └── docker.instructions.md        ← Se activa en **/Dockerfile, **/compose**
```

Cada archivo de instrucciones usa un frontmatter con `applyTo` para definir los globs:

```markdown
---
applyTo: "**/repository/**,**/entity/**,**/migration/**"
---

# Persistencia — Spring Data JPA

## Reglas mandatorias
- Toda entidad usa @Entity con @Table explícito
- Auditoría automática con @EntityListeners(AuditingEntityListener.class)
- Migraciones Flyway versionadas: V001__description.sql
- Repositorios extienden JpaRepository, consultas complejas con @Query JPQL
- NUNCA usar CascadeType.ALL — definir cascadas explícitas
```

> **Diferencia clave**: Claude Code lee los `SKILL.md` nativamente (son más completos, con decenas de patrones). Para Copilot hay que **adaptar y resumir** el contenido al formato de instrucciones de GitHub. Son dos formatos distintos para el mismo propósito.

> **Nota**: No es necesario crear estos archivos ahora. Cuando los necesiten, tomen el contenido relevante de cada `SKILL.md` y adáptenlo al formato mostrado arriba.

---

## 5. Ejemplos Prácticos

Prompts listos para copiar y pegar en Claude Code.

### spring-hexagonal

```
Creá la estructura de paquetes para el módulo de órdenes siguiendo arquitectura hexagonal.
```

```
Siguiendo spring-hexagonal, agregá un nuevo puerto de salida para notificaciones
en el módulo de usuarios.
```

```
Refactorizá el módulo de productos para que siga la skill spring-hexagonal.
El controller no debería conocer la entidad JPA directamente.
```

### spring-security

```
Implementá RBAC con JWT para el endpoint de crear producto. Solo ADMIN puede crear.
```

```
Siguiendo spring-security, configurá el SecurityFilterChain con JWT validation
y CORS para el frontend en localhost:4200.
```

```
Agregá rate limiting al endpoint de login para evitar fuerza bruta.
```

### spring-persistence

```
Creá la entidad Order con auditoría automática y una consulta no trivial
para buscar órdenes por rango de fechas y estado.
```

```
Siguiendo spring-persistence, creá la migración Flyway para la tabla de productos
con los índices necesarios.
```

```
Implementá el repositorio de usuarios con una consulta custom usando @Query JPQL.
```

### spring-api-design

```
Creá el controller REST para órdenes con HATEOAS y error handling estandarizado RFC 7807.
```

```
Siguiendo spring-api-design, agregá validación con Bean Validation al DTO
de crear producto y manejo de errores consistente.
```

```
Diseñá el endpoint de búsqueda de productos con paginación, filtros y links HATEOAS.
```

### spring-testing

```
Escribí tests unitarios AAA para OrderService cubriendo el caso exitoso
y los dos casos de error principales.
```

```
Siguiendo spring-testing, creá un feature file Gherkin para el flujo de crear orden
y los step definitions con Cucumber.
```

```
Escribí un integration test con Testcontainers para el repositorio de productos.
```

### spring-observability

```
Configurá logging estructurado JSON con MDC para traceId y userId en todos los requests.
```

```
Siguiendo spring-observability, agregá métricas custom de Micrometer para
el tiempo de procesamiento de órdenes.
```

```
Configurá health checks custom para la conexión a la base de datos y el servicio de emails.
```

### spring-docker-deploy

```
Creá el Dockerfile multi-stage y docker-compose con PostgreSQL y la app Spring Boot.
```

```
Siguiendo spring-docker-deploy, configurá el GitHub Actions workflow para CI
con build, test y push a Docker Hub.
```

```
Agregá un profile de Docker Compose para desarrollo local con hot-reload.
```

### sonarcloud-gates

```
Configurá JaCoCo en el build.gradle para reportes de cobertura
y el quality gate de SonarCloud.
```

```
Siguiendo sonarcloud-gates, configurá las reglas de complejidad ciclomática
y cognitiva en el proyecto.
```

```
Agregá la integración de SonarCloud al pipeline de GitHub Actions.
```

---

## 6. Tips y Buenas Prácticas

1. **Nombrá la skill cuando el contexto no sea obvio.** Si le pedís "creá un service", no queda claro si querés hexagonal, testing, o qué. Sé explícito.

2. **Combiná skills cuando tenga sentido.** "Usando spring-hexagonal y spring-persistence, creá el módulo completo de pagos" es un pedido válido y potente.

3. **Revisá lo que genera contra la skill.** La skill es la fuente de verdad del proyecto. Si el AI genera algo que contradice la skill, pedile que corrija.

4. **Dá contexto del QUÉ, no del CÓMO.** En vez de "usá @Entity con @Table", decí "creá la entidad de productos con auditoría". La skill ya tiene el CÓMO.

5. **Las skills se complementan entre sí.** La de hexagonal define la estructura, la de persistence define cómo modelar datos dentro de esa estructura, la de testing define cómo verificar todo.

> **Regla de oro**: Si no entendés lo que la IA generó, no lo aceptes. Leé la skill, entendé el patrón, y después evaluá si el código es correcto.

---

## 7. Troubleshooting

### "La IA no sigue los patrones de la skill"

Referenciá la skill explícitamente en tu prompt:
```
Seguí ESTRICTAMENTE la skill spring-hexagonal para esto.
```

### "No sé qué skill usar"

Consultá el archivo `.agent/AGENTS.md`. Cada skill tiene una descripción y condiciones de activación. Si tu tarea involucra varias áreas, combiná las skills relevantes.

### "¿Puedo modificar las skills?"

Sí, son archivos Markdown. Pero coordiná con tu equipo — las skills son convenciones compartidas. Si cambiás una regla, todos los que usen la skill van a generar código con esa nueva regla.

### "Copilot no parece usar las skills"

Copilot no lee `.agent/skills/` directamente. Necesitás crear los archivos en `.github/instructions/` como se explica en la sección 4. Es un paso extra pero necesario.

### "La IA genera código que compila pero no sigue la arquitectura"

Esto pasa cuando no especificás la skill. Sin contexto arquitectónico, la IA genera código "genérico" que funciona pero no respeta las convenciones del proyecto. Siempre mencioná la skill.
