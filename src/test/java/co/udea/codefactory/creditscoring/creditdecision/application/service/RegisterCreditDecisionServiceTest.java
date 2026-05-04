package co.udea.codefactory.creditscoring.creditdecision.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import co.udea.codefactory.creditscoring.creditdecision.application.dto.RegisterCreditDecisionCommand;
import co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionAlreadyExistsException;
import co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionKnockoutException;
import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.EscalationNotificationPort;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterCreditDecisionServiceTest {

    @Mock
    private CreditDecisionRepositoryPort creditDecisionRepository;

    @Mock
    private GetEvaluationUseCase getEvaluationUseCase;

    @Mock
    private EscalationNotificationPort escalationNotificationPort;

    @InjectMocks
    private RegisterCreditDecisionService service;

    private static final UUID EVALUATION_ID = UUID.randomUUID();
    private static final String ANALYST = "analista";
    private static final String VALID_OBSERVATIONS = "Esta es una observación válida con más de veinte caracteres";

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(ANALYST);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Happy path — registrar decisión")
    class HappyPath {

        @Test
        @DisplayName("Debe registrar una decisión APPROVED exitosamente")
        void debeRegistrarDecisionApproada() {
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> {
                CreditDecision d = invocation.getArgument(0);
                return new CreditDecision(d.id(), d.evaluationId(), d.decision(),
                        d.observations(), d.analystId(), d.decidedAt(), d.createdAt(), d.createdBy(),
                        d.supervisorId(), d.resolutionDeadlineAt());
            });

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "APPROVED", VALID_OBSERVATIONS);

            CreditDecision result = service.registrar(command);

            assertThat(result.decision()).isEqualTo(DecisionStatus.APPROVED);
            assertThat(result.observations()).isEqualTo(VALID_OBSERVATIONS);
            assertThat(result.analystId()).isEqualTo(ANALYST);
            verify(creditDecisionRepository).save(any(CreditDecision.class));
        }

        @Test
        @DisplayName("Debe registrar una decisión ESCALATED exitosamente")
        void debeRegistrarDecisionEscalated() {
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> {
                CreditDecision d = invocation.getArgument(0);
                return new CreditDecision(d.id(), d.evaluationId(), d.decision(),
                        d.observations(), d.analystId(), d.decidedAt(), d.createdAt(), d.createdBy(),
                        d.supervisorId(), d.resolutionDeadlineAt());
            });

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "ESCALATED", VALID_OBSERVATIONS);

            CreditDecision result = service.registrar(command);

            assertThat(result.decision()).isEqualTo(DecisionStatus.ESCALATED);
        }

        @Test
        @DisplayName("Debe registrar una decisión MANUAL_REVIEW exitosamente")
        void debeRegistrarDecisionManualReview() {
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> {
                CreditDecision d = invocation.getArgument(0);
                return new CreditDecision(d.id(), d.evaluationId(), d.decision(),
                        d.observations(), d.analystId(), d.decidedAt(), d.createdAt(), d.createdBy(),
                        d.supervisorId(), d.resolutionDeadlineAt());
            });

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "MANUAL_REVIEW", VALID_OBSERVATIONS);

            CreditDecision result = service.registrar(command);

            assertThat(result.decision()).isEqualTo(DecisionStatus.MANUAL_REVIEW);
        }
    }

    @Nested
    @DisplayName("CA1 — Solo una decisión por evaluación")
    class OnlyOneDecisionPerEvaluation {

        @Test
        @DisplayName("Debe lanzar CreditDecisionAlreadyExistsException si ya existe decisión")
        void debeLanzarExceptionSiYaExisteDecision() {
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(true);

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "APPROVED", VALID_OBSERVATIONS);

            assertThatThrownBy(() -> service.registrar(command))
                    .isInstanceOf(CreditDecisionAlreadyExistsException.class)
                    .hasMessageContaining("Ya existe una decisión");
        }
    }

    @Nested
    @DisplayName("RN1 — Knockout restriction")
    class KnockoutRestriction {

        @Test
        @DisplayName("Debe rechazar APPROVED si evaluación fue knock-out")
        void debeRechazarApprovedSiKnockout() {
            Evaluation evaluation = createEvaluation(true);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "APPROVED", VALID_OBSERVATIONS);

            assertThatThrownBy(() -> service.registrar(command))
                    .isInstanceOf(CreditDecisionKnockoutException.class)
                    .hasMessageContaining("knock-out");
        }

        @Test
        @DisplayName("Debe rechazar ESCALATED si evaluación fue knock-out")
        void debeRechazarEscalatedSiKnockout() {
            Evaluation evaluation = createEvaluation(true);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "ESCALATED", VALID_OBSERVATIONS);

            assertThatThrownBy(() -> service.registrar(command))
                    .isInstanceOf(CreditDecisionKnockoutException.class);
        }

        @Test
        @DisplayName("Debe permitir REJECTED si evaluación fue knock-out")
        void debePermitirRejectedSiKnockout() {
            Evaluation evaluation = createEvaluation(true);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> {
                CreditDecision d = invocation.getArgument(0);
                return new CreditDecision(d.id(), d.evaluationId(), d.decision(),
                        d.observations(), d.analystId(), d.decidedAt(), d.createdAt(), d.createdBy(),
                        d.supervisorId(), d.resolutionDeadlineAt());
            });

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "REJECTED", VALID_OBSERVATIONS);

            CreditDecision result = service.registrar(command);

            assertThat(result.decision()).isEqualTo(DecisionStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada")
    class InputValidation {

        @Test
        @DisplayName("Debe rechazar estado de decisión inválido")
        void debeRechazarEstadoInvalido() {
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(createEvaluation(false));

            RegisterCreditDecisionCommand command = new RegisterCreditDecisionCommand(
                    EVALUATION_ID, "CANCELLED", VALID_OBSERVATIONS);

            assertThatThrownBy(() -> service.registrar(command))
                    .isInstanceOf(co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionValidationException.class)
                    .hasMessageContaining("Estado de decisión inválido");
        }
    }

    @Nested
    @DisplayName("CA7/RN4 — Escalation notification and 48h deadline")
    class EscalationBehavior {

        @Test
        @DisplayName("Debe notificar al puerto de escalamiento cuando la decisión es ESCALATED")
        void debeNotificarEscalamientoCuandoDecisionEsEscalated() {
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> {
                CreditDecision d = invocation.getArgument(0);
                return new CreditDecision(d.id(), d.evaluationId(), d.decision(),
                        d.observations(), d.analystId(), d.decidedAt(), d.createdAt(), d.createdBy(),
                        d.supervisorId(), d.resolutionDeadlineAt());
            });

            service.registrar(new RegisterCreditDecisionCommand(EVALUATION_ID, "ESCALATED", VALID_OBSERVATIONS));

            verify(escalationNotificationPort).notifyEscalation(any(CreditDecision.class));
        }

        @Test
        @DisplayName("No debe notificar cuando la decisión NO es ESCALATED")
        void noDebeNotificarCuandoDecisionNoEsEscalated() {
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> {
                CreditDecision d = invocation.getArgument(0);
                return new CreditDecision(d.id(), d.evaluationId(), d.decision(),
                        d.observations(), d.analystId(), d.decidedAt(), d.createdAt(), d.createdBy(),
                        d.supervisorId(), d.resolutionDeadlineAt());
            });

            service.registrar(new RegisterCreditDecisionCommand(EVALUATION_ID, "APPROVED", VALID_OBSERVATIONS));

            org.mockito.Mockito.verifyNoInteractions(escalationNotificationPort);
        }

        @Test
        @DisplayName("Debe calcular el deadline a 48h para decisiones ESCALATED (RN4)")
        void debeCalcularDeadlineDe48HorasParaEscalated() {
            java.time.OffsetDateTime antes = java.time.OffsetDateTime.now();
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CreditDecision result = service.registrar(
                    new RegisterCreditDecisionCommand(EVALUATION_ID, "ESCALATED", VALID_OBSERVATIONS));

            assertThat(result.resolutionDeadlineAt()).isNotNull();
            assertThat(result.resolutionDeadlineAt()).isAfterOrEqualTo(antes.plusHours(48));
            assertThat(result.resolutionDeadlineAt()).isBefore(antes.plusHours(48).plusMinutes(1));
        }

        @Test
        @DisplayName("resolutionDeadlineAt debe ser null para decisiones que no son ESCALATED")
        void resolutionDeadlineAtDebeSerNullParaNoEscalated() {
            Evaluation evaluation = createEvaluation(false);
            when(getEvaluationUseCase.obtenerPorId(EVALUATION_ID)).thenReturn(evaluation);
            when(creditDecisionRepository.existsByEvaluationId(EVALUATION_ID)).thenReturn(false);
            when(creditDecisionRepository.save(any(CreditDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CreditDecision result = service.registrar(
                    new RegisterCreditDecisionCommand(EVALUATION_ID, "APPROVED", VALID_OBSERVATIONS));

            assertThat(result.resolutionDeadlineAt()).isNull();
        }
    }

    // Helper
    private Evaluation createEvaluation(boolean knockedOut) {
        return Evaluation.rehydrate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                java.math.BigDecimal.valueOf(75),
                co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel.LOW,
                knockedOut,
                knockedOut ? "Regla knockout activada" : null,
                java.time.OffsetDateTime.now(),
                "analista",
                java.time.OffsetDateTime.now(),
                "analista",
                java.util.List.of(),
                java.util.List.of()
        );
    }
}
