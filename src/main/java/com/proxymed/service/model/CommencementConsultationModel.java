package com.proxymed.service.model;

import com.proxymed.enums.StatutConsultation;
import lombok.Builder;

import java.util.UUID;

/**
 * Resultat du commencement d'une consultation initiale a partir d'une demande : identifiants
 * crees ou lies, sans aucune donnee d'identite ni medicale.
 */
@Builder(toBuilder = true)
public record CommencementConsultationModel(
        UUID demandeId,
        UUID patientId,
        UUID consultationId,
        StatutConsultation statutConsultation
) {
}
