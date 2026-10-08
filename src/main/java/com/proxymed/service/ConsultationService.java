package com.proxymed.service;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.service.model.ConsultationModel;

import org.springframework.core.io.Resource;

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

    /**
     * Fait passer la fiche de BROUILLON a VALIDEE. Aucun controle de completude n'est fait a
     * cette etape : c'est voulu, une fiche VALIDEE reste modifiable (examen par appareil,
     * constantes vitales, etc. peuvent encore etre ajoutes). Les champs obligatoires ne sont
     * exiges qu'a la signature, cf. {@link #signer(UUID)}.
     *
     * @throws com.proxymed.exception.ConflitEtatException si la fiche n'est pas en BROUILLON
     */
    ConsultationModel valider(UUID id);

    /**
     * Signature par le medecin senior : fait passer la fiche de VALIDEE a SIGNEE, apres quoi
     * elle n'est plus modifiable. C'est ici, et seulement ici, que la completude est verifiee
     * (date/heure, origine et motif, evaluation sociale, resume CIM-10, decision
     * d'eligibilite, examen par appareil et au moins une constante vitale).
     *
     * @throws com.proxymed.exception.ConflitEtatException si la fiche n'est pas VALIDEE
     * @throws com.proxymed.exception.RegleGestionException si des champs obligatoires manquent
     */
    ConsultationModel signer(UUID id);

    /**
     * PDF de la Fiche 1 (section 5.2), genere a la volee a partir de l'etat courant de la fiche,
     * quel que soit son statut. La Resource renvoyee est a usage unique (fichier temporaire
     * supprime apres lecture).
     */
    Resource genererPdf(UUID id);
}
