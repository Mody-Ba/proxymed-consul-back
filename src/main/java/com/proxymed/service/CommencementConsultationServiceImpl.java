package com.proxymed.service;

import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.exception.ConsultationNonCommencableException;
import com.proxymed.service.model.CommencementConsultationModel;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.DemandeConsultationModel;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.PatientModel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestre trois services dans une seule transaction (chacun garde l'acces exclusif a sa propre
 * entite). Ordre des controles : demande introuvable (404), puis non ACCEPTEE (409 : une demande
 * en attente ou refusee n'a pas de junior assigne), puis autre junior (403), puis deja commencee
 * (409). Aucune donnee d'identite ni medicale n'est journalisee.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommencementConsultationServiceImpl implements CommencementConsultationService {

    private final DemandeConsultationService demandeConsultationService;
    private final PatientService patientService;
    private final ConsultationService consultationService;

    @Override
    public CommencementConsultationModel commencer(UUID demandeId, UUID juniorConnecteId,
                                                   PatientModel patientSaisi, Long medecinSeniorId) {
        DemandeConsultationModel demande = demandeConsultationService.findById(demandeId);
        if (demande.statut() != StatutDemandeConsultation.ACCEPTEE) {
            throw ConsultationNonCommencableException.nonAcceptee(demandeId);
        }
        if (!juniorConnecteId.equals(demande.juniorId())) {
            throw new AccessDeniedException("Demande assignee a un autre medecin junior");
        }
        if (demande.consultationId() != null) {
            throw ConsultationNonCommencableException.dejaCommencee(demandeId);
        }

        PatientModel patient = patientService.create(patientSaisi.toBuilder()
                .id(null)
                .nomComplet(demande.nomPatient())
                .build());

        // Origine de la demande laissee vide : elle n'est pas connue avec certitude, le junior la
        // renseigne ensuite dans la fiche. Date et heure : celles du commencement (defaut du brouillon).
        ConsultationModel consultation = consultationService.creerBrouillon(ConsultationModel.builder()
                .patient(PatientModel.builder().id(patient.id()).build())
                .medecinSenior(MedecinModel.builder().id(medecinSeniorId).build())
                .motifPrincipalConsultation(demande.motif())
                .build());

        demandeConsultationService.lierConsultation(demandeId, consultation.id());

        return CommencementConsultationModel.builder()
                .demandeId(demandeId)
                .patientId(patient.id())
                .consultationId(consultation.id())
                .statutConsultation(consultation.statut())
                .build();
    }
}
