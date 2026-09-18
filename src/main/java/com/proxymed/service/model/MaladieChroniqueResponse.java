package com.proxymed.service.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record MaladieChroniqueResponse(
        Long id,
        String libelle,
        boolean actif
) {
}
