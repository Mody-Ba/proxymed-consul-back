package com.proxymed.service;

import com.proxymed.service.model.ResultatDecisionModel;

import java.util.List;
import java.util.UUID;

public interface VisaFichesLotService {

    int MAX_FICHES = 50;

    /** Longueur maximale du motif de renvoi (colonne fiche_signee.motif_renvoi). */
    int MOTIF_MAX = 1000;

    /** Longueur maximale du nom du senior enregistre avec le visa (colonne fiche_signee.nom_senior_visa). */
    int NOM_SENIOR_MAX = 200;

    /**
     * Vise chaque fiche independamment, dans l'ordre (VisaFicheSigneeService.viser, une transaction
     * par fiche) : l'echec d'une fiche n'annule pas les autres. Un resultat par visite, dans le meme ordre.
     *
     * @throws com.proxymed.exception.RegleGestionException si la liste est vide, depasse MAX_FICHES,
     *         contient un doublon ou un identifiant vide, ou si le nom du senior est vide ou trop long
     *         (rien n'est vise)
     */
    List<ResultatDecisionModel> viser(List<UUID> visiteIds, UUID seniorId, String nomSenior);

    /**
     * Renvoie chaque fiche independamment, avec le meme motif, comme viser.
     *
     * @throws com.proxymed.exception.RegleGestionException si la liste est invalide (comme viser) ou
     *         si le motif est vide ou depasse MOTIF_MAX caracteres (rien n'est renvoye)
     */
    List<ResultatDecisionModel> renvoyer(List<UUID> visiteIds, String motif);
}
