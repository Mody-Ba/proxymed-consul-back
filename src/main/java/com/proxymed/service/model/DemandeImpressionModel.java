package com.proxymed.service.model;

import com.proxymed.enums.TypeVisite;
import lombok.Builder;

import java.util.UUID;

/**
 * Couche interne neutre : une fiche vierge a imprimer, pour un patient existant et un type de visite.
 */
@Builder(toBuilder = true)
public record DemandeImpressionModel(
        UUID patientId,
        TypeVisite typeVisite
) {
}
