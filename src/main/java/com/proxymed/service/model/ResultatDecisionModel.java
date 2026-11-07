package com.proxymed.service.model;

import com.proxymed.enums.StatutFicheSignee;
import lombok.Builder;

import java.util.UUID;

/**
 * Resultat d'un visa ou d'un renvoi pour une fiche d'un lot : soit le nouveau statut, soit l'erreur
 * qui l'a empeche (exactement l'un des deux). index est la position dans la requete (a partir de 0).
 */
@Builder(toBuilder = true)
public record ResultatDecisionModel(
        int index,
        UUID visiteId,
        StatutFicheSignee statut,
        RuntimeException erreur
) {

    public boolean reussi() {
        return erreur == null;
    }
}
