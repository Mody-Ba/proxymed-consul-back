package com.proxymed.service;

import com.proxymed.service.model.DemandeImpressionModel;
import com.proxymed.service.model.FichesImprimeesModel;

import java.util.List;

public interface ImpressionFichesService {

    /**
     * Cree une visite vierge par demande puis genere un seul PDF combine, une fiche par demande,
     * dans l'ordre. Tout ou rien : si un patient est introuvable ou si la generation d'un PDF
     * echoue, aucune visite n'est enregistree. Chaque fiche ne porte que l'identite du patient,
     * l'id et le type de la visite (dans son QR code).
     *
     * @throws com.proxymed.exception.RegleGestionException si la meme demande (patient et type)
     *                                                      apparait plusieurs fois
     * @throws com.proxymed.exception.ResourceNotFoundException si un patient n'existe pas
     */
    FichesImprimeesModel imprimer(List<DemandeImpressionModel> demandes);
}
