package com.proxymed.service;

import com.proxymed.entity.ConstanteVitale;
import com.proxymed.enums.TypeConstanteVitale;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.ConstanteVitaleModel;
import com.proxymed.repository.ConstanteVitaleRepository;
import com.proxymed.service.mappers.ConstanteVitaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est ConstanteVitale (l'aggregat propre a ce
 * service). ConsultationInitiale (aggregat parent) n'est jamais manipulee ici : la
 * verification qu'elle existe et est modifiable passe par ConsultationService.verifierModifiable,
 * et le rattachement JPA (id -> reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConstanteVitaleServiceImpl implements ConstanteVitaleService {

    private final ConstanteVitaleRepository constanteVitaleRepository;
    private final ConsultationService consultationService;
    private final ConstanteVitaleMapper constanteVitaleMapper;

    @Override
    public ConstanteVitaleModel ajouter(UUID consultationId, ConstanteVitaleModel model) {
        consultationService.verifierModifiable(consultationId);
        ConstanteVitaleModel avecAlerte = BornesConstantesVitales.appliquer(model);
        ConstanteVitale entity = constanteVitaleRepository.save(constanteVitaleMapper.toEntity(avecAlerte, consultationId));
        constanteVitaleMapper.synchroniserAvecConsultation(consultationId, entity);
        if (model.type() == TypeConstanteVitale.POIDS || model.type() == TypeConstanteVitale.TAILLE) {
            recalculerImc(consultationId);
        }
        return constanteVitaleMapper.toModel(entity);
    }

    @Override
    public ConstanteVitaleModel modifier(UUID consultationId, Long constanteId, ConstanteVitaleModel model) {
        consultationService.verifierModifiable(consultationId);
        ConstanteVitale entity = constanteVitaleRepository.findById(constanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Constante vitale introuvable : " + constanteId));
        ConstanteVitaleModel actuel = constanteVitaleMapper.toModel(entity);
        if (!consultationId.equals(actuel.consultationId())) {
            throw new ResourceNotFoundException("Constante vitale introuvable pour cette consultation : " + constanteId);
        }
        ConstanteVitaleModel avecAlerte = BornesConstantesVitales.appliquer(model);
        constanteVitaleMapper.applyToEntity(entity, avecAlerte);
        entity = constanteVitaleRepository.save(entity);
        if (model.type() == TypeConstanteVitale.POIDS || model.type() == TypeConstanteVitale.TAILLE) {
            recalculerImc(consultationId);
        }
        return constanteVitaleMapper.toModel(entity);
    }

    private void recalculerImc(UUID consultationId) {
        List<ConstanteVitale> entites = constanteVitaleRepository.findByConsultationId(consultationId);
        List<ConstanteVitaleModel> constantes = constanteVitaleMapper.toModelList(entites);
        Optional<ConstanteVitaleModel> poids = dernierParType(constantes, TypeConstanteVitale.POIDS);
        Optional<ConstanteVitaleModel> taille = dernierParType(constantes, TypeConstanteVitale.TAILLE);
        if (poids.isEmpty() || taille.isEmpty()) {
            return;
        }
        BigDecimal imcValeur = BornesConstantesVitales.calculerImc(poids.get().valeur(), taille.get().valeur());
        Optional<ConstanteVitaleModel> imcExistant = dernierParType(constantes, TypeConstanteVitale.IMC);

        ConstanteVitaleModel imcModel = BornesConstantesVitales.appliquer(
                imcExistant.orElseGet(() -> ConstanteVitaleModel.builder().type(TypeConstanteVitale.IMC).build())
                        .toBuilder()
                        .valeur(imcValeur)
                        .heure(LocalTime.now())
                        .build());

        ConstanteVitale imcEntity;
        if (imcExistant.isPresent()) {
            Long imcId = imcExistant.get().id();
            imcEntity = entites.stream().filter(e -> e.getId().equals(imcId)).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Constante vitale introuvable : " + imcId));
            constanteVitaleMapper.applyToEntity(imcEntity, imcModel);
        } else {
            imcEntity = constanteVitaleMapper.toEntity(imcModel, consultationId);
        }
        imcEntity = constanteVitaleRepository.save(imcEntity);
        if (imcExistant.isEmpty()) {
            constanteVitaleMapper.synchroniserAvecConsultation(consultationId, imcEntity);
        }
    }

    private Optional<ConstanteVitaleModel> dernierParType(List<ConstanteVitaleModel> constantes, TypeConstanteVitale type) {
        return constantes.stream()
                .filter(c -> c.type() == type)
                .reduce((first, second) -> second);
    }
}
