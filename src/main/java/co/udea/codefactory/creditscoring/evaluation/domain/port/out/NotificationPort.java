package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.util.UUID;

public interface NotificationPort {
    void notifySupervisor(UUID evaluationId);
}