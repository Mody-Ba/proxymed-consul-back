package com.proxymed.service;

import com.proxymed.exception.FicheSigneeExistanteException;
import com.proxymed.exception.FichierNonPdfException;
import com.proxymed.exception.FichierTropVolumineuxException;
import com.proxymed.exception.QrCodeIntrouvableException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.FicheSigneeModel;
import com.proxymed.service.model.FichierAImporterModel;
import com.proxymed.service.model.ResultatImportModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Volontairement sans @Transactional : chaque appel a FicheSigneeService.importer (bean
 * transactionnel distinct) ouvre et termine sa propre transaction, si bien qu'un fichier refuse
 * n'annule pas ceux deja ranges.
 * Journaux : seulement la position du fichier et le type d'erreur imprevue, jamais le nom du
 * fichier, son contenu ni le message de l'erreur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportFichesLotServiceImpl implements ImportFichesLotService {

    private final FicheSigneeService ficheSigneeService;

    @Override
    public List<ResultatImportModel> importer(List<FichierAImporterModel> fichiers) {
        if (fichiers == null || fichiers.isEmpty()) {
            throw new RegleGestionException("Aucun fichier a importer : joignez au moins un PDF");
        }
        if (fichiers.size() > MAX_FICHIERS) {
            throw new RegleGestionException("Au plus " + MAX_FICHIERS + " fichiers par import ("
                    + fichiers.size() + " recus) : rien n'a ete importe");
        }
        List<ResultatImportModel> resultats = new ArrayList<>();
        for (int index = 0; index < fichiers.size(); index++) {
            resultats.add(importerUn(index, fichiers.get(index)));
        }
        return resultats;
    }

    private ResultatImportModel importerUn(int index, FichierAImporterModel fichier) {
        ResultatImportModel.ResultatImportModelBuilder resultat = ResultatImportModel.builder()
                .index(index).nomFichier(fichier.nomFichier());
        try {
            if (fichier.tailleOctets() > FicheSigneeServiceImpl.TAILLE_MAX_OCTETS) {
                // Refuse sans lire le contenu.
                throw new FichierTropVolumineuxException();
            }
            FicheSigneeModel fiche = ficheSigneeService.importer(FicheSigneeModel.builder()
                    .nomFichier(fichier.nomFichier())
                    .typeContenu(fichier.typeContenu())
                    .tailleOctets(fichier.tailleOctets())
                    .contenu(fichier.contenu().get())
                    .build());
            return resultat.fiche(fiche).build();
        } catch (RuntimeException e) {
            if (!erreurPrevue(e)) {
                log.error("Import en lot : erreur imprevue sur le fichier n° {} ({})", index + 1, e.getClass().getName());
            }
            return resultat.erreur(e).build();
        }
    }

    /** Erreurs metier attendues d'un import : elles sont rendues au client, sans journal. */
    static boolean erreurPrevue(RuntimeException e) {
        return e instanceof RegleGestionException
                || e instanceof ResourceNotFoundException
                || e instanceof QrCodeIntrouvableException
                || e instanceof FicheSigneeExistanteException
                || e instanceof FichierNonPdfException
                || e instanceof FichierTropVolumineuxException;
    }
}
