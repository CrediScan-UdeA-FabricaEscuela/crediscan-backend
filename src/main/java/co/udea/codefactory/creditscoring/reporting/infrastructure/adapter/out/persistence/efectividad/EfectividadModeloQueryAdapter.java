package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.efectividad;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.EfectividadModeloQueryPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de persistencia que implementa {@link EfectividadModeloQueryPort}
 * delegando a {@link EfectividadModeloJpaRepository}.
 */
@Component
@RequiredArgsConstructor
public class EfectividadModeloQueryAdapter implements EfectividadModeloQueryPort {

    private final EfectividadModeloJpaRepository repo;

    @Override
    public List<MatrizAggregate> queryMatriz(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String analistaId) {
        return repo.queryMatriz(desde, hasta, analistaId).stream()
                .map(p -> new MatrizAggregate(p.getRiskLevel(), p.getDecision(), p.getCount()))
                .toList();
    }
}
