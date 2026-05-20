package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para consultas de solicitantes desde el BC de evaluación.
 * Delega al puerto público del BC applicant para preservar separación de bounded contexts.
 */
public interface ApplicantQueryPort {

    /**
     * Busca el nombre de un solicitante por su identificador.
     *
     * @param applicantId identificador único del solicitante
     * @return {@link Optional} con el nombre, vacío si el solicitante no existe
     */
    Optional<String> findNameById(UUID applicantId);
}
