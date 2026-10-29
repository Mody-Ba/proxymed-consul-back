package com.proxymed.service;

import com.proxymed.service.model.CommencementConsultationModel;
import com.proxymed.service.model.PatientModel;

import java.util.UUID;

/**
 * Relie une demande de consultation ACCEPTEE a la Fiche 1 : le medecin junior assigne cree le
 * patient et le brouillon de consultation initiale. Le role est verifie dans le controller ;
 * ce service recoit l'identifiant du compte deja lu dans le badge.
 */
public interface CommencementConsultationService {

    /**
     * En une seule transaction : cree le patient (nom repris de la demande), le brouillon de
     * consultation initiale (motif repris de la demande) et garde le lien dans la demande.
     * Si une etape echoue, rien n'est enregistre.
     *
     * @param juniorConnecteId compte du badge ; doit etre le junior assigne a la demande
     * @param patientSaisi     champs obligatoires du patient que la demande ne fournit pas
     * @throws com.proxymed.exception.ResourceNotFoundException            demande ou medecin introuvable
     * @throws com.proxymed.exception.ConsultationNonCommencableException demande non ACCEPTEE ou deja commencee
     * @throws org.springframework.security.access.AccessDeniedException   demande assignee a un autre junior
     * @throws com.proxymed.exception.PatientExistantException             numero de dossier deja utilise
     * @throws com.proxymed.exception.RegleGestionException                medecin qui n'est pas SENIOR
     */
    CommencementConsultationModel commencer(UUID demandeId, UUID juniorConnecteId,
                                            PatientModel patientSaisi, Long medecinSeniorId);
}
