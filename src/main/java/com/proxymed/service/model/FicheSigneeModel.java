package com.proxymed.service.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (fichier multipart / FicheSigneeResponse), ni l'entite JPA.
 * id, consultationId et dateImport ne sont renseignes que sur le chemin de lecture (Entity -> Model).
 */
@Builder(toBuilder = true)
public record FicheSigneeModel(
        UUID id,
        UUID consultationId,
        String nomFichier,
        String typeContenu,
        long tailleOctets,
        Instant dateImport,
        byte[] contenu
) {
}
