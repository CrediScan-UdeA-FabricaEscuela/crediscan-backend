package co.udea.codefactory.creditscoring.evaluation.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link ExportLimitExceededException}.
 */
class ExportLimitExceededExceptionTest {

    @Test
    void httpStatusCode_retorna422() {
        ExportLimitExceededException ex = new ExportLimitExceededException(1500L, 1000L);
        assertThat(ex.httpStatusCode()).isEqualTo(422);
    }

    @Test
    void errorCode_retornaExportLimitExceeded() {
        ExportLimitExceededException ex = new ExportLimitExceededException(1500L, 1000L);
        assertThat(ex.errorCode()).isEqualTo("EXPORT_LIMIT_EXCEEDED");
    }
}
