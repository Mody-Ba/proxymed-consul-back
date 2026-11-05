package com.proxymed.service;

import com.proxymed.exception.RegleGestionException;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.DemandeImpressionModel;
import com.proxymed.service.model.FichesImprimeesModel;
import com.proxymed.service.model.PatientModel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestre ConsultationService (visites vierges), PatientService (identite) et
 * PdfGenerationService, dans une seule transaction : le PDF est entierement ecrit avant la fin de
 * la methode, donc un echec de generation annule aussi les visites creees.
 * Ce que l'admin voit sur la fiche est construit ici, champ par champ (identiteSeulement) : rien
 * d'autre que l'identite du patient ne peut arriver dans le template, quel que soit son contenu.
 * Aucune journalisation (ni identite, ni donnee medicale).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ImpressionFichesServiceImpl implements ImpressionFichesService {

    private final ConsultationService consultationService;
    private final PatientService patientService;
    private final PdfGenerationService pdfGenerationService;

    @Override
    public FichesImprimeesModel imprimer(List<DemandeImpressionModel> demandes) {
        verifierSansDoublon(demandes);
        List<UUID> visiteIds = new ArrayList<>();
        List<ConsultationModel> fiches = new ArrayList<>();
        Map<UUID, PatientModel> identites = new HashMap<>();
        for (DemandeImpressionModel demande : demandes) {
            ConsultationModel visite = consultationService.creerVisiteVierge(demande.patientId(), demande.typeVisite());
            PatientModel identite = identites.computeIfAbsent(demande.patientId(),
                    id -> identiteSeulement(patientService.findById(id)));
            visiteIds.add(visite.id());
            fiches.add(ficheVierge(visite, identite));
        }
        Resource pdf = pdfGenerationService.genererFichesCombinees(fiches);
        return FichesImprimeesModel.builder().visiteIds(List.copyOf(visiteIds)).pdf(pdf).build();
    }

    /**
     * Deux fois la meme demande (meme patient, meme type de visite) creerait deux visites pour une
     * seule fiche attendue : c'est presque toujours un double clic ou une erreur de selection.
     * Le meme patient avec deux types de visite differents reste accepte.
     */
    private static void verifierSansDoublon(List<DemandeImpressionModel> demandes) {
        Map<DemandeImpressionModel, Integer> premiere = new HashMap<>();
        for (int i = 0; i < demandes.size(); i++) {
            Integer deja = premiere.putIfAbsent(demandes.get(i), i);
            if (deja != null) {
                throw new RegleGestionException("La fiche n° " + (i + 1) + " est en double avec la fiche n° " + (deja + 1)
                        + " (meme patient, meme type de visite) : chaque fiche ne peut etre demandee qu'une fois");
            }
        }
    }

    /** Fiche a imprimer : id, statut et type de la visite, identite du patient, rien d'autre. */
    static ConsultationModel ficheVierge(ConsultationModel visite, PatientModel identite) {
        return ConsultationModel.builder()
                .id(visite.id())
                .statut(visite.statut())
                .typeVisite(visite.typeVisite())
                .patient(identite)
                .maladiesChroniques(List.of())
                .facteursDeRisque(List.of())
                .situationSociale(List.of())
                .constantesVitales(List.of())
                .build();
    }

    /**
     * Champs d'identite du patient imprimes pour l'admin. La couverture sociale n'en fait pas
     * partie (information administrative et financiere, non necessaire pour identifier le patient).
     */
    static PatientModel identiteSeulement(PatientModel patient) {
        return PatientModel.builder()
                .id(patient.id())
                .numeroDossierProxymed(patient.numeroDossierProxymed())
                .numeroDmi(patient.numeroDmi())
                .nomComplet(patient.nomComplet())
                .dateNaissance(patient.dateNaissance())
                .age(patient.age())
                .sexe(patient.sexe())
                .telephone(patient.telephone())
                .adresseDomicile(patient.adresseDomicile())
                .commune(patient.commune())
                .quartier(patient.quartier())
                .personneAContacterNom(patient.personneAContacterNom())
                .personneAContacterTelephone(patient.personneAContacterTelephone())
                .build();
    }
}
