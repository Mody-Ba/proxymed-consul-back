package com.proxymed.mappers;

import com.proxymed.service.model.CommencementConsultationModel;
import com.proxymed.service.model.CommencementConsultationRequest;
import com.proxymed.service.model.CommencementConsultationResponse;
import com.proxymed.service.model.DemandeConsultationModel;
import com.proxymed.service.model.DemandeConsultationRequest;
import com.proxymed.service.model.DemandeConsultationResponse;
import com.proxymed.service.model.PageResponse;
import com.proxymed.service.model.PatientModel;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper API : convertit uniquement DemandeConsultationRequest -> DemandeConsultationModel,
 * DemandeConsultationModel -> DemandeConsultationResponse, et la requete / reponse du commencement
 * de la consultation initiale. Ne doit jamais connaitre l'entite JPA.
 */
@Component("apiDemandeConsultationMapper")
public class DemandeConsultationMapper {

    /**
     * @param demandeurId identifiant du compte lu dans le badge, jamais dans le corps de la requete
     */
    public DemandeConsultationModel toModel(DemandeConsultationRequest req, UUID demandeurId) {
        return DemandeConsultationModel.builder()
                .demandeurId(demandeurId)
                .nomPatient(req.nomPatient())
                .pourProche(req.pourProche())
                .motif(req.motif())
                .build();
    }

    public DemandeConsultationResponse toResponse(DemandeConsultationModel model) {
        return DemandeConsultationResponse.builder()
                .id(model.id())
                .demandeurId(model.demandeurId())
                .nomPatient(model.nomPatient())
                .pourProche(model.pourProche())
                .motif(model.motif())
                .statut(model.statut())
                .medecinSeniorId(model.medecinSeniorId())
                .juniorId(model.juniorId())
                .motifRefus(model.motifRefus())
                .dateCreation(model.dateCreation())
                .dateDecision(model.dateDecision())
                .consultationId(model.consultationId())
                .build();
    }

    /**
     * Champs du patient saisis par le junior. Le nom vient de la demande, il est pose par le service.
     */
    public PatientModel toPatientModel(CommencementConsultationRequest req) {
        return PatientModel.builder()
                .numeroDossierProxymed(req.numeroDossierProxymed().strip())
                .dateNaissance(req.dateNaissance())
                .sexe(req.sexe())
                .build();
    }

    public CommencementConsultationResponse toResponse(CommencementConsultationModel model) {
        return CommencementConsultationResponse.builder()
                .demandeId(model.demandeId())
                .patientId(model.patientId())
                .consultationId(model.consultationId())
                .statutConsultation(model.statutConsultation())
                .build();
    }

    public PageResponse<DemandeConsultationResponse> toPageResponse(Page<DemandeConsultationModel> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
