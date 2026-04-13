package co.udea.codefactory.creditscoring.financialdata.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

public interface FinancialDataRepositoryPort {

    Optional<Integer> findMaxVersionByApplicantId(UUID applicantId);

    Optional<FinancialData> findByApplicantIdAndVersion(UUID applicantId, int version);

    /** Retorna todas las versiones de un solicitante, ordenadas por versión descendente. */
    List<FinancialData> findAllByApplicantId(UUID applicantId);

    FinancialData save(FinancialData financialData);
}
