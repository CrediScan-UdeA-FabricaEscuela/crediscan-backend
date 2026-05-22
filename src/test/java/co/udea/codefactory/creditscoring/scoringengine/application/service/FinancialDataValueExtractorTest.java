package co.udea.codefactory.creditscoring.scoringengine.application.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialDataValueExtractorTest {

    @Mock
    private FinancialData financialData;

    private FinancialDataValueExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new FinancialDataValueExtractor();
    }

    @Test
    @DisplayName("Debe extraer todos los alias cuando todos los valores están presentes")
    void shouldExtractAllAliasesWhenAllValuesArePresent() {
        // Arrange
        when(financialData.annualIncome()).thenReturn(new BigDecimal("50000"));
        when(financialData.monthlyExpenses()).thenReturn(new BigDecimal("2000"));
        when(financialData.currentDebts()).thenReturn(new BigDecimal("10000"));
        when(financialData.assetsValue()).thenReturn(new BigDecimal("80000"));
        when(financialData.declaredPatrimony()).thenReturn(new BigDecimal("70000"));
        when(financialData.creditHistoryMonths()).thenReturn(48);
        when(financialData.defaultsLast12m()).thenReturn(1);
        when(financialData.defaultsLast24m()).thenReturn(2);
        when(financialData.externalBureauScore()).thenReturn(720);
        when(financialData.activeCreditProducts()).thenReturn(3);
        when(financialData.debtToIncomeRatio()).thenReturn(new BigDecimal("0.20"));

        // Act
        Map<String, BigDecimal> resultado = extractor.extraer(financialData);

        // Assert
        assertAll(
                () -> assertEquals(new BigDecimal("50000"), resultado.get("ingreso_anual")),
                () -> assertEquals(new BigDecimal("50000"), resultado.get("annual_income")),
                () -> assertEquals(new BigDecimal("2000"), resultado.get("gastos_mensuales")),
                () -> assertEquals(new BigDecimal("2000"), resultado.get("monthly_expenses")),
                () -> assertEquals(new BigDecimal("10000"), resultado.get("deudas_actuales")),
                () -> assertEquals(new BigDecimal("10000"), resultado.get("current_debts")),
                () -> assertEquals(new BigDecimal("10000"), resultado.get("deuda_total")),
                () -> assertEquals(new BigDecimal("80000"), resultado.get("valor_activos")),
                () -> assertEquals(new BigDecimal("80000"), resultado.get("assets_value")),
                () -> assertEquals(new BigDecimal("70000"), resultado.get("patrimonio_declarado")),
                () -> assertEquals(new BigDecimal("70000"), resultado.get("declared_patrimony")),
                () -> assertEquals(BigDecimal.valueOf(48), resultado.get("meses_historial_credito")),
                () -> assertEquals(BigDecimal.valueOf(48), resultado.get("credit_history_months")),
                () -> assertEquals(BigDecimal.valueOf(48), resultado.get("antiguedad_credito")),
                () -> assertEquals(BigDecimal.valueOf(1), resultado.get("moras_12_meses")),
                () -> assertEquals(BigDecimal.valueOf(1), resultado.get("defaults_last_12m")),
                () -> assertEquals(BigDecimal.valueOf(1), resultado.get("moras_ultimos_12_meses")),
                () -> assertEquals(BigDecimal.valueOf(2), resultado.get("moras_24_meses")),
                () -> assertEquals(BigDecimal.valueOf(2), resultado.get("defaults_last_24m")),
                () -> assertEquals(BigDecimal.valueOf(720), resultado.get("score_buro")),
                () -> assertEquals(BigDecimal.valueOf(720), resultado.get("external_bureau_score")),
                () -> assertEquals(BigDecimal.valueOf(720), resultado.get("score_externo")),
                () -> assertEquals(BigDecimal.valueOf(3), resultado.get("productos_credito_activos")),
                () -> assertEquals(BigDecimal.valueOf(3), resultado.get("active_credit_products")),
                () -> assertEquals(new BigDecimal("0.20"), resultado.get("ratio_deuda_ingreso")),
                () -> assertEquals(new BigDecimal("0.20"), resultado.get("debt_to_income_ratio"))
        );
    }

    @Test
    @DisplayName("Debe excluir los alias del score de buró cuando externalBureauScore es null")
    void shouldExcludeExternalBureauScoreAliasesWhenNull() {
        // Arrange
        when(financialData.annualIncome()).thenReturn(new BigDecimal("50000"));
        when(financialData.monthlyExpenses()).thenReturn(new BigDecimal("2000"));
        when(financialData.currentDebts()).thenReturn(new BigDecimal("10000"));
        when(financialData.assetsValue()).thenReturn(new BigDecimal("80000"));
        when(financialData.declaredPatrimony()).thenReturn(new BigDecimal("70000"));
        when(financialData.creditHistoryMonths()).thenReturn(48);
        when(financialData.defaultsLast12m()).thenReturn(1);
        when(financialData.defaultsLast24m()).thenReturn(2);
        when(financialData.externalBureauScore()).thenReturn(null);
        when(financialData.activeCreditProducts()).thenReturn(3);
        when(financialData.debtToIncomeRatio()).thenReturn(new BigDecimal("0.20"));

        // Act
        Map<String, BigDecimal> resultado = extractor.extraer(financialData);

        // Assert
        assertAll(
                () -> assertFalse(resultado.containsKey("score_buro")),
                () -> assertFalse(resultado.containsKey("external_bureau_score")),
                () -> assertFalse(resultado.containsKey("score_externo")),
                () -> assertTrue(resultado.containsKey("ingreso_anual"))
        );
    }

    @Test
    @DisplayName("Debe excluir todos los campos BigDecimal opcionales cuando son null")
    void shouldExcludeOptionalBigDecimalFieldsWhenNull() {
        // Arrange
        when(financialData.annualIncome()).thenReturn(null);
        when(financialData.monthlyExpenses()).thenReturn(null);
        when(financialData.currentDebts()).thenReturn(null);
        when(financialData.assetsValue()).thenReturn(null);
        when(financialData.declaredPatrimony()).thenReturn(null);
        when(financialData.creditHistoryMonths()).thenReturn(0);
        when(financialData.defaultsLast12m()).thenReturn(0);
        when(financialData.defaultsLast24m()).thenReturn(0);
        when(financialData.externalBureauScore()).thenReturn(null);
        when(financialData.activeCreditProducts()).thenReturn(0);
        when(financialData.debtToIncomeRatio()).thenReturn(null);

        // Act
        Map<String, BigDecimal> resultado = extractor.extraer(financialData);

        // Assert
        assertAll(
                () -> assertFalse(resultado.containsKey("ingreso_anual")),
                () -> assertFalse(resultado.containsKey("annual_income")),
                () -> assertFalse(resultado.containsKey("gastos_mensuales")),
                () -> assertFalse(resultado.containsKey("monthly_expenses")),
                () -> assertFalse(resultado.containsKey("deudas_actuales")),
                () -> assertFalse(resultado.containsKey("current_debts")),
                () -> assertFalse(resultado.containsKey("deuda_total")),
                () -> assertFalse(resultado.containsKey("valor_activos")),
                () -> assertFalse(resultado.containsKey("assets_value")),
                () -> assertFalse(resultado.containsKey("patrimonio_declarado")),
                () -> assertFalse(resultado.containsKey("declared_patrimony")),
                () -> assertFalse(resultado.containsKey("score_buro")),
                () -> assertFalse(resultado.containsKey("ratio_deuda_ingreso")),
                () -> assertFalse(resultado.containsKey("debt_to_income_ratio")),
                () -> assertTrue(resultado.containsKey("meses_historial_credito")),
                () -> assertTrue(resultado.containsKey("moras_12_meses")),
                () -> assertTrue(resultado.containsKey("moras_24_meses")),
                () -> assertTrue(resultado.containsKey("productos_credito_activos"))
        );
    }

    @Test
    @DisplayName("Debe devolver un mapa inmutable")
    void shouldReturnImmutableMap() {
        // Arrange
        when(financialData.annualIncome()).thenReturn(new BigDecimal("50000"));
        when(financialData.monthlyExpenses()).thenReturn(new BigDecimal("2000"));
        when(financialData.currentDebts()).thenReturn(new BigDecimal("10000"));
        when(financialData.assetsValue()).thenReturn(new BigDecimal("80000"));
        when(financialData.declaredPatrimony()).thenReturn(new BigDecimal("70000"));
        when(financialData.creditHistoryMonths()).thenReturn(48);
        when(financialData.defaultsLast12m()).thenReturn(1);
        when(financialData.defaultsLast24m()).thenReturn(2);
        when(financialData.externalBureauScore()).thenReturn(720);
        when(financialData.activeCreditProducts()).thenReturn(3);
        when(financialData.debtToIncomeRatio()).thenReturn(new BigDecimal("0.20"));

        // Act
        Map<String, BigDecimal> resultado = extractor.extraer(financialData);

        // Assert
        assertThrows(UnsupportedOperationException.class,
                () -> resultado.put("nuevo_campo", BigDecimal.ONE));
    }

    @Test
    @DisplayName("buscarValor debe devolver Optional con valor cuando la clave existe")
    void shouldReturnOptionalWithValueWhenKeyExists() {
        // Arrange
        Map<String, BigDecimal> mapa = Map.of("ingreso_anual", new BigDecimal("50000"));

        // Act
        Optional<BigDecimal> resultado = extractor.buscarValor(mapa, "ingreso_anual");

        // Assert
        assertAll(
                () -> assertTrue(resultado.isPresent()),
                () -> assertEquals(new BigDecimal("50000"), resultado.get())
        );
    }

    @Test
    @DisplayName("buscarValor debe devolver Optional vacío cuando la clave no existe")
    void shouldReturnEmptyOptionalWhenKeyDoesNotExist() {
        // Arrange
        Map<String, BigDecimal> mapa = Map.of("ingreso_anual", new BigDecimal("50000"));

        // Act
        Optional<BigDecimal> resultado = extractor.buscarValor(mapa, "campo_inexistente");

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("buscarValor debe normalizar mayúsculas a minúsculas")
    void shouldNormalizeUppercaseInBuscarValor() {
        // Arrange
        Map<String, BigDecimal> mapa = Map.of("ingreso_anual", new BigDecimal("50000"));

        // Act
        Optional<BigDecimal> resultado = extractor.buscarValor(mapa, "INGRESO_ANUAL");

        // Assert
        assertAll(
                () -> assertTrue(resultado.isPresent()),
                () -> assertEquals(new BigDecimal("50000"), resultado.get())
        );
    }

    @Test
    @DisplayName("buscarValor debe reemplazar espacios por guiones bajos")
    void shouldReplaceSpacesWithUnderscoresInBuscarValor() {
        // Arrange
        Map<String, BigDecimal> mapa = Map.of("ingreso_anual", new BigDecimal("50000"));

        // Act
        Optional<BigDecimal> resultado = extractor.buscarValor(mapa, "Ingreso Anual");

        // Assert
        assertAll(
                () -> assertTrue(resultado.isPresent()),
                () -> assertEquals(new BigDecimal("50000"), resultado.get())
        );
    }
}