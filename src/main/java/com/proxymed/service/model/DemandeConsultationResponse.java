package com.proxymed.service.model;

import com.proxymed.enums.StatutDemandeConsultation;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record DemandeConsultationResponse(
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
        Instant dateDecision,
        // Consultation initiale commencee a partir de cette demande (null sinon)
        UUID consultationId
) {
}
