package com.proxymed.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proxymed.enums.FrequenceVisites;
import com.proxymed.enums.StatutFicheSignee;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Metadonnees uniquement : le contenu du fichier n'est jamais renvoye ici.
 * patientId, consultationId et dateConsultation indiquent ou la fiche a ete rangee (deduits du QR code).
 * frequenceVisitesSenior : seulement si le senior l'a decidee au visa (absente sinon).
 */
@Builder(toBuilder = true)
public record FicheSigneeResponse(
        UUID id,
        UUID patientId,
        UUID consultationId,
        LocalDate dateConsultation,
        String nomFichier,
        long tailleOctets,
        Instant dateImport,
        StatutFicheSignee statut,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        FrequenceVisites frequenceVisitesSenior
) {
}
