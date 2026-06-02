package co.udea.codefactory.creditscoring.scoringengine.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutOperator;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelStatus;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.KnockoutRuleRepositoryPort;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ScoringCalculatorTest {

    @Mock
    private KnockoutRuleRepositoryPort koRepo;

    @Mock
    private ScoringVariableRepositoryPort variableRepo;

    @InjectMocks
    private ScoringCalculator calculator;

    private static final UUID MODELO_ID = UUID.randomUUID();
    private static final UUID CONTEXTO_ID = UUID.randomUUID();

    // =========================================================================
    // Knockout — rechazo inmediato
    // =========================================================================

    @Test
    void calcular_conReglaKoActivada_devuelveResultadoRechazado() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));

        KnockoutRule regla = KnockoutRule.rehydrate(
                UUID.randomUUID(), MODELO_ID, "deuda_actual", KnockoutOperator.GT,
                new BigDecimal("10000"), "Deuda excesiva", 1, true);
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of(regla));

        // deuda_actual = 20000 → supera umbral 10000 → KO activado
        Map<String, BigDecimal> valores = Map.of("deuda_actual", new BigDecimal("20000"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.rechazadoPorKo()).isTrue();
        assertThat(resultado.mensajeKo()).isEqualTo("Deuda excesiva");
        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.desglose()).isEmpty();
    }

    @Test
    void calcular_conReglaKoNoActivada_continuaAlCalculoDePuntaje() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));

        // regla: deuda > 50000; el valor es 5000 → no se activa
        KnockoutRule regla = KnockoutRule.rehydrate(
                UUID.randomUUID(), MODELO_ID, "deuda_actual", KnockoutOperator.GT,
                new BigDecimal("50000"), "Deuda muy alta", 1, true);
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of(regla));

        ScoringVariable variable = variableNumerica(varId, "deuda_actual",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("10000"), 80)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        Map<String, BigDecimal> valores = Map.of("deuda_actual", new BigDecimal("5000"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.rechazadoPorKo()).isFalse();
        assertThat(resultado.reglasKoEvaluadas()).hasSize(1);
        assertThat(resultado.reglasKoEvaluadas().get(0).activada()).isFalse();
    }

    @Test
    void calcular_conReglaKoInactiva_noActivaElRechazo() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));

        // regla inactiva aunque el valor satisfaría la condición
        KnockoutRule reglaInactiva = KnockoutRule.rehydrate(
                UUID.randomUUID(), MODELO_ID, "deuda_actual", KnockoutOperator.GT,
                new BigDecimal("0"), "Cualquier deuda", 1, false);
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of(reglaInactiva));

        ScoringVariable variable = variableNumerica(varId, "deuda_actual",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("100000"), 70)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        Map<String, BigDecimal> valores = Map.of("deuda_actual", new BigDecimal("5000"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.rechazadoPorKo()).isFalse();
    }

    @Test
    void calcular_conCampoAusenteEnValores_usaCeroParaEvalKO() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));

        // KO: campo ausente → valor 0 → LT 100 → se activa (RN3)
        KnockoutRule regla = KnockoutRule.rehydrate(
                UUID.randomUUID(), MODELO_ID, "score_bureau", KnockoutOperator.LT,
                new BigDecimal("100"), "Score insuficiente", 1, true);
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of(regla));

        // 'score_bureau' no está en el mapa → se usa 0
        Map<String, BigDecimal> valores = Map.of("otro_campo", new BigDecimal("999"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.rechazadoPorKo()).isTrue();
    }

    // =========================================================================
    // Cálculo de puntaje ponderado — variables numéricas
    // =========================================================================

    @Test
    void calcular_conVariableNumerica_calculaContribucionCorrectamente() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("0.5"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        // variable numérica: nombre "Ingresos Anuales", rango [0, 100000) → puntaje 60
        ScoringVariable variable = variableNumerica(varId, "Ingresos Anuales",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("100000"), 60)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        // key normalizado: "ingresos_anuales"
        Map<String, BigDecimal> valores = Map.of("ingresos_anuales", new BigDecimal("50000"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.rechazadoPorKo()).isFalse();
        // contribución = 60 * 0.5 = 30.00
        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(resultado.desglose()).hasSize(1);
        assertThat(resultado.desglose().get(0).puntajeParcial()).isEqualTo(60);
        assertThat(resultado.desglose().get(0).contribucion()).isEqualByComparingTo(new BigDecimal("30.0000"));
    }

    @Test
    void calcular_conValorFueraDeRangoNumerico_asignaPuntajeCero() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        // rango cubre solo [0, 100)
        ScoringVariable variable = variableNumerica(varId, "score_bureau",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("100"), 80)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        // valor 999 está fuera del rango → puntaje 0, etiqueta "Fuera de rango"
        Map<String, BigDecimal> valores = Map.of("score_bureau", new BigDecimal("999"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.desglose().get(0).puntajeParcial()).isEqualTo(0);
        assertThat(resultado.desglose().get(0).etiquetaRango()).isEqualTo("Fuera de rango");
    }

    @Test
    void calcular_conVariableNombreConEspaciosYGuiones_normalizaParaBuscarValor() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        // nombre con espacios y guión → se normaliza a "historial_crediticio_meses"
        ScoringVariable variable = variableNumerica(varId, "Historial-Crediticio Meses",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("100"), 90)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        // clave en el mapa ya normalizada
        Map<String, BigDecimal> valores = Map.of("historial_crediticio_meses", new BigDecimal("50"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    void calcular_conCampoAusenteEnValoresParaVariable_usaCeroYBuscaRango() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("0.4"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        // rango [0, 1000) → puntaje 20; valor 0 caerá en este rango (CA2/RN3)
        ScoringVariable variable = variableNumerica(varId, "moras_12m",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("1000"), 20)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        // "moras_12m" no está en el mapa de valores
        Map<String, BigDecimal> valores = Map.of();

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        // contribución = 20 * 0.4 = 8.0000
        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(new BigDecimal("8.00"));
    }

    @Test
    void calcular_conVariableEliminadaDelRepositorio_omiteElCampoDelCalculo() {
        UUID varExistenteId = UUID.randomUUID();
        UUID varEliminadaId = UUID.randomUUID();

        ModelVariable mvExistente = new ModelVariable(UUID.randomUUID(), MODELO_ID, varExistenteId, new BigDecimal("0.6"), null);
        ModelVariable mvEliminada = new ModelVariable(UUID.randomUUID(), MODELO_ID, varEliminadaId, new BigDecimal("0.4"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mvExistente, mvEliminada));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        ScoringVariable varExistente = variableNumerica(varExistenteId, "score_bureau",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("1000"), 100)));
        when(variableRepo.findById(varExistenteId)).thenReturn(Optional.of(varExistente));
        // variable eliminada → el repo devuelve vacío
        when(variableRepo.findById(varEliminadaId)).thenReturn(Optional.empty());

        Map<String, BigDecimal> valores = Map.of("score_bureau", new BigDecimal("500"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        // Solo la variable existente contribuye: 100 * 0.6 = 60
        assertThat(resultado.desglose()).hasSize(1);
        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    // =========================================================================
    // Cálculo de puntaje ponderado — variables categóricas
    // =========================================================================

    @Test
    void calcular_conVariableCategorica_encuentraCategoriaCaseInsensitive() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("0.3"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        VariableCategory catEmpleado = new VariableCategory(UUID.randomUUID(), varId, "Empleado", 80, "Estable");
        ScoringVariable variable = variableCategorica(varId, "tipo_empleo", List.of(catEmpleado));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        // El valor en el mapa es la representación numérica de "Empleado" → el toPlainString de BigDecimal
        // Para categorías, el valor se convierte con .toPlainString()
        // Necesitamos que la búsqueda encuentre "Empleado" con "empleado" (case insensitive)
        Map<String, BigDecimal> valores = Map.of("tipo_empleo", BigDecimal.ONE);

        // Aquí el valor 1.toPlainString = "1" → no coincide con "Empleado"
        // → puntaje 0, "Sin categoría"
        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.desglose()).hasSize(1);
        assertThat(resultado.desglose().get(0).etiquetaRango()).isEqualTo("Sin categoría");
    }

    @Test
    void calcular_conVariableCategoricaSinCoincidencia_asignaPuntajeCero() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        VariableCategory cat = new VariableCategory(UUID.randomUUID(), varId, "Empleado", 80, "Estable");
        ScoringVariable variable = variableCategorica(varId, "tipo_empleo", List.of(cat));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        // valor numérico no coincide con ninguna categoría
        Map<String, BigDecimal> valores = Map.of("tipo_empleo", new BigDecimal("999"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.desglose().get(0).puntajeParcial()).isEqualTo(0);
    }

    // =========================================================================
    // Múltiples variables — acumulación
    // =========================================================================

    @Test
    void calcular_conMultiplesVariables_acumulaContribucionesCorrectamente() {
        UUID var1Id = UUID.randomUUID();
        UUID var2Id = UUID.randomUUID();
        ModelVariable mv1 = new ModelVariable(UUID.randomUUID(), MODELO_ID, var1Id, new BigDecimal("0.6"), null);
        ModelVariable mv2 = new ModelVariable(UUID.randomUUID(), MODELO_ID, var2Id, new BigDecimal("0.4"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv1, mv2));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        ScoringVariable v1 = variableNumerica(var1Id, "ingresos",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("100000"), 100)));
        ScoringVariable v2 = variableNumerica(var2Id, "deuda",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("100000"), 50)));
        when(variableRepo.findById(var1Id)).thenReturn(Optional.of(v1));
        when(variableRepo.findById(var2Id)).thenReturn(Optional.of(v2));

        Map<String, BigDecimal> valores = Map.of(
                "ingresos", new BigDecimal("50000"),
                "deuda", new BigDecimal("30000"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        // puntaje = 100 * 0.6 + 50 * 0.4 = 60 + 20 = 80
        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(resultado.desglose()).hasSize(2);
    }

    @Test
    void calcular_sinReglas_KO_devuelveResultadoAprobado() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        ScoringVariable variable = variableNumerica(varId, "score",
                List.of(rango(BigDecimal.ZERO, new BigDecimal("1000"), 75)));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        Map<String, BigDecimal> valores = Map.of("score", new BigDecimal("500"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.rechazadoPorKo()).isFalse();
        assertThat(resultado.reglasKoEvaluadas()).isEmpty();
        assertThat(resultado.modeloId()).isEqualTo(MODELO_ID);
        assertThat(resultado.aplicanteId()).isEqualTo(CONTEXTO_ID);
    }

    @Test
    void calcular_rangoConEtiquetaNula_usaEtiquetaFallback() {
        UUID varId = UUID.randomUUID();
        ModelVariable mv = new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, new BigDecimal("1.0"), null);
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of(mv));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());

        // VariableRange con etiqueta null
        VariableRange rangoSinEtiqueta = new VariableRange(
                UUID.randomUUID(), varId, BigDecimal.ZERO, new BigDecimal("1000"), 50, null);
        ScoringVariable variable = variableNumerica(varId, "campo_test", List.of(rangoSinEtiqueta));
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variable));

        Map<String, BigDecimal> valores = Map.of("campo_test", new BigDecimal("500"));

        ScoringResult resultado = calculator.calcular(modelo, CONTEXTO_ID, valores);

        assertThat(resultado.desglose().get(0).etiquetaRango()).isEqualTo("Sin etiqueta");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ScoringModel modeloConVariables(UUID id, List<ModelVariable> variables) {
        return ScoringModel.rehydrate(id, "Modelo Test", "Desc", 1, ModelStatus.ACTIVE,
                variables, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private ScoringVariable variableNumerica(UUID id, String nombre, List<VariableRange> rangos) {
        return ScoringVariable.rehydrate(id, nombre, "Desc", VariableType.NUMERIC,
                new BigDecimal("0.5"), true, rangos, List.of());
    }

    private ScoringVariable variableCategorica(UUID id, String nombre, List<VariableCategory> categorias) {
        return ScoringVariable.rehydrate(id, nombre, "Desc", VariableType.CATEGORICAL,
                new BigDecimal("0.5"), true, List.of(), categorias);
    }

    private VariableRange rango(BigDecimal inferior, BigDecimal superior, int puntaje) {
        return new VariableRange(UUID.randomUUID(), null, inferior, superior, puntaje, "Etiqueta");
    }
}
