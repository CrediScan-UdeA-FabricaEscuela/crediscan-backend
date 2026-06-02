package co.udea.codefactory.creditscoring.scoring.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableListResponse;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ScoringVariableQueryServiceTest {

    @Mock
    private ScoringVariableRepositoryPort repositorio;

    @InjectMocks
    private ScoringVariableQueryService service;

    // =========================================================================
    // listar() — advertencias de pesos
    // =========================================================================

    @Test
    void listar_sinVariables_retornaRespuestaVacia() {
        when(repositorio.findAll()).thenReturn(List.of());

        ScoringVariableListResponse resultado = service.listar();

        assertThat(resultado.variables()).isEmpty();
        assertThat(resultado.sumaPesos()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void listar_conVariablesActivasCuyaPesosNoSumanUno_agregaAdvertencia() {
        // 3 variables activas, cada una con peso 0.20 → suma = 0.60 ≠ 1.00
        ScoringVariable v1 = variableActiva("Ingresos", new BigDecimal("0.20"));
        ScoringVariable v2 = variableActiva("Deuda", new BigDecimal("0.20"));
        ScoringVariable v3 = variableActiva("Score", new BigDecimal("0.20"));
        when(repositorio.findAll()).thenReturn(List.of(v1, v2, v3));

        ScoringVariableListResponse resultado = service.listar();

        assertThat(resultado.advertencias())
                .anyMatch(a -> a.contains("CA4") && a.contains("1.0000"));
    }

    @Test
    void listar_conVariablesActivasCuyaPesosSumanUno_noAgregarAdvertenciaDePesos() {
        // 3 variables activas que suman exactamente 1.00
        ScoringVariable v1 = variableActiva("Ingresos", new BigDecimal("0.40"));
        ScoringVariable v2 = variableActiva("Deuda", new BigDecimal("0.40"));
        ScoringVariable v3 = variableActiva("Score", new BigDecimal("0.20"));
        when(repositorio.findAll()).thenReturn(List.of(v1, v2, v3));

        ScoringVariableListResponse resultado = service.listar();

        assertThat(resultado.advertencias())
                .noneMatch(a -> a.contains("CA4"));
    }

    @Test
    void listar_conMenosDeTresVariablesActivas_agregaAdvertenciaRN4() {
        // Solo 2 activas → viola RN4
        ScoringVariable v1 = variableActiva("Ingresos", new BigDecimal("0.60"));
        ScoringVariable v2 = variableActiva("Deuda", new BigDecimal("0.40"));
        when(repositorio.findAll()).thenReturn(List.of(v1, v2));

        ScoringVariableListResponse resultado = service.listar();

        assertThat(resultado.advertencias())
                .anyMatch(a -> a.contains("RN4") && a.contains("2"));
    }

    @Test
    void listar_conTresOmasVariablesActivas_noAgregarAdvertenciaRN4() {
        ScoringVariable v1 = variableActiva("Ingresos", new BigDecimal("0.40"));
        ScoringVariable v2 = variableActiva("Deuda", new BigDecimal("0.40"));
        ScoringVariable v3 = variableActiva("Score", new BigDecimal("0.20"));
        when(repositorio.findAll()).thenReturn(List.of(v1, v2, v3));

        ScoringVariableListResponse resultado = service.listar();

        assertThat(resultado.advertencias())
                .noneMatch(a -> a.contains("RN4"));
    }

    @Test
    void listar_conVariablesInactivas_noLasContabilizaParaAdvertencias() {
        // 3 activas correctas + 1 inactiva con peso incorrecto
        ScoringVariable v1 = variableActiva("Ingresos", new BigDecimal("0.40"));
        ScoringVariable v2 = variableActiva("Deuda", new BigDecimal("0.40"));
        ScoringVariable v3 = variableActiva("Score", new BigDecimal("0.20"));
        ScoringVariable vInactiva = variableInactiva("Patrimonio", new BigDecimal("0.99"));
        when(repositorio.findAll()).thenReturn(List.of(v1, v2, v3, vInactiva));

        ScoringVariableListResponse resultado = service.listar();

        // No hay advertencias: las 3 activas suman 1.00 y son >= 3
        assertThat(resultado.advertencias()).isEmpty();
    }

    @Test
    void listar_sinVariablesActivas_noAgregaAdvertenciaDeCA4() {
        // Sin variables activas, la condición !activas.isEmpty() protege el CA4
        ScoringVariable vInactiva = variableInactiva("Campo", new BigDecimal("0.50"));
        when(repositorio.findAll()).thenReturn(List.of(vInactiva));

        ScoringVariableListResponse resultado = service.listar();

        // RN4 sí aplica (0 < 3), CA4 no (lista vacía)
        assertThat(resultado.advertencias())
                .noneMatch(a -> a.contains("CA4"));
        assertThat(resultado.advertencias())
                .anyMatch(a -> a.contains("RN4"));
    }

    @Test
    void listar_mapeoDeVariablesEnRespuesta_incluyeRangosYCategorias() {
        ScoringVariable v1 = variableActiva("Ingresos", new BigDecimal("0.40"));
        ScoringVariable v2 = variableActiva("Deuda", new BigDecimal("0.40"));
        ScoringVariable v3 = variableActiva("Score", new BigDecimal("0.20"));
        when(repositorio.findAll()).thenReturn(List.of(v1, v2, v3));

        ScoringVariableListResponse resultado = service.listar();

        assertThat(resultado.variables()).hasSize(3);
        assertThat(resultado.variables().get(0).nombre()).isEqualTo("Ingresos");
        assertThat(resultado.variables().get(0).rangos()).hasSize(1);
    }

    @Test
    void listar_sumaPesosCalculadaSoloConVariablesActivas() {
        ScoringVariable activa = variableActiva("Score", new BigDecimal("0.50"));
        ScoringVariable inactiva = variableInactiva("Otro", new BigDecimal("0.30"));
        // Tercera activa para alcanzar las 3 mínimas... pero este test es solo para suma
        ScoringVariable activa2 = variableActiva("Ingresos", new BigDecimal("0.50"));
        ScoringVariable activa3 = variableActiva("Deuda", new BigDecimal("0.01"));
        when(repositorio.findAll()).thenReturn(List.of(activa, activa2, activa3, inactiva));

        ScoringVariableListResponse resultado = service.listar();

        // suma de activas = 0.50 + 0.50 + 0.01 = 1.01 (no incluye la inactiva con 0.30)
        assertThat(resultado.sumaPesos()).isEqualByComparingTo(new BigDecimal("1.01"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ScoringVariable variableActiva(String nombre, BigDecimal peso) {
        VariableRange rango = new VariableRange(
                UUID.randomUUID(), null, BigDecimal.ZERO, new BigDecimal("1000000"), 80, "Normal");
        return ScoringVariable.rehydrate(
                UUID.randomUUID(), nombre, "Desc", VariableType.NUMERIC,
                peso, true, List.of(rango), List.of());
    }

    private ScoringVariable variableInactiva(String nombre, BigDecimal peso) {
        VariableRange rango = new VariableRange(
                UUID.randomUUID(), null, BigDecimal.ZERO, new BigDecimal("1000000"), 60, "Normal");
        return ScoringVariable.rehydrate(
                UUID.randomUUID(), nombre, "Desc", VariableType.NUMERIC,
                peso, false, List.of(rango), List.of());
    }
}
