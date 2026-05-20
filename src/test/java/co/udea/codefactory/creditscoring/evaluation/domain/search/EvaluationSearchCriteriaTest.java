package co.udea.codefactory.creditscoring.evaluation.domain.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.DecisionFilterValue;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;

/**
 * Tests unitarios del record {@link EvaluationSearchCriteria}.
 * Verifica invariantes del compact constructor.
 */
class EvaluationSearchCriteriaTest {

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-06-30T23:59:59Z");

    @Test
    void fechaDesde_nula_lanzaEvaluationValidationException() {
        assertThatThrownBy(() -> new EvaluationSearchCriteria(null, HASTA, null, null, null, null, null))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void fechaHasta_nula_lanzaEvaluationValidationException() {
        assertThatThrownBy(() -> new EvaluationSearchCriteria(DESDE, null, null, null, null, null, null))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void rangoMayorA365Dias_lanzaEvaluationValidationException() {
        OffsetDateTime hastaFuera = DESDE.plusDays(366);
        assertThatThrownBy(() -> new EvaluationSearchCriteria(DESDE, hastaFuera, null, null, null, null, null))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void hastaAntesQueDesde_lanzaEvaluationValidationException() {
        assertThatThrownBy(() -> new EvaluationSearchCriteria(HASTA, DESDE, null, null, null, null, null))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void puntajeMinMayorQueMax_lanzaEvaluationValidationException() {
        assertThatThrownBy(() -> new EvaluationSearchCriteria(
                DESDE, HASTA, null, new BigDecimal("80"), new BigDecimal("40"), null, null))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void listaVaciaNiveles_normalizaANull() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, List.of(), null, null, null, null);
        assertThat(criteria.niveles()).isNull();
    }

    @Test
    void listaVaciaDecisiones_normalizaANull() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, List.of(), null);
        assertThat(criteria.decisiones()).isNull();
    }

    @Test
    void happyPath_criterioValido_construido() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA,
                List.of(RiskLevel.HIGH, RiskLevel.MEDIUM),
                new BigDecimal("40"), new BigDecimal("80"),
                List.of(DecisionFilterValue.APPROVED, DecisionFilterValue.SIN_DECISION),
                "jsmith");
        assertThat(criteria.fechaDesde()).isEqualTo(DESDE);
        assertThat(criteria.fechaHasta()).isEqualTo(HASTA);
        assertThat(criteria.niveles()).containsExactlyInAnyOrder(RiskLevel.HIGH, RiskLevel.MEDIUM);
        assertThat(criteria.puntajeMin()).isEqualByComparingTo("40");
        assertThat(criteria.puntajeMax()).isEqualByComparingTo("80");
        assertThat(criteria.decisiones()).containsExactlyInAnyOrder(
                DecisionFilterValue.APPROVED, DecisionFilterValue.SIN_DECISION);
        assertThat(criteria.analista()).isEqualTo("jsmith");
    }
}
