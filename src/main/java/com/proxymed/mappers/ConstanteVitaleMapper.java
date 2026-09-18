package com.proxymed.mappers;

import com.proxymed.service.model.ConstanteVitaleModel;
import com.proxymed.service.model.ConstanteVitaleRequest;
import com.proxymed.service.model.ConstanteVitaleResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement ConstanteVitaleRequest <-> ConstanteVitaleModel
 * et ConstanteVitaleModel -> ConstanteVitaleResponse. Ne doit jamais connaitre l'entite JPA.
 */
@Component("apiConstanteVitaleMapper")
public class ConstanteVitaleMapper {

    public ConstanteVitaleModel toModel(ConstanteVitaleRequest req) {
        return ConstanteVitaleModel.builder()
                .type(req.type())
                .valeur(req.valeur())
                .heure(req.heure())
                .commentaire(req.commentaire())
                .build();
    }

    public ConstanteVitaleResponse toResponse(ConstanteVitaleModel model) {
        return ConstanteVitaleResponse.builder()
                .id(model.id())
                .type(model.type())
                .valeur(model.valeur())
                .heure(model.heure())
                .estNormal(model.estNormal())
                .estAlerte(model.estAlerte())
                .commentaire(model.commentaire())
                .build();
    }

    public List<ConstanteVitaleResponse> toResponseList(List<ConstanteVitaleModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
