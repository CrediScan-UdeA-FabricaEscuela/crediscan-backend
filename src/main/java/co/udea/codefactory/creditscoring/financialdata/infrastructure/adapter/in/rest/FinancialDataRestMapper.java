package co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.in.rest;

import org.springframework.stereotype.Component;

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
}
