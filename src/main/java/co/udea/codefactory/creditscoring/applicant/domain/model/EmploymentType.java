package co.udea.codefactory.creditscoring.applicant.domain.model;

import java.util.Arrays;
import java.util.List;

import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;

public enum EmploymentType {
    EMPLEADO("Empleado"),
    INDEPENDIENTE("Independiente"),
    PENSIONADO("Pensionado"),
    DESEMPLEADO("Desempleado");

            private static final String INVALID_MESSAGE =
                "Tipo de empleo no válido. Valores permitidos: Empleado, Independiente, Pensionado, Desempleado";

    private final String apiValue;

    EmploymentType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static EmploymentType fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new ApplicantValidationException("El tipo_empleo es obligatorio");
        }

        return Arrays.stream(values())
                .filter(type -> type.apiValue.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new ApplicantValidationException(INVALID_MESSAGE));
    }

    public static List<String> allowedValues() {
        return Arrays.stream(values())
                .map(EmploymentType::apiValue)
                .toList();
    }
}
