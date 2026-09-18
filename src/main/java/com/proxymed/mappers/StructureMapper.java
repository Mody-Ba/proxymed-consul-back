package com.proxymed.mappers;

import com.proxymed.service.model.StructureModel;
import com.proxymed.service.model.StructureResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement entre StructureModel et le contrat API (StructureResponse).
 * Ne doit jamais connaitre l'entite JPA Structure. Pas de StructureRequest : ce referentiel
 * n'a pas d'endpoint de creation dans le cahier des charges.
 */
@Component("apiStructureMapper")
public class StructureMapper {

    public StructureResponse toResponse(StructureModel model) {
        return StructureResponse.builder()
                .id(model.id())
                .libelle(model.libelle())
                .actif(model.actif())
                .build();
    }

    public List<StructureResponse> toResponseList(List<StructureModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
