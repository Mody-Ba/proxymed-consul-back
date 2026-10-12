package com.proxymed.service.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadonnees uniquement : le contenu du fichier n'est jamais renvoye ici.
 */
@Builder(toBuilder = true)
public record FicheSigneeResponse(
        UUID id,
        String nomFichier,
        long tailleOctets,
        Instant dateImport
) {
}
