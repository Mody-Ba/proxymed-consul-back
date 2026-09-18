package com.proxymed.mappers;

import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.MedecinRequest;
import com.proxymed.service.model.MedecinResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement MedecinRequest <-> MedecinModel et MedecinModel -> MedecinResponse.
 * Ne doit jamais connaitre l'entite JPA Medecin ni Structure.
 */
@Component("apiMedecinMapper")
public class MedecinMapper {

    public MedecinModel toModel(MedecinRequest req) {
        return MedecinModel.builder()
                .nom(req.nom())
                .prenom(req.prenom())
                .numeroOrdre(req.numeroOrdre())
                .role(req.role())
                .structureRattachementId(req.structureRattachementId())
                .build();
    }

    public MedecinResponse toResponse(MedecinModel model) {
        return MedecinResponse.builder()
                .id(model.id())
                .nom(model.nom())
                .prenom(model.prenom())
                .numeroOrdre(model.numeroOrdre())
                .role(model.role())
                .structureRattachementId(model.structureRattachementId())
                .structureRattachementLibelle(model.structureRattachementLibelle())
                .build();
    }
}
