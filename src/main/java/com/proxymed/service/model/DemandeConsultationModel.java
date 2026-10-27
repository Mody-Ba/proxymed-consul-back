package com.proxymed.service.model;

import com.proxymed.enums.StatutDemandeConsultation;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (DemandeConsultation*Request/Response), ni l'entite JPA.
 * A la creation, seuls nomPatient, pourProche et motif viennent du client : demandeurId est lu
 * dans le badge par le controller, le reste est fixe par le service.
 */
@Builder(toBuilder = true)
public record DemandeConsultationModel(
        UUID id,
        UUID demandeurId,
        String nomPatient,
        boolean pourProche,
        String motif,
        StatutDemandeConsultation statut,
        UUID medecinSeniorId,
        UUID juniorId,
        String motifRefus,
        Instant dateCreation,
        Instant dateDecision
) {
}
