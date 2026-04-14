package co.udea.codefactory.creditscoring.scoring.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableListResponse;
import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableResponse;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.port.in.GetScoringVariablesUseCase;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;

@Service
@Transactional(readOnly = true)
public class ScoringVariableQueryService implements GetScoringVariablesUseCase {

    private static final int MIN_VARIABLES_ACTIVAS = 3;

    private final ScoringVariableRepositoryPort repositorio;

    @Autowired
    public ScoringVariableQueryService(ScoringVariableRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public ScoringVariableListResponse listar() {
        List<ScoringVariable> todas = repositorio.findAll();
        List<ScoringVariable> activas = todas.stream().filter(ScoringVariable::activa).toList();

        BigDecimal sumaPesos = activas.stream()
                .map(ScoringVariable::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> advertencias = new ArrayList<>();

        // CA4/RN4: la suma de pesos de variables activas debe ser exactamente 1.00
        if (!activas.isEmpty() && sumaPesos.compareTo(BigDecimal.ONE) != 0) {
            advertencias.add(String.format(
                    "La suma de pesos de variables activas es %.4f, debe ser 1.0000 (CA4)", sumaPesos));
        }

        // RN4: al menos 3 variables activas para que el modelo sea válido
        if (activas.size() < MIN_VARIABLES_ACTIVAS) {
            advertencias.add(String.format(
                    "El modelo tiene %d variable(s) activa(s); se requieren al menos %d (RN4)",
                    activas.size(), MIN_VARIABLES_ACTIVAS));
        }

        List<ScoringVariableResponse> respuestas = todas.stream()
                .map(this::toResponse)
                .toList();

        return new ScoringVariableListResponse(respuestas, sumaPesos, advertencias);
    }

    // -------------------------------------------------------------------------
    // Helpers de mapeo
    // -------------------------------------------------------------------------

    private ScoringVariableResponse toResponse(ScoringVariable variable) {
        List<ScoringVariableResponse.RangoResponse> rangos = variable.rangos().stream()
                .map(this::toRangoResponse)
                .toList();
        List<ScoringVariableResponse.CategoriaResponse> categorias = variable.categorias().stream()
                .map(this::toCategoriaResponse)
                .toList();
        return new ScoringVariableResponse(
                variable.id(),
                variable.nombre(),
                variable.descripcion(),
                variable.tipo().name(),
                variable.peso(),
                variable.activa(),
                rangos,
                categorias);
    }

    private ScoringVariableResponse.RangoResponse toRangoResponse(VariableRange rango) {
        return new ScoringVariableResponse.RangoResponse(
                rango.id(),
                rango.limiteInferior(),
                rango.limiteSuperior(),
                rango.puntaje(),
                rango.etiqueta());
    }

    private ScoringVariableResponse.CategoriaResponse toCategoriaResponse(VariableCategory categoria) {
        return new ScoringVariableResponse.CategoriaResponse(
                categoria.id(),
                categoria.categoria(),
                categoria.puntaje(),
                categoria.etiqueta());
    }
}
