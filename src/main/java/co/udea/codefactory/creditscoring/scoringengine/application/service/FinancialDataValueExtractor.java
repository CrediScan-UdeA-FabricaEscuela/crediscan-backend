package co.udea.codefactory.creditscoring.scoringengine.application.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

/**
 * Convierte un {@link FinancialData} en un mapa de campos numéricos
 * que el motor de scoring puede consultar por nombre.
 *
 * <p>Cada campo expone al menos un alias en español (snake_case) y otro en inglés,
 * permitiendo que las reglas knockout y las variables de scoring referencien
 * el campo por cualquiera de los nombres reconocidos.</p>
 */
@Component
public class FinancialDataValueExtractor {

    /**
     * Extrae los valores numéricos del {@code FinancialData} indexados por nombre de campo.
     *
     * @return mapa inmutable; los valores de campos opcionales nulos no se incluyen.
     */
    public Map<String, BigDecimal> extraer(FinancialData datos) {
        Map<String, BigDecimal> mapa = new HashMap<>();

        // Ingreso anual
        put(mapa, datos.annualIncome(),
                "ingreso_anual", "annual_income");

        // Gastos mensuales
        put(mapa, datos.monthlyExpenses(),
                "gastos_mensuales", "monthly_expenses");

        // Deudas actuales
        put(mapa, datos.currentDebts(),
                "deudas_actuales", "current_debts", "deuda_total");

        // Valor de activos
        put(mapa, datos.assetsValue(),
                "valor_activos", "assets_value");

        // Patrimonio declarado
        put(mapa, datos.declaredPatrimony(),
                "patrimonio_declarado", "declared_patrimony");

        // Meses de historial crediticio
        BigDecimal historial = BigDecimal.valueOf(datos.creditHistoryMonths());
        put(mapa, historial,
                "meses_historial_credito", "credit_history_months", "antiguedad_credito");

        // Moras en los últimos 12 meses
        BigDecimal moras12 = BigDecimal.valueOf(datos.defaultsLast12m());
        put(mapa, moras12,
                "moras_12_meses", "defaults_last_12m", "moras_ultimos_12_meses");

        // Moras en los últimos 24 meses
        BigDecimal moras24 = BigDecimal.valueOf(datos.defaultsLast24m());
        put(mapa, moras24,
                "moras_24_meses", "defaults_last_24m");

        // Score de buró externo (puede ser nulo)
        if (datos.externalBureauScore() != null) {
            BigDecimal score = BigDecimal.valueOf(datos.externalBureauScore());
            put(mapa, score, "score_buro", "external_bureau_score", "score_externo");
        }

        // Productos de crédito activos
        BigDecimal productos = BigDecimal.valueOf(datos.activeCreditProducts());
        put(mapa, productos,
                "productos_credito_activos", "active_credit_products");

        // Ratio deuda/ingreso (calculado)
        put(mapa, datos.debtToIncomeRatio(),
                "ratio_deuda_ingreso", "debt_to_income_ratio");

        return Map.copyOf(mapa);
    }

    /**
     * Busca el valor de un campo en el mapa.
     * Normaliza el nombre a lowercase para tolerar variantes de capitalización.
     */
    public Optional<BigDecimal> buscarValor(Map<String, BigDecimal> mapa, String campo) {
        String key = campo.toLowerCase().replace(" ", "_");
        return Optional.ofNullable(mapa.get(key));
    }

    private void put(Map<String, BigDecimal> mapa, BigDecimal valor, String... nombres) {
        if (valor == null) return;
        for (String nombre : nombres) {
            mapa.put(nombre.toLowerCase(), valor);
        }
    }
}
