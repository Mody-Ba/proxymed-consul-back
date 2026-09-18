package com.proxymed.mappers;

import com.proxymed.service.model.MaladieChroniqueModel;
import com.proxymed.service.model.MaladieChroniqueResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("apiMaladieChroniqueMapper")
public class MaladieChroniqueMapper {

    public MaladieChroniqueResponse toResponse(MaladieChroniqueModel model) {
        return MaladieChroniqueResponse.builder()
                .id(model.id())
                .libelle(model.libelle())
                .actif(model.actif())
                .build();
    }

    public List<MaladieChroniqueResponse> toResponseList(List<MaladieChroniqueModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
