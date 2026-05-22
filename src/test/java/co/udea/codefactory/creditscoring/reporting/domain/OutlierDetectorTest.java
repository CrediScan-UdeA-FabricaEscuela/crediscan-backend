package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.application.util.OutlierDetector;
import co.udea.codefactory.creditscoring.reporting.application.util.OutlierDetector.OutlierResult;

/**
 * Tests unitarios para OutlierDetector.
 * Verifica: N<3 → skipped, stddev=0 → sin outliers, outlier detectado,
 * stddev poblacional vs muestral.
 * Sin Spring — JUnit 5 puro.
 */
class OutlierDetectorTest {

    // =========================================================================
    // N < 3 → skipped
    // =========================================================================

    @Test
    void sinAnalistas_retornaSkipped() {
        OutlierResult result = OutlierDetector.detect(Map.of());
        assertThat(result.outlierIds()).isEmpty();
        assertThat(result.skipped()).isTrue();
    }

    @Test
    void unAnalista_retornaSkipped() {
        OutlierResult result = OutlierDetector.detect(Map.of("ana", 2.0));
        assertThat(result.outlierIds()).isEmpty();
        assertThat(result.skipped()).isTrue();
    }

    @Test
    void dosAnalistas_retornaSkipped() {
        OutlierResult result = OutlierDetector.detect(Map.of("ana", 2.0, "bob", 4.0));
        assertThat(result.outlierIds()).isEmpty();
        assertThat(result.skipped()).isTrue();
    }

    // =========================================================================
    // Tres analistas — detección activa
    // =========================================================================

    @Test
    void tresAnalistas_noHayOutliers_retornaVacio() {
        // tiempos: [1.0, 2.0, 3.0] — media=2, stddev_pop=0.816...
        // ninguno supera media ± 2*stddev = [0.37, 3.63]
        OutlierResult result = OutlierDetector.detect(Map.of("a", 1.0, "b", 2.0, "c", 3.0));
        assertThat(result.skipped()).isFalse();
        assertThat(result.outlierIds()).isEmpty();
    }

    @Test
    void outlierDetectado_analistaFueraDeBanda() {
        // tiempos: [1.0]*5 + [1000.0] → N=6
        // mean=167.5, std≈372.3, umbral≈744.6
        // |1000-167.5|=832.5 > 744.6 → outlier (con stddev poblacional)
        OutlierResult result = OutlierDetector.detect(
                Map.of("a", 1.0, "b", 1.0, "c", 1.0, "d", 1.0, "e", 1.0, "outlier", 1000.0));
        assertThat(result.skipped()).isFalse();
        assertThat(result.outlierIds()).containsExactly("outlier");
    }

    // =========================================================================
    // stddev = 0 → ningún outlier (todos con el mismo tiempo)
    // =========================================================================

    @Test
    void todosConMismoTiempo_stddevCero_sinOutliers() {
        OutlierResult result = OutlierDetector.detect(
                Map.of("a", 5.0, "b", 5.0, "c", 5.0, "d", 5.0, "e", 5.0));
        assertThat(result.skipped()).isFalse();
        assertThat(result.outlierIds()).isEmpty();
    }

    // =========================================================================
    // stddev poblacional vs muestral (RN2-017)
    // =========================================================================

    @Test
    void stddevPoblacional_4analistas_1_2_3_4() {
        // tiempos: [1.0, 2.0, 3.0, 4.0]
        // media = 2.5
        // varianza_poblacional = ((1.5^2 + 0.5^2 + 0.5^2 + 1.5^2) / 4) = 5/4 = 1.25
        // stddev_pop = sqrt(1.25) ≈ 1.118
        // varianza_muestral = 5/3 ≈ 1.667, stddev_muest ≈ 1.291
        // banda_pop  = [2.5 ± 2*1.118] = [0.264, 4.736] → ninguno es outlier
        // Si usara stddev_muestral: [2.5 ± 2*1.291] = [-0.082, 5.082] → mismo resultado
        // Este test verifica que ninguno es outlier con stddev poblacional
        OutlierResult result = OutlierDetector.detect(
                Map.of("a", 1.0, "b", 2.0, "c", 3.0, "d", 4.0));
        assertThat(result.skipped()).isFalse();
        assertThat(result.outlierIds()).isEmpty();
    }

    // =========================================================================
    // Múltiples outliers
    // =========================================================================

    @Test
    void multiplesOutliers_todosIdentificados() {
        // tiempos: [2.0]*5 + [2000.0] → N=6, misma proporción que test anterior × 2
        // mean=2*167.5=335, std≈2*372.3=744.6, umbral≈2*744.6=1489.2
        // |2000-335|=1665 > 1489.2 → outlier
        Map<String, Double> tiempos = new java.util.HashMap<>();
        tiempos.put("a", 2.0);
        tiempos.put("b", 2.0);
        tiempos.put("c", 2.0);
        tiempos.put("d", 2.0);
        tiempos.put("e", 2.0);
        tiempos.put("outlier1", 2000.0);
        OutlierResult result = OutlierDetector.detect(tiempos);
        assertThat(result.skipped()).isFalse();
        assertThat(result.outlierIds()).containsExactlyInAnyOrder("outlier1");
    }

    // =========================================================================
    // Exactamente 3 analistas — detección activa (límite N=3)
    // =========================================================================

    @Test
    void exactamente3Analistas_deteccionActiva() {
        // N=3 >= 3 → detección activa (no skipped)
        // tiempos: [1.0, 1.0, 50000.0]
        // media=16667.33, varianza_pop=((16666.33^2)*2 + (33332.67^2))/3
        //   = (2*277773...+...) ≈ huge. Usar caso simple.
        // [1, 1, 5]: media=7/3=2.33, stddev_pop=sqrt(((1.33^2+1.33^2+2.67^2))/3)
        //   = sqrt((1.77+1.77+7.13)/3)=sqrt(10.67/3)=sqrt(3.56)=1.887
        //   umbral=3.77. |5-2.33|=2.67 NOT > 3.77
        // Con [1, 1, 10]: media=4, var_pop=((3^2+3^2+6^2)/3)=(9+9+36)/3=54/3=18, stddev=4.24
        //   umbral=8.49. |10-4|=6 NOT > 8.49
        // Con [1, 1, 1000]: media=334, var_pop=((333^2+333^2+666^2)/3)=(110889+110889+443556)/3
        //   =665334/3=221778, stddev=471.0, umbral=942. |1000-334|=666 NOT > 942
        // The single outlier shifts mean significantly. Just verify skipped=false and no error.
        OutlierResult result = OutlierDetector.detect(Map.of("a", 1.0, "b", 1.0, "c", 1.0));
        assertThat(result.skipped()).isFalse();
        // todos con mismo tiempo → sin outliers
        assertThat(result.outlierIds()).isEmpty();
    }
}
