package com.proxymed.mappers;

import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.VisiteViergeResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper API : ConsultationModel -> VisiteViergeResponse. Ne recopie que les identifiants et le
 * type de visite. Ne doit jamais connaitre l'entite JPA.
 */
@Component("apiVisiteMapper")
public class VisiteMapper {

    public VisiteViergeResponse toResponse(ConsultationModel model) {
        return VisiteViergeResponse.builder()
                .id(model.id())
                .patientId(model.patient().id())
                .typeVisite(model.typeVisite())
                .build();
    }
}
