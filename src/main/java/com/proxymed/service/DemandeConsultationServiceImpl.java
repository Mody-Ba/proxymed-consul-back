package com.proxymed.service;

import com.proxymed.entity.DemandeConsultation;
import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.exception.DemandeDejaTraiteeException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.DemandeConsultationRepository;
import com.proxymed.service.mappers.DemandeConsultationMapper;
import com.proxymed.service.model.DemandeConsultationModel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est DemandeConsultation. Une demande ne se decide
 * qu'une fois : accepter ou refuser exige le statut EN_ATTENTE (sinon 409), et la colonne version
 * fait echouer la decision perdante de deux decisions simultanees.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DemandeConsultationServiceImpl implements DemandeConsultationService {

    static final int TAILLE_PAGE_MAX = 100;
    private static final Sort TRI_PLUS_RECENTES = Sort.by(Sort.Order.desc("dateCreation"), Sort.Order.asc("id"));

    private final DemandeConsultationRepository demandeConsultationRepository;
    private final DemandeConsultationMapper demandeConsultationMapper;

    @Override
    public DemandeConsultationModel creer(DemandeConsultationModel model) {
        DemandeConsultationModel aEnregistrer = model.toBuilder()
                .id(null)
                .statut(StatutDemandeConsultation.EN_ATTENTE)
                .medecinSeniorId(null)
                .juniorId(null)
                .motifRefus(null)
                .dateCreation(Instant.now())
                .dateDecision(null)
                .build();
        return demandeConsultationMapper.toModel(
                demandeConsultationRepository.save(demandeConsultationMapper.toEntity(aEnregistrer)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeConsultationModel> findByDemandeur(UUID demandeurId, int page, int taille) {
        return demandeConsultationRepository.findByDemandeurId(demandeurId, pagination(page, taille))
                .map(demandeConsultationMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeConsultationModel> findAll(StatutDemandeConsultation statut, int page, int taille) {
        Pageable pageable = pagination(page, taille);
        Page<DemandeConsultation> demandes = statut != null
                ? demandeConsultationRepository.findByStatut(statut, pageable)
                : demandeConsultationRepository.findAll(pageable);
        return demandes.map(demandeConsultationMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandeConsultationModel> findAssigneesAuJunior(UUID juniorId, int page, int taille) {
        return demandeConsultationRepository
                .findByJuniorIdAndStatut(juniorId, StatutDemandeConsultation.ACCEPTEE, pagination(page, taille))
                .map(demandeConsultationMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public DemandeConsultationModel findById(UUID id) {
        return demandeConsultationMapper.toModel(getEntityById(id));
    }

    @Override
    public DemandeConsultationModel accepter(UUID id, UUID medecinSeniorId, UUID juniorId) {
        DemandeConsultation demande = getEntityEnAttente(id);
        demande.setStatut(StatutDemandeConsultation.ACCEPTEE);
        demande.setMedecinSeniorId(medecinSeniorId);
        demande.setJuniorId(juniorId);
        demande.setDateDecision(Instant.now());
        return enregistrerDecision(demande);
    }

    @Override
    public DemandeConsultationModel refuser(UUID id, UUID medecinSeniorId, String motifRefus) {
        DemandeConsultation demande = getEntityEnAttente(id);
        demande.setStatut(StatutDemandeConsultation.REFUSEE);
        demande.setMedecinSeniorId(medecinSeniorId);
        demande.setMotifRefus(motifRefus.strip());
        demande.setDateDecision(Instant.now());
        return enregistrerDecision(demande);
    }

    /**
     * saveAndFlush : le controle de version a lieu ici, et non au commit, pour que la decision
     * simultanee perdante recoive elle aussi un 409.
     */
    private DemandeConsultationModel enregistrerDecision(DemandeConsultation demande) {
        try {
            return demandeConsultationMapper.toModel(demandeConsultationRepository.saveAndFlush(demande));
        } catch (OptimisticLockingFailureException e) {
            throw dejaTraitee(demande.getId());
        }
    }

    private DemandeConsultation getEntityEnAttente(UUID id) {
        DemandeConsultation demande = getEntityById(id);
        if (demande.getStatut() != StatutDemandeConsultation.EN_ATTENTE) {
            throw dejaTraitee(id);
        }
        return demande;
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private DemandeConsultation getEntityById(UUID id) {
        return demandeConsultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de consultation introuvable : " + id));
    }

    private static DemandeDejaTraiteeException dejaTraitee(UUID id) {
        return new DemandeDejaTraiteeException(
                "La demande de consultation " + id + " a deja ete traitee : seule une demande EN_ATTENTE "
                        + "peut etre acceptee ou refusee");
    }

    private static Pageable pagination(int page, int taille) {
        if (page < 0) {
            throw new RegleGestionException("Le numero de page doit etre positif ou nul");
        }
        if (taille < 1 || taille > TAILLE_PAGE_MAX) {
            throw new RegleGestionException("La taille de page doit etre comprise entre 1 et " + TAILLE_PAGE_MAX);
        }
        return PageRequest.of(page, taille, TRI_PLUS_RECENTES);
    }
}
