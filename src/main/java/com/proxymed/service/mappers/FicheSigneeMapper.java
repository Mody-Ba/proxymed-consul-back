package com.proxymed.service.mappers;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeResume;
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

    /**
     * Reimport d'une fiche RENVOYEE (entite managee, mutee en place) : nouveau scan, de nouveau
     * EN_ATTENTE, sans visa. motifRenvoi est volontairement laisse tel quel (dernier motif conserve).
     */
    public void remplacerScan(FicheSignee entity, FicheSigneeModel model) {
        entity.setNomFichier(model.nomFichier());
        entity.setTypeContenu(model.typeContenu());
        entity.setTailleOctets(model.tailleOctets());
        entity.setDateImport(model.dateImport());
        entity.setContenu(model.contenu());
        entity.setStatut(StatutFicheSignee.EN_ATTENTE);
        entity.setSeniorVisaId(null);
        entity.setNomSeniorVisa(null);
        entity.setDateVisa(null);
    }

    public FicheSigneeModel toModel(FicheSignee entity) {
        ConsultationInitiale consultation = entity.getConsultation();
        return FicheSigneeModel.builder()
                .id(entity.getId())
                .consultationId(consultation.getId())
                .patientId(consultation.getPatient() != null ? consultation.getPatient().getId() : null)
                .dateConsultation(consultation.getDateConsultation())
                .nomFichier(entity.getNomFichier())
                .typeContenu(entity.getTypeContenu())
                .tailleOctets(entity.getTailleOctets())
                .dateImport(entity.getDateImport())
                .contenu(entity.getContenu())
                .statut(entity.getStatut())
                .seniorVisaId(entity.getSeniorVisaId())
                .nomSeniorVisa(entity.getNomSeniorVisa())
                .dateVisa(entity.getDateVisa())
                .motifRenvoi(entity.getMotifRenvoi())
                .build();
    }

    public FicheSigneeModel toModel(FicheSigneeResume resume) {
        return FicheSigneeModel.builder()
                .id(resume.getId())
                .consultationId(resume.getConsultationId())
                .patientId(resume.getPatientId())
                .dateConsultation(resume.getDateConsultation())
                .nomFichier(resume.getNomFichier())
                .typeContenu(resume.getTypeContenu())
                .tailleOctets(resume.getTailleOctets())
                .dateImport(resume.getDateImport())
                .statut(resume.getStatut())
                .seniorVisaId(resume.getSeniorVisaId())
                .nomSeniorVisa(resume.getNomSeniorVisa())
                .dateVisa(resume.getDateVisa())
                .motifRenvoi(resume.getMotifRenvoi())
                .build();
    }
}
