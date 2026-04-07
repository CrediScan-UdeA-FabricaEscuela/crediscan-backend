package co.udea.codefactory.creditscoring.financialdata.application.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataRequest;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.in.CreateFinancialDataUseCase;
import co.udea.codefactory.creditscoring.financialdata.domain.port.in.UpdateFinancialDataUseCase;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
public class FinancialDataCommandService implements CreateFinancialDataUseCase, UpdateFinancialDataUseCase {

    private final FinancialDataRepositoryPort financialDataRepositoryPort;
    private final ApplicantRepositoryPort applicantRepositoryPort;
    private final Clock clock;

    @Autowired
    public FinancialDataCommandService(
            FinancialDataRepositoryPort financialDataRepositoryPort,
            ApplicantRepositoryPort applicantRepositoryPort) {
        this(financialDataRepositoryPort, applicantRepositoryPort, Clock.systemUTC());
    }

    FinancialDataCommandService(
            FinancialDataRepositoryPort financialDataRepositoryPort,
            ApplicantRepositoryPort applicantRepositoryPort,
            Clock clock) {
        this.financialDataRepositoryPort = financialDataRepositoryPort;
        this.applicantRepositoryPort = applicantRepositoryPort;
        this.clock = clock;
    }

    @Override
    public FinancialData create(UUID applicantId, FinancialDataRequest request) {
        ensureApplicantExists(applicantId);
        int nextVersion = financialDataRepositoryPort.findMaxVersionByApplicantId(applicantId).orElse(0) + 1;
        OffsetDateTime now = OffsetDateTime.now(clock);
        FinancialData financialData = new FinancialData(
                UUID.randomUUID(),
                applicantId,
                nextVersion,
                request.annualIncome(),
                request.monthlyExpenses(),
                request.currentDebts(),
                request.assetsValue(),
                request.declaredPatrimony(),
                request.hasOutstandingDefaults(),
                request.creditHistoryMonths(),
                request.defaultsLast12m(),
                request.defaultsLast24m(),
                request.externalBureauScore(),
                request.activeCreditProducts(),
                now,
                now);
        return financialDataRepositoryPort.save(financialData);
    }

    @Override
    public FinancialData update(UUID applicantId, int version, FinancialDataRequest request) {
        ensureApplicantExists(applicantId);
        financialDataRepositoryPort.findByApplicantIdAndVersion(applicantId, version)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialData", "version", version));
        int nextVersion = financialDataRepositoryPort.findMaxVersionByApplicantId(applicantId).orElse(version) + 1;
        OffsetDateTime now = OffsetDateTime.now(clock);
        FinancialData financialData = new FinancialData(
                UUID.randomUUID(),
                applicantId,
                nextVersion,
                request.annualIncome(),
                request.monthlyExpenses(),
                request.currentDebts(),
                request.assetsValue(),
                request.declaredPatrimony(),
                request.hasOutstandingDefaults(),
                request.creditHistoryMonths(),
                request.defaultsLast12m(),
                request.defaultsLast24m(),
                request.externalBureauScore(),
                request.activeCreditProducts(),
                now,
                now);
        return financialDataRepositoryPort.save(financialData);
    }

    private void ensureApplicantExists(UUID applicantId) {
        if (applicantRepositoryPort.findById(applicantId).isEmpty()) {
            throw new ResourceNotFoundException("Applicant", "id", applicantId);
        }
    }
}
