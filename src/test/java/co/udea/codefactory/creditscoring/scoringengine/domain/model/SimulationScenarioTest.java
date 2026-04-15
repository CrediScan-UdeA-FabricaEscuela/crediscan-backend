package co.udea.codefactory.creditscoring.scoringengine.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SimulationScenarioTest {

    private final UUID MODELO_ID = UUID.randomUUID();

    @Test
    void crear_conDatosValidos_retornaEscenario() {
        SimulationScenario escenario = SimulationScenario.crear(
                MODELO_ID, "Escenario A", "Descripción", Map.of("moras_12_meses", BigDecimal.ONE), "user");

        assertThat(escenario.id()).isNotNull();
        assertThat(escenario.modeloId()).isEqualTo(MODELO_ID);
        assertThat(escenario.nombre()).isEqualTo("Escenario A");
        assertThat(escenario.valoresVariables()).containsKey("moras_12_meses");
        assertThat(escenario.fechaCreacion()).isNotNull();
    }

    @Test
    void crear_sinModeloId_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> SimulationScenario.crear(
                null, "Escenario", "Desc", Map.of("campo", BigDecimal.ONE), "user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeloId");
    }

    @Test
    void crear_sinNombre_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> SimulationScenario.crear(
                MODELO_ID, "", "Desc", Map.of("campo", BigDecimal.ONE), "user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void crear_sinValores_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> SimulationScenario.crear(
                MODELO_ID, "Escenario", "Desc", Map.of(), "user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valor");
    }

    @Test
    void valoresVariables_esInmutable() {
        SimulationScenario escenario = SimulationScenario.crear(
                MODELO_ID, "Test", null, Map.of("moras_12_meses", BigDecimal.TEN), "user");

        assertThatThrownBy(() -> escenario.valoresVariables().put("extra", BigDecimal.ONE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rehydrate_reconstruyeCorrectamente() {
        UUID id = UUID.randomUUID();
        Map<String, BigDecimal> valores = Map.of("score_buro", new BigDecimal("720"));

        SimulationScenario escenario = SimulationScenario.rehydrate(
                id, MODELO_ID, "Rehydrated", "Desc", valores,
                java.time.OffsetDateTime.now(), "test_user");

        assertThat(escenario.id()).isEqualTo(id);
        assertThat(escenario.creadoPor()).isEqualTo("test_user");
    }
}
