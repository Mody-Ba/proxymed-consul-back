package com.proxymed.service.mappers;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
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
                .build();
    }
}
