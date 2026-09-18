package com.proxymed.mappers;

import com.proxymed.service.model.ExamenParAppareilModel;
import com.proxymed.service.model.ExamenParAppareilRequest;
import com.proxymed.service.model.ExamenParAppareilResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement ExamenParAppareilRequest <-> ExamenParAppareilModel
 * et ExamenParAppareilModel -> ExamenParAppareilResponse. Ne doit jamais connaitre l'entite JPA.
 */
@Component("apiExamenParAppareilMapper")
public class ExamenParAppareilMapper {

    public ExamenParAppareilModel toModel(ExamenParAppareilRequest req) {
        return ExamenParAppareilModel.builder()
                .etatGeneral(req.etatGeneral())
                .cardioVasculaire(req.cardioVasculaire())
                .respiratoire(req.respiratoire())
                .digestif(req.digestif())
                .neurologique(req.neurologique())
                .locomoteurCutaneAutre(req.locomoteurCutaneAutre())
                .build();
    }

    public ExamenParAppareilResponse toResponse(ExamenParAppareilModel model) {
        if (model == null) {
            return null;
        }
        return ExamenParAppareilResponse.builder()
                .id(model.id())
                .etatGeneral(model.etatGeneral())
                .cardioVasculaire(model.cardioVasculaire())
                .respiratoire(model.respiratoire())
                .digestif(model.digestif())
                .neurologique(model.neurologique())
                .locomoteurCutaneAutre(model.locomoteurCutaneAutre())
                .build();
    }
}
