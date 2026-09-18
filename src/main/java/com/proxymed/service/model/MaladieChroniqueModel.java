package com.proxymed.service.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record MaladieChroniqueModel(
        Long id,
        String libelle,
        boolean actif
) {
}
