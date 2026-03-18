---
agent: agent
description: Implementar una HU en orden hexagonal inside-out
---

Implementa esta User Story en este repositorio respetando arquitectura hexagonal y flujo por fases.

## Reglas

1. Primero analiza y presenta plan.
2. No escribas codigo hasta que yo apruebe el plan.
3. Orden obligatorio: Domain -> Application -> Infrastructure -> Security -> Testing -> Observability.
4. Si hay dependencias no implementadas, crea stubs con `// TODO: HU-XXX`.

## HU

ID: {{id_hu}}
Como: {{como}}
Quiero: {{quiero}}
Para: {{para}}

Criterios de Aceptacion:
{{criterios_aceptacion}}

Reglas de Negocio:
{{reglas_negocio}}

Notas Tecnicas:
{{notas_tecnicas}}

Dependencias:
{{dependencias}}

## Formato del plan que quiero

## Plan: {{id_hu}} - <titulo>
- Modulo objetivo
- Skills a utilizar (en orden)
- CAs y RNs a cubrir
- Dependencias a stubear
- Fases de implementacion

Luego preguntame: "Procedo? (yes / adjust plan)"