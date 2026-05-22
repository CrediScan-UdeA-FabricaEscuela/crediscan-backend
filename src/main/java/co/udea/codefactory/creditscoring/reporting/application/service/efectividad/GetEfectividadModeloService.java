package co.udea.codefactory.creditscoring.reporting.application.service.efectividad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.reporting.application.util.ConcordanceCategory;
import co.udea.codefactory.creditscoring.reporting.application.util.ConcordanceClassifier;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.CasoOverride;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.CeldaMatriz;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.IndicadoresEfectividad;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.MatrizConfusion;
import co.udea.codefactory.creditscoring.reporting.domain.port.in.efectividad.GetEfectividadModeloUseCase;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.EfectividadModeloQueryPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.EfectividadModeloQueryPort.MatrizAggregate;
import lombok.RequiredArgsConstructor;

/**
 * Servicio de aplicación para el reporte de efectividad del modelo (HU-016).
 * <p>
 * Realiza el gap-fill de la matriz (5 niveles × 4 decisiones = 20 celdas),
 * agrupando REJECTED bajo VERY_HIGH según la decisión bloqueada #5.
 * Calcula la tasa de concordancia global y las tasas de override con 2 decimales.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GetEfectividadModeloService implements GetEfectividadModeloUseCase {

    /** Niveles de riesgo que aparecen en la matriz (REJECTED se mapea a VERY_HIGH). */
    private static final List<RiskLevel> MATRIX_RISK_LEVELS = List.of(
            RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH
    );

    /** Estados de decisión que aparecen como columnas en la matriz. */
    private static final List<DecisionStatus> MATRIX_DECISIONS = List.of(
            DecisionStatus.APPROVED, DecisionStatus.REJECTED,
            DecisionStatus.MANUAL_REVIEW, DecisionStatus.ESCALATED
    );

    private final EfectividadModeloQueryPort queryPort;

    @Override
    @Transactional(readOnly = true)
    public EfectividadModeloReporte reporte(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String analistaId) {

        validarRango(desde, hasta);

        List<MatrizAggregate> raw = queryPort.queryMatriz(desde, hasta, analistaId);

        if (raw.isEmpty()) {
            return EfectividadModeloReporte.empty(desde, hasta, analistaId);
        }

        // Agrupar REJECTED → VERY_HIGH y construir mapa de conteos
        Map<RiskLevel, Map<DecisionStatus, Long>> conteos = buildConteosMap(raw);

        // Gap-fill: 20 celdas (5 niveles × 4 decisiones)
        List<CeldaMatriz> celdas = buildCeldas(conteos);
        MatrizConfusion matriz = new MatrizConfusion(celdas);

        // Calcular indicadores (denominador = APPROVED + REJECTED solamente)
        IndicadoresEfectividad indicadores = calcularIndicadores(conteos);

        // Overrides individuales
        List<CasoOverride> overrides = buildOverrides(conteos);

        return new EfectividadModeloReporte(matriz, indicadores, overrides, true, desde, hasta, analistaId);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void validarRango(OffsetDateTime desde, OffsetDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "El parámetro 'desde' (" + desde + ") no puede ser posterior a 'hasta' (" + hasta + ")");
        }
    }

    /**
     * Construye el mapa de conteos agrupando REJECTED→VERY_HIGH.
     */
    private Map<RiskLevel, Map<DecisionStatus, Long>> buildConteosMap(List<MatrizAggregate> raw) {
        Map<RiskLevel, Map<DecisionStatus, Long>> map = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : MATRIX_RISK_LEVELS) {
            map.put(level, new EnumMap<>(DecisionStatus.class));
        }
        raw.forEach(agg -> acumularAggregate(agg, map));
        return map;
    }

    /**
     * Acumula un aggregate en el mapa de conteos; ignora valores de enum desconocidos.
     */
    private void acumularAggregate(
            MatrizAggregate agg,
            Map<RiskLevel, Map<DecisionStatus, Long>> map) {
        RiskLevel riskLevel = parsearRiskLevel(agg.riskLevel());
        if (riskLevel == null) return;

        DecisionStatus decision = parsearDecision(agg.decision());
        if (decision == null) return;

        // Decisión bloqueada #5: REJECTED se agrupa bajo VERY_HIGH
        RiskLevel efectivo = ConcordanceClassifier.effectiveLevel(riskLevel);
        map.get(efectivo).merge(decision, agg.count(), Long::sum);
    }

    private static RiskLevel parsearRiskLevel(String value) {
        try {
            return RiskLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null; // ignora valores desconocidos
        }
    }

    private static DecisionStatus parsearDecision(String value) {
        try {
            return DecisionStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null; // ignora valores desconocidos
        }
    }

    /**
     * Crea la lista de 20 celdas con gap-fill (celdas sin datos tienen count=0).
     */
    private List<CeldaMatriz> buildCeldas(Map<RiskLevel, Map<DecisionStatus, Long>> conteos) {
        List<CeldaMatriz> celdas = new ArrayList<>();
        for (RiskLevel level : MATRIX_RISK_LEVELS) {
            Map<DecisionStatus, Long> decMap = conteos.get(level);
            for (DecisionStatus decision : MATRIX_DECISIONS) {
                long count = decMap.getOrDefault(decision, 0L);
                celdas.add(new CeldaMatriz(level, decision, count));
            }
        }
        return celdas;
    }

    /**
     * Calcula la tasa de concordancia global y las tasas de override.
     * El denominador solo incluye APPROVED y REJECTED.
     */
    private IndicadoresEfectividad calcularIndicadores(
            Map<RiskLevel, Map<DecisionStatus, Long>> conteos) {

        long concordantes = 0;
        long overrideAprobacion = 0;
        long overrideRechazo = 0;
        long totalActivo = 0;

        for (RiskLevel level : MATRIX_RISK_LEVELS) {
            Map<DecisionStatus, Long> decMap = conteos.get(level);
            for (DecisionStatus decision : List.of(DecisionStatus.APPROVED, DecisionStatus.REJECTED)) {
                long count = decMap.getOrDefault(decision, 0L);
                totalActivo += count;

                ConcordanceCategory cat = ConcordanceClassifier.classify(level, decision);
                if (cat == ConcordanceCategory.CONCORDANT) {
                    concordantes += count;
                } else if (cat == ConcordanceCategory.OVERRIDE_APPROVE_HIGH_RISK) {
                    overrideAprobacion += count;
                } else if (cat == ConcordanceCategory.OVERRIDE_REJECT_LOW_RISK) {
                    overrideRechazo += count;
                }
            }
        }

        BigDecimal total = BigDecimal.valueOf(totalActivo);
        BigDecimal tasaConcordancia = totalActivo == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(concordantes * 100L)
                        .divide(total, 2, RoundingMode.HALF_UP);

        BigDecimal tasaOverrideAprobacion = totalActivo == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(overrideAprobacion * 100L)
                        .divide(total, 2, RoundingMode.HALF_UP);

        BigDecimal tasaOverrideRechazo = totalActivo == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(overrideRechazo * 100L)
                        .divide(total, 2, RoundingMode.HALF_UP);

        return new IndicadoresEfectividad(tasaConcordancia, tasaOverrideAprobacion,
                tasaOverrideRechazo, totalActivo);
    }

    /**
     * Lista de casos de override individuales (combinaciones con cuenta > 0 y categoría override).
     */
    private List<CasoOverride> buildOverrides(Map<RiskLevel, Map<DecisionStatus, Long>> conteos) {
        List<CasoOverride> overrides = new ArrayList<>();
        for (RiskLevel level : MATRIX_RISK_LEVELS) {
            Map<DecisionStatus, Long> decMap = conteos.get(level);
            for (DecisionStatus decision : List.of(DecisionStatus.APPROVED, DecisionStatus.REJECTED)) {
                long count = decMap.getOrDefault(decision, 0L);
                if (count == 0) continue;
                ConcordanceCategory cat = ConcordanceClassifier.classify(level, decision);
                if (cat == ConcordanceCategory.OVERRIDE_APPROVE_HIGH_RISK
                        || cat == ConcordanceCategory.OVERRIDE_REJECT_LOW_RISK) {
                    overrides.add(new CasoOverride(level, decision, count));
                }
            }
        }
        return overrides;
    }
}
