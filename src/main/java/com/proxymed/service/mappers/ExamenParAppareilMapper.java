package com.proxymed.service.mappers;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.ExamenParAppareil;
import com.proxymed.service.model.ExamenParAppareilModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre ExamenParAppareilModel et l'entite JPA ExamenParAppareil.
 * Ne doit jamais connaitre ExamenParAppareilRequest/Response.
 */
@Component("dbExamenParAppareilMapper")
public class ExamenParAppareilMapper {

    public ExamenParAppareil toEntity(ExamenParAppareilModel model, ConsultationInitiale consultation) {
        return ExamenParAppareil.builder()
                .consultation(consultation)
                .etatGeneral(model.etatGeneral())
                .cardioVasculaire(model.cardioVasculaire())
                .respiratoire(model.respiratoire())
                .digestif(model.digestif())
                .neurologique(model.neurologique())
                .locomoteurCutaneAutre(model.locomoteurCutaneAutre())
                .build();
    }

    /**
     * Applique le modele sur une entite managee existante (mutation en place).
     */
    public void applyToEntity(ExamenParAppareil entity, ExamenParAppareilModel model) {
        entity.setEtatGeneral(model.etatGeneral());
        entity.setCardioVasculaire(model.cardioVasculaire());
        entity.setRespiratoire(model.respiratoire());
        entity.setDigestif(model.digestif());
        entity.setNeurologique(model.neurologique());
        entity.setLocomoteurCutaneAutre(model.locomoteurCutaneAutre());
    }

    public ExamenParAppareilModel toModel(ExamenParAppareil entity) {
        if (entity == null) {
            return null;
        }
        return ExamenParAppareilModel.builder()
                .id(entity.getId())
                .etatGeneral(entity.getEtatGeneral())
                .cardioVasculaire(entity.getCardioVasculaire())
                .respiratoire(entity.getRespiratoire())
                .digestif(entity.getDigestif())
                .neurologique(entity.getNeurologique())
                .locomoteurCutaneAutre(entity.getLocomoteurCutaneAutre())
                .build();
    }
}
