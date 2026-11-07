package com.proxymed.service;

import org.springframework.core.io.Resource;

import java.util.UUID;

public interface RapportFicheService {

    /**
     * Rapport PDF d'une fiche VISEE, regenere a chaque appel (rien n'est stocke) : page de garde
     * (identite du patient, type et date de la visite, senior et date du visa) puis le scan signe.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si la visite n'existe pas ou n'a aucune fiche importee
     * @throws com.proxymed.exception.ConflitEtatException si la fiche n'est pas VISEE
     */
    Resource generer(UUID consultationId);
}
