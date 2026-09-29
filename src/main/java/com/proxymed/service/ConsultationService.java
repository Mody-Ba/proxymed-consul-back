package com.proxymed.service;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.service.model.ConsultationModel;

import java.util.List;
import java.util.UUID;


public interface ConsultationService {

    ConsultationModel findById(UUID id);


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

    /**
     * PDF de la Fiche 1 (section 5.2), genere a la volee a partir de l'etat courant de la fiche,
     * quel que soit son statut.
     */
    byte[] genererPdf(UUID id);
}
