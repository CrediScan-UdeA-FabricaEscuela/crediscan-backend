package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;

@Component
public class AuditLogAdapter implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAdapter.class);

    private final JpaAuditLogRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public AuditLogAdapter(JpaAuditLogRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void record(String entityType, UUID entityId, String action, String actor,
                       Object dataBefore, Object dataAfter) {
        JpaAuditLogEntity entry = new JpaAuditLogEntity();
        Instant now = Instant.now();
        entry.setId(UUID.randomUUID());
        entry.setCreatedAt(now);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setActor(actor);
        entry.setDataBefore(toJson(dataBefore));
        entry.setDataAfter(toJson(dataAfter));
        jpaRepository.save(entry);
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
