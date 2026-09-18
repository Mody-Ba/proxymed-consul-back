package com.proxymed.service;

import com.proxymed.entity.Medecin;
import com.proxymed.entity.Structure;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.StructureRepository;
import com.proxymed.service.mappers.MedecinMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MedecinService {

    private final MedecinRepository medecinRepository;
    private final StructureRepository structureRepository;
    private final MedecinMapper medecinMapper;

    public List<MedecinModel> findAll(RoleMedecin role) {
        List<Medecin> medecins = role != null ? medecinRepository.findByRole(role) : medecinRepository.findAll();
        return medecins.stream().map(medecinMapper::toModel).toList();
    }

    public MedecinModel findById(Long id) {
        return medecinMapper.toModel(getEntityById(id));
    }

    /**
     * Reservee aux autres services qui doivent rattacher un medecin a une entite
     * avant un appel repository (ex. ConsultationService) : frontiere JPA.
     */
    public Medecin getEntityById(Long id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medecin introuvable : " + id));
    }

    public MedecinModel create(MedecinModel model) {
        Structure structure = null;
        if (model.structureRattachementId() != null) {
            structure = structureRepository.findById(model.structureRattachementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Structure introuvable : " + model.structureRattachementId()));
        }
        Medecin medecin = medecinMapper.toEntity(model, structure);
        return medecinMapper.toModel(medecinRepository.save(medecin));
    }
}
