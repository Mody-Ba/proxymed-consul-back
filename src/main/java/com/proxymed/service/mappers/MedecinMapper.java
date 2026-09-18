package com.proxymed.service.mappers;

import com.proxymed.entity.Medecin;
import com.proxymed.entity.Structure;
import com.proxymed.service.model.MedecinModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre MedecinModel et l'entite JPA Medecin.
 * Ne doit jamais connaitre MedecinRequest/MedecinResponse.
 * La structure rattachee est fournie par le service (qui l'a chargee via son repository) :
 * ce mapper ne fait aucun appel repository lui-meme.
 */
@Component("dbMedecinMapper")
public class MedecinMapper {

    public Medecin toEntity(MedecinModel model, Structure structure) {
        return Medecin.builder()
                .nom(model.nom())
                .prenom(model.prenom())
                .numeroOrdre(model.numeroOrdre())
                .role(model.role())
                .structureRattachement(structure)
                .build();
    }

    public MedecinModel toModel(Medecin entity) {
        Structure structure = entity.getStructureRattachement();
        return MedecinModel.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .numeroOrdre(entity.getNumeroOrdre())
                .role(entity.getRole())
                .structureRattachementId(structure != null ? structure.getId() : null)
                .structureRattachementLibelle(structure != null ? structure.getLibelle() : null)
                .build();
    }
}
