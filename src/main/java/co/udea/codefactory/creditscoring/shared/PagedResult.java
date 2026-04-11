package co.udea.codefactory.creditscoring.shared;

import java.util.List;

/**
 * Domain-safe pagination result. Replaces {@code org.springframework.data.domain.Page}
 * in domain ports and use case interfaces to avoid framework coupling.
 *
 * <p>Infrastructure adapters convert between this type and Spring's {@code Page}.</p>
 *
 * @param <T> type of the content elements
 */
public record PagedResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize) {
}
