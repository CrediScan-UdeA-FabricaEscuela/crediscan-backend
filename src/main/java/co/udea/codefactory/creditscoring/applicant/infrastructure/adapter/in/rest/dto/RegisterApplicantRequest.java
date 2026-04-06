package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterApplicantRequest(
        @JsonProperty("nombre")
        @NotBlank(message = "Todos los campos obligatorios deben estar diligenciados")
        String name,

        @JsonProperty("identificacion")
        @NotBlank(message = "La identificacion es obligatoria")
        String identification,

        @JsonProperty("fecha_nacimiento")
        @NotNull(message = "La fecha_nacimiento es obligatoria")
        LocalDate birthDate,

        @JsonProperty("tipo_empleo")
        @NotBlank(message = "El tipo_empleo es obligatorio")
        String employmentType,

        @JsonProperty("ingresos_mensuales")
        @NotNull(message = "Los ingresos_mensuales son obligatorios")
        BigDecimal monthlyIncome,

        @JsonProperty("antiguedad_laboral")
        @NotNull(message = "La antiguedad_laboral es obligatoria")
        @Min(value = 0, message = "La antiguedad_laboral debe ser mayor o igual a 0")
        Integer workExperienceMonths,

        @JsonProperty("direccion")
        String address,

        @JsonProperty("correo_electronico")
        String email) {
}
