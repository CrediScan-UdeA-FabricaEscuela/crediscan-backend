package co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataComparisonResponse;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataRequest;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataResponse;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.in.CreateFinancialDataUseCase;
import co.udea.codefactory.creditscoring.financialdata.domain.port.in.GetFinancialHistoryUseCase;
import co.udea.codefactory.creditscoring.financialdata.domain.port.in.UpdateFinancialDataUseCase;

@RestController
@RequestMapping("/api/v1/solicitantes")
@Tag(name = "Datos Financieros", description = "Operaciones sobre datos financieros de solicitantes")
public class FinancialDataController {

    private final CreateFinancialDataUseCase createFinancialDataUseCase;
    private final UpdateFinancialDataUseCase updateFinancialDataUseCase;
    private final GetFinancialHistoryUseCase getFinancialHistoryUseCase;
    private final FinancialDataRestMapper mapper;

    public FinancialDataController(
            CreateFinancialDataUseCase createFinancialDataUseCase,
            UpdateFinancialDataUseCase updateFinancialDataUseCase,
            GetFinancialHistoryUseCase getFinancialHistoryUseCase,
            FinancialDataRestMapper mapper) {
        this.createFinancialDataUseCase = createFinancialDataUseCase;
        this.updateFinancialDataUseCase = updateFinancialDataUseCase;
        this.getFinancialHistoryUseCase = getFinancialHistoryUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/datos-financieros")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Registrar datos financieros",
               description = "Registra un conjunto de datos financieros para un solicitante existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Datos financieros registrados"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitante no encontrado",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<FinancialDataResponse> createFinancialData(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FinancialDataRequest request) {
        FinancialData saved = createFinancialDataUseCase.create(id, request);
        FinancialDataResponse response = mapper.toResponse(saved);
        return ResponseEntity.created(URI.create("/api/v1/solicitantes/" + id + "/datos-financieros/" + saved.version()))
                .body(response);
    }

    @PutMapping("/{id}/datos-financieros/{version}")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar datos financieros",
               description = "Toma la versión indicada como referencia y persiste una nueva versión histórica con los datos proporcionados. La operación NO es idempotente: cada llamada genera una versión adicional.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos financieros actualizados"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitante o versión no encontrada",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<FinancialDataResponse> updateFinancialData(
            @PathVariable("id") UUID id,
            @PathVariable("version") int version,
            @Valid @RequestBody FinancialDataRequest request) {
        FinancialData updated = updateFinancialDataUseCase.update(id, version, request);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @GetMapping("/{id}/datos-financieros")
    @PreAuthorize("hasRole('ANALYST') or hasRole('RISK_MANAGER') or hasRole('ADMIN') or hasRole('CREDIT_SUPERVISOR')")
    @Operation(summary = "Consultar historial de datos financieros",
               description = "Retorna todas las versiones de datos financieros de un solicitante, "
                           + "ordenadas por versión descendente (más reciente primero). "
                           + "Cada versión incluye los ratios calculados y las alertas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial retornado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Solicitante no encontrado",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<List<FinancialDataResponse>> obtenerHistorial(
            @PathVariable("id") UUID id) {
        List<FinancialDataResponse> historial = getFinancialHistoryUseCase.obtenerHistorial(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/{id}/datos-financieros/comparar")
    @PreAuthorize("hasRole('ANALYST') or hasRole('RISK_MANAGER') or hasRole('ADMIN') or hasRole('CREDIT_SUPERVISOR')")
    @Operation(summary = "Comparar dos versiones de datos financieros",
               description = "Compara dos versiones de datos financieros side-by-side. "
                           + "Retorna ambas versiones, los campos que cambiaron con su estado "
                           + "(MEJORA/DETERIORO) y la tendencia general calculada según RN1.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comparación retornada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Versiones iguales o solicitud inválida",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Solicitante o versión no encontrada",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<FinancialDataComparisonResponse> comparar(
            @PathVariable("id") UUID id,
            @RequestParam("v1") int version1,
            @RequestParam("v2") int version2) {
        FinancialDataComparisonResponse respuesta = mapper.toComparisonResponse(
                getFinancialHistoryUseCase.comparar(id, version1, version2));
        return ResponseEntity.ok(respuesta);
    }
}
