package com.proxymed.service.model;

import com.proxymed.enums.RoleMedecin;
import lombok.Builder;

/**
 * Couche interne neutre : ni le contrat API (MedecinRequest/Response), ni l'entite JPA.
 * structureRattachementLibelle n'est renseigne que sur le chemin de lecture (Entity -> Model) ;
 * a la creation, seul structureRattachementId est utilise pour rattacher la structure.
 */
@Builder(toBuilder = true)
public record MedecinModel(
        Long id,
        String nom,
        String prenom,
        String numeroOrdre,
        RoleMedecin role,
        Long structureRattachementId,
        String structureRattachementLibelle
) {
}
