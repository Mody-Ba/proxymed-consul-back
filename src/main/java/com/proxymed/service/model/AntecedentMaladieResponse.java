package com.proxymed.service.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record AntecedentMaladieResponse(
        Long id,
        Long maladieChroniqueId,
        String maladieChroniqueLibelle,
        String precision
) {
}
