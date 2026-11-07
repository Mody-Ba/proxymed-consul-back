package com.proxymed.service;

import com.proxymed.service.model.FichierAImporterModel;
import com.proxymed.service.model.ResultatImportModel;

import java.util.List;

public interface ImportFichesLotService {

    int MAX_FICHIERS = 20;

    /**
     * Importe chaque fichier independamment, dans l'ordre (FicheSigneeService.importer, une
     * transaction par fichier) : l'echec d'un fichier n'annule pas les autres. Un resultat par
     * fichier, dans le meme ordre.
     *
     * @throws com.proxymed.exception.RegleGestionException si la liste est vide ou contient plus de
     *                                                      MAX_FICHIERS fichiers (rien n'est importe)
     */
    List<ResultatImportModel> importer(List<FichierAImporterModel> fichiers);
}
