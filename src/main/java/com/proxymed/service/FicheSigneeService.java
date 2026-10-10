package com.proxymed.service;

import com.proxymed.service.model.FicheSigneeModel;

import java.util.UUID;

public interface FicheSigneeService {

    /**
     * Importe la fiche signee (PDF) et la rattache a la consultation.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si la consultation n'existe pas
     * @throws com.proxymed.exception.RegleGestionException si le fichier n'est pas un PDF
     */
    FicheSigneeModel importer(UUID consultationId, FicheSigneeModel model);

    /**
     * Renvoie la fiche signee la plus recente (par date d'import) de la consultation.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si la consultation n'existe pas
     *                                                          ou n'a aucune fiche importee
     */
    FicheSigneeModel telecharger(UUID consultationId);
}
