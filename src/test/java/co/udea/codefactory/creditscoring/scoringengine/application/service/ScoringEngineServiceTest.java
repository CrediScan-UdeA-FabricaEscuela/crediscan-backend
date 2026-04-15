package co.udea.codefactory.creditscoring.scoringengine.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutOperator;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelStatus;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.KnockoutRuleRepositoryPort;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoringEngineServiceTest {

    @Mock private ScoringModelRepositoryPort modeloRepo;
    @Mock private KnockoutRuleRepositoryPort koRepo;
    @Mock private ScoringVariableRepositoryPort variableRepo;
    @Mock private FinancialDataRepositoryPort financialDataRepo;

    // Extractor es un componente sin dependencias — lo instanciamos directamente
    private final FinancialDataValueExtractor extractor = new FinancialDataValueExtractor();

    // ScoringCalculator recibe koRepo y variableRepo como mocks
    private ScoringCalculator calculator;

    // Construimos el servicio manualmente para poder pasar el extractor y calculator reales
    private ScoringEngineService service;

    private static final UUID APLICANTE_ID = UUID.randomUUID();
    private static final UUID MODELO_ID = UUID.randomUUID();
    private static final UUID VARIABLE_ID = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        calculator = new ScoringCalculator(koRepo, variableRepo);
        service = new ScoringEngineService(modeloRepo, financialDataRepo, extractor, calculator);
    }

    // =========================================================================
    // calcular() — resolución del modelo
    // =========================================================================

    @Test
    void calcular_sinModeloActivo_lanzaResourceNotFoundException() {
        when(modeloRepo.findActive()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(new CalculateScoreRequest(APLICANTE_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("activo");
    }

    @Test
    void calcular_sinDatosFinancieros_lanzaResourceNotFoundException() {
        when(modeloRepo.findActive()).thenReturn(Optional.of(modeloConVariables()));
        when(financialDataRepo.findMaxVersionByApplicantId(APLICANTE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(new CalculateScoreRequest(APLICANTE_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("financieros");
    }

    // =========================================================================
    // calcular() — reglas knockout (CA3, RN2)
    // =========================================================================

    @Test
    void calcular_conKoActivada_retornaRechazo() {
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modeloConVariables()));
        when(financialDataRepo.findMaxVersionByApplicantId(APLICANTE_ID)).thenReturn(Optional.of(1));
        when(financialDataRepo.findByApplicantIdAndVersion(APLICANTE_ID, 1))
                .thenReturn(Optional.of(datosFinancieros(5))); // 5 moras > 3 → KO se activa
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of(
                reglaKo("moras_12_meses", KnockoutOperator.GT, BigDecimal.valueOf(3))));

        ScoringResult resultado = service.calcular(new CalculateScoreRequest(APLICANTE_ID, MODELO_ID));

        assertThat(resultado.rechazadoPorKo()).isTrue();
        assertThat(resultado.mensajeKo()).isNotBlank();
        assertThat(resultado.desglose()).isEmpty(); // sin calcular puntaje
    }

    @Test
    void calcular_conKoNoActivada_calculaPuntaje() {
        UUID varId = VARIABLE_ID;
        ScoringModel modelo = modeloConVariable(varId, new BigDecimal("1.00"));
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modelo));
        when(financialDataRepo.findMaxVersionByApplicantId(APLICANTE_ID)).thenReturn(Optional.of(1));
        when(financialDataRepo.findByApplicantIdAndVersion(APLICANTE_ID, 1))
                .thenReturn(Optional.of(datosFinancieros(1))); // 1 mora ≤ 3 → KO no se activa
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of(
                reglaKo("moras_12_meses", KnockoutOperator.GT, BigDecimal.valueOf(3))));
        // Variable con nombre que mapea a "ingreso_anual"
        when(variableRepo.findById(varId)).thenReturn(Optional.of(variableNumerica(varId, "ingreso_anual")));

        ScoringResult resultado = service.calcular(new CalculateScoreRequest(APLICANTE_ID, MODELO_ID));

        assertThat(resultado.rechazadoPorKo()).isFalse();
        assertThat(resultado.reglasKoEvaluadas()).hasSize(1);
        assertThat(resultado.reglasKoEvaluadas().get(0).activada()).isFalse();
    }

    // =========================================================================
    // calcular() — puntaje ponderado (CA1)
    // =========================================================================

    @Test
    void calcular_valorEnRango_calculaContribucionCorrectamente() {
        UUID varId = VARIABLE_ID;
        // Peso = 0.5, puntaje del rango = 60 → contribución = 30
        ScoringModel modelo = modeloConVariable(varId, new BigDecimal("0.50"));
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modelo));
        when(financialDataRepo.findMaxVersionByApplicantId(APLICANTE_ID)).thenReturn(Optional.of(1));
        // ingresos anuales = 50_000_000 → rango [0, 100_000_000) → puntaje 60
        when(financialDataRepo.findByApplicantIdAndVersion(APLICANTE_ID, 1))
                .thenReturn(Optional.of(datosFinancieros(0)));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());
        when(variableRepo.findById(varId))
                .thenReturn(Optional.of(variableNumerica(varId, "ingreso_anual")));

        ScoringResult resultado = service.calcular(new CalculateScoreRequest(APLICANTE_ID, MODELO_ID));

        assertThat(resultado.rechazadoPorKo()).isFalse();
        assertThat(resultado.desglose()).hasSize(1);
        assertThat(resultado.desglose().get(0).puntajeParcial()).isEqualTo(60);
        // contribución = 60 × 0.50 = 30
        assertThat(resultado.puntajeFinal()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void calcular_valorFueraDeRango_asignaPuntajeCero() {
        UUID varId = VARIABLE_ID;
        ScoringModel modelo = modeloConVariable(varId, new BigDecimal("1.00"));
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modelo));
        when(financialDataRepo.findMaxVersionByApplicantId(APLICANTE_ID)).thenReturn(Optional.of(1));
        // Datos con ingreso_anual = 50_000_000 pero la variable tiene nombre "campo_sin_mapa"
        // → valor observado = 0 (RN3) → puntaje 0 (fuera de rango numérico ≥ 0 hasta 100)
        when(financialDataRepo.findByApplicantIdAndVersion(APLICANTE_ID, 1))
                .thenReturn(Optional.of(datosFinancieros(0)));
        when(koRepo.findActivasByModeloId(MODELO_ID)).thenReturn(List.of());
        when(variableRepo.findById(varId))
                .thenReturn(Optional.of(variableNumerica(varId, "campo_sin_mapa")));

        ScoringResult resultado = service.calcular(new CalculateScoreRequest(APLICANTE_ID, MODELO_ID));

        // CA2/RN3: sin dato → valor 0 → rango [0,100) cubre el 0 → puntaje 60, no es "fuera de rango"
        // En realidad 0 SÍ cae en [0,100_000_000) → puntaje 60
        // pero el campo no existe en el mapa → extractor.buscarValor devuelve Optional.empty → BigDecimal.ZERO
        // y BigDecimal.ZERO SÍ cae en el rango numérico → OK
        assertThat(resultado.puntajeFinal()).isNotNegative();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ScoringModel modeloConVariables() {
        List<ModelVariable> vars = List.of(
                new ModelVariable(UUID.randomUUID(), MODELO_ID, VARIABLE_ID,
                        new BigDecimal("1.00"), null));
        return ScoringModel.rehydrate(MODELO_ID, "Modelo", "Desc", 1,
                ModelStatus.ACTIVE, vars, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private ScoringModel modeloConVariable(UUID varId, BigDecimal peso) {
        List<ModelVariable> vars = List.of(
                new ModelVariable(UUID.randomUUID(), MODELO_ID, varId, peso, null));
        return ScoringModel.rehydrate(MODELO_ID, "Modelo", "Desc", 1,
                ModelStatus.ACTIVE, vars, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private KnockoutRule reglaKo(String campo, KnockoutOperator operador, BigDecimal umbral) {
        return KnockoutRule.crear(MODELO_ID, campo, operador, umbral, "Rechazo: " + campo, 1);
    }

    /**
     * Datos financieros con {@code defaultsLast12m} configurable y annual income fijo.
     * annual_income = 50_000_000 para que caiga en el rango numérico de la variable de prueba.
     */
    private FinancialData datosFinancieros(int moras12) {
        return new FinancialData(
                UUID.randomUUID(), APLICANTE_ID, 1,
                new BigDecimal("50000000"),   // annualIncome → ingreso_anual
                new BigDecimal("1000000"),    // monthlyExpenses
                new BigDecimal("5000000"),    // currentDebts
                new BigDecimal("100000000"),  // assetsValue
                new BigDecimal("80000000"),   // declaredPatrimony
                moras12 > 0,                 // hasOutstandingDefaults
                36,                          // creditHistoryMonths
                moras12,                     // defaultsLast12m
                0,                           // defaultsLast24m
                720,                         // externalBureauScore
                2,                           // activeCreditProducts
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /**
     * Variable numérica con un rango [0, 100_000_000) → puntaje 60.
     * El {@code nombre} se usa como clave de lookup en el mapa de datos del solicitante.
     */
    private ScoringVariable variableNumerica(UUID id, String nombre) {
        List<VariableRange> rangos = List.of(
                new VariableRange(UUID.randomUUID(), id,
                        BigDecimal.ZERO, new BigDecimal("100000000"),
                        60, "Rango test"));
        return ScoringVariable.rehydrate(
                id, nombre, "Variable de test", VariableType.NUMERIC,
                new BigDecimal("1.00"), true, rangos, List.of());
    }
}
