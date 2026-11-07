package com.proxymed.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proxymed.enums.StatutFicheSignee;
import lombok.Builder;

import java.util.UUID;

/**
 * Resultat d'un fichier d'un import en lot. Identifiants, nom du fichier et codes seulement :
 * jamais d'identite du patient ni de donnee medicale. Reussite : resultat IMPORTEE, code 201, ids
 * de la fiche, de la consultation et du patient, statut de la fiche (EN_ATTENTE) et reimportee.
 * Echec : resultat ERREUR, code HTTP de l'erreur (413, 415, 422, 400, 404, 409, 500), erreur et
 * message au meme format qu'une reponse d'erreur du service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record ResultatImportResponse(
        int index,
        String nomFichier,
        String resultat,
        int code,
        UUID ficheId,
        UUID consultationId,
        UUID patientId,
        StatutFicheSignee statut,
        Boolean reimportee,
        String erreur,
        String message
) {

    public static final String IMPORTEE = "IMPORTEE";
    public static final String ERREUR = "ERREUR";
}
