package co.udea.codefactory.creditscoring.scoring.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableRangeTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VARIABLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    @DisplayName("Debe crear un VariableRange válido con todos los campos correctos")
    void shouldCreateValidVariableRange() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("0");
        BigDecimal limiteSuperior = new BigDecimal("100");
        int puntaje = 50;
        String etiqueta = "Rango medio";

        // Act
        VariableRange range = new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, puntaje, etiqueta);

        // Assert
        assertAll(
                () -> assertNotNull(range),
                () -> assertEquals(ID, range.id()),
                () -> assertEquals(VARIABLE_ID, range.variableId()),
                () -> assertEquals(limiteInferior, range.limiteInferior()),
                () -> assertEquals(limiteSuperior, range.limiteSuperior()),
                () -> assertEquals(puntaje, range.puntaje()),
                () -> assertEquals(etiqueta, range.etiqueta())
        );
    }

    @Test
    @DisplayName("Debe crear un VariableRange válido con puntaje en el límite inferior (0)")
    void shouldCreateValidVariableRangeWithMinScore() {
        // Arrange
        BigDecimal limiteInferior = BigDecimal.ZERO;
        BigDecimal limiteSuperior = BigDecimal.ONE;

        // Act
        VariableRange range = new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, 0, "Min");

        // Assert
        assertEquals(0, range.puntaje());
    }

    @Test
    @DisplayName("Debe crear un VariableRange válido con puntaje en el límite superior (100)")
    void shouldCreateValidVariableRangeWithMaxScore() {
        // Arrange
        BigDecimal limiteInferior = BigDecimal.ZERO;
        BigDecimal limiteSuperior = BigDecimal.ONE;

        // Act
        VariableRange range = new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, 100, "Max");

        // Assert
        assertEquals(100, range.puntaje());
    }

    @Test
    @DisplayName("Debe crear un VariableRange válido con límite inferior igual a cero")
    void shouldCreateValidVariableRangeWithLimiteInferiorZero() {
        // Arrange
        BigDecimal limiteInferior = BigDecimal.ZERO;
        BigDecimal limiteSuperior = new BigDecimal("10");

        // Act
        VariableRange range = new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, 25, "Zero");

        // Assert
        assertEquals(0, range.limiteInferior().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el límite inferior es null")
    void shouldThrowWhenLimiteInferiorIsNull() {
        // Arrange
        BigDecimal limiteSuperior = new BigDecimal("100");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, null, limiteSuperior, 50, "Etiqueta")
        );
        assertEquals("Los límites inferior y superior del rango son obligatorios", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el límite superior es null")
    void shouldThrowWhenLimiteSuperiorIsNull() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("0");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, limiteInferior, null, 50, "Etiqueta")
        );
        assertEquals("Los límites inferior y superior del rango son obligatorios", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ambos límites son null")
    void shouldThrowWhenBothLimitsAreNull() {
        // Arrange, Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, null, null, 50, "Etiqueta")
        );
        assertEquals("Los límites inferior y superior del rango son obligatorios", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el límite inferior es negativo")
    void shouldThrowWhenLimiteInferiorIsNegative() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("-1");
        BigDecimal limiteSuperior = new BigDecimal("100");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, 50, "Etiqueta")
        );
        assertEquals("El límite inferior de un rango no puede ser negativo", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el límite inferior es igual al límite superior")
    void shouldThrowWhenLimiteInferiorEqualsLimiteSuperior() {
        // Arrange
        BigDecimal limite = new BigDecimal("50");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, limite, limite, 50, "Etiqueta")
        );
        assertEquals("El límite inferior debe ser estrictamente menor que el límite superior", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el límite inferior es mayor que el límite superior")
    void shouldThrowWhenLimiteInferiorIsGreaterThanLimiteSuperior() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("100");
        BigDecimal limiteSuperior = new BigDecimal("50");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, 50, "Etiqueta")
        );
        assertEquals("El límite inferior debe ser estrictamente menor que el límite superior", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el puntaje es negativo")
    void shouldThrowWhenPuntajeIsNegative() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("0");
        BigDecimal limiteSuperior = new BigDecimal("100");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, -1, "Etiqueta")
        );
        assertEquals("El puntaje del rango debe estar entre 0 y 100 (valor recibido: -1)", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el puntaje es mayor a 100")
    void shouldThrowWhenPuntajeIsGreaterThan100() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("0");
        BigDecimal limiteSuperior = new BigDecimal("100");

        // Act & Assert
        ScoringVariableValidationException ex = assertThrows(
                ScoringVariableValidationException.class,
                () -> new VariableRange(ID, VARIABLE_ID, limiteInferior, limiteSuperior, 101, "Etiqueta")
        );
        assertEquals("El puntaje del rango debe estar entre 0 y 100 (valor recibido: 101)", ex.getMessage());
    }

    @Test
    @DisplayName("Debe permitir id, variableId y etiqueta nulos al no ser validados en el constructor")
    void shouldAllowNullIdVariableIdAndEtiqueta() {
        // Arrange
        BigDecimal limiteInferior = new BigDecimal("0");
        BigDecimal limiteSuperior = new BigDecimal("100");

        // Act
        VariableRange range = new VariableRange(null, null, limiteInferior, limiteSuperior, 50, null);

        // Assert
        assertAll(
                () -> assertNotNull(range),
                () -> assertNull(range.id()),
                () -> assertNull(range.variableId()),
                () -> assertNull(range.etiqueta())
        );
    }
}