package co.udea.codefactory.creditscoring.financialdata.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

import co.udea.codefactory.creditscoring.financialdata.domain.exception.InvalidFinancialDataException;

public record FinancialData(
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
        int activeCreditProducts,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public FinancialData {
        if (id == null) {
            throw new InvalidFinancialDataException("El identificador de datos financieros es obligatorio");
        }
        if (applicantId == null) {
            throw new InvalidFinancialDataException("El solicitante asociado es obligatorio");
        }
        if (version < 0) {
            throw new InvalidFinancialDataException("La versión debe ser mayor o igual a cero");
        }
        validateNonNegative(annualIncome, "annualIncome");
        validateNonNegative(monthlyExpenses, "monthlyExpenses");
        validateNonNegative(currentDebts, "currentDebts");
        validateNonNegative(assetsValue, "assetsValue");
        validateNonNegative(declaredPatrimony, "declaredPatrimony");
        validateNonNegative(creditHistoryMonths, "creditHistoryMonths");
        validateNonNegative(defaultsLast12m, "defaultsLast12m");
        validateNonNegative(defaultsLast24m, "defaultsLast24m");
        validateNonNegative(activeCreditProducts, "activeCreditProducts");
        if (externalBureauScore != null && (externalBureauScore < 0 || externalBureauScore > 999)) {
            throw new InvalidFinancialDataException("El score de buro externo debe estar entre 0 y 999");
        }
        if (createdAt == null || updatedAt == null) {
            throw new InvalidFinancialDataException("Las marcas de tiempo de creación y actualización son obligatorias");
        }
    }

    private static void validateNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new InvalidFinancialDataException(String.format("El campo %s es obligatorio", fieldName));
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFinancialDataException(String.format("El campo %s no puede ser negativo", fieldName));
        }
    }

    private static void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new InvalidFinancialDataException(String.format("El campo %s no puede ser negativo", fieldName));
        }
    }

    public FinancialData withVersionAndTimestamps(int version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new FinancialData(id, applicantId, version, annualIncome, monthlyExpenses,
                currentDebts, assetsValue, declaredPatrimony, hasOutstandingDefaults,
                creditHistoryMonths, defaultsLast12m, defaultsLast24m, externalBureauScore,
                activeCreditProducts, createdAt, updatedAt);
    }

    public BigDecimal debtToIncomeRatio() {
        if (annualIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentDebts.divide(annualIncome, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal expenseToIncomeRatio() {
        BigDecimal monthlyIncome = annualIncome.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return monthlyExpenses.divide(monthlyIncome, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean debtToIncomeAlert() {
        return debtToIncomeRatio().compareTo(BigDecimal.valueOf(60)) > 0;
    }

    public boolean expensesExceedMonthlyIncome() {
        BigDecimal monthlyIncome = annualIncome.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        return monthlyExpenses.compareTo(monthlyIncome) > 0;
    }

    public boolean hasExternalBureauScore() {
        return externalBureauScore != null;
    }

    public boolean liabilitiesExceedAssetsLimit() {
        return assetsValue.compareTo(BigDecimal.ZERO) > 0
                && currentDebts.compareTo(assetsValue.multiply(BigDecimal.valueOf(100))) > 0;
    }
}
