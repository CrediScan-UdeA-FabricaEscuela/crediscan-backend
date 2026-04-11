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

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

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

    // Resultado de paginación vacío reutilizable en los tests
    private static final PagedResult<ApplicantSummary> PAGINA_VACIA =
            new PagedResult<>(List.of(), 0, 0, 0, 20);

    private ApplicantSummary aSummary(String name) {
        return new ApplicantSummary(UUID.randomUUID(), name, "1017234567",
                LocalDate.of(1990, 1, 1), "Empleado", new BigDecimal("3500000"), 36, null, null, null);
    }

    @Test
    void search_hashesTheCriteriaBeforePassingToRepo() {
        // Arrange: se crea el PageRequest custom del dominio (página 0, tamaño 20)
        PageRequest pageRequest = new PageRequest(0, 20);
        when(identificationCryptoPort.hash("1017234567")).thenReturn("hashed-value");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageRequest)))
                .thenReturn(PAGINA_VACIA);

        // Act
        searchApplicantService.search("1017234567", pageRequest);

        // Assert: el hash del criterio debe llegar al repositorio
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(applicantRepositoryPort).search(hashCaptor.capture(), anyString(), eq(pageRequest));
        assertThat(hashCaptor.getValue()).isEqualTo("hashed-value");
    }

    @Test
    void search_formatsNameCriteriaAsLikePattern() {
        // Arrange: el criterio de nombre debe envolverse como patrón LIKE
        PageRequest pageRequest = new PageRequest(0, 20);
        when(identificationCryptoPort.hash("carlos")).thenReturn("any-hash");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageRequest)))
                .thenReturn(PAGINA_VACIA);

        // Act
        searchApplicantService.search("carlos", pageRequest);

        // Assert: el nombre debe llegar con formato %criterio%
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(applicantRepositoryPort).search(anyString(), nameCaptor.capture(), eq(pageRequest));
        assertThat(nameCaptor.getValue()).isEqualTo("%carlos%");
    }

    @Test
    void search_returnsPageFromRepository() {
        // Arrange: el repositorio devuelve dos solicitantes
        PageRequest pageRequest = new PageRequest(0, 20);
        List<ApplicantSummary> items = List.of(aSummary("Juan"), aSummary("Carlos"));
        PagedResult<ApplicantSummary> expected = new PagedResult<>(items, 2, 1, 0, 20);
        when(identificationCryptoPort.hash(anyString())).thenReturn("any-hash");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageRequest))).thenReturn(expected);

        // Act
        PagedResult<ApplicantSummary> result = searchApplicantService.search("juan", pageRequest);

        // Assert: el resultado debe contener los dos elementos
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void search_withEmptyResults_returnsEmptyPage() {
        // Arrange: el repositorio no encuentra resultados
        PageRequest pageRequest = new PageRequest(0, 20);
        when(identificationCryptoPort.hash(anyString())).thenReturn("any-hash");
        when(applicantRepositoryPort.search(anyString(), anyString(), eq(pageRequest))).thenReturn(PAGINA_VACIA);

        // Act
        PagedResult<ApplicantSummary> result = searchApplicantService.search("ZZZ999", pageRequest);

        // Assert: la lista debe estar vacía y el total en cero
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void search_withNullCriteria_callsFindAll() {
        // Arrange: criterio nulo debe derivar al findAll del repositorio
        PageRequest pageRequest = new PageRequest(0, 20);
        when(applicantRepositoryPort.findAll(pageRequest)).thenReturn(PAGINA_VACIA);

        // Act
        searchApplicantService.search(null, pageRequest);

        // Assert: debe haberse llamado findAll con el mismo pageRequest
        verify(applicantRepositoryPort).findAll(pageRequest);
    }

    @Test
    void search_withBlankCriteria_callsFindAll() {
        // Arrange: criterio en blanco también debe derivar al findAll
        PageRequest pageRequest = new PageRequest(0, 20);
        when(applicantRepositoryPort.findAll(pageRequest)).thenReturn(PAGINA_VACIA);

        // Act
        searchApplicantService.search("   ", pageRequest);

        // Assert: debe haberse llamado findAll con el mismo pageRequest
        verify(applicantRepositoryPort).findAll(pageRequest);
    }
}
