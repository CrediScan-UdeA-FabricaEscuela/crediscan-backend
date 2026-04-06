package co.udea.codefactory.creditscoring.applicant.domain.model;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;

public record Applicant(
        UUID id,
        String name,
        String identification,
        LocalDate birthDate,
        EmploymentType employmentType,
        BigDecimal monthlyIncome,
        Integer workExperienceMonths,
        String phone,
        String address,
        String email) {

    private static final String NAME_REQUIRED_MESSAGE = "Todos los campos obligatorios deben estar diligenciados";
    private static final String IDENTIFICATION_REQUIRED_MESSAGE = "La identificacion es obligatoria";
    private static final String BIRTH_DATE_REQUIRED_MESSAGE = "La fecha_nacimiento es obligatoria";
    private static final String INCOME_INVALID_MESSAGE = "Los ingresos mensuales deben ser un valor numérico mayor a cero";
    private static final String AGE_INVALID_MESSAGE = "Solo se aceptan solicitantes mayores de 18 años";
    private static final String WORK_EXPERIENCE_INVALID_MESSAGE = "La antiguedad_laboral debe ser mayor o igual a 0";
    private static final String PHONE_TOO_LONG_MESSAGE = "El teléfono no puede superar 20 caracteres";
    private static final String ADDRESS_TOO_LONG_MESSAGE = "La dirección no puede superar 500 caracteres";
    private static final String EMAIL_INVALID_MESSAGE = "El correo electrónico no es válido";

    public static Applicant registerNew(
            String name,
            String identification,
            LocalDate birthDate,
            EmploymentType employmentType,
            BigDecimal monthlyIncome,
            Integer workExperienceMonths,
            String phone,
            String address,
            String email,
            Clock clock) {

        validate(name, identification, birthDate, employmentType, monthlyIncome, workExperienceMonths, phone, address, email, clock);
        return new Applicant(
                UUID.randomUUID(),
                name.trim(),
                identification.trim(),
                birthDate,
                employmentType,
                monthlyIncome,
                workExperienceMonths,
                phone,
                address,
                email);
    }

    public static Applicant rehydrate(
            UUID id,
            String name,
            String identification,
            LocalDate birthDate,
            EmploymentType employmentType,
            BigDecimal monthlyIncome,
            Integer workExperienceMonths,
            String phone,
            String address,
            String email,
            Clock clock) {

        validate(name, identification, birthDate, employmentType, monthlyIncome, workExperienceMonths, phone, address, email, clock);
        if (id == null) {
            throw new ApplicantValidationException("El id es obligatorio");
        }
        return new Applicant(id, name.trim(), identification.trim(), birthDate, employmentType, monthlyIncome, workExperienceMonths, phone, address, email);
    }

    private static void validate(
            String name,
            String identification,
            LocalDate birthDate,
            EmploymentType employmentType,
            BigDecimal monthlyIncome,
            Integer workExperienceMonths,
            String phone,
            String address,
            String email,
            Clock clock) {

        if (name == null || name.isBlank()) {
            throw new ApplicantValidationException(NAME_REQUIRED_MESSAGE);
        }
        if (identification == null || identification.isBlank()) {
            throw new ApplicantValidationException(IDENTIFICATION_REQUIRED_MESSAGE);
        }
        if (birthDate == null) {
            throw new ApplicantValidationException(BIRTH_DATE_REQUIRED_MESSAGE);
        }
        if (employmentType == null) {
            throw new ApplicantValidationException("El tipo_empleo es obligatorio");
        }
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicantValidationException(INCOME_INVALID_MESSAGE);
        }
        if (workExperienceMonths == null || workExperienceMonths < 0) {
            throw new ApplicantValidationException(WORK_EXPERIENCE_INVALID_MESSAGE);
        }
        if (phone != null && phone.length() > 20) {
            throw new ApplicantValidationException(PHONE_TOO_LONG_MESSAGE);
        }
        if (address != null && address.length() > 500) {
            throw new ApplicantValidationException(ADDRESS_TOO_LONG_MESSAGE);
        }
        if (email != null && !email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ApplicantValidationException(EMAIL_INVALID_MESSAGE);
        }

        LocalDate now = LocalDate.now(Objects.requireNonNullElse(clock, Clock.systemUTC()));
        int age = Period.between(birthDate, now).getYears();
        if (age < 18) {
            throw new ApplicantValidationException(AGE_INVALID_MESSAGE);
        }
    }
}
