package co.udea.codefactory.creditscoring.scoringengine.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.KnockoutEvaluationDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.VariableScoreDetail;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.KnockoutRuleRepositoryPort;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;

/**
 * Núcleo del cálculo de scoring: evaluación de reglas knockout y cómputo
 * del puntaje ponderado.
 *
 * <p>Puede ser reutilizado tanto por el motor real (con datos financieros del solicitante)
 * como por el modo de simulación (con valores ingresados manualmente).</p>
 */
@Component
public class ScoringCalculator {

    private final KnockoutRuleRepositoryPort koRepo;
    private final ScoringVariableRepositoryPort variableRepo;

    public ScoringCalculator(
            KnockoutRuleRepositoryPort koRepo,
            ScoringVariableRepositoryPort variableRepo) {
        this.koRepo = koRepo;
        this.variableRepo = variableRepo;
    }

    /**
     * Calcula el resultado de scoring para un modelo y un mapa de valores observados.
     *
     * @param modelo     modelo de scoring a aplicar (DRAFT o ACTIVE)
     * @param contextoId UUID que identifica el contexto (solicitante o escenario de simulación)
     * @param valores    mapa de nombre_campo → valor observado
     * @return resultado con puntaje, desglose por variable y detalle de reglas KO evaluadas
     */
    public ScoringResult calcular(ScoringModel modelo, UUID contextoId, Map<String, BigDecimal> valores) {
        // 1. Evaluar reglas knockout en orden de prioridad (CA3, RN2, RN5)
        List<KnockoutRule> koRules = koRepo.findActivasByModeloId(modelo.id());
        List<KnockoutEvaluationDetail> koEvaluadas = new ArrayList<>();

        for (KnockoutRule regla : koRules) {
            BigDecimal valorObservado = buscarValor(valores, regla.campo())
                    .orElse(BigDecimal.ZERO); // RN3: campo sin dato → valor 0

            boolean activada = regla.evaluar(valorObservado);
            koEvaluadas.add(new KnockoutEvaluationDetail(
                    regla.id(), regla.campo(), regla.operador(),
                    regla.umbral(), valorObservado, activada, regla.mensaje()));

            if (activada) {
                // RN2: rechazo inmediato — no se calcula el puntaje
                return ScoringResult.rechazado(modelo.id(), contextoId, koEvaluadas, regla.mensaje());
            }
        }

        // 2. Calcular puntaje ponderado (CA1)
        List<VariableScoreDetail> desglose = new ArrayList<>();
        BigDecimal puntajeFinal = BigDecimal.ZERO;

        for (ModelVariable mv : modelo.variables()) {
            ScoringVariable variable = variableRepo.findById(mv.variableId()).orElse(null);
            if (variable == null) {
                continue; // variable eliminada — se omite del cálculo
            }

            String campoNormalizado = normalizarNombre(variable.nombre());
            BigDecimal valorObservado = buscarValor(valores, campoNormalizado)
                    .orElse(BigDecimal.ZERO); // CA2/RN3: sin dato → puntaje 0

            int puntajeParcial;
            String etiquetaRango;

            if (variable.tipo() == VariableType.NUMERIC) {
                VariableRange rango = buscarRangoNumerico(variable.rangos(), valorObservado);
                if (rango != null) {
                    puntajeParcial = rango.puntaje();
                    etiquetaRango = rango.etiqueta() != null ? rango.etiqueta() : "Sin etiqueta";
                } else {
                    puntajeParcial = 0;
                    etiquetaRango = "Fuera de rango";
                }
            } else {
                VariableCategory cat = buscarCategoria(
                        variable.categorias(), valorObservado.toPlainString());
                if (cat != null) {
                    puntajeParcial = cat.puntaje();
                    etiquetaRango = cat.categoria();
                } else {
                    puntajeParcial = 0;
                    etiquetaRango = "Sin categoría";
                }
            }

            BigDecimal contribucion = BigDecimal.valueOf(puntajeParcial)
                    .multiply(mv.peso())
                    .setScale(4, RoundingMode.HALF_UP);
            puntajeFinal = puntajeFinal.add(contribucion);

            desglose.add(new VariableScoreDetail(
                    variable.id(), variable.nombre(),
                    valorObservado, etiquetaRango,
                    puntajeParcial, mv.peso(), contribucion));
        }

        puntajeFinal = puntajeFinal.setScale(2, RoundingMode.HALF_UP);

        return ScoringResult.aprobado(modelo.id(), contextoId, puntajeFinal, desglose, koEvaluadas);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private Optional<BigDecimal> buscarValor(Map<String, BigDecimal> mapa, String campo) {
        String key = campo.toLowerCase().replace(" ", "_");
        return Optional.ofNullable(mapa.get(key));
    }

    private VariableRange buscarRangoNumerico(List<VariableRange> rangos, BigDecimal valor) {
        return rangos.stream()
                .filter(r -> valor.compareTo(r.limiteInferior()) >= 0
                          && valor.compareTo(r.limiteSuperior()) < 0)
                .min(Comparator.comparing(VariableRange::limiteInferior))
                .orElse(null);
    }

    private VariableCategory buscarCategoria(List<VariableCategory> categorias, String valor) {
        return categorias.stream()
                .filter(c -> c.categoria().equalsIgnoreCase(valor))
                .findFirst()
                .orElse(null);
    }

    private String normalizarNombre(String nombre) {
        return nombre.toLowerCase()
                .replace(" ", "_")
                .replace("-", "_");
    }
}
