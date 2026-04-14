package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelResponse;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;

/** Traduce entre el modelo de dominio y los DTOs de la capa REST. */
@Component
public class ScoringModelRestMapper {

    public ScoringModelResponse toResponse(ScoringModel modelo) {
        List<ScoringModelResponse.ModelVariableResponse> vars = modelo.variables().stream()
                .map(this::toVariableResponse)
                .toList();
        return new ScoringModelResponse(
                modelo.id(),
                modelo.nombre(),
                modelo.descripcion(),
                modelo.version(),
                modelo.estado().name(),
                vars,
                modelo.fechaCreacion(),
                modelo.fechaActivacion());
    }

    private ScoringModelResponse.ModelVariableResponse toVariableResponse(ModelVariable mv) {
        return new ScoringModelResponse.ModelVariableResponse(
                mv.id(), mv.variableId(), mv.peso(), mv.rangosSnapshot());
    }
}
