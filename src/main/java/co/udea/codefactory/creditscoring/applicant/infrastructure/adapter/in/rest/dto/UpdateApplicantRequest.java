package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PATCH request body. All fields nullable — PATCH semantics.
 * If identification or birthDate are non-null, the service returns 400 IMMUTABLE_FIELD.
 */
public record UpdateApplicantRequest(
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
        String phone,

        @JsonProperty("direccion")
        String address,

        @JsonProperty("correo_electronico")
        String email) {
}
