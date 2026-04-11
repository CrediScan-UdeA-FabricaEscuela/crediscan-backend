package co.udea.codefactory.creditscoring.shared;

/**
 * Domain-safe pagination request. Replaces {@code org.springframework.data.domain.Pageable}
 * in domain ports and use case interfaces to avoid framework coupling.
 *
 * @param page zero-based page index
 * @param size number of elements per page
 */
public record PageRequest(int page, int size) {
}
