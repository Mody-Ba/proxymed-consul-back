package com.proxymed.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proxymed.enums.StatutFicheSignee;
import lombok.Builder;

import java.util.UUID;

/**
 * Resultat d'une fiche d'un visa ou d'un renvoi en lot. Reussite : resultat VISEE ou RENVOYEE, code
 * 200, statut. Echec : resultat ERREUR, code HTTP de l'erreur (404 fiche introuvable, 409 fiche qui
 * n'est plus EN_ATTENTE, 500), erreur et message au meme format qu'une reponse d'erreur du service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record ResultatDecisionResponse(
        int index,
        UUID visiteId,
        String resultat,
        int code,
        StatutFicheSignee statut,
        String erreur,
        String message
) {

    public static final String ERREUR = "ERREUR";
}
