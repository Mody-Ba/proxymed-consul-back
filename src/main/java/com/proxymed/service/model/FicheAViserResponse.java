package com.proxymed.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne de la liste des fiches du senior. Reservee au senior, qui a le droit de voir l'identite du
 * patient (nom, numero de dossier). Le scan se lit par GET /api/consultations/{visiteId}/fiche-signee.
 * nomSeniorVisa et dateVisa : fiche VISEE seulement ; motifRenvoi : fiche RENVOYEE (ou EN_ATTENTE
 * reimportee apres un renvoi, pour voir pourquoi elle revient).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record FicheAViserResponse(
        UUID ficheId,
        UUID visiteId,
        UUID patientId,
        String nomPatient,
        String numeroDossierProxymed,
        TypeVisite typeVisite,
        LocalDate dateConsultation,
        Instant dateImport,
        StatutFicheSignee statut,
        String nomSeniorVisa,
        Instant dateVisa,
        String motifRenvoi
) {
}
