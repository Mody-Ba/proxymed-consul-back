package com.proxymed.mappers;

import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.DemandeImpressionModel;
import com.proxymed.service.model.ImpressionFichesRequest;
import com.proxymed.service.model.VisiteViergeResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : ConsultationModel -> VisiteViergeResponse (identifiants et type de visite seulement)
 * et ImpressionFichesRequest -> demandes d'impression. Ne doit jamais connaitre l'entite JPA.
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

    public List<DemandeImpressionModel> toDemandes(ImpressionFichesRequest request) {
        return request.fiches().stream()
                .map(fiche -> DemandeImpressionModel.builder()
                        .patientId(fiche.patientId())
                        .typeVisite(fiche.typeVisite())
                        .build())
                .toList();
    }
}
