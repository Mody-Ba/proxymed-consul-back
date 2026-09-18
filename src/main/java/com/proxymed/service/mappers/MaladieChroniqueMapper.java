package com.proxymed.service.mappers;

import com.proxymed.entity.MaladieChronique;
import com.proxymed.service.model.MaladieChroniqueModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("dbMaladieChroniqueMapper")
public class MaladieChroniqueMapper {

    public MaladieChroniqueModel toModel(MaladieChronique entity) {
        return MaladieChroniqueModel.builder()
                .id(entity.getId())
                .libelle(entity.getLibelle())
                .actif(entity.isActif())
                .build();
    }

    public List<MaladieChroniqueModel> toModelList(List<MaladieChronique> entities) {
        return entities.stream().map(this::toModel).toList();
    }
}
