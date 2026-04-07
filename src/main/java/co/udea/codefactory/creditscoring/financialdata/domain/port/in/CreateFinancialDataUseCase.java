package co.udea.codefactory.creditscoring.financialdata.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataRequest;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

public interface CreateFinancialDataUseCase {

    FinancialData create(UUID applicantId, FinancialDataRequest request);
}
