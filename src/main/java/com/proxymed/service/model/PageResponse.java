package com.proxymed.service.model;

import java.util.List;

/**
 * Page de resultats renvoyee par l'API. page commence a 0.
 */
public record PageResponse<T>(
        List<T> elements,
        int page,
        int taille,
        long totalElements,
        int totalPages
) {
}
