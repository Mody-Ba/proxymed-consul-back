package com.proxymed.service.model;

import java.util.UUID;

/**
 * Le medecin senior qui accepte n'est pas dans le corps : il vient du badge. juniorId est
 * facultatif (le corps entier aussi) : il est seulement enregistre, aucun flux junior ne l'utilise.
 */
public record AcceptationDemandeRequest(
        UUID juniorId
) {
}
