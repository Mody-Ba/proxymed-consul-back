package com.proxymed.service;

import com.proxymed.entity.ConstanteVitale;
import com.proxymed.entity.ConsultationInitiale;
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

@Service
@RequiredArgsConstructor
@Transactional
public class ConstanteVitaleService {

    private final ConstanteVitaleRepository constanteVitaleRepository;
    private final ConsultationService consultationService;
    private final ConstanteVitaleMapper constanteVitaleMapper;

    public ConstanteVitaleModel ajouter(UUID consultationId, ConstanteVitaleModel model) {
        ConsultationInitiale consultation = consultationService.getModifiableEntityById(consultationId);
        ConstanteVitaleModel avecAlerte = BornesConstantesVitales.appliquer(model);
        ConstanteVitale entity = constanteVitaleRepository.save(constanteVitaleMapper.toEntity(avecAlerte, consultation));
        // Garde la collection en memoire de la consultation synchronisee : indispensable si
        // la meme session/entite est relue plus tard dans la meme transaction (ex. tests
        // d'integration, ou un futur appel groupe dans la meme requete).
        consultation.getConstantesVitales().add(entity);
        if (model.type() == TypeConstanteVitale.POIDS || model.type() == TypeConstanteVitale.TAILLE) {
            recalculerImc(consultation);
        }
        return constanteVitaleMapper.toModel(entity);
    }

    public ConstanteVitaleModel modifier(UUID consultationId, Long constanteId, ConstanteVitaleModel model) {
        ConsultationInitiale consultation = consultationService.getModifiableEntityById(consultationId);
        ConstanteVitale entity = constanteVitaleRepository.findById(constanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Constante vitale introuvable : " + constanteId));
        if (!entity.getConsultation().getId().equals(consultationId)) {
            throw new ResourceNotFoundException("Constante vitale introuvable pour cette consultation : " + constanteId);
        }
        ConstanteVitaleModel avecAlerte = BornesConstantesVitales.appliquer(model);
        constanteVitaleMapper.applyToEntity(entity, avecAlerte);
        entity = constanteVitaleRepository.save(entity);
        if (model.type() == TypeConstanteVitale.POIDS || model.type() == TypeConstanteVitale.TAILLE) {
            recalculerImc(consultation);
        }
        return constanteVitaleMapper.toModel(entity);
    }

    private void recalculerImc(ConsultationInitiale consultation) {
        List<ConstanteVitale> constantes = constanteVitaleRepository.findByConsultationId(consultation.getId());
        Optional<ConstanteVitale> poids = dernierParType(constantes, TypeConstanteVitale.POIDS);
        Optional<ConstanteVitale> taille = dernierParType(constantes, TypeConstanteVitale.TAILLE);
        if (poids.isEmpty() || taille.isEmpty()) {
            return;
        }
        BigDecimal imcValeur = BornesConstantesVitales.calculerImc(poids.get().getValeur(), taille.get().getValeur());
        Optional<ConstanteVitale> imcExistant = dernierParType(constantes, TypeConstanteVitale.IMC);
        ConstanteVitale imcEntity = imcExistant.orElseGet(
                () -> ConstanteVitale.builder().consultation(consultation).type(TypeConstanteVitale.IMC).build());

        ConstanteVitaleModel imcModel = BornesConstantesVitales.appliquer(
                constanteVitaleMapper.toModel(imcEntity).toBuilder()
                        .valeur(imcValeur)
                        .heure(LocalTime.now())
                        .build());

        constanteVitaleMapper.applyToEntity(imcEntity, imcModel);
        imcEntity = constanteVitaleRepository.save(imcEntity);
        if (imcExistant.isEmpty()) {
            consultation.getConstantesVitales().add(imcEntity);
        }
    }

    private Optional<ConstanteVitale> dernierParType(List<ConstanteVitale> constantes, TypeConstanteVitale type) {
        return constantes.stream()
                .filter(c -> c.getType() == type)
                .reduce((first, second) -> second);
    }
}
