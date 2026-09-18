package com.proxymed.mappers;

import com.proxymed.service.model.AntecedentMaladieModel;
import com.proxymed.service.model.AntecedentMaladieRequest;
import com.proxymed.service.model.AntecedentMaladieResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement AntecedentMaladieRequest <-> AntecedentMaladieModel
 * et AntecedentMaladieModel -> AntecedentMaladieResponse. Ne doit jamais connaitre l'entite JPA.
 */
@Component("apiAntecedentMaladieMapper")
public class AntecedentMaladieMapper {

    public AntecedentMaladieModel toModel(AntecedentMaladieRequest req) {
        return AntecedentMaladieModel.builder()
                .maladieChroniqueId(req.maladieChroniqueId())
                .precision(req.precision())
                .build();
    }

    public AntecedentMaladieResponse toResponse(AntecedentMaladieModel model) {
        return AntecedentMaladieResponse.builder()
                .id(model.id())
                .maladieChroniqueId(model.maladieChroniqueId())
                .maladieChroniqueLibelle(model.maladieChroniqueLibelle())
                .precision(model.precision())
                .build();
    }

    public List<AntecedentMaladieResponse> toResponseList(List<AntecedentMaladieModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
