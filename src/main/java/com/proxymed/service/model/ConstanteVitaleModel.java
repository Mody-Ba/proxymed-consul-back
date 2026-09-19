package com.proxymed.service.model;

import com.proxymed.enums.TypeConstanteVitale;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (ConstanteVitaleRequest/Response), ni l'entite JPA.
 * consultationId n'est renseigne que sur le chemin de lecture (Entity -> Model), pour permettre
 * au service de verifier l'appartenance a la consultation sans relire l'entite.
 */
@Builder(toBuilder = true)
public record ConstanteVitaleModel(
        Long id,
        UUID consultationId,
        TypeConstanteVitale type,
        BigDecimal valeur,
        LocalTime heure,
        Boolean estNormal,
        Boolean estAlerte,
        String commentaire
) {
}
