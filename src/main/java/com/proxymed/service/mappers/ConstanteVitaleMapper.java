package com.proxymed.service.mappers;

import com.proxymed.entity.ConstanteVitale;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.service.model.ConstanteVitaleModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper DB : convertit uniquement entre ConstanteVitaleModel et l'entite JPA ConstanteVitale.
 * Ne doit jamais connaitre ConstanteVitaleRequest/ConstanteVitaleResponse.
 */
@Component("dbConstanteVitaleMapper")
public class ConstanteVitaleMapper {

    public ConstanteVitale toEntity(ConstanteVitaleModel model, ConsultationInitiale consultation) {
        return ConstanteVitale.builder()
                .consultation(consultation)
                .type(model.type())
                .valeur(model.valeur())
                .heure(model.heure())
                .estNormal(model.estNormal())
                .estAlerte(model.estAlerte())
                .commentaire(model.commentaire())
                .build();
    }

    /**
     * Applique le modele sur une entite managee existante (mutation en place).
     */
    public void applyToEntity(ConstanteVitale entity, ConstanteVitaleModel model) {
        entity.setType(model.type());
        entity.setValeur(model.valeur());
        entity.setHeure(model.heure());
        entity.setEstNormal(model.estNormal());
        entity.setEstAlerte(model.estAlerte());
        entity.setCommentaire(model.commentaire());
    }

    public ConstanteVitaleModel toModel(ConstanteVitale entity) {
        return ConstanteVitaleModel.builder()
                .id(entity.getId())
                .type(entity.getType())
                .valeur(entity.getValeur())
                .heure(entity.getHeure())
                .estNormal(entity.getEstNormal())
                .estAlerte(entity.getEstAlerte())
                .commentaire(entity.getCommentaire())
                .build();
    }

    public List<ConstanteVitaleModel> toModelList(List<ConstanteVitale> entities) {
        return entities.stream().map(this::toModel).toList();
    }
}
