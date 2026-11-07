package com.proxymed.service.model;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Couche interne neutre : une ligne de la liste des fiches du senior (metadonnees, jamais le contenu).
 */
@Builder(toBuilder = true)
public record FicheAViserModel(
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
