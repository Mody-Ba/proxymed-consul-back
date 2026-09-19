package com.proxymed.service;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.service.model.ConsultationModel;

import java.util.List;
import java.util.UUID;

/**
 * Travaille avec ConsultationModel (couche interne neutre) comme type de travail :
 * l'entite JPA n'est jamais exposee en dehors de ce service (frontiere repository <-> mapper) ;
 * les autres services qui ont besoin de verifier ou reagir a l'etat d'une consultation passent
 * par verifierModifiable(UUID) ou findById(UUID), qui ne renvoient que des Model.
 */
public interface ConsultationService {

    ConsultationModel findById(UUID id);

    /**
     * A utiliser par les services des sous-ressources (constantes vitales, antecedents, etc.)
     * qui doivent s'assurer que la fiche parente existe et n'est pas deja signee avant
     * d'y rattacher une sous-ressource. Ne renvoie jamais l'entite : ces services chargent
     * eux-memes la reference JPA dont ils ont besoin via leur propre repository.
     */
    void verifierModifiable(UUID id);

    List<ConsultationModel> rechercher(
            String nomPatient,
            String numeroDossierProxymed,
            String numeroDmi,
            Long medecinSeniorId,
            Long medecinJuniorAffecteId,
            StatutConsultation statut,
            DecisionEligibilite decisionEligibilite,
            Long structureId
    );

    List<ConsultationModel> fichesEnAttenteValidationDass();

    ConsultationModel creerBrouillon(ConsultationModel intention);

    ConsultationModel mettreAJour(UUID id, ConsultationModel intention);

    ConsultationModel valider(UUID id);

    ConsultationModel signer(UUID id);
}
