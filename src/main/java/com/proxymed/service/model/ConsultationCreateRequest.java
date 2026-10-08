package com.proxymed.service.model;

import com.proxymed.enums.OrigineDemande;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Payload minimal pour creer une fiche en BROUILLON (section 1 : identification patient et medecin),
 * avec optionnellement le motif de la demande (section 2) deja connu a la creation.
 */
@Builder(toBuilder = true)
public record ConsultationCreateRequest(
        @NotNull(message = "le patient est obligatoire")
        UUID patientId,

        @NotNull(message = "le medecin senior est obligatoire")
        Long medecinSeniorId,

        LocalDate dateConsultation,
        LocalTime heureConsultation,
        String saisiePar,

        // Section 2 : motif de la demande (optionnel a la creation)
        OrigineDemande origineDemande,
        String origineDemandeAutrePrecision,
        String motifPrincipalConsultation
) {
}
