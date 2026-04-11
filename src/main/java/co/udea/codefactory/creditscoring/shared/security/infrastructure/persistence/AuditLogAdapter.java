package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;

import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogQueryPort;

@Component
public class AuditLogAdapter implements AuditLogPort, AuditLogQueryPort {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAdapter.class);

    private final JpaAuditLogRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public AuditLogAdapter(JpaAuditLogRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(String entityType, UUID entityId, String action, String actor,
                       String actorIp, String result, Object dataBefore, Object dataAfter) {
        JpaAuditLogEntity entry = new JpaAuditLogEntity();
        Instant now = Instant.now();
        entry.setId(UUID.randomUUID());
        entry.setCreatedAt(now);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setActor(actor);
        entry.setActorIp(actorIp);
        entry.setResult(result);
        entry.setDataBefore(toJson(dataBefore));
        entry.setDataAfter(toJson(dataAfter));
        jpaRepository.save(entry);
    }

    @Override
    public PagedResult<AuditLogRecord> search(AuditLogFilter filter, PageRequest pageRequest) {
        // Convierte el PageRequest de dominio al Pageable de Spring para la consulta JPA
        Pageable springPageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size());
        Page<AuditLogRecord> page = jpaRepository.findAll(buildSpecification(filter), springPageable)
                .map(this::toAuditLogRecord);
        return toPagedResult(page);
    }

    // Convierte Page de Spring a PagedResult de dominio
    private <T> PagedResult<T> toPagedResult(Page<T> page) {
        return new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    private AuditLogRecord toAuditLogRecord(JpaAuditLogEntity entity) {
        return new AuditLogRecord(
                entity.getId(),
                entity.getCreatedAt(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getActor(),
                entity.getActorIp(),
                entity.getResult(),
                entity.getDataBefore(),
                entity.getDataAfter(),
                entity.getDetails());
    }

    private Specification<JpaAuditLogEntity> buildSpecification(AuditLogFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
            }
            if (filter.actor() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actor"), filter.actor()));
            }
            if (filter.action() != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), filter.action()));
            }
            if (filter.entityType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), filter.entityType()));
            }
            if (filter.entityId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), filter.entityId()));
            }
            if (filter.actorIp() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorIp"), filter.actorIp()));
            }

            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit data: {}", e.getMessage());
            return obj.toString();
        }
    }
}
