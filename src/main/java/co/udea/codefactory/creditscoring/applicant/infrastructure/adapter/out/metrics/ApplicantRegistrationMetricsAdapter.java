package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRegistrationMetricsPort;

@Component
public class ApplicantRegistrationMetricsAdapter implements ApplicantRegistrationMetricsPort {

    private final Counter successCounter;
    private final MeterRegistry meterRegistry;

    public ApplicantRegistrationMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.successCounter = Counter.builder("applicant.registration.success")
                .description("Number of successful applicant registrations")
                .register(meterRegistry);
    }

    @Override
    public void recordSuccess() {
        successCounter.increment();
    }

    @Override
    public void recordFailure(String reason) {
        Counter.builder("applicant.registration.failure")
                .description("Number of failed applicant registrations")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}
