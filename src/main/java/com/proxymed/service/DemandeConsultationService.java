package com.proxymed.service;

import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.service.model.DemandeConsultationModel;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Les droits par role sont verifies dans le controller : ce service recoit seulement les
 * identifiants de comptes deja lus dans le badge. Les listes sont triees de la plus recente
 * a la plus ancienne demande.
 */
public interface DemandeConsultationService {

    /**
     * Cree une demande EN_ATTENTE pour model.demandeurId().
     */
    DemandeConsultationModel creer(DemandeConsultationModel model);

    /**
     * @throws com.proxymed.exception.RegleGestionException si page < 0 ou taille hors [1, 100]
     */
    Page<DemandeConsultationModel> findByDemandeur(UUID demandeurId, int page, int taille);

    /**
     * @param statut filtre optionnel (null : toutes les demandes)
     * @throws com.proxymed.exception.RegleGestionException si page < 0 ou taille hors [1, 100]
     */
    Page<DemandeConsultationModel> findAll(StatutDemandeConsultation statut, int page, int taille);

    /**
     * Demandes ACCEPTEES assignees a ce medecin junior.
     *
     * @throws com.proxymed.exception.RegleGestionException si page < 0 ou taille hors [1, 100]
     */
    Page<DemandeConsultationModel> findAssigneesAuJunior(UUID juniorId, int page, int taille);

    /**
     * @throws com.proxymed.exception.ResourceNotFoundException si la demande n'existe pas
     */
    DemandeConsultationModel findById(UUID id);

    /**
     * Passe la demande a ACCEPTEE et l'assigne au medecin junior.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException   si la demande n'existe pas
     * @throws com.proxymed.exception.DemandeDejaTraiteeException si elle n'est plus EN_ATTENTE
     */
    DemandeConsultationModel accepter(UUID id, UUID medecinSeniorId, UUID juniorId);

    /**
     * Passe la demande a REFUSEE avec son motif.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException   si la demande n'existe pas
     * @throws com.proxymed.exception.DemandeDejaTraiteeException si elle n'est plus EN_ATTENTE
     */
    DemandeConsultationModel refuser(UUID id, UUID medecinSeniorId, String motifRefus);

    /**
     * Garde le lien entre une demande ACCEPTEE et la consultation initiale commencee a partir
     * d'elle. Une demande n'est liee qu'une fois.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException            si la demande n'existe pas
     * @throws com.proxymed.exception.ConsultationNonCommencableException si elle n'est pas ACCEPTEE,
     *                                                                     ou deja liee a une consultation
     */
    DemandeConsultationModel lierConsultation(UUID id, UUID consultationId);
}
