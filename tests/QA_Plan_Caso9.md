# Plan de Pruebas de Calidad - Motor de Scoring (Caso 9)

## HU-008: Regla Knock-out (Rechazo Automático)
**Feature:** Evaluación de reglas excluyentes de crédito.

  **Scenario:** El sistema bloquea a usuarios con fraude activo.
    **Given** que el solicitante tiene el ID "103244"
    **And** su estado en el Bureau de Crédito es "Fraude Activo"
    **When** el motor de scoring calcula el puntaje final
    **Then** el sistema asigna un puntaje de "0"
    **And** clasifica el riesgo como "Muy Alto"
    **And** bloquea la solicitud inmediatamente.

## Criterios de Aceptación Técnicos (Definition of Done)
1. Cobertura de pruebas unitarias (AAA) >= 40%.
2. Cero vulnerabilidades críticas en SonarCloud.
