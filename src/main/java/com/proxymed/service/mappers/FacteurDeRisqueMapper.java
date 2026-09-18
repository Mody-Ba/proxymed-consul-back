package com.proxymed.service.mappers;

import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.service.model.FacteurDeRisqueModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("dbFacteurDeRisqueMapper")
public class FacteurDeRisqueMapper {

    public FacteurDeRisqueModel toModel(FacteurDeRisque entity) {
        return FacteurDeRisqueModel.builder()
                .id(entity.getId())
                .libelle(entity.getLibelle())
                .actif(entity.isActif())
                .build();
    }

    public List<FacteurDeRisqueModel> toModelList(List<FacteurDeRisque> entities) {
        return entities.stream().map(this::toModel).toList();
    }
}
