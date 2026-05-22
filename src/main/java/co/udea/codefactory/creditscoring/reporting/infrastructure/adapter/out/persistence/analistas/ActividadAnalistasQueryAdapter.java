package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.analistas;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de persistencia que implementa {@link ActividadAnalistasQueryPort}
 * delegando a {@link ActividadAnalistasJpaRepository}.
 * <p>
 * Los timestamps retornados por native queries se proyectan como {@link Instant};
 * se convierten a {@link OffsetDateTime} UTC antes de pasarlos al dominio.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ActividadAnalistasQueryAdapter implements ActividadAnalistasQueryPort {

    private final ActividadAnalistasJpaRepository repo;

    @Override
    public List<AnalistaCountsAggregate> queryCounts(OffsetDateTime desde, OffsetDateTime hasta) {
        return repo.queryCounts(desde, hasta).stream()
                .map(p -> new AnalistaCountsAggregate(
                        p.getEvaluatedBy(),
                        p.getTotal(),
                        p.getAprobadas(),
                        p.getRechazadas(),
                        p.getRevisionManual(),
                        p.getEscaladas()))
                .toList();
    }

    @Override
    public List<AnalistaTimestampAggregate> queryTimestamps(
            OffsetDateTime desde, OffsetDateTime hasta) {
        return repo.queryTimestamps(desde, hasta).stream()
                .map(p -> new AnalistaTimestampAggregate(
                        p.getEvaluatedBy(),
                        toOffsetDateTime(p.getEvaluatedAt()),
                        toOffsetDateTime(p.getDecidedAt())))
                .toList();
    }

    /** Convierte {@link Instant} a {@link OffsetDateTime} en UTC (para paso al dominio). */
    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
