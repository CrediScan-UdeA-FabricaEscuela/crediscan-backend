package co.udea.codefactory.creditscoring.evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import co.udea.codefactory.creditscoring.evaluation.application.dto.EvaluationProperties;
import co.udea.codefactory.creditscoring.evaluation.application.dto.ExecuteEvaluationCommand;
import co.udea.codefactory.creditscoring.evaluation.application.service.ExecuteEvaluationService;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.ApplicantNoFinancialDataException;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationCooldownException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.KnockoutEvaluationDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.VariableScoreDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.CalculateScoreUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutOperator;

/**
 * Tests unitarios del servicio de aplicación ExecuteEvaluationService.
 * Sin Spring — usa Mockito para aislar el comportamiento.
 */
@ExtendWith(MockitoExtension.class)
class ExecuteEvaluationServiceTest {

    @Mock private EvaluationRepositoryPort evaluationRepository;
    @Mock private CalculateScoreUseCase calculateScoreUseCase;
    @Mock private FinancialDataRepositoryPort financialDataRepository;
    @Mock private EvaluationProperties evaluationProperties;

    @InjectMocks
    private ExecuteEvaluationService service;

    private final UUID applicantId = UUID.randomUUID();
    private final UUID modelId = UUID.randomUUID();
    private final UUID financialDataId = UUID.randomUUID();

    @BeforeEach
    void configurarSecurityContext() {
        // Simular usuario autenticado en el SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("analista", "pass", List.of()));
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void ejecutar_happyPath_persisteEvaluacionYRetornaResultado() {
        // Arrange
        when(evaluationProperties.getCooldownHours()).thenReturn(24L);
        when(evaluationRepository.existsByApplicantIdAndEvaluatedAtAfter(
                eq(applicantId), any(OffsetDateTime.class))).thenReturn(false);

        VariableScoreDetail varDetail = new VariableScoreDetail(
                UUID.randomUUID(), "moras_12_meses", BigDecimal.ZERO,
                "Bajo", 70, BigDecimal.valueOf(0.40), BigDecimal.valueOf(28));
        ScoringResult resultado = ScoringResult.aprobado(
                modelId, applicantId, BigDecimal.valueOf(75),
                List.of(varDetail), List.of());

        when(calculateScoreUseCase.calcular(any(CalculateScoreRequest.class)))
                .thenReturn(resultado);
        when(financialDataRepository.findMaxVersionByApplicantId(applicantId))
                .thenReturn(Optional.of(1));

        FinancialData fd = buildFinancialData();
        when(financialDataRepository.findByApplicantIdAndVersion(applicantId, 1))
                .thenReturn(Optional.of(fd));

        // Simular persistencia devolviendo la misma evaluación
        when(evaluationRepository.save(any(Evaluation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExecuteEvaluationCommand command = new ExecuteEvaluationCommand(applicantId, modelId);
        Evaluation result = service.ejecutar(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.applicantId()).isEqualTo(applicantId);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.knockedOut()).isFalse();
        assertThat(result.evaluatedBy()).isEqualTo("analista");
        assertThat(result.details()).hasSize(1);
    }

    // =========================================================================
    // Cooldown activo
    // =========================================================================

    @Test
    void ejecutar_cooldownActivo_lanzaEvaluationCooldownException() {
        when(evaluationProperties.getCooldownHours()).thenReturn(24L);
        when(evaluationRepository.existsByApplicantIdAndEvaluatedAtAfter(
                eq(applicantId), any(OffsetDateTime.class))).thenReturn(true);

        ExecuteEvaluationCommand command = new ExecuteEvaluationCommand(applicantId, modelId);

        assertThatThrownBy(() -> service.ejecutar(command))
                .isInstanceOf(EvaluationCooldownException.class)
                .hasMessageContaining("últimas 24 horas");
    }

    // =========================================================================
    // Sin datos financieros
    // =========================================================================

    @Test
    void ejecutar_sinDatosFinancieros_lanzaApplicantNoFinancialDataException() {
        when(evaluationProperties.getCooldownHours()).thenReturn(24L);
        when(evaluationRepository.existsByApplicantIdAndEvaluatedAtAfter(
                eq(applicantId), any(OffsetDateTime.class))).thenReturn(false);

        // No hay versión de datos financieros disponible — el servicio falla antes de llamar al motor
        when(financialDataRepository.findMaxVersionByApplicantId(applicantId))
                .thenReturn(Optional.empty());

        ExecuteEvaluationCommand command = new ExecuteEvaluationCommand(applicantId, modelId);

        assertThatThrownBy(() -> service.ejecutar(command))
                .isInstanceOf(ApplicantNoFinancialDataException.class)
                .hasMessageContaining("datos financieros");
    }

    // =========================================================================
    // KO activado
    // =========================================================================

    @Test
    void ejecutar_koActivado_riskLevelEsREJECTEDYknockOutEsTrue() {
        when(evaluationProperties.getCooldownHours()).thenReturn(24L);
        when(evaluationRepository.existsByApplicantIdAndEvaluatedAtAfter(
                eq(applicantId), any(OffsetDateTime.class))).thenReturn(false);

        KnockoutEvaluationDetail koDetail = new KnockoutEvaluationDetail(
                UUID.randomUUID(), "moras_12_meses", KnockoutOperator.GT,
                BigDecimal.valueOf(3), BigDecimal.valueOf(5), true, "Más de 3 moras");
        ScoringResult resultado = ScoringResult.rechazado(
                modelId, applicantId, List.of(koDetail), "Más de 3 moras");

        when(calculateScoreUseCase.calcular(any(CalculateScoreRequest.class)))
                .thenReturn(resultado);
        when(financialDataRepository.findMaxVersionByApplicantId(applicantId))
                .thenReturn(Optional.of(1));

        FinancialData fd = buildFinancialData();
        when(financialDataRepository.findByApplicantIdAndVersion(applicantId, 1))
                .thenReturn(Optional.of(fd));

        when(evaluationRepository.save(any(Evaluation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ExecuteEvaluationCommand command = new ExecuteEvaluationCommand(applicantId, modelId);
        Evaluation result = service.ejecutar(command);

        assertThat(result.knockedOut()).isTrue();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.REJECTED);
        assertThat(result.knockoutReasons()).isEqualTo("Más de 3 moras");
        assertThat(result.knockouts()).hasSize(1);
        assertThat(result.knockouts().get(0).triggered()).isTrue();
    }

    // =========================================================================
    // Helper para construir FinancialData de test
    // =========================================================================

    private FinancialData buildFinancialData() {
        OffsetDateTime ts = OffsetDateTime.now();
        return new FinancialData(
                financialDataId, applicantId, 1,
                BigDecimal.valueOf(50_000_000),
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(5_000_000),
                BigDecimal.valueOf(100_000_000),
                BigDecimal.valueOf(80_000_000),
                false, 36, 0, 0, 720, 2, ts, ts);
    }
}
