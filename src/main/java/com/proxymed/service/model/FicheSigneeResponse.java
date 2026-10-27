package com.proxymed.service.model;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Metadonnees uniquement : le contenu du fichier n'est jamais renvoye ici.
 * patientId, consultationId et dateConsultation indiquent ou la fiche a ete rangee (deduits du QR code).
 */
@Builder(toBuilder = true)
public record FicheSigneeResponse(
        UUID id,
        UUID patientId,
        UUID consultationId,
        LocalDate dateConsultation,
        String nomFichier,
        long tailleOctets,
        Instant dateImport
) {
}
