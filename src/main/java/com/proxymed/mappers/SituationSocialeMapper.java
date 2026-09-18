package com.proxymed.mappers;

import com.proxymed.service.model.SituationSocialeModel;
import com.proxymed.service.model.SituationSocialeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("apiSituationSocialeMapper")
public class SituationSocialeMapper {

    public SituationSocialeResponse toResponse(SituationSocialeModel model) {
        return SituationSocialeResponse.builder()
                .id(model.id())
                .libelle(model.libelle())
                .actif(model.actif())
                .build();
    }

    public List<SituationSocialeResponse> toResponseList(List<SituationSocialeModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
