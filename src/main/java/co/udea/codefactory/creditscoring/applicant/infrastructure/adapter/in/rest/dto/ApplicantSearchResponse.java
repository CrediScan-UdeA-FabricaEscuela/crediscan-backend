package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single item in search results. Contains decrypted identification (plaintext).
 */
public record ApplicantSearchResponse(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("nombre")
        String name,

        @JsonProperty("identificacion")
        String identification,

        @JsonProperty("fecha_nacimiento")
        LocalDate birthDate,

        @JsonProperty("tipo_empleo")
        String employmentType,

        @JsonProperty("ingresos_mensuales")
        BigDecimal monthlyIncome,

        @JsonProperty("antiguedad_laboral")
        Integer workExperienceMonths,

        @JsonProperty("telefono")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String phone,

        @JsonProperty("direccion")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String address,

        @JsonProperty("correo_electronico")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        String email) {
}
