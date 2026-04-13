package co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.in.rest;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataComparison;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataComparisonResponse;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataResponse;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

@Component
public class FinancialDataRestMapper {

    public FinancialDataResponse toResponse(FinancialData financialData) {
        return new FinancialDataResponse(
                financialData.id(),
                financialData.applicantId(),
                financialData.version(),
                financialData.annualIncome(),
                financialData.monthlyExpenses(),
                financialData.currentDebts(),
                financialData.assetsValue(),
                financialData.declaredPatrimony(),
                financialData.hasOutstandingDefaults(),
                financialData.creditHistoryMonths(),
                financialData.defaultsLast12m(),
                financialData.defaultsLast24m(),
                financialData.externalBureauScore(),
                financialData.hasExternalBureauScore(),
                financialData.activeCreditProducts(),
                financialData.createdAt(),
                financialData.updatedAt(),
                financialData.debtToIncomeRatio(),
                financialData.expenseToIncomeRatio(),
                financialData.debtToIncomeAlert(),
                financialData.expensesExceedMonthlyIncome(),
                financialData.liabilitiesExceedAssetsLimit());
    }

    /** Convierte el resultado de comparación del dominio al DTO de respuesta REST. */
    public FinancialDataComparisonResponse toComparisonResponse(FinancialDataComparison comparacion) {
        return new FinancialDataComparisonResponse(
                toResponse(comparacion.base()),
                toResponse(comparacion.comparada()),
                comparacion.camposModificados().stream()
                        .map(c -> new FinancialDataComparisonResponse.CampoComparado(
                                c.campo(), c.valorBase(), c.valorComparado(), c.estado()))
                        .toList(),
                comparacion.tendencia());
    }
}
