package com.proxymed.service;

import com.proxymed.entity.AntecedentMaladie;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.MaladieChronique;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.AntecedentMaladieModel;
import com.proxymed.repository.AntecedentMaladieRepository;
import com.proxymed.service.mappers.AntecedentMaladieMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gere les antecedents (maladies chroniques cochees) d'une consultation (section 3).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AntecedentMaladieService {

    private final AntecedentMaladieRepository antecedentMaladieRepository;
    private final MaladieChroniqueService maladieChroniqueService;
    private final ConsultationService consultationService;
    private final AntecedentMaladieMapper antecedentMaladieMapper;

    public List<AntecedentMaladieModel> findByConsultation(UUID consultationId) {
        return antecedentMaladieMapper.toModelList(antecedentMaladieRepository.findByConsultationId(consultationId));
    }

    public AntecedentMaladieModel ajouter(UUID consultationId, AntecedentMaladieModel model) {
        ConsultationInitiale consultation = consultationService.getModifiableEntityById(consultationId);
        MaladieChronique maladie = maladieChroniqueService.getEntityById(model.maladieChroniqueId());
        AntecedentMaladie antecedent = antecedentMaladieMapper.toEntity(model, consultation, maladie);
        antecedent = antecedentMaladieRepository.save(antecedent);
        return antecedentMaladieMapper.toModel(antecedent);
    }

    public void supprimer(UUID consultationId, Long antecedentId) {
        consultationService.getModifiableEntityById(consultationId);
        AntecedentMaladie antecedent = antecedentMaladieRepository.findById(antecedentId)
                .orElseThrow(() -> new ResourceNotFoundException("Antecedent introuvable : " + antecedentId));
        if (!antecedent.getConsultation().getId().equals(consultationId)) {
            throw new ResourceNotFoundException("Antecedent introuvable pour cette consultation : " + antecedentId);
        }
        antecedentMaladieRepository.delete(antecedent);
    }
}
