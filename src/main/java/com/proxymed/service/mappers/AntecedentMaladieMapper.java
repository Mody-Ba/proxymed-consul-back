package com.proxymed.service.mappers;

import com.proxymed.entity.AntecedentMaladie;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.MaladieChronique;
import com.proxymed.service.model.AntecedentMaladieModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper DB : convertit uniquement entre AntecedentMaladieModel et l'entite JPA AntecedentMaladie.
 * Ne doit jamais connaitre AntecedentMaladieRequest/Response.
 * La consultation et la maladie chronique rattachees sont fournies par le service
 * (charge via leurs repositories respectifs) : ce mapper ne fait aucun appel repository.
 */
@Component("dbAntecedentMaladieMapper")
public class AntecedentMaladieMapper {

    public AntecedentMaladie toEntity(AntecedentMaladieModel model, ConsultationInitiale consultation, MaladieChronique maladieChronique) {
        return AntecedentMaladie.builder()
                .consultation(consultation)
                .maladieChronique(maladieChronique)
                .precision(model.precision())
                .build();
    }

    public AntecedentMaladieModel toModel(AntecedentMaladie entity) {
        MaladieChronique maladie = entity.getMaladieChronique();
        return AntecedentMaladieModel.builder()
                .id(entity.getId())
                .maladieChroniqueId(maladie.getId())
                .maladieChroniqueLibelle(maladie.getLibelle())
                .precision(entity.getPrecision())
                .build();
    }

    public List<AntecedentMaladieModel> toModelList(List<AntecedentMaladie> entities) {
        return entities.stream().map(this::toModel).toList();
    }
}
