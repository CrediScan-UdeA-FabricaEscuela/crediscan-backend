package co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "financial_data",
       uniqueConstraints = @UniqueConstraint(columnNames = {"applicant_id", "version"}))
public class FinancialDataJpaEntity {

    @Id
    private UUID id;

    @Column(name = "applicant_id", nullable = false)
    private UUID applicantId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "annual_income", nullable = false)
    private BigDecimal annualIncome;

    @Column(name = "monthly_expenses", nullable = false)
    private BigDecimal monthlyExpenses;

    @Column(name = "current_debts", nullable = false)
    private BigDecimal currentDebts;

    @Column(name = "assets_value", nullable = false)
    private BigDecimal assetsValue;

    @Column(name = "declared_patrimony", nullable = false)
    private BigDecimal declaredPatrimony;

    @Column(name = "has_outstanding_defaults", nullable = false)
    private boolean hasOutstandingDefaults;

    @Column(name = "credit_history_months", nullable = false)
    private int creditHistoryMonths;

    @Column(name = "defaults_last_12m", nullable = false)
    private int defaultsLast12m;

    @Column(name = "defaults_last_24m", nullable = false)
    private int defaultsLast24m;

    @Column(name = "external_bureau_score")
    private Integer externalBureauScore;

    @Column(name = "active_credit_products", nullable = false)
    private int activeCreditProducts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(UUID applicantId) {
        this.applicantId = applicantId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public BigDecimal getCurrentDebts() {
        return currentDebts;
    }

    public void setCurrentDebts(BigDecimal currentDebts) {
        this.currentDebts = currentDebts;
    }

    public BigDecimal getAssetsValue() {
        return assetsValue;
    }

    public void setAssetsValue(BigDecimal assetsValue) {
        this.assetsValue = assetsValue;
    }

    public BigDecimal getDeclaredPatrimony() {
        return declaredPatrimony;
    }

    public void setDeclaredPatrimony(BigDecimal declaredPatrimony) {
        this.declaredPatrimony = declaredPatrimony;
    }

    public boolean isHasOutstandingDefaults() {
        return hasOutstandingDefaults;
    }

    public void setHasOutstandingDefaults(boolean hasOutstandingDefaults) {
        this.hasOutstandingDefaults = hasOutstandingDefaults;
    }

    public int getCreditHistoryMonths() {
        return creditHistoryMonths;
    }

    public void setCreditHistoryMonths(int creditHistoryMonths) {
        this.creditHistoryMonths = creditHistoryMonths;
    }

    public int getDefaultsLast12m() {
        return defaultsLast12m;
    }

    public void setDefaultsLast12m(int defaultsLast12m) {
        this.defaultsLast12m = defaultsLast12m;
    }

    public int getDefaultsLast24m() {
        return defaultsLast24m;
    }

    public void setDefaultsLast24m(int defaultsLast24m) {
        this.defaultsLast24m = defaultsLast24m;
    }

    public Integer getExternalBureauScore() {
        return externalBureauScore;
    }

    public void setExternalBureauScore(Integer externalBureauScore) {
        this.externalBureauScore = externalBureauScore;
    }

    public int getActiveCreditProducts() {
        return activeCreditProducts;
    }

    public void setActiveCreditProducts(int activeCreditProducts) {
        this.activeCreditProducts = activeCreditProducts;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
