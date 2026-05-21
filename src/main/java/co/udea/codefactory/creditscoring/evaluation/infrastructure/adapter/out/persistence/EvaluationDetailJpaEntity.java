package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para la tabla evaluation_detail.
 * Registra el puntaje parcial de cada variable de scoring evaluada.
 */
@Entity
@Table(name = "evaluation_detail")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationDetailJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private EvaluationJpaEntity evaluation;

    @Column(name = "variable_id", nullable = false)
    private UUID variableId;

    @Column(name = "variable_name", nullable = false, length = 100)
    private String variableName;

    @Column(name = "raw_value", nullable = false, length = 255)
    private String rawValue;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "weight", nullable = false, precision = 5, scale = 4)
    private BigDecimal weight;

    @Column(name = "weighted_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Setter manual requerido por la gestión de relación bidireccional en addDetail()
    public void setEvaluation(EvaluationJpaEntity evaluation) {
        this.evaluation = evaluation;
    }
}
