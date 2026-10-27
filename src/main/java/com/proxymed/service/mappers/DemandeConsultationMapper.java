package com.proxymed.service.mappers;

import com.proxymed.entity.DemandeConsultation;
import com.proxymed.service.model.DemandeConsultationModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre DemandeConsultationModel et l'entite JPA DemandeConsultation.
 * Ne doit jamais connaitre DemandeConsultationRequest/Response.
 */
@Component("dbDemandeConsultationMapper")
public class DemandeConsultationMapper {

    public DemandeConsultation toEntity(DemandeConsultationModel model) {
        return DemandeConsultation.builder()
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
                .build();
    }

    public DemandeConsultationModel toModel(DemandeConsultation entity) {
        return DemandeConsultationModel.builder()
                .id(entity.getId())
                .demandeurId(entity.getDemandeurId())
                .nomPatient(entity.getNomPatient())
                .pourProche(entity.isPourProche())
                .motif(entity.getMotif())
                .statut(entity.getStatut())
                .medecinSeniorId(entity.getMedecinSeniorId())
                .juniorId(entity.getJuniorId())
                .motifRefus(entity.getMotifRefus())
                .dateCreation(entity.getDateCreation())
                .dateDecision(entity.getDateDecision())
                .build();
    }
}
