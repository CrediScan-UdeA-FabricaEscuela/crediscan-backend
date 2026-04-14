package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.in.rest;

import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableResponse;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;

/** Traduce entre el modelo de dominio y los DTOs de la capa REST. */
@Component
public class ScoringVariableRestMapper {

    public ScoringVariableResponse toResponse(ScoringVariable variable) {
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
