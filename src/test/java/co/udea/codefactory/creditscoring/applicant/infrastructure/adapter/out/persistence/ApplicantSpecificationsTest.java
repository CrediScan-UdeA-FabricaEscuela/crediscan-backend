package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ApplicantSpecificationsTest {

    @Mock
    private Root<ApplicantJpaEntity> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    // Predicado genérico devuelto por los mocks de CriteriaBuilder
    private Predicate predicadoMock;

    @BeforeEach
    void configurarMocks() {
        predicadoMock = mock(Predicate.class);
        // Configuración lenient para evitar errores de interacción no esperada
        lenient().when(cb.conjunction()).thenReturn(predicadoMock);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicadoMock);
        lenient().when(cb.or(any(Predicate[].class))).thenReturn(predicadoMock);
        lenient().when(cb.equal(any(), any())).thenReturn(predicadoMock);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicadoMock);
        lenient().when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicadoMock);
        lenient().when(cb.like(any(), anyString())).thenReturn(predicadoMock);
        lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
        lenient().when(root.get(anyString())).thenReturn(mock(Path.class));
    }

    @Test
    void build_sinCriteriosNiHash_retornaConjuncion() {
        // Arrange: criterios completamente vacíos → sin filtros activos
        ApplicantFilterCriteria criteriosVacios = criterios(null, null, null, null, null, null, null, null);

        // Act
        Specification<ApplicantJpaEntity> spec = ApplicantSpecifications.build(criteriosVacios, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        // Assert: se retorna la conjunción que significa "sin restricción" (todos los registros)
        assertThat(resultado).isNotNull();
        verify(cb).conjunction();
        verify(cb, never()).and(any(Predicate[].class));
    }

    @Test
    void build_conIngresosMin_agregaPredicadoMayorOIgual() {
        // Arrange: solo se filtra por ingreso mínimo
        ApplicantFilterCriteria filtro = criterios(null, new BigDecimal("3000000"), null, null, null, null, null, null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: se usa greaterThanOrEqualTo para el campo monthlyIncome
        verify(cb).greaterThanOrEqualTo(any(), eq(new BigDecimal("3000000")));
    }

    @Test
    void build_conIngresosMax_agregaPredicadoMenorOIgual() {
        // Arrange: solo se filtra por ingreso máximo
        ApplicantFilterCriteria filtro = criterios(null, null, new BigDecimal("8000000"), null, null, null, null, null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: se usa lessThanOrEqualTo para el campo monthlyIncome
        verify(cb).lessThanOrEqualTo(any(), eq(new BigDecimal("8000000")));
    }

    @Test
    void build_conTipoEmpleo_agregaPredicadoDeIgualdad() {
        // Arrange: filtro por tipo de empleo exacto
        ApplicantFilterCriteria filtro = criterios(null, null, null, "Empleado", null, null, null, null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: se usa equal para coincidencia exacta del tipo de empleo
        verify(cb).equal(any(), eq("Empleado"));
    }

    @Test
    void build_conAntiguedadMin_agregaPredicadoMayorOIgual() {
        // Arrange: filtro por antigüedad mínima
        ApplicantFilterCriteria filtro = criterios(null, null, null, null, 24, null, null, null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: greaterThanOrEqualTo para workExperienceMonths
        verify(cb).greaterThanOrEqualTo(any(), eq(24));
    }

    @Test
    void build_conAntiguedadMax_agregaPredicadoMenorOIgual() {
        // Arrange: filtro por antigüedad máxima
        ApplicantFilterCriteria filtro = criterios(null, null, null, null, null, 60, null, null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: lessThanOrEqualTo para workExperienceMonths
        verify(cb).lessThanOrEqualTo(any(), eq(60));
    }

    @Test
    void build_conFechaDesde_agregaPredicadoDeRangoPorFecha() {
        // Arrange: filtro por fecha de registro desde
        ApplicantFilterCriteria filtro = criterios(null, null, null, null, null, null,
                LocalDate.of(2025, 1, 1), null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: se llama greaterThanOrEqualTo con un valor OffsetDateTime
        verify(cb).greaterThanOrEqualTo(any(), any(java.time.OffsetDateTime.class));
    }

    @Test
    void build_conFechaHasta_agregaPredicadoDeRangoPorFecha() {
        // Arrange: filtro por fecha de registro hasta
        ApplicantFilterCriteria filtro = criterios(null, null, null, null, null, null,
                null, LocalDate.of(2025, 12, 31));

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: se llama lessThanOrEqualTo con un valor OffsetDateTime
        verify(cb).lessThanOrEqualTo(any(), any(java.time.OffsetDateTime.class));
    }

    @Test
    void build_conQYHash_agregaPredicadoOrParaBusquedaLibre() {
        // Arrange: búsqueda libre activa → debe construirse un OR entre hash e ILIKE
        ApplicantFilterCriteria filtro = criterios("carlos", null, null, null, null, null, null, null);

        // Act
        ApplicantSpecifications.build(filtro, "hash-ejemplo").toPredicate(root, query, cb);

        // Assert: se usa or() para combinar hash exacto con nombre ILIKE
        verify(cb).or(any(Predicate[].class));
    }

    @Test
    void build_conSoloCriterioQ_sinHash_agregaPredicadoOrConNombreSolamente() {
        // Arrange: q presente pero sin hash precalculado → solo predica por nombre
        ApplicantFilterCriteria filtro = criterios("maria", null, null, null, null, null, null, null);

        // Act
        ApplicantSpecifications.build(filtro, null).toPredicate(root, query, cb);

        // Assert: se usa or() (con un solo elemento: el ILIKE de nombre)
        verify(cb).or(any(Predicate[].class));
        verify(cb).like(any(), anyString());
    }

    @Test
    void build_retornaSpecificationNoNulaParaCualquierEntrada() {
        // Arrange/Act/Assert: build() nunca debe retornar null
        assertThat(ApplicantSpecifications.build(
                criterios(null, null, null, null, null, null, null, null), null))
                .isNotNull();
        assertThat(ApplicantSpecifications.build(
                criterios("q", new BigDecimal("1000"), null, "Empleado", 12, null, null, null), "hash"))
                .isNotNull();
    }

    // Método auxiliar para construir criterios de filtrado sin los campos de ordenamiento
    private ApplicantFilterCriteria criterios(
            String q, BigDecimal incomeMin, BigDecimal incomeMax, String employmentType,
            Integer experienceMin, Integer experienceMax,
            LocalDate dateFrom, LocalDate dateTo) {
        return new ApplicantFilterCriteria(
                q, incomeMin, incomeMax, employmentType,
                experienceMin, experienceMax, dateFrom, dateTo,
                null, null);
    }
}
