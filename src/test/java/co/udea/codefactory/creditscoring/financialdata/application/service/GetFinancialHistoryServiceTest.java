package co.udea.codefactory.creditscoring.financialdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
import co.udea.codefactory.creditscoring.financialdata.domain.exception.InvalidFinancialDataException;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class GetFinancialHistoryServiceTest {

    @Mock
    private FinancialDataRepositoryPort financialDataRepositoryPort;

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    @InjectMocks
    private GetFinancialHistoryService service;

    private static final UUID SOLICITANTE_ID = UUID.randomUUID();
    private static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    // --------------------------------------------------------------------------
    // Tests de obtenerHistorial()
    // --------------------------------------------------------------------------

    @Test
    void obtenerHistorial_conSolicitanteInexistente_lanzaResourceNotFoundException() {
        when(applicantRepositoryPort.findById(SOLICITANTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerHistorial(SOLICITANTE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant");

        verifyNoInteractions(financialDataRepositoryPort);
    }

    @Test
    void obtenerHistorial_sinDatosFinancieros_retornaListaVacia() {
        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findAllByApplicantId(SOLICITANTE_ID))
                .thenReturn(List.of());

        List<FinancialData> resultado = service.obtenerHistorial(SOLICITANTE_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void obtenerHistorial_conDosVersiones_retornaOrdenadaDescendente() {
        FinancialData version1 = datosFinancieros(SOLICITANTE_ID, 1);
        FinancialData version2 = datosFinancieros(SOLICITANTE_ID, 2);

        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        // El repositorio devuelve en orden descendente (versión más reciente primero)
        when(financialDataRepositoryPort.findAllByApplicantId(SOLICITANTE_ID))
                .thenReturn(List.of(version2, version1));

        List<FinancialData> resultado = service.obtenerHistorial(SOLICITANTE_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).version()).isEqualTo(2);
        assertThat(resultado.get(1).version()).isEqualTo(1);
    }

    // --------------------------------------------------------------------------
    // Tests de comparar()
    // --------------------------------------------------------------------------

    @Test
    void comparar_conVersionesIguales_lanzaInvalidFinancialDataException() {
        assertThatThrownBy(() -> service.comparar(SOLICITANTE_ID, 1, 1))
                .isInstanceOf(InvalidFinancialDataException.class)
                .hasMessageContaining("diferentes");

        verifyNoInteractions(applicantRepositoryPort);
        verifyNoInteractions(financialDataRepositoryPort);
    }

    @Test
    void comparar_conSolicitanteInexistente_lanzaResourceNotFoundException() {
        when(applicantRepositoryPort.findById(SOLICITANTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comparar(SOLICITANTE_ID, 1, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant");
    }

    @Test
    void comparar_conVersionBaseInexistente_lanzaResourceNotFoundException() {
        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comparar(SOLICITANTE_ID, 1, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FinancialData");
    }

    @Test
    void comparar_conVersionComparadaInexistente_lanzaResourceNotFoundException() {
        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 1))
                .thenReturn(Optional.of(datosFinancieros(SOLICITANTE_ID, 1)));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 2))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comparar(SOLICITANTE_ID, 1, 2))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FinancialData");
    }

    @Test
    void comparar_siempreUsaVersionMenorComoBase_independientementeDelOrdenDeLosParametros() {
        FinancialData version1 = datosFinancieros(SOLICITANTE_ID, 1);
        FinancialData version2 = datosFinancieros(SOLICITANTE_ID, 2);

        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 1))
                .thenReturn(Optional.of(version1));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 2))
                .thenReturn(Optional.of(version2));

        // Pasar v2 primero y v1 segundo → la base debe seguir siendo la versión 1
        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 2, 1);

        assertThat(resultado.base().version()).isEqualTo(1);
        assertThat(resultado.comparada().version()).isEqualTo(2);
    }

    @Test
    void comparar_conDeudaMayorEnVersionNueva_clasificaComoDeterioro() {
        FinancialData base = datosFinancierosConDeuda(SOLICITANTE_ID, 1, new BigDecimal("5000000"));
        FinancialData peor = datosFinancierosConDeuda(SOLICITANTE_ID, 2, new BigDecimal("10000000"));

        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 1))
                .thenReturn(Optional.of(base));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 2))
                .thenReturn(Optional.of(peor));

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        // La deuda aumentó → deterioro
        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("deuda_actual") && c.estado().equals("DETERIORO"));
        // El ratio de deuda/ingreso empeoró → tendencia deterioro
        assertThat(resultado.tendencia()).isEqualTo("DETERIORO");
    }

    @Test
    void comparar_conDeudaMenorEnVersionNueva_clasificaComoMejora() {
        FinancialData base = datosFinancierosConDeuda(SOLICITANTE_ID, 1, new BigDecimal("10000000"));
        FinancialData mejor = datosFinancierosConDeuda(SOLICITANTE_ID, 2, new BigDecimal("5000000"));

        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 1))
                .thenReturn(Optional.of(base));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 2))
                .thenReturn(Optional.of(mejor));

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados())
                .anyMatch(c -> c.campo().equals("deuda_actual") && c.estado().equals("MEJORA"));
        assertThat(resultado.tendencia()).isEqualTo("MEJORA");
    }

    @Test
    void comparar_sinCambiosEntrVersiones_retornaListaDeModificadosVacia() {
        FinancialData version1 = datosFinancieros(SOLICITANTE_ID, 1);
        FinancialData version2 = datosFinancieros(SOLICITANTE_ID, 2); // mismos valores, distinto ID

        when(applicantRepositoryPort.findById(SOLICITANTE_ID))
                .thenReturn(Optional.of(solicitanteEjemplo()));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 1))
                .thenReturn(Optional.of(version1));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(SOLICITANTE_ID, 2))
                .thenReturn(Optional.of(version2));

        FinancialDataComparison resultado = service.comparar(SOLICITANTE_ID, 1, 2);

        assertThat(resultado.camposModificados()).isEmpty();
        assertThat(resultado.tendencia()).isEqualTo("ESTABLE");
    }

    // --------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------

    private Applicant solicitanteEjemplo() {
        return Applicant.rehydrate(
                SOLICITANTE_ID, "Carlos López", "1017234567",
                LocalDate.of(1990, 5, 15), EmploymentType.EMPLEADO,
                new BigDecimal("3500000"), 36, null, null, null,
                java.time.Clock.systemUTC());
    }

    private FinancialData datosFinancieros(UUID solicitanteId, int version) {
        return new FinancialData(
                UUID.randomUUID(), solicitanteId, version,
                new BigDecimal("36000000"),   // annualIncome
                new BigDecimal("2000000"),    // monthlyExpenses
                new BigDecimal("5000000"),    // currentDebts
                new BigDecimal("20000000"),   // assetsValue
                new BigDecimal("15000000"),   // declaredPatrimony
                false,                        // hasOutstandingDefaults
                12,                           // creditHistoryMonths
                1,                            // defaultsLast12m
                2,                            // defaultsLast24m
                720,                          // externalBureauScore
                3,                            // activeCreditProducts
                AHORA, AHORA);
    }

    private FinancialData datosFinancierosConDeuda(UUID solicitanteId, int version, BigDecimal deuda) {
        return new FinancialData(
                UUID.randomUUID(), solicitanteId, version,
                new BigDecimal("36000000"),
                new BigDecimal("2000000"),
                deuda,                        // currentDebts variable
                new BigDecimal("20000000"),
                new BigDecimal("15000000"),
                false, 12, 1, 2, 720, 3,
                AHORA, AHORA);
    }
}
