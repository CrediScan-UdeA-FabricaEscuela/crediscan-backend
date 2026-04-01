package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stub controller for /api/v1/variables-scoring.
 * Provides no business logic — exists solely so Spring Security can enforce
 * @PreAuthorize rules (without a mapped route, the filter would return 404 instead of 403).
 */
@RestController
@RequestMapping("/api/v1/variables-scoring")
public class ScoringVariableController {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createVariable() {
        return ResponseEntity.status(501).build();
    }
}
