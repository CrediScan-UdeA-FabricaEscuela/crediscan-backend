package co.udea.codefactory.creditscoring.creditdecision.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CreditDecisionTest {

    private static final UUID EVALUATION_ID = UUID.randomUUID();
    private static final String ANALYST = "analista";
    private static final String VALID_OBSERVATIONS = "Esta es una observación válida con más de veinte caracteres";

    @Nested
    @DisplayName("Factory method crear()")
    class Create {

        @Test
        @DisplayName("Debe crear una decisión con id y timestamps automáticos")
        void debeCrearDecisionConIdAutomatico() {
            UUID evaluationId = UUID.randomUUID();
            CreditDecision decision = CreditDecision.crear(
                    evaluationId, DecisionStatus.APPROVED, VALID_OBSERVATIONS, ANALYST);

            assertThat(decision.id()).isNotNull();
            assertThat(decision.evaluationId()).isEqualTo(evaluationId);
            assertThat(decision.decision()).isEqualTo(DecisionStatus.APPROVED);
            assertThat(decision.observations()).isEqualTo(VALID_OBSERVATIONS);
            assertThat(decision.analystId()).isEqualTo(ANALYST);
            assertThat(decision.decidedAt()).isNotNull();
            assertThat(decision.createdAt()).isNotNull();
            assertThat(decision.createdBy()).isEqualTo(ANALYST);
        }

        @Test
        @DisplayName("Debe crear decisión con estado REJECTED")
        void debeCrearDecisionConEstadoRejected() {
            CreditDecision decision = CreditDecision.crear(
                    EVALUATION_ID, DecisionStatus.REJECTED, VALID_OBSERVATIONS, ANALYST);
            assertThat(decision.decision()).isEqualTo(DecisionStatus.REJECTED);
        }

        @Test
        @DisplayName("Debe crear decisión con estado MANUAL_REVIEW")
        void debeCrearDecisionConEstadoManualReview() {
            CreditDecision decision = CreditDecision.crear(
                    EVALUATION_ID, DecisionStatus.MANUAL_REVIEW, VALID_OBSERVATIONS, ANALYST);
            assertThat(decision.decision()).isEqualTo(DecisionStatus.MANUAL_REVIEW);
        }

        @Test
        @DisplayName("Debe crear decisión con estado ESCALATED")
        void debeCrearDecisionConEstadoEscalated() {
            CreditDecision decision = CreditDecision.crear(
                    EVALUATION_ID, DecisionStatus.ESCALATED, VALID_OBSERVATIONS, ANALYST);
            assertThat(decision.decision()).isEqualTo(DecisionStatus.ESCALATED);
        }

        @Test
        @DisplayName("decidedAt y createdAt deben ser el mismo momento")
        void decidedAtYCreatedAtDebenSerElMismoMomento() {
            CreditDecision decision = CreditDecision.crear(
                    EVALUATION_ID, DecisionStatus.APPROVED, VALID_OBSERVATIONS, ANALYST);
            assertThat(decision.decidedAt()).isEqualTo(decision.createdAt());
        }

        @Test
        @DisplayName("createdBy y analystId deben ser el mismo valor")
        void createdByYAnalystIdDebenSerElMismo() {
            CreditDecision decision = CreditDecision.crear(
                    EVALUATION_ID, DecisionStatus.APPROVED, VALID_OBSERVATIONS, ANALYST);
            assertThat(decision.createdBy()).isEqualTo(decision.analystId());
        }
    }

    @Nested
    @DisplayName("Factory method rehydrate()")
    class Rehydrate {

        @Test
        @DisplayName("Debe reconstruir una decisión sin generar nuevos ids ni timestamps")
        void debeReconstruirSinGenerarNuevosIds() {
            UUID existingId = UUID.randomUUID();
            OffsetDateTime existingDecidedAt = OffsetDateTime.now();
            OffsetDateTime existingCreatedAt = OffsetDateTime.now();

            CreditDecision decision = CreditDecision.rehydrate(
                    existingId, EVALUATION_ID, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, ANALYST, existingDecidedAt,
                    existingCreatedAt, ANALYST, null, null);

            assertThat(decision.id()).isEqualTo(existingId);
            assertThat(decision.decidedAt()).isEqualTo(existingDecidedAt);
            assertThat(decision.createdAt()).isEqualTo(existingCreatedAt);
        }
    }

    @Nested
    @DisplayName("Validaciones del record")
    class Validations {

        @Test
        @DisplayName("Debe rechazar evaluationId null")
        void debeRechazarEvaluationIdNull() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), null, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, ANALYST,
                    OffsetDateTime.now(), OffsetDateTime.now(), ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("evaluationId");
        }

        @Test
        @DisplayName("Debe rechazar decision null")
        void debeRechazarDecisionNull() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, null,
                    VALID_OBSERVATIONS, ANALYST,
                    OffsetDateTime.now(), OffsetDateTime.now(), ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("estado");
        }

        @Test
        @DisplayName("Debe rechazar observaciones con menos de 20 caracteres")
        void debeRechazarObservacionesCortas() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    "Observación corta", ANALYST,
                    OffsetDateTime.now(), OffsetDateTime.now(), ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("20 caracteres");
        }

        @Test
        @DisplayName("Debe aceptar observaciones con exactamente 20 caracteres")
        void debeAceptarObservacionesDeVeinteCaracteres() {
            String exactly20 = "12345678901234567890";
            assertThat(exactly20.length()).isEqualTo(20);
            // No debe lanzar excepción
            new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    exactly20, ANALYST,
                    OffsetDateTime.now(), OffsetDateTime.now(), ANALYST, null, null);
        }

        @Test
        @DisplayName("Debe rechazar analystId null")
        void debeRechazarAnalystIdNull() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, null,
                    OffsetDateTime.now(), OffsetDateTime.now(), ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("analista");
        }

        @Test
        @DisplayName("Debe rechazar analystId en blanco")
        void debeRechazarAnalystIdEnBlanco() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, "   ",
                    OffsetDateTime.now(), OffsetDateTime.now(), ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("analista");
        }

        @Test
        @DisplayName("Debe rechazar decidedAt null")
        void debeRechazarDecidedAtNull() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, ANALYST,
                    null, OffsetDateTime.now(), ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha de decisión");
        }

        @Test
        @DisplayName("Debe rechazar createdAt null")
        void debeRechazarCreatedAtNull() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, ANALYST,
                    OffsetDateTime.now(), null, ANALYST, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha de creación");
        }

        @Test
        @DisplayName("Debe rechazar createdBy null")
        void debeRechazarCreatedByNull() {
            assertThatThrownBy(() -> new CreditDecision(
                    UUID.randomUUID(), EVALUATION_ID, DecisionStatus.APPROVED,
                    VALID_OBSERVATIONS, ANALYST,
                    OffsetDateTime.now(), OffsetDateTime.now(), null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("creador");
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class Immutability {

        @Test
        @DisplayName("El record debe ser inmutable (fields final)")
        void debeSerInmutable() {
            CreditDecision decision = CreditDecision.crear(
                    EVALUATION_ID, DecisionStatus.APPROVED, VALID_OBSERVATIONS, ANALYST);

            // Los fields de record son final por defecto — no hay setters
            // Esto se verifica en tiempo de compilación
            assertThat(decision.id()).isNotNull();
            assertThat(decision.decision()).isEqualTo(DecisionStatus.APPROVED);
        }
    }
}
