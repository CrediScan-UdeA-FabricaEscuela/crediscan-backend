package co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.out.persistence;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;

@Component
public class FinancialDataPersistenceAdapter implements FinancialDataRepositoryPort {

    private final JpaFinancialDataRepository jpaRepository;
    private final Clock clock;

    @Autowired
    public FinancialDataPersistenceAdapter(JpaFinancialDataRepository jpaRepository) {
        this(jpaRepository, Clock.systemUTC());
    }

    FinancialDataPersistenceAdapter(JpaFinancialDataRepository jpaRepository, Clock clock) {
        this.jpaRepository = jpaRepository;
        this.clock = clock;
    }

    @Override
    public Optional<Integer> findMaxVersionByApplicantId(UUID applicantId) {
        return jpaRepository.findFirstByApplicantIdOrderByVersionDesc(applicantId)
                .map(FinancialDataJpaEntity::getVersion);
    }

    @Override
    public Optional<FinancialData> findByApplicantIdAndVersion(UUID applicantId, int version) {
        return jpaRepository.findByApplicantIdAndVersion(applicantId, version)
                .map(this::toDomain);
    }

    @Override
    public FinancialData save(FinancialData financialData) {
        FinancialDataJpaEntity entity = toEntity(financialData);
        entity.setCreatedBy(currentUsername());
        entity.setUpdatedBy(currentUsername());
        jpaRepository.save(entity);
        return financialData;
    }

    private FinancialData toDomain(FinancialDataJpaEntity entity) {
        return new FinancialData(
                entity.getId(),
                entity.getApplicantId(),
                entity.getVersion(),
                entity.getAnnualIncome(),
                entity.getMonthlyExpenses(),
                entity.getCurrentDebts(),
                entity.getAssetsValue(),
                entity.getDeclaredPatrimony(),
                entity.isHasOutstandingDefaults(),
                entity.getCreditHistoryMonths(),
                entity.getDefaultsLast12m(),
                entity.getDefaultsLast24m(),
                entity.getExternalBureauScore(),
                entity.getActiveCreditProducts(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private FinancialDataJpaEntity toEntity(FinancialData financialData) {
        FinancialDataJpaEntity entity = new FinancialDataJpaEntity();
        entity.setId(financialData.id());
        entity.setApplicantId(financialData.applicantId());
        entity.setVersion(financialData.version());
        entity.setAnnualIncome(financialData.annualIncome());
        entity.setMonthlyExpenses(financialData.monthlyExpenses());
        entity.setCurrentDebts(financialData.currentDebts());
        entity.setAssetsValue(financialData.assetsValue());
        entity.setDeclaredPatrimony(financialData.declaredPatrimony());
        entity.setHasOutstandingDefaults(financialData.hasOutstandingDefaults());
        entity.setCreditHistoryMonths(financialData.creditHistoryMonths());
        entity.setDefaultsLast12m(financialData.defaultsLast12m());
        entity.setDefaultsLast24m(financialData.defaultsLast24m());
        entity.setExternalBureauScore(financialData.externalBureauScore());
        entity.setActiveCreditProducts(financialData.activeCreditProducts());
        entity.setCreatedAt(financialData.createdAt());
        entity.setUpdatedAt(financialData.updatedAt());
        return entity;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }
}
