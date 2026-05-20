package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ScoringModelQueryPort;
import co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence.JpaScoringModelRepository;

/**
 * Adaptador que implementa {@link ScoringModelQueryPort} delegando a
 * {@code JpaScoringModelRepository} del BC scoringmodel (repositorio público).
 */
@Component
public class ScoringModelQueryAdapter implements ScoringModelQueryPort {

    private final JpaScoringModelRepository jpaScoringModelRepository;

    public ScoringModelQueryAdapter(JpaScoringModelRepository jpaScoringModelRepository) {
        this.jpaScoringModelRepository = jpaScoringModelRepository;
    }

    @Override
    public Optional<ModelInfo> findById(UUID modelId) {
        return jpaScoringModelRepository.findById(modelId)
                .map(m -> new ModelInfo(m.getId(), m.getName(), m.getVersion()));
    }
}
