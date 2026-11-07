package com.proxymed.service;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.FicheSigneeModel;
import com.proxymed.service.model.PatientModel;
import com.proxymed.service.model.RapportFicheModel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Assemble le rapport a partir des services existants (aucune entite JPA ici) : la visite par
 * ConsultationService (404 si elle n'existe pas), la fiche et son scan par FicheSigneeService (404
 * si aucune fiche), puis le PDF par PdfGenerationService. Seuls l'identite du patient, la visite et
 * le visa sont repris : aucune donnee medicale de la consultation n'entre dans le rapport.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RapportFicheServiceImpl implements RapportFicheService {

    private final ConsultationService consultationService;
    private final FicheSigneeService ficheSigneeService;
    private final PdfGenerationService pdfGenerationService;

    @Override
    public Resource generer(UUID consultationId) {
        ConsultationModel visite = consultationService.findById(consultationId);
        FicheSigneeModel fiche = ficheSigneeService.telecharger(consultationId);
        if (fiche.statut() != StatutFicheSignee.VISEE) {
            throw new ConflitEtatException("Le rapport n'est disponible que pour une fiche VISEE : la fiche de la visite "
                    + consultationId + " est " + fiche.statut());
        }
        PatientModel patient = visite.patient();
        return pdfGenerationService.genererRapport(RapportFicheModel.builder()
                .consultationId(consultationId)
                .nomPatient(patient != null ? patient.nomComplet() : null)
                .numeroDossierProxymed(patient != null ? patient.numeroDossierProxymed() : null)
                .numeroDmi(patient != null ? patient.numeroDmi() : null)
                .dateNaissance(patient != null ? patient.dateNaissance() : null)
                .sexe(patient != null ? patient.sexe() : null)
                .typeVisite(visite.typeVisite())
                .dateVisite(visite.dateConsultation())
                .nomSeniorVisa(fiche.nomSeniorVisa())
                .dateVisa(fiche.dateVisa())
                .scan(fiche.contenu())
                .build());
    }
}
