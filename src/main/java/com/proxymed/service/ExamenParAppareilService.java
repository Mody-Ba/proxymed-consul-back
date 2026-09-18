package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.ExamenParAppareil;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.ExamenParAppareilModel;
import com.proxymed.repository.ExamenParAppareilRepository;
import com.proxymed.service.mappers.ExamenParAppareilMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gere l'examen par appareil (section 5, relation 1-1) d'une consultation.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExamenParAppareilService {

    private final ExamenParAppareilRepository examenParAppareilRepository;
    private final ConsultationService consultationService;
    private final ExamenParAppareilMapper examenParAppareilMapper;

    public ExamenParAppareilModel findByConsultation(UUID consultationId) {
        ExamenParAppareil examen = examenParAppareilRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun examen par appareil enregistre pour la consultation : " + consultationId));
        return examenParAppareilMapper.toModel(examen);
    }

    public ExamenParAppareilModel creerOuMettreAJour(UUID consultationId, ExamenParAppareilModel model) {
        ConsultationInitiale consultation = consultationService.getModifiableEntityById(consultationId);
        ExamenParAppareil examen = consultation.getExamenParAppareil();
        if (examen == null) {
            examen = examenParAppareilMapper.toEntity(model, consultation);
            consultation.setExamenParAppareil(examen);
        } else {
            examenParAppareilMapper.applyToEntity(examen, model);
        }
        examen = examenParAppareilRepository.save(examen);
        return examenParAppareilMapper.toModel(examen);
    }
}
