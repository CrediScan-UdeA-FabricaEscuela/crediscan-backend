package co.udea.codefactory.creditscoring.applicant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;
import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

@ExtendWith(MockitoExtension.class)
class ListApplicantsServiceTest {

    @Mock
    private ApplicantRepositoryPort applicantRepositoryPort;

    @Mock
    private IdentificationCryptoPort identificationCryptoPort;

    @InjectMocks
    private ListApplicantsService listApplicantsService;

    // Página vacía reutilizable en los tests que no validan el contenido
    private static final PagedResult<ApplicantSummary> PAGINA_VACIA =
            new PagedResult<>(List.of(), 0, 0, 0, 20);

    private static final PageRequest PAGE_REQUEST = new PageRequest(0, 20);

    private ApplicantFilterCriteria criteriosSoloQ(String q) {
        return new ApplicantFilterCriteria(q, null, null, null, null, null, null, null, null, null);
    }

    private ApplicantFilterCriteria criteriosVacios() {
        return criteriosSoloQ(null);
    }

    // --- Tests de list() ---

    @Test
    void list_sinCriterios_llamaFindByFilterConHashNulo() {
        // Arrange: criterios completamente vacíos → no debe calcularse ningún hash
        ApplicantFilterCriteria criterios = criteriosVacios();
        when(applicantRepositoryPort.findByFilter(criterios, null, PAGE_REQUEST)).thenReturn(PAGINA_VACIA);

        // Act
        listApplicantsService.list(criterios, PAGE_REQUEST);

        // Assert: el repositorio recibe null como hash y no se toca el módulo de crypto
        verify(applicantRepositoryPort).findByFilter(criterios, null, PAGE_REQUEST);
        verifyNoInteractions(identificationCryptoPort);
    }

    @Test
    void list_conQ_calculaHashAntesDePassarAlRepositorio() {
        // Arrange: cuando q tiene valor, se debe calcular su hash HMAC
        ApplicantFilterCriteria criterios = criteriosSoloQ("1017234567");
        when(identificationCryptoPort.hash("1017234567")).thenReturn("hash-calculado");
        when(applicantRepositoryPort.findByFilter(eq(criterios), eq("hash-calculado"), eq(PAGE_REQUEST)))
                .thenReturn(PAGINA_VACIA);

        // Act
        listApplicantsService.list(criterios, PAGE_REQUEST);

        // Assert: el hash llega correctamente al repositorio
        ArgumentCaptor<String> captorHash = ArgumentCaptor.forClass(String.class);
        verify(applicantRepositoryPort).findByFilter(eq(criterios), captorHash.capture(), eq(PAGE_REQUEST));
        assertThat(captorHash.getValue()).isEqualTo("hash-calculado");
    }

    @Test
    void list_conQEnBlanco_trataCriterioComoVacioYNoHashea() {
        // Arrange: q en blanco equivale a sin criterio de búsqueda libre
        ApplicantFilterCriteria criterios = criteriosSoloQ("   ");
        when(applicantRepositoryPort.findByFilter(criterios, null, PAGE_REQUEST)).thenReturn(PAGINA_VACIA);

        // Act
        listApplicantsService.list(criterios, PAGE_REQUEST);

        // Assert: no se calcula hash para cadenas en blanco
        verify(applicantRepositoryPort).findByFilter(criterios, null, PAGE_REQUEST);
        verifyNoInteractions(identificationCryptoPort);
    }

    @Test
    void list_pasaLosCriteriosSinModificarAlRepositorio() {
        // Arrange: los criterios de filtrado deben llegar íntegros al repositorio
        ApplicantFilterCriteria criterios = new ApplicantFilterCriteria(
                null, new BigDecimal("3000000"), new BigDecimal("8000000"),
                "Empleado", 12, 60,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                null, null);
        when(applicantRepositoryPort.findByFilter(criterios, null, PAGE_REQUEST)).thenReturn(PAGINA_VACIA);

        // Act
        listApplicantsService.list(criterios, PAGE_REQUEST);

        // Assert: el mismo objeto de criterios llega al repositorio sin modificaciones
        ArgumentCaptor<ApplicantFilterCriteria> captorCriterios =
                ArgumentCaptor.forClass(ApplicantFilterCriteria.class);
        verify(applicantRepositoryPort).findByFilter(captorCriterios.capture(), isNull(), eq(PAGE_REQUEST));
        assertThat(captorCriterios.getValue()).isEqualTo(criterios);
    }

