package com.proxymed.mappers;

import com.proxymed.exception.ErrorResponse;
import com.proxymed.exception.GlobalExceptionHandler;
import com.proxymed.service.model.FicheSigneeModel;
import com.proxymed.service.model.FichierAImporterModel;
import com.proxymed.service.model.ImportLotResponse;
import com.proxymed.service.model.ResultatImportModel;
import com.proxymed.service.model.ResultatImportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Mapper API de l'import en lot : fichiers multipart -> FichierAImporterModel (lecture differee),
 * et resultats -> ImportLotResponse. Le code, l'erreur et le message d'un fichier refuse viennent de
 * GlobalExceptionHandler : exactement ceux qu'aurait une requete d'un seul fichier.
 */
@Component("apiImportLotMapper")
@RequiredArgsConstructor
public class ImportLotMapper {

    private final GlobalExceptionHandler globalExceptionHandler;

    public List<FichierAImporterModel> toModels(List<MultipartFile> fichiers) {
        return fichiers.stream().map(fichier -> FichierAImporterModel.builder()
                .nomFichier(fichier.getOriginalFilename())
                .typeContenu(fichier.getContentType())
                .tailleOctets(fichier.getSize())
                .contenu(() -> lire(fichier))
                .build()).toList();
    }

    public ImportLotResponse toResponse(List<ResultatImportModel> resultats) {
        List<ResultatImportResponse> lignes = resultats.stream().map(this::toResponse).toList();
        int importees = (int) resultats.stream().filter(ResultatImportModel::reussi).count();
        return ImportLotResponse.builder()
                .nombreFichiers(lignes.size())
                .nombreImportees(importees)
                .nombreErreurs(lignes.size() - importees)
                .resultats(lignes)
                .build();
    }

    private ResultatImportResponse toResponse(ResultatImportModel resultat) {
        ResultatImportResponse.ResultatImportResponseBuilder ligne = ResultatImportResponse.builder()
                .index(resultat.index())
                .nomFichier(resultat.nomFichier());
        if (resultat.reussi()) {
            FicheSigneeModel fiche = resultat.fiche();
            return ligne.resultat(ResultatImportResponse.IMPORTEE)
                    .code(HttpStatus.CREATED.value())
                    .ficheId(fiche.id())
                    .consultationId(fiche.consultationId())
                    .patientId(fiche.patientId())
                    .statut(fiche.statut())
                    .reimportee(fiche.reimportee())
                    .build();
        }
        ErrorResponse erreur = globalExceptionHandler.erreurDeFichier(resultat.erreur());
        return ligne.resultat(ResultatImportResponse.ERREUR)
                .code(erreur.status())
                .erreur(erreur.erreur())
                .message(erreur.message())
                .build();
    }

    private static byte[] lire(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier importe impossible", e);
        }
    }
}
