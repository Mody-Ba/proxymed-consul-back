package com.proxymed.service.mappers;

import com.proxymed.entity.Structure;
import com.proxymed.service.model.StructureModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper DB : convertit uniquement entre StructureModel et l'entite JPA Structure.
 * Ne doit jamais connaitre StructureRequest/StructureResponse.
 */
@Component("dbStructureMapper")
public class StructureMapper {

    public StructureModel toModel(Structure entity) {
        return StructureModel.builder()
                .id(entity.getId())
                .libelle(entity.getLibelle())
                .actif(entity.isActif())
                .build();
    }

    public List<StructureModel> toModelList(List<Structure> entities) {
        return entities.stream().map(this::toModel).toList();
    }
}
