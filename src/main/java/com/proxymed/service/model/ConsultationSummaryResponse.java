package com.proxymed.service.model;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne allegee pour les listes/recherches du DMI (section 5.3/5.4).
 */
@Builder(toBuilder = true)
public record ConsultationSummaryResponse(
        UUID id,
        String patientNomComplet,
        String patientNumeroDossierProxymed,
        String patientNumeroDmi,
        String medecinSeniorNomComplet,
        String medecinJuniorAffecteNomComplet,
        LocalDate dateConsultation,
        StatutConsultation statut,
        DecisionEligibilite decisionEligibilite
) {
}
