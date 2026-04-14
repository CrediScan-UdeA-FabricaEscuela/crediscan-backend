package co.udea.codefactory.creditscoring.scoring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.scoring.application.dto.CreateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.UpdateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.VariableCategoryRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.VariableRangeRequest;
import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoringVariableCommandServiceTest {

    @Mock
    private ScoringVariableRepositoryPort repositorio;

    @InjectMocks
    private ScoringVariableCommandService service;

    private static final UUID VARIABLE_ID = UUID.randomUUID();

    // =========================================================================
    // crear()
    // =========================================================================

    @Test
    void crear_conNombreDuplicado_lanzaExcepcion() {
        when(repositorio.existsByNombre("Antigüedad")).thenReturn(true);

        CreateScoringVariableRequest request = new CreateScoringVariableRequest(
                "Antigüedad", "Desc", "NUMERIC", new BigDecimal("0.30"),
                rangoRequestValido(), List.of());

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("Antigüedad");

        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_conTipoInvalido_lanzaExcepcion() {
        when(repositorio.existsByNombre("Variable")).thenReturn(false);

        CreateScoringVariableRequest request = new CreateScoringVariableRequest(
                "Variable", "Desc", "INVALID_TYPE", new BigDecimal("0.30"),
                rangoRequestValido(), List.of());

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crear_variableNumerica_valida_guardaYRetornaVariable() {
        when(repositorio.existsByNombre("Antigüedad laboral")).thenReturn(false);
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateScoringVariableRequest request = new CreateScoringVariableRequest(
                "Antigüedad laboral", "Años en el empleo actual", "NUMERIC",
                new BigDecimal("0.30"), rangoRequestValido(), List.of());

        ScoringVariable resultado = service.crear(request);

        assertThat(resultado.nombre()).isEqualTo("Antigüedad laboral");
        assertThat(resultado.tipo()).isEqualTo(VariableType.NUMERIC);
        assertThat(resultado.activa()).isTrue();
        assertThat(resultado.id()).isNotNull();
        verify(repositorio).save(any(ScoringVariable.class));
    }

    @Test
    void crear_variableCategorica_valida_guardaYRetornaVariable() {
        when(repositorio.existsByNombre("Tipo empleo")).thenReturn(false);
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<VariableCategoryRequest> categorias = List.of(
                new VariableCategoryRequest("Empleado", 80, null),
                new VariableCategoryRequest("Independiente", 50, null));

        CreateScoringVariableRequest request = new CreateScoringVariableRequest(
                "Tipo empleo", "Modalidad laboral", "CATEGORICAL",
                new BigDecimal("0.20"), List.of(), categorias);

        ScoringVariable resultado = service.crear(request);

        assertThat(resultado.tipo()).isEqualTo(VariableType.CATEGORICAL);
        assertThat(resultado.categorias()).hasSize(2);
    }

    // =========================================================================
    // actualizar()
    // =========================================================================

    @Test
    void actualizar_conIdInexistente_lanzaResourceNotFoundException() {
        when(repositorio.findById(VARIABLE_ID)).thenReturn(Optional.empty());

        UpdateScoringVariableRequest request = new UpdateScoringVariableRequest(
                "Nuevo nombre", "Desc", new BigDecimal("0.30"),
                true, rangoRequestValido(), List.of());

        assertThatThrownBy(() -> service.actualizar(VARIABLE_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repositorio, never()).update(any());
    }

    @Test
    void actualizar_conNombreDuplicadoEnOtraVariable_lanzaExcepcion() {
        ScoringVariable existente = variableNumerica("Nombre original");
        when(repositorio.findById(VARIABLE_ID)).thenReturn(Optional.of(existente));
        when(repositorio.existsByNombre("Nombre duplicado")).thenReturn(true);

        UpdateScoringVariableRequest request = new UpdateScoringVariableRequest(
                "Nombre duplicado", "Desc", new BigDecimal("0.30"),
                true, rangoRequestValido(), List.of());

        assertThatThrownBy(() -> service.actualizar(VARIABLE_ID, request))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("Nombre duplicado");

        verify(repositorio, never()).update(any());
    }

    @Test
    void actualizar_conMismoNombre_noVerificaDuplicado() {
        ScoringVariable existente = variableNumerica("Nombre original");
        when(repositorio.findById(VARIABLE_ID)).thenReturn(Optional.of(existente));
        when(repositorio.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateScoringVariableRequest request = new UpdateScoringVariableRequest(
                "Nombre original", "Nueva descripción", new BigDecimal("0.40"),
                true, rangoRequestValido(), List.of());

        ScoringVariable resultado = service.actualizar(VARIABLE_ID, request);

        assertThat(resultado.nombre()).isEqualTo("Nombre original");
        assertThat(resultado.peso()).isEqualByComparingTo(new BigDecimal("0.40"));
        verify(repositorio, never()).existsByNombre("Nombre original");
    }

    @Test
    void actualizar_desactivarVariable_guardaConActivaFalse() {
        ScoringVariable existente = variableNumerica("Variable activa");
        when(repositorio.findById(VARIABLE_ID)).thenReturn(Optional.of(existente));
        when(repositorio.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateScoringVariableRequest request = new UpdateScoringVariableRequest(
                "Variable activa", "Desc", new BigDecimal("0.30"),
                false, rangoRequestValido(), List.of());

        ScoringVariable resultado = service.actualizar(VARIABLE_ID, request);

        assertThat(resultado.activa()).isFalse();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<VariableRangeRequest> rangoRequestValido() {
        return List.of(new VariableRangeRequest(
                BigDecimal.ZERO, new BigDecimal("100"), 50, "Rango único"));
    }

    private ScoringVariable variableNumerica(String nombre) {
        List<VariableRange> rangos = List.of(
                new VariableRange(UUID.randomUUID(), VARIABLE_ID, BigDecimal.ZERO,
                        new BigDecimal("100"), 50, null));
        return ScoringVariable.rehydrate(
                VARIABLE_ID, nombre, "Descripción", VariableType.NUMERIC,
                new BigDecimal("0.30"), true, rangos, List.of());
    }

    private ScoringVariable variableCategorica(String nombre) {
        List<VariableCategory> categorias = List.of(
                new VariableCategory(UUID.randomUUID(), VARIABLE_ID, "Empleado", 80, null));
        return ScoringVariable.rehydrate(
                VARIABLE_ID, nombre, "Descripción", VariableType.CATEGORICAL,
                new BigDecimal("0.20"), true, List.of(), categorias);
    }
}
