package com.proxymed.service.mappers;

import com.proxymed.entity.FicheSignee;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.service.model.FicheSigneeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre FicheSigneeModel et l'entite JPA FicheSignee.
 * Ne doit jamais connaitre le fichier multipart ni FicheSigneeResponse.
 * Resout lui-meme la reference JPA vers la consultation parente (getReferenceById, simple
 * traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi jamais
 * l'entite ConsultationInitiale, seulement l'id.
 */
@Component("dbFicheSigneeMapper")
@RequiredArgsConstructor
public class FicheSigneeMapper {

    private final ConsultationRepository consultationRepository;

    public FicheSignee toEntity(FicheSigneeModel model, UUID consultationId) {
        return FicheSignee.builder()
                .consultation(consultationRepository.getReferenceById(consultationId))
                .nomFichier(model.nomFichier())
                .typeContenu(model.typeContenu())
                .tailleOctets(model.tailleOctets())
                .dateImport(model.dateImport())
                .contenu(model.contenu())
                .build();
    }

    public FicheSigneeModel toModel(FicheSignee entity) {
        return FicheSigneeModel.builder()
                .id(entity.getId())
                .consultationId(entity.getConsultation().getId())
                .nomFichier(entity.getNomFichier())
                .typeContenu(entity.getTypeContenu())
                .tailleOctets(entity.getTailleOctets())
                .dateImport(entity.getDateImport())
                .contenu(entity.getContenu())
                .build();
    }
}
