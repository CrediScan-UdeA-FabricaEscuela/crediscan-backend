package co.udea.codefactory.creditscoring.scoringengine.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.KnockoutEvaluationDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.VariableScoreDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.CalculateScoreUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.KnockoutRuleRepositoryPort;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ScoringEngineService implements CalculateScoreUseCase {

    private final ScoringModelRepositoryPort modeloRepo;
    private final KnockoutRuleRepositoryPort koRepo;
    private final ScoringVariableRepositoryPort variableRepo;
    private final FinancialDataRepositoryPort financialDataRepo;
    private final FinancialDataValueExtractor extractor;

    public ScoringEngineService(
            ScoringModelRepositoryPort modeloRepo,
            KnockoutRuleRepositoryPort koRepo,
            ScoringVariableRepositoryPort variableRepo,
            FinancialDataRepositoryPort financialDataRepo,
            FinancialDataValueExtractor extractor) {
        this.modeloRepo = modeloRepo;
        this.koRepo = koRepo;
        this.variableRepo = variableRepo;
        this.financialDataRepo = financialDataRepo;
        this.extractor = extractor;
    }

    @Override
    public ScoringResult calcular(CalculateScoreRequest request) {
        // 1. Resolver el modelo (por ID o el activo)
        ScoringModel modelo = resolverModelo(request.modeloId());

        // 2. Obtener la versión más reciente de los datos financieros del solicitante
        int maxVersion = financialDataRepo.findMaxVersionByApplicantId(request.aplicanteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay datos financieros para el solicitante: " + request.aplicanteId()));
        FinancialData datos = financialDataRepo
                .findByApplicantIdAndVersion(request.aplicanteId(), maxVersion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay datos financieros para el solicitante: " + request.aplicanteId()));

        // 3. Extraer valores como mapa de campos
        Map<String, BigDecimal> valoresSolicitante = extractor.extraer(datos);

        // 4. Evaluar reglas knockout en orden de prioridad (CA3, RN2, RN5)
        List<KnockoutRule> koRules = koRepo.findActivasByModeloId(modelo.id());
        List<KnockoutEvaluationDetail> koEvaluadas = new ArrayList<>();

        for (KnockoutRule regla : koRules) {
            BigDecimal valorObservado = extractor
                    .buscarValor(valoresSolicitante, regla.campo())
                    .orElse(BigDecimal.ZERO); // RN3: campo sin dato → valor 0

            boolean activada = regla.evaluar(valorObservado);
            koEvaluadas.add(new KnockoutEvaluationDetail(
                    regla.id(), regla.campo(), regla.operador(),
                    regla.umbral(), valorObservado, activada, regla.mensaje()));

            if (activada) {
                // RN2: rechazo inmediato — no se calcula el puntaje
                return ScoringResult.rechazado(
                        modelo.id(), request.aplicanteId(),
                        koEvaluadas, regla.mensaje());
            }
        }

        // 5. Calcular puntaje ponderado (CA1)
        List<VariableScoreDetail> desglose = new ArrayList<>();
        BigDecimal puntajeFinal = BigDecimal.ZERO;

        for (ModelVariable mv : modelo.variables()) {
            ScoringVariable variable = variableRepo.findById(mv.variableId())
                    .orElse(null);

            if (variable == null) {
                // Variable eliminada — se omite del cálculo (advertencia implícita)
                continue;
            }

            // Buscar el valor del solicitante para esta variable (por nombre normalizado)
            String campoNormalizado = normalizarNombre(variable.nombre());
            BigDecimal valorObservado = extractor
                    .buscarValor(valoresSolicitante, campoNormalizado)
                    .orElse(BigDecimal.ZERO); // CA2/RN3: sin dato → puntaje 0

            // Calcular el puntaje parcial según el tipo de variable
            int puntajeParcial;
            String etiquetaRango;

            if (variable.tipo() == VariableType.NUMERIC) {
                VariableRange rango = buscarRangoNumerico(variable.rangos(), valorObservado);
                if (rango != null) {
                    puntajeParcial = rango.puntaje();
                    etiquetaRango = rango.etiqueta() != null ? rango.etiqueta() : "Sin etiqueta";
                } else {
                    // CA2: valor fuera de todos los rangos → puntaje 0
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

        // RN1: el puntaje final está en el rango 0-100 (la fórmula lo garantiza por diseño)
        puntajeFinal = puntajeFinal.setScale(2, RoundingMode.HALF_UP);

        return ScoringResult.aprobado(
                modelo.id(), request.aplicanteId(),
                puntajeFinal, desglose, koEvaluadas);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ScoringModel resolverModelo(UUID modeloId) {
        if (modeloId != null) {
            return modeloRepo.findById(modeloId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Modelo de scoring no encontrado: " + modeloId));
        }
        return modeloRepo.findActive()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay un modelo de scoring activo en el sistema"));
    }

    /**
     * Busca el rango numérico que contiene {@code valor}.
     * Un rango abarca [limiteInferior, limiteSuperior).
     */
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

    /** Convierte el nombre de la variable a snake_case para buscar en el mapa de campos. */
    private String normalizarNombre(String nombre) {
        return nombre.toLowerCase()
                .replace(" ", "_")
                .replace("-", "_");
    }
}
