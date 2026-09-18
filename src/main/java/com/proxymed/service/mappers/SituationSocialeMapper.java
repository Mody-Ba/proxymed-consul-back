package com.proxymed.service.mappers;

import com.proxymed.entity.SituationSociale;
import com.proxymed.service.model.SituationSocialeModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("dbSituationSocialeMapper")
public class SituationSocialeMapper {

    public SituationSocialeModel toModel(SituationSociale entity) {
        return SituationSocialeModel.builder()
                .id(entity.getId())
                .libelle(entity.getLibelle())
                .actif(entity.isActif())
                .build();
    }

    public List<SituationSocialeModel> toModelList(List<SituationSociale> entities) {
        return entities.stream().map(this::toModel).toList();
    }
}
