package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterApplicantResponse(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("mensaje")
        String message,

        @JsonProperty("solicitante")
        ApplicantResponse applicant) {
}
