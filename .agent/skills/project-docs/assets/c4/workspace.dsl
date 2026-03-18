workspace "Credit Risk Scoring Engine" "Sistema de scoring de riesgo crediticio - Universidad de Antioquia, CodeF@ctory Advanced" {

    model {
        # ─────────────────────────────────────────
        # PERSONAS / ACTORES
        # ─────────────────────────────────────────
        analista = person "Analista de Crédito" "Evalúa solicitudes de crédito, consulta el score de riesgo y toma decisiones de aprobación/rechazo." "Analyst"
        admin    = person "Administrador" "Gestiona usuarios, roles, reglas de scoring y versiones del modelo." "Admin"

        # ─────────────────────────────────────────
        # SISTEMAS EXTERNOS
        # ─────────────────────────────────────────
        bureaExterno = softwareSystem "Bureaux de Crédito (externo)" "Proveedor externo de historial crediticio (p. ej. Experian, DataCrédito). Consultado opcionalmente." "External"
        emailSystem  = softwareSystem "Servicio de Email" "Envío de notificaciones de resultado de evaluación (SMTP / SendGrid)." "External"

        # ─────────────────────────────────────────
        # SISTEMA PRINCIPAL — Level 1 Context
        # ─────────────────────────────────────────
        creditRiskSystem = softwareSystem "Credit Risk Scoring Engine" "Calcula el score de riesgo crediticio de un solicitante aplicando reglas configurables y modelos de scoring. Expone una API REST versionada con HATEOAS." {

            # ─────────────────────────────────────
            # CONTAINERS — Level 2
            # ─────────────────────────────────────
            webApp = container "Spring Boot Application" "Núcleo de la aplicación. Expone la API REST, ejecuta la lógica de scoring y persiste los datos." "Java 21, Spring Boot 3.4" "SpringBoot" {

                # ─────────────────────────────────
                # COMPONENTS — Level 3
                # ─────────────────────────────────

                # --- Inbound Adapters (driving) ---
                restController = component "REST Controllers" "Recibe peticiones HTTP. Maneja versionado /api/v1/**. Implementa HATEOAS." "Spring MVC @RestController" "Adapter-In"

                securityFilter = component "Security Filter Chain" "Valida JWT, aplica RBAC/ABAC, registra auditoría de acceso." "Spring Security 6.x" "Adapter-In"

                # --- Application / Use Cases ---
                applicantUC    = component "Applicant Use Cases" "CU: registrar solicitante, consultar perfil, actualizar datos." "com.udea.creditrisk.applicant.application" "UseCase"
                financialUC    = component "Financial Data Use Cases" "CU: cargar datos financieros, calcular ratios." "com.udea.creditrisk.financialdata.application" "UseCase"
                scoringUC      = component "Scoring Use Cases" "CU: ejecutar motor de scoring, versionar modelo, administrar reglas." "com.udea.creditrisk.scoring.application" "UseCase"
                evaluationUC   = component "Evaluation Use Cases" "CU: crear evaluación de riesgo, clasificar riesgo, generar decisión." "com.udea.creditrisk.evaluation.application" "UseCase"
                reportingUC    = component "Reporting Use Cases" "CU: generar reportes, exportar historial de decisiones." "com.udea.creditrisk.reporting.application" "UseCase"

                # --- Domain (Ports) ---
                domainPorts    = component "Domain Ports (interfaces)" "IApplicantRepository, IScoringEngine, IEvaluationRepository, etc." "com.udea.creditrisk.*.domain.port" "Port"

                # --- Outbound Adapters (driven) ---
                jpaAdapters    = component "JPA Adapters" "Implementan los repository ports usando Spring Data JPA + Flyway." "com.udea.creditrisk.*.infrastructure.persistence" "Adapter-Out"
                bureaAdapter   = component "Bureau Adapter" "Llama al bureaux externo vía HTTP (Feign/RestClient)." "com.udea.creditrisk.financialdata.infrastructure.external" "Adapter-Out"
                emailAdapter   = component "Email Adapter" "Envía notificaciones al solicitante vía SMTP/SendGrid." "com.udea.creditrisk.evaluation.infrastructure.notification" "Adapter-Out"

                # --- Shared / Cross-cutting ---
                auditModule    = component "Audit Module" "Intercepta operaciones y persiste AuditLog. Anotación @Auditable." "com.udea.creditrisk.shared.audit" "Shared"
                securityModule = component "Security Module" "Utilidades JWT, UserDetails, PasswordEncoder." "com.udea.creditrisk.shared.security" "Shared"
                loggingModule  = component "Logging Module" "Structured logging (JSON), MDC correlation-id, OpenTelemetry." "com.udea.creditrisk.shared.logging" "Shared"
            }

            database   = container "PostgreSQL 16" "Almacena solicitantes, datos financieros, evaluaciones, scores, decisiones, auditoría." "PostgreSQL 16" "Database"
            cache      = container "Redis (opcional)" "Cache de scores recientes y sesiones JWT revocadas. Sprint 3." "Redis 7" "Cache"
            prometheus = container "Prometheus" "Scrape de métricas expuestas por /actuator/prometheus." "Prometheus" "Monitoring"
            grafana    = container "Grafana" "Dashboards de métricas operativas y de negocio." "Grafana" "Monitoring"
        }

        # ─────────────────────────────────────────
        # RELATIONSHIPS — Level 1
        # ─────────────────────────────────────────
        analista -> creditRiskSystem "Evalúa solicitudes y consulta scores" "HTTPS"
        admin    -> creditRiskSystem "Administra configuración y usuarios" "HTTPS"
        creditRiskSystem -> bureaExterno "Consulta historial crediticio" "HTTPS/REST"
        creditRiskSystem -> emailSystem  "Envía notificaciones" "SMTP/HTTPS"

        # ─────────────────────────────────────────
        # RELATIONSHIPS — Level 2
        # ─────────────────────────────────────────
        analista -> webApp   "Usa API REST" "HTTPS/JSON"
        admin    -> webApp   "Administra" "HTTPS/JSON"
        webApp   -> database "Lee y escribe" "JDBC/JPA"
        webApp   -> cache    "Cache de scores y blacklist JWT" "TCP 6379"
        prometheus -> webApp "Scrape /actuator/prometheus" "HTTP"
        grafana    -> prometheus "Consulta métricas" "HTTP PromQL"
        webApp   -> bureaExterno "Consulta historial" "HTTPS"
        webApp   -> emailSystem  "Notificaciones" "SMTP"

        # ─────────────────────────────────────────
        # RELATIONSHIPS — Level 3 (Components)
        # ─────────────────────────────────────────
        analista      -> restController "HTTP Request" "HTTPS"
        restController -> securityFilter "Filtrado autenticación/autorización"
        restController -> applicantUC   "Delega CU solicitante"
        restController -> financialUC   "Delega CU datos financieros"
        restController -> scoringUC     "Delega CU scoring"
        restController -> evaluationUC  "Delega CU evaluación"
        restController -> reportingUC   "Delega CU reportes"

        applicantUC  -> domainPorts "Usa ports"
        financialUC  -> domainPorts "Usa ports"
        scoringUC    -> domainPorts "Usa ports"
        evaluationUC -> domainPorts "Usa ports"
        reportingUC  -> domainPorts "Usa ports"

        domainPorts -> jpaAdapters   "Implementados por"
        domainPorts -> bureaAdapter  "Implementado por"
        domainPorts -> emailAdapter  "Implementado por"

        jpaAdapters  -> database "SQL vía JPA"
        bureaAdapter -> bureaExterno "REST call"
        emailAdapter -> emailSystem  "SMTP"

        securityFilter -> securityModule "Usa"
        restController -> auditModule    "Interceptado por"
        restController -> loggingModule  "Usa MDC"
    }

    # ─────────────────────────────────────────────
    # VIEWS
    # ─────────────────────────────────────────────
    views {

        # Level 1 — System Context
        systemContext creditRiskSystem "SystemContext" {
            include *
            autoLayout lr
            title "Level 1 — System Context: Credit Risk Scoring Engine"
            description "Usuarios y sistemas externos que interactúan con el sistema."
        }

        # Level 2 — Container
        container creditRiskSystem "Containers" {
            include *
            autoLayout lr
            title "Level 2 — Container: Credit Risk Scoring Engine"
            description "Contenedores que componen el sistema y sus responsabilidades."
        }

        # Level 3 — Component (Spring Boot App)
        component webApp "Components" {
            include *
            autoLayout tb
            title "Level 3 — Component: Spring Boot Application"
            description "Módulos internos de la aplicación siguiendo arquitectura hexagonal."
        }

        # ─────────────────────────────────────────
        # STYLES
        # ─────────────────────────────────────────
        styles {
            element "Person" {
                shape Person
                background "#08427B"
                color "#ffffff"
            }
            element "Admin" {
                shape Person
                background "#1168BD"
                color "#ffffff"
            }
            element "Software System" {
                background "#1168BD"
                color "#ffffff"
            }
            element "External" {
                background "#999999"
                color "#ffffff"
            }
            element "SpringBoot" {
                background "#6DB33F"
                color "#ffffff"
            }
            element "Database" {
                shape Cylinder
                background "#336791"
                color "#ffffff"
            }
            element "Cache" {
                shape Cylinder
                background "#DC382D"
                color "#ffffff"
            }
            element "Monitoring" {
                background "#E6522C"
                color "#ffffff"
            }
            element "Adapter-In" {
                background "#85BBF0"
                color "#000000"
            }
            element "Adapter-Out" {
                background "#F5A623"
                color "#000000"
            }
            element "UseCase" {
                background "#6DB33F"
                color "#ffffff"
            }
            element "Port" {
                background "#ffffff"
                color "#000000"
                border dashed
            }
            element "Shared" {
                background "#CCCCCC"
                color "#000000"
            }
        }

        theme default
    }
}
