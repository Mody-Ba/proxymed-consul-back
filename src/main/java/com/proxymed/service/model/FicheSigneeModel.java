package com.proxymed.service.model;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (fichier multipart / FicheSigneeResponse), ni l'entite JPA.
 * id, consultationId, patientId, dateConsultation et dateImport ne sont renseignes que sur le
 * chemin de lecture (Entity -> Model) : a l'import, la consultation est deduite du QR code du PDF.
 * contenu est null dans les listes du dossier patient (metadonnees seulement).
 * Une seule fiche par consultation (contrainte uk_fiche_signee_consultation).
 */
@Builder(toBuilder = true)
public record FicheSigneeModel(
        UUID id,
        UUID consultationId,
        UUID patientId,
        LocalDate dateConsultation,
        String nomFichier,
        String typeContenu,
        long tailleOctets,
        Instant dateImport,
        byte[] contenu
) {
}
