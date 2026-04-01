package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateApplicantResponse(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("mensaje")
        String message,

        @JsonProperty("campos_auditados")
        List<String> changedFields,

        @JsonProperty("solicitante")
        ApplicantResponse applicant) {
}
