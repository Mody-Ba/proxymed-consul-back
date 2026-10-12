package com.proxymed.mappers;

import com.proxymed.service.model.FicheSigneeModel;
import com.proxymed.service.model.FicheSigneeResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Mapper API : convertit uniquement le fichier multipart recu -> FicheSigneeModel
 * et FicheSigneeModel -> FicheSigneeResponse. Ne doit jamais connaitre l'entite JPA.
 */
@Component("apiFicheSigneeMapper")
public class FicheSigneeMapper {

    public FicheSigneeModel toModel(MultipartFile fichier) {
        try {
            return FicheSigneeModel.builder()
                    .nomFichier(fichier.getOriginalFilename())
                    .typeContenu(fichier.getContentType())
                    .tailleOctets(fichier.getSize())
                    .contenu(fichier.getBytes())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier importe impossible", e);
        }
    }

    public FicheSigneeResponse toResponse(FicheSigneeModel model) {
        return FicheSigneeResponse.builder()
                .id(model.id())
                .nomFichier(model.nomFichier())
                .tailleOctets(model.tailleOctets())
                .dateImport(model.dateImport())
                .build();
    }
}
