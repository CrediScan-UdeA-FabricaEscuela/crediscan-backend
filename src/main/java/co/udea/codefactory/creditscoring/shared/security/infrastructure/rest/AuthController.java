package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest;

import java.net.URI;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.shared.security.domain.exception.InvalidCredentialsException;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AppUser;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuthResult;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.AuthenticateUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.ChangeUserRoleUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.CreateUserUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogPort;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.ChangeRoleRequest;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.CreateUserRequest;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.CreateUserResponse;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.LoginRequest;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.LoginResponse;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticación y gestión de usuarios")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final AuditLogPort auditLog;

    public AuthController(
            AuthenticateUseCase authenticateUseCase,
            ChangeUserRoleUseCase changeUserRoleUseCase,
            CreateUserUseCase createUserUseCase,
            AuditLogPort auditLog) {
        this.authenticateUseCase = authenticateUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
        this.createUserUseCase = createUserUseCase;
        this.auditLog = auditLog;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autenticar usuario y obtener JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest servletRequest) {
        try {
            AuthResult result = authenticateUseCase.authenticate(request.username(), request.password());
            auditLog.record("USER", null, "LOGIN", request.username(), getClientIp(servletRequest), "SUCCESS", null,
                    null);
            return ResponseEntity.ok(new LoginResponse(result.token(), result.role(), result.expiresAt()));
        } catch (InvalidCredentialsException ex) {
            auditLog.record("USER", null, "LOGIN", request.username(), getClientIp(servletRequest), "FAILURE", null,
                    null);
            throw ex;
        }
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear usuario", description = "Solo ADMIN puede crear usuarios")
    public ResponseEntity<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        AppUser created = createUserUseCase.create(
                request.username(),
                request.email(),
                request.password(),
                request.rol(),
                authentication.getName());
        URI location = URI.create("/api/v1/auth/usuarios/" + created.id());
        return ResponseEntity.created(location)
                .body(new CreateUserResponse(created.id(), created.username(), created.email(), created.role()));
    }

    @PatchMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar rol de usuario", description = "Solo ADMIN puede cambiar roles")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication) {
        changeUserRoleUseCase.changeRole(id, request.rol(), authentication.getName());
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
