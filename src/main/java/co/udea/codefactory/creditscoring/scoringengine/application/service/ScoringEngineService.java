package co.udea.codefactory.creditscoring.scoringengine.application.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.CalculateScoreUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ScoringEngineService implements CalculateScoreUseCase {

    private final ScoringModelRepositoryPort modeloRepo;
    private final FinancialDataRepositoryPort financialDataRepo;
    private final FinancialDataValueExtractor extractor;
    private final ScoringCalculator calculator;

    public ScoringEngineService(
            ScoringModelRepositoryPort modeloRepo,
            FinancialDataRepositoryPort financialDataRepo,
            FinancialDataValueExtractor extractor,
            ScoringCalculator calculator) {
        this.modeloRepo = modeloRepo;
        this.financialDataRepo = financialDataRepo;
        this.extractor = extractor;
        this.calculator = calculator;
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

        // 4. Delegar cálculo al componente compartido
        return calculator.calcular(modelo, request.aplicanteId(), valoresSolicitante);
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
}
