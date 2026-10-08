package com.proxymed.service;

import com.proxymed.entity.Patient;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.PatientModel;
import com.proxymed.repository.PatientRepository;
import com.proxymed.service.mappers.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    public List<PatientModel> findAll() {
        return patientRepository.findAll().stream().map(this::toModelAvecAge).toList();
    }

    @Override
    public PatientModel findById(UUID id) {
        return toModelAvecAge(getEntityById(id));
    }

    @Override
    public PatientModel create(PatientModel model) {
        Patient patient = patientMapper.toEntity(model);
        return toModelAvecAge(patientRepository.save(patient));
    }

    @Override
    public PatientModel update(UUID id, PatientModel model) {
        Patient patient = getEntityById(id);
        patientMapper.applyToEntity(patient, model);
        return toModelAvecAge(patientRepository.save(patient));
    }

    /**
     * L'age est une regle de gestion (derivee de la date de naissance), jamais persistee :
     * calculee ici plutot que dans l'entite ou les mappers.
     */
    @Override
    public PatientModel completerAge(PatientModel model) {
        if (model == null) {
            return null;
        }
        return model.toBuilder().age(calculerAge(model.dateNaissance())).build();
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private Patient getEntityById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient introuvable : " + id));
    }

    private PatientModel toModelAvecAge(Patient entity) {
        return completerAge(patientMapper.toModel(entity));
    }

    private Integer calculerAge(LocalDate dateNaissance) {
        return dateNaissance != null ? Period.between(dateNaissance, LocalDate.now()).getYears() : null;
    }
}
