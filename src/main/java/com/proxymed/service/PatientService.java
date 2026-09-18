package com.proxymed.service;

import com.proxymed.entity.Patient;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.PatientModel;
import com.proxymed.repository.PatientRepository;
import com.proxymed.service.mappers.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public List<PatientModel> findAll() {
        return patientRepository.findAll().stream().map(patientMapper::toModel).toList();
    }

    public PatientModel findById(UUID id) {
        return patientMapper.toModel(getEntityById(id));
    }

    /**
     * Reservee aux autres services qui doivent rattacher un patient a une entite
     * avant un appel repository (ex. ConsultationService) : frontiere JPA.
     */
    public Patient getEntityById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable : " + id));
    }

    public PatientModel create(PatientModel model) {
        Patient patient = patientMapper.toEntity(model);
        return patientMapper.toModel(patientRepository.save(patient));
    }

    public PatientModel update(UUID id, PatientModel model) {
        Patient patient = getEntityById(id);
        patientMapper.applyToEntity(patient, model);
        return patientMapper.toModel(patientRepository.save(patient));
    }
}
