package co.udea.codefactory.creditscoring.financialdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataRequest;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class FinancialDataCommandServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private FinancialDataRepositoryPort financialDataRepositoryPort;

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    private FinancialDataCommandService createService() {
        return new FinancialDataCommandService(financialDataRepositoryPort, applicantRepositoryPort, FIXED_CLOCK);
    }

    private FinancialDataRequest sampleRequest() {
        return new FinancialDataRequest(
                new BigDecimal("36000000"),
                new BigDecimal("2000000"),
                new BigDecimal("5000000"),
                new BigDecimal("20000000"),
                new BigDecimal("15000000"),
                false,
                12,
                1,
                2,
                720,
                3);
    }

    private Applicant existingApplicant(UUID id) {
        return Applicant.rehydrate(id, "Ana Mora", "1234567890",
                LocalDate.of(1990, 6, 15), EmploymentType.EMPLEADO, new BigDecimal("42000000"), 24,
                null, null, null, FIXED_CLOCK);
    }

    @Test
    void create_withUnknownApplicant_throwsResourceNotFoundException() {
        UUID applicantId = UUID.randomUUID();
        when(applicantRepositoryPort.findById(applicantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService().create(applicantId, sampleRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Applicant");
    }

    @Test
    void create_withValidRequest_savesWithVersionOne() {
        UUID applicantId = UUID.randomUUID();
        when(applicantRepositoryPort.findById(applicantId)).thenReturn(Optional.of(existingApplicant(applicantId)));
        when(financialDataRepositoryPort.findMaxVersionByApplicantId(applicantId)).thenReturn(Optional.empty());
        when(financialDataRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createService().create(applicantId, sampleRequest());

        assertThat(result.version()).isEqualTo(1);
        assertThat(result.debtToIncomeRatio()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.expenseToIncomeRatio()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.ofInstant(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void update_withUnknownVersion_throwsResourceNotFoundException() {
        UUID applicantId = UUID.randomUUID();
        when(applicantRepositoryPort.findById(applicantId)).thenReturn(Optional.of(existingApplicant(applicantId)));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(applicantId, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createService().update(applicantId, 1, sampleRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FinancialData");
    }

    @Test
    void update_withExistingVersion_createsNewVersion() {
        UUID applicantId = UUID.randomUUID();
        when(applicantRepositoryPort.findById(applicantId)).thenReturn(Optional.of(existingApplicant(applicantId)));
        FinancialData existing = new FinancialData(
                UUID.randomUUID(),
                applicantId,
                1,
                new BigDecimal("36000000"),
                new BigDecimal("2000000"),
                new BigDecimal("5000000"),
                new BigDecimal("20000000"),
                new BigDecimal("15000000"),
                false,
                12,
                1,
                2,
                720,
                3,
                OffsetDateTime.ofInstant(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        when(financialDataRepositoryPort.findByApplicantIdAndVersion(applicantId, 1)).thenReturn(Optional.of(existing));
        when(financialDataRepositoryPort.findMaxVersionByApplicantId(applicantId)).thenReturn(Optional.of(1));
        when(financialDataRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createService().update(applicantId, 1, sampleRequest());

        assertThat(result.version()).isEqualTo(2);
        assertThat(result.applicantId()).isEqualTo(applicantId);
        assertThat(result.createdAt()).isEqualTo(OffsetDateTime.ofInstant(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }
}
