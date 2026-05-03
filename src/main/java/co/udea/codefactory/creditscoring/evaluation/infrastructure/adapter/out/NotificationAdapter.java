package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out;

import co.udea.codefactory.creditscoring.evaluation.domain.port.out.NotificationPort;
import org.springframework.stereotype.Component;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;@Component
public class NotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdapter.class);

    @Override
    public void notifySupervisor(UUID evaluationId) {
        log.info(">>> Evaluación escalada: {}", evaluationId);
    }
}