package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.shared.security.domain.port.in.GetPermissionMatrixUseCase;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.PermissionMatrixResponse;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Gestión de roles y permisos")
public class RoleController {

    private final GetPermissionMatrixUseCase getPermissionMatrixUseCase;

    public RoleController(GetPermissionMatrixUseCase getPermissionMatrixUseCase) {
        this.getPermissionMatrixUseCase = getPermissionMatrixUseCase;
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener matriz de permisos", description = "Solo ADMIN puede consultar la matriz completa")
    public ResponseEntity<PermissionMatrixResponse> getPermissions() {
        return ResponseEntity.ok(new PermissionMatrixResponse(getPermissionMatrixUseCase.getAll()));
    }
}
