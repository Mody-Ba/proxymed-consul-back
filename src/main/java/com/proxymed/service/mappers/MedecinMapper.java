package com.proxymed.service.mappers;

import com.proxymed.entity.Medecin;
import com.proxymed.entity.Structure;
import com.proxymed.repository.StructureRepository;
import com.proxymed.service.model.MedecinModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre MedecinModel et l'entite JPA Medecin.
 * Ne doit jamais connaitre MedecinRequest/MedecinResponse.
 * Resout lui-meme la reference JPA vers la structure de rattachement (getReferenceById,
 * simple traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi
 * jamais l'entite Structure, seulement son id.
 */
@Component("dbMedecinMapper")
@RequiredArgsConstructor
public class MedecinMapper {

    private final StructureRepository structureRepository;

    public Medecin toEntity(MedecinModel model) {
        return Medecin.builder()
                .nom(model.nom())
                .prenom(model.prenom())
                .numeroOrdre(model.numeroOrdre())
                .role(model.role())
                .structureRattachement(model.structureRattachementId() != null
                        ? structureRepository.getReferenceById(model.structureRattachementId()) : null)
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
