package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.util.Collections;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub controller for /api/v1/evaluaciones.
 * Provides no business logic — exists solely so Spring Security can enforce
 * @PreAuthorize rules (without a mapped route, the filter would return 404 instead of 403).
 */
@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluationController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<List<Object>> listEvaluaciones() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<Void> createEvaluacion() {
        return ResponseEntity.status(501).build();
    }
}
