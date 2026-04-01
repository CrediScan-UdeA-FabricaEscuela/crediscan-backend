package co.udea.codefactory.creditscoring.shared.security.domain.model;

public record RolePermission(
        Role role,
        String resource,
        String action) {
}
