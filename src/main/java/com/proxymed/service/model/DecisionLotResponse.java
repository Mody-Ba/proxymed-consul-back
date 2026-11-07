package com.proxymed.service.model;

import lombok.Builder;

import java.util.List;

/**
 * Reponse d'un visa ou d'un renvoi en lot : un resultat par fiche, dans l'ordre de la requete.
 */
@Builder(toBuilder = true)
public record DecisionLotResponse(
        int nombreFiches,
        int nombreReussies,
        int nombreErreurs,
        List<ResultatDecisionResponse> resultats
) {
}
