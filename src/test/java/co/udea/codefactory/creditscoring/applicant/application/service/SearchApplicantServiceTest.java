package co.udea.codefactory.creditscoring.applicant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;

/**
 * T-14 — SearchApplicantServiceTest
 */
@ExtendWith(MockitoExtension.class)
class SearchApplicantServiceTest {

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    @Mock
    private IdentificationCryptoPort identificationCryptoPort;

    @InjectMocks
    private SearchApplicantService searchApplicantService;

    private ApplicantSummary aSummary(String name) {
        return new ApplicantSummary(UUID.randomUUID(), name, "1017234567",
                LocalDate.of(1990, 1, 1), "Empleado", new BigDecimal("3500000"), 36, null);
    }

    @Test
    void search_hashesTheCriteriaBeforePassingToRepo() {
        Pageable pageable = PageRequest.of(0, 20);
        when(identificationCryptoPort.hash("1017234567")).thenReturn("hashed-value");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageable)))
                .thenReturn(Page.empty());

        searchApplicantService.search("1017234567", pageable);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(applicantRepositoryPort).search(hashCaptor.capture(), anyString(), eq(pageable));
        assertThat(hashCaptor.getValue()).isEqualTo("hashed-value");
    }

    @Test
    void search_formatsNameCriteriaAsLikePattern() {
        Pageable pageable = PageRequest.of(0, 20);
        when(identificationCryptoPort.hash("carlos")).thenReturn("any-hash");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageable)))
                .thenReturn(Page.empty());

        searchApplicantService.search("carlos", pageable);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(applicantRepositoryPort).search(anyString(), nameCaptor.capture(), eq(pageable));
        assertThat(nameCaptor.getValue()).isEqualTo("%carlos%");
    }

    @Test
    void search_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ApplicantSummary> expected = new PageImpl<>(List.of(aSummary("Juan"), aSummary("Carlos")));
        when(identificationCryptoPort.hash(anyString())).thenReturn("any-hash");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageable))).thenReturn(expected);

        Page<ApplicantSummary> result = searchApplicantService.search("juan", pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void search_withEmptyResults_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(identificationCryptoPort.hash(anyString())).thenReturn("any-hash");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageable))).thenReturn(Page.empty());

        Page<ApplicantSummary> result = searchApplicantService.search("ZZZ999", pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void search_withNullCriteria_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(applicantRepositoryPort.findAll(pageable)).thenReturn(Page.empty());

        searchApplicantService.search(null, pageable);

        verify(applicantRepositoryPort).findAll(pageable);
    }

    @Test
    void search_withBlankCriteria_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(applicantRepositoryPort.findAll(pageable)).thenReturn(Page.empty());

        searchApplicantService.search("   ", pageable);

        verify(applicantRepositoryPort).findAll(pageable);
    }
}
