package com.proxymed.service.model;

import jakarta.validation.constraints.NotNull;

public record AntecedentMaladieRequest(
        @NotNull(message = "l'identifiant de la maladie chronique est obligatoire")
        Long maladieChroniqueId,

        String precision
) {
}