    @Test
    void list_retornaElResultadoDelRepositorio() {
        // Arrange: el repositorio devuelve una página con datos
        ApplicantSummary resumen = new ApplicantSummary(
                UUID.randomUUID(), "Juan", "12345678", LocalDate.of(1990, 1, 1),
                "Empleado", new BigDecimal("3500000"), 36, null, null, null);
        PagedResult<ApplicantSummary> resultadoEsperado = new PagedResult<>(List.of(resumen), 1, 1, 0, 20);
        ApplicantFilterCriteria criterios = criteriosVacios();
        when(applicantRepositoryPort.findByFilter(criterios, null, PAGE_REQUEST)).thenReturn(resultadoEsperado);

        // Act
        PagedResult<ApplicantSummary> resultado = listApplicantsService.list(criterios, PAGE_REQUEST);

        // Assert: el resultado del repositorio llega sin modificaciones al llamador
        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.totalElements()).isEqualTo(1);
    }

    // --- Tests de export() ---

    @Test
    void export_limiteDeResultadosEsFijadoEn10000() {
        // Arrange: el límite máximo del export siempre debe ser MAX_EXPORT_SIZE
        ApplicantFilterCriteria criterios = criteriosVacios();
        when(applicantRepositoryPort.findAllByFilter(any(), isNull(), eq(ListApplicantsService.MAX_EXPORT_SIZE)))
                .thenReturn(List.of());

        // Act
        listApplicantsService.export(criterios);

        // Assert: el repositorio recibe exactamente 10.000 como límite
        ArgumentCaptor<Integer> captorLimite = ArgumentCaptor.forClass(Integer.class);
        verify(applicantRepositoryPort).findAllByFilter(any(), isNull(), captorLimite.capture());
        assertThat(captorLimite.getValue()).isEqualTo(10_000);
    }

    @Test
    void export_conQ_tambienCalculaElHash() {
        // Arrange: la exportación también debe hashear el criterio de búsqueda libre
        ApplicantFilterCriteria criterios = criteriosSoloQ("carlos");
        when(identificationCryptoPort.hash("carlos")).thenReturn("hash-carlos");
        when(applicantRepositoryPort.findAllByFilter(eq(criterios), eq("hash-carlos"), anyInt()))
                .thenReturn(List.of());

        // Act
        listApplicantsService.export(criterios);

        // Assert: el hash calculado llega al repositorio
        ArgumentCaptor<String> captorHash = ArgumentCaptor.forClass(String.class);
        verify(applicantRepositoryPort).findAllByFilter(eq(criterios), captorHash.capture(), anyInt());
        assertThat(captorHash.getValue()).isEqualTo("hash-carlos");
    }

    @Test
    void export_retornaListaDelRepositorio() {
        // Arrange: el repositorio devuelve registros para exportar
        ApplicantSummary s = new ApplicantSummary(
                UUID.randomUUID(), "Maria", "98765432", LocalDate.of(1985, 3, 20),
                "Independiente", new BigDecimal("5000000"), 48, null, null, null);
        ApplicantFilterCriteria criterios = criteriosVacios();
        when(applicantRepositoryPort.findAllByFilter(any(), isNull(), anyInt())).thenReturn(List.of(s));

        // Act
        List<ApplicantSummary> resultado = listApplicantsService.export(criterios);

        // Assert: el resultado debe contener exactamente el solicitante devuelto por el repositorio
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).name()).isEqualTo("Maria");
    }

    // Método auxiliar para ArgumentCaptor de int
    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
