package com.proxymed.mappers;

import com.proxymed.service.model.FacteurDeRisqueModel;
import com.proxymed.service.model.FacteurDeRisqueResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("apiFacteurDeRisqueMapper")
public class FacteurDeRisqueMapper {

    public FacteurDeRisqueResponse toResponse(FacteurDeRisqueModel model) {
        return FacteurDeRisqueResponse.builder()
                .id(model.id())
                .libelle(model.libelle())
                .actif(model.actif())
                .build();
    }

    public List<FacteurDeRisqueResponse> toResponseList(List<FacteurDeRisqueModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
