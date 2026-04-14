package co.udea.codefactory.creditscoring.scoringmodel.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record KnockoutRuleResponse(
        UUID id,
        UUID modeloId,
        String campo,
        String operador,
        BigDecimal umbral,
        String mensaje,
        int prioridad,
        boolean activa
) {}
