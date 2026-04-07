package co.udea.codefactory.creditscoring.financialdata.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FinancialDataResponse(
        UUID id,
        UUID applicantId,
        int version,
        BigDecimal annualIncome,
        BigDecimal monthlyExpenses,
        BigDecimal currentDebts,
        BigDecimal assetsValue,
        BigDecimal declaredPatrimony,
        boolean hasOutstandingDefaults,
        int creditHistoryMonths,
        int defaultsLast12m,
        int defaultsLast24m,
        Integer externalBureauScore,
        boolean externalBureauScoreAvailable,
        int activeCreditProducts,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        BigDecimal debtToIncomeRatio,
        BigDecimal expenseToIncomeRatio,
        boolean debtToIncomeAlert,
        boolean expensesExceedMonthlyIncome,
        boolean liabilitiesExceedAssetsLimit) {
}
