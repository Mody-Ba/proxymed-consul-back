package com.proxymed.service.model;

import com.proxymed.enums.TypeVisite;
import lombok.Builder;

import java.util.UUID;

/**
 * Reponse destinee a l'admin : identifiants et type de visite uniquement, jamais l'identite du
 * patient ni une donnee medicale.
 */
@Builder(toBuilder = true)
public record VisiteViergeResponse(
        UUID id,
        UUID patientId,
        TypeVisite typeVisite
) {
}
