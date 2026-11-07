package com.proxymed.service.model;

import lombok.Builder;

import java.util.List;

/**
 * Reponse d'un import en lot : un resultat par fichier, dans l'ordre de la requete.
 */
@Builder(toBuilder = true)
public record ImportLotResponse(
        int nombreFichiers,
        int nombreImportees,
        int nombreErreurs,
        List<ResultatImportResponse> resultats
) {
}
