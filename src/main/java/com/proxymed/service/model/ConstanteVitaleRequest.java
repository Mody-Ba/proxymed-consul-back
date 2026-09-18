package com.proxymed.service.model;

import com.proxymed.enums.TypeConstanteVitale;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ConstanteVitaleRequest(
        @NotNull(message = "le type de constante est obligatoire")
        TypeConstanteVitale type,

        @NotNull(message = "la valeur est obligatoire")
        BigDecimal valeur,

        LocalTime heure,
        String commentaire
) {
}
