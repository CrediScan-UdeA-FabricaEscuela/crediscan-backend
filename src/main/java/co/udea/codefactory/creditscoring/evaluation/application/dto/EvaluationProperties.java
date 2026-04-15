package co.udea.codefactory.creditscoring.evaluation.application.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuración del módulo de evaluación.
 * Se leen desde application.yml bajo el prefijo "app.evaluation".
 */
@Component
@ConfigurationProperties(prefix = "app.evaluation")
public class EvaluationProperties {

    /** Horas mínimas entre evaluaciones del mismo solicitante. */
    private long cooldownHours = 24;

    public long getCooldownHours() {
        return cooldownHours;
    }

    public void setCooldownHours(long cooldownHours) {
        this.cooldownHours = cooldownHours;
    }
}
