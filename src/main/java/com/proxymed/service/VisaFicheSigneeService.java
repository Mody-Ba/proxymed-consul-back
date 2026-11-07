package com.proxymed.service;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.service.model.FicheAViserModel;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Visa et renvoi des fiches scannees par un senior, une fiche a la fois (une transaction par appel).
 * Pour l'instant, tout senior voit et peut viser toutes les fiches.
 */
public interface VisaFicheSigneeService {

    int TAILLE_PAGE_MAX = 100;

    /**
     * Fiches du statut donne (EN_ATTENTE si null), de la plus anciennement importee a la plus recente.
     *
     * @throws com.proxymed.exception.RegleGestionException si page < 0 ou taille hors [1, TAILLE_PAGE_MAX]
     */
    Page<FicheAViserModel> findByStatut(StatutFicheSignee statut, int page, int taille);

    /**
     * Vise la fiche EN_ATTENTE de la visite : statut VISEE, senior, nom et date du visa.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si la visite n'existe pas ou n'a aucune fiche
     * @throws com.proxymed.exception.ConflitEtatException si la fiche n'est pas EN_ATTENTE, ou vient
     *                                                     d'etre modifiee par une autre requete (verrou optimiste)
     */
    void viser(UUID visiteId, UUID seniorId, String nomSenior);

    /**
     * Renvoie la fiche EN_ATTENTE de la visite avec son motif : statut RENVOYEE, l'admin pourra la reimporter.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si la visite n'existe pas ou n'a aucune fiche
     * @throws com.proxymed.exception.ConflitEtatException si la fiche n'est pas EN_ATTENTE, ou vient
     *                                                     d'etre modifiee par une autre requete (verrou optimiste)
     */
    void renvoyer(UUID visiteId, String motif);
}
