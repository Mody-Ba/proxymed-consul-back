package com.proxymed.service;

import com.proxymed.entity.AntecedentMaladie;
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
 * La seule entite JPA visible dans cette classe est AntecedentMaladie (l'aggregat propre a
 * ce service). ConsultationInitiale et MaladieChronique (aggregats voisins) ne sont jamais
 * manipulees ici : leur existence/validite est verifiee via ConsultationService.verifierModifiable
 * et MaladieChroniqueService.findById (Model), et le rattachement JPA (id -> reference) est
 * delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AntecedentMaladieServiceImpl implements AntecedentMaladieService {

    private final AntecedentMaladieRepository antecedentMaladieRepository;
    private final MaladieChroniqueService maladieChroniqueService;
    private final ConsultationService consultationService;
    private final AntecedentMaladieMapper antecedentMaladieMapper;

    @Override
    public List<AntecedentMaladieModel> findByConsultation(UUID consultationId) {
        return antecedentMaladieMapper.toModelList(antecedentMaladieRepository.findByConsultationId(consultationId));
    }

    @Override
    public AntecedentMaladieModel ajouter(UUID consultationId, AntecedentMaladieModel model) {
        consultationService.verifierModifiable(consultationId);
        maladieChroniqueService.findById(model.maladieChroniqueId());

        AntecedentMaladie antecedent = antecedentMaladieMapper.toEntity(model, consultationId);
        antecedent = antecedentMaladieRepository.save(antecedent);
        antecedentMaladieMapper.synchroniserAvecConsultation(consultationId, antecedent);
        return antecedentMaladieMapper.toModel(antecedent);
    }

    @Override
    public void supprimer(UUID consultationId, Long antecedentId) {
        consultationService.verifierModifiable(consultationId);
        AntecedentMaladie antecedent = antecedentMaladieRepository.findById(antecedentId)
                .orElseThrow(() -> new ResourceNotFoundException("Antecedent introuvable : " + antecedentId));
        AntecedentMaladieModel model = antecedentMaladieMapper.toModel(antecedent);
        if (!consultationId.equals(model.consultationId())) {
            throw new ResourceNotFoundException("Antecedent introuvable pour cette consultation : " + antecedentId);
        }
        antecedentMaladieRepository.deleteById(antecedentId);
    }
}
