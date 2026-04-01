package co.udea.codefactory.creditscoring.applicant.domain.port.out;

public interface ApplicantRegistrationMetricsPort {

    void recordSuccess();

    void recordFailure(String reason);
}
