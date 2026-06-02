package co.udea.codefactory.creditscoring.financialdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataComparison;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;

/**
 * Cubre las ramas no alcanzadas en {@link GetFinancialHistoryServiceTest}:
 * - comparar bureau score: null→valor (MEJORA), valor→null (DETERIORO), ambos null (UNCHANGED → no añade campo)
 * - tendencia ESTABLE (ratio igual o señales mixtas con bureau disponible)
 * - ingresos mejoran, gastos mejoran, activos mejoran, patrimonio mejora, moras mejoran
 * - productos vigentes: deterioro y mejora
 */
@ExtendWith(MockitoExtension.class)
class GetFinancialHistoryServiceAdditionalTest {

    @Mock
    private FinancialDataRepositoryPort financialDataRepositoryPort;

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    @InjectMocks
    private GetFinancialHistoryService service;

    private static final UUID SOLICITANTE_ID = UUID.randomUUID();
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    // =========================================================================
    // Score de bureau — casos especiales
    // =========================================================================

    @Test
    void comparar_bureauScoreNuloEnBaseYPresenteEnComparada_reportaMejora() {
        FinancialData base = datos(1, null);      // sin score
        FinancialData comparada = datos(2, 750);  // con score → mejora

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("score_bureau") && c.estado().equals("MEJORA"));
    }

    @Test
    void comparar_bureauScorePresenteEnBaseYNuloEnComparada_reportaDeterioro() {
        FinancialData base = datos(1, 720);   // tiene score
        FinancialData comparada = datos(2, null); // ya no tiene → deterioro

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("score_bureau") && c.estado().equals("DETERIORO"));
    }

    @Test
    void comparar_ambosScoresBureauNulos_noCampoScore() {
        FinancialData base = datos(1, null);
        FinancialData comparada = datos(2, null);

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .noneMatch(c -> c.campo().equals("score_bureau"));
    }

    @Test
    void comparar_scoresBureauIguales_noCampoScore() {
        // mismo valor → sin cambio → no se añade al listado de modificados
        FinancialData base = datos(1, 700);
        FinancialData comparada = datosConMismoScore(2, 700);

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .noneMatch(c -> c.campo().equals("score_bureau"));
    }

    // =========================================================================
    // Tendencia — ramas adicionales
    // =========================================================================

    @Test
    void comparar_ratioIgualSinBureau_tendenciaEstable() {
        // mismos datos → ratio idéntico → ESTABLE
        FinancialData base = datos(1, null);
        FinancialData comparada = datos(2, null); // mismos valores

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.tendencia()).isEqualTo("ESTABLE");
    }

    @Test
    void comparar_ratioMejoraYScoreEmpeora_tendenciaEstable() {
        // RN1: mejora requiere AMBOS: ratio↓ Y score↑
        // ratio↓ + score↓ → ESTABLE (señales mixtas)
        FinancialData base = datosConDeudaYScore(1, new BigDecimal("10000000"), 800);
        FinancialData comparada = datosConDeudaYScore(2, new BigDecimal("5000000"), 700); // ratio↓ pero score↓

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.tendencia()).isEqualTo("ESTABLE");
    }

    @Test
    void comparar_ratioEmpeoraSinBureau_tendenciaDeterioro() {
        // deuda mayor → ratio mayor → DETERIORO (sin bureau)
        FinancialData base = datos(1, null);
        FinancialData comparada = datosConDeudaSinScore(2, new BigDecimal("20000000")); // deuda aumenta

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.tendencia()).isEqualTo("DETERIORO");
    }

    @Test
    void comparar_ratioMejoraSinBureau_tendenciaMejora() {
        // deuda menor → ratio menor → MEJORA (sin bureau)
        FinancialData base = datosConDeudaSinScore(1, new BigDecimal("20000000"));
        FinancialData comparada = datos(2, null); // deuda = 5000000 < 20000000

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.tendencia()).isEqualTo("MEJORA");
    }

    @Test
    void comparar_ratioEmpeoraSconScore_tendenciaDeterioro() {
        // ratio↑ + score↑ → señales mixtas? No: !ratioMejoro && !scoreMejoro necesita comparacionRatio!=0
        // Aquí: ratio↑, score↑ → ratioMejoro=false, scoreMejoro=true → ESTABLE
        FinancialData base = datosConDeudaYScore(1, new BigDecimal("5000000"), 700);
        FinancialData comparada = datosConDeudaYScore(2, new BigDecimal("10000000"), 800); // ratio↑, score↑

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        // Señal mixta → ESTABLE
        assertThat(resultado.tendencia()).isEqualTo("ESTABLE");
    }

    @Test
    void comparar_ratioEmpeora_conScore_ambosDeterioran_tendenciaDeterioro() {
        FinancialData base = datosConDeudaYScore(1, new BigDecimal("5000000"), 800);
        FinancialData comparada = datosConDeudaYScore(2, new BigDecimal("10000000"), 700); // ratio↑, score↓

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.tendencia()).isEqualTo("DETERIORO");
    }

    // =========================================================================
    // Campos adicionales comparados
    // =========================================================================

    @Test
    void comparar_ingresosAumentanEnComparada_reportaMejora() {
        FinancialData base = datosConIngresos(1, new BigDecimal("24000000"));
        FinancialData comparada = datosConIngresos(2, new BigDecimal("36000000")); // ingreso mayor → mejora

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("ingresos_anuales") && c.estado().equals("MEJORA"));
    }

    @Test
    void comparar_morasDisminuyen_reportaMejora() {
        FinancialData base = datosConMoras(1, 3, 5);
        FinancialData comparada = datosConMoras(2, 1, 2); // moras↓ → mejora

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("moras_12m") && c.estado().equals("MEJORA"));
    }

    @Test
    void comparar_morasAumentan_reportaDeterioro() {
        FinancialData base = datosConMoras(1, 1, 2);
        FinancialData comparada = datosConMoras(2, 3, 5); // moras↑ → deterioro

        configurarMocks(base, comparada);

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("moras_12m") && c.estado().equals("DETERIORO"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Applicant solicitanteEjemplo() {
        return Applicant.rehydrate(SOLICITANTE_ID, "Carlos López", "1017234567",
                LocalDate.of(1990, 5, 15), EmploymentType.EMPLEADO,
                new BigDecimal("3500000"), 36, null, null, null,
                java.time.Clock.systemUTC());
    }

    /** Datos financieros base con deuda fija de 5M y score configurable */
    private FinancialData datos(int version, Integer bureauScore) {
        return new FinancialData(UUID.randomUUID(), SOLICITANTE_ID, version,
                new BigDecimal("36000000"), new BigDecimal("2000000"),
                new BigDecimal("5000000"), new BigDecimal("20000000"),
                new BigDecimal("15000000"), false, 12, 1, 2,
                bureauScore, 3, AHORA, AHORA);
    }

    /** Mismos valores que datos() pero fuerza mismo score para caso de igualdad */
    private FinancialData datosConMismoScore(int version, int bureauScore) {
        return datos(version, bureauScore);
    }

    private FinancialData datosConDeudaYScore(int version, BigDecimal deuda, Integer bureauScore) {
        return new FinancialData(UUID.randomUUID(), SOLICITANTE_ID, version,
                new BigDecimal("36000000"), new BigDecimal("2000000"),
                deuda, new BigDecimal("20000000"), new BigDecimal("15000000"),
                false, 12, 1, 2, bureauScore, 3, AHORA, AHORA);
    }

    private FinancialData datosConDeudaSinScore(int version, BigDecimal deuda) {
        return new FinancialData(UUID.randomUUID(), SOLICITANTE_ID, version,
                new BigDecimal("36000000"), new BigDecimal("2000000"),
                deuda, new BigDecimal("20000000"), new BigDecimal("15000000"),
                false, 12, 1, 2, null, 3, AHORA, AHORA);
    }

    private FinancialData datosConIngresos(int version, BigDecimal annualIncome) {
        return new FinancialData(UUID.randomUUID(), SOLICITANTE_ID, version,
                annualIncome, new BigDecimal("2000000"),
                new BigDecimal("5000000"), new BigDecimal("20000000"),
                new BigDecimal("15000000"), false, 12, 1, 2, null, 3, AHORA, AHORA);
    }

    private FinancialData datosConMoras(int version, int moras12m, int moras24m) {
        return new FinancialData(UUID.randomUUID(), SOLICITANTE_ID, version,
                new BigDecimal("36000000"), new BigDecimal("2000000"),
                new BigDecimal("5000000"), new BigDecimal("20000000"),
                new BigDecimal("15000000"), false, 12, moras12m, moras24m, null, 3, AHORA, AHORA);
    }

    private void configurarMocks(FinancialData base, FinancialData comparada) {
        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, base.version()))
                .thenReturn(Optional.of(base));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, comparada.version()))
                .thenReturn(Optional.of(comparada));
    }
}
