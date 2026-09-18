package com.proxymed.service.model;

import com.proxymed.enums.TypeConstanteVitale;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalTime;

@Builder(toBuilder = true)
public record ConstanteVitaleResponse(
        Long id,
        TypeConstanteVitale type,
        BigDecimal valeur,
        LocalTime heure,
        Boolean estNormal,
        Boolean estAlerte,
        String commentaire
) {
}
