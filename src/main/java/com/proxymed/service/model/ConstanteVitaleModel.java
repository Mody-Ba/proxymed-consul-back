package com.proxymed.service.model;

import com.proxymed.enums.TypeConstanteVitale;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Couche interne neutre : ni le contrat API (ConstanteVitaleRequest/Response), ni l'entite JPA.
 */
@Builder(toBuilder = true)
public record ConstanteVitaleModel(
        Long id,
        TypeConstanteVitale type,
        BigDecimal valeur,
        LocalTime heure,
        Boolean estNormal,
        Boolean estAlerte,
        String commentaire
) {
}
