package com.proxymed.service.model;

import lombok.Builder;

/**
 * Resultat de l'import d'un fichier d'un lot : soit la fiche rangee, soit l'erreur qui l'a refuse
 * (exactement l'un des deux). index est la position du fichier dans la requete (a partir de 0).
 */
@Builder(toBuilder = true)
public record ResultatImportModel(
        int index,
        String nomFichier,
        FicheSigneeModel fiche,
        RuntimeException erreur
) {

    public boolean reussi() {
        return erreur == null;
    }
}
