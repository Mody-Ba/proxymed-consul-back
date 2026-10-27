package com.proxymed.service.model;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Le medecin senior qui accepte n'est pas dans le corps : il vient du badge.
 */
public record AcceptationDemandeRequest(
        @NotNull(message = "l'identifiant du medecin junior est obligatoire")
        UUID juniorId
) {
}
