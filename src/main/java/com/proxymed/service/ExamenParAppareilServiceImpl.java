package com.proxymed.service;

import com.proxymed.entity.ExamenParAppareil;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.ExamenParAppareilModel;
import com.proxymed.repository.ExamenParAppareilRepository;
import com.proxymed.service.mappers.ExamenParAppareilMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est ExamenParAppareil (l'aggregat propre a
 * ce service). ConsultationInitiale (aggregat parent) n'est jamais manipulee ici : la
 * verification qu'elle existe et est modifiable passe par ConsultationService.verifierModifiable,
 * et le rattachement JPA (id -> reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExamenParAppareilServiceImpl implements ExamenParAppareilService {

    private final ExamenParAppareilRepository examenParAppareilRepository;
    private final ConsultationService consultationService;
    private final ExamenParAppareilMapper examenParAppareilMapper;

    @Override
    public ExamenParAppareilModel findByConsultation(UUID consultationId) {
        ExamenParAppareil examen = examenParAppareilRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun examen par appareil enregistre pour la consultation : " + consultationId));
        return examenParAppareilMapper.toModel(examen);
    }

    @Override
    public ExamenParAppareilModel creerOuMettreAJour(UUID consultationId, ExamenParAppareilModel model) {
        consultationService.verifierModifiable(consultationId);
        Optional<ExamenParAppareil> existant = examenParAppareilRepository.findByConsultationId(consultationId);
        ExamenParAppareil examen;
        boolean creation = existant.isEmpty();
        if (existant.isPresent()) {
            examen = existant.get();
            examenParAppareilMapper.applyToEntity(examen, model);
        } else {
            examen = examenParAppareilMapper.toEntity(model, consultationId);
        }
        examen = examenParAppareilRepository.save(examen);
        if (creation) {
            examenParAppareilMapper.synchroniserAvecConsultation(consultationId, examen);
        }
        return examenParAppareilMapper.toModel(examen);
    }
}
