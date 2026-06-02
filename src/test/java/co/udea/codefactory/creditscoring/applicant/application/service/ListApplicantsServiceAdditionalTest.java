package co.udea.codefactory.creditscoring.applicant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;

/**
 * Cubre la rama {@code findById()} de {@link ListApplicantsService},
 * que no estaba cubierta en {@link ListApplicantsServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class ListApplicantsServiceAdditionalTest {

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    @Mock
    private IdentificationCryptoPort identificationCryptoPort;

    @InjectMocks
    private ListApplicantsService service;

    // =========================================================================
    // findById()
    // =========================================================================

    @Test
    void findById_conIdExistente_retornaSummaryMapeado() {
        UUID id = UUID.randomUUID();
        Applicant applicant = Applicant.rehydrate(id, "María García", "9876543210",
                LocalDate.of(1985, 3, 20), EmploymentType.INDEPENDIENTE,
                new BigDecimal("5000000"), 48, "+57 300 000 0000", "Calle 10 # 5-30",
                "maria@example.com", Clock.systemUTC());

        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(applicant));

        Optional<ApplicantSummary> resultado = service.findById(id);

        assertThat(resultado).isPresent();
        ApplicantSummary summary = resultado.get();
        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.name()).isEqualTo("María García");
        assertThat(summary.identification()).isEqualTo("9876543210");
        assertThat(summary.employmentType()).isEqualTo("Independiente");
        assertThat(summary.monthlyIncome()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(summary.phone()).isEqualTo("+57 300 000 0000");
        assertThat(summary.email()).isEqualTo("maria@example.com");
    }

    @Test
    void findById_conIdInexistente_retornaOptionalVacio() {
        UUID id = UUID.randomUUID();
        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.empty());

        Optional<ApplicantSummary> resultado = service.findById(id);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findById_mapeaEmploymentTypeApiValue() {
        UUID id = UUID.randomUUID();
        Applicant applicant = Applicant.rehydrate(id, "Carlos Pérez", "11111111",
                LocalDate.of(1990, 1, 1), EmploymentType.PENSIONADO,
                new BigDecimal("3000000"), 0, null, null, null, Clock.systemUTC());

        when(applicantRepositoryPort.findById(id)).thenReturn(Optional.of(applicant));

        Optional<ApplicantSummary> resultado = service.findById(id);

        assertThat(resultado).isPresent();
        // El apiValue de PENSIONADO es "Pensionado"
        assertThat(resultado.get().employmentType()).isEqualTo("Pensionado");
    }
}
