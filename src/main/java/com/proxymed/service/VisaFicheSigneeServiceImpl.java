package com.proxymed.service;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.EtatFicheSignee;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.service.mappers.FicheSigneeMapper;
import com.proxymed.service.model.FicheAViserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Le contenu du PDF n'est jamais charge ici : on lit le statut et la version (projection), puis on
 * met a jour la ligne a condition qu'elle ait toujours cette version et soit toujours EN_ATTENTE.
 * De deux visas simultanes sur la meme fiche, un seul modifie la ligne ; l'autre obtient 0 ligne
 * modifiee (ou un conflit de verrou de la base) et recoit un 409.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VisaFicheSigneeServiceImpl implements VisaFicheSigneeService {

    static final String MESSAGE_MODIFIEE_ENTRE_TEMPS =
            "La fiche de la visite %s vient d'etre traitee par une autre requete : rechargez la liste";

    /** La plus ancienne d'abord ; id pour un ordre stable entre deux pages a date d'import egale. */
    private static final Sort TRI_PLUS_ANCIENNES = Sort.by(Sort.Order.asc("dateImport"), Sort.Order.asc("id"));

    private final FicheSigneeRepository ficheSigneeRepository;
    private final FicheSigneeMapper ficheSigneeMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<FicheAViserModel> findByStatut(StatutFicheSignee statut, int page, int taille) {
        if (page < 0) {
            throw new RegleGestionException("Le numero de page doit etre positif ou nul");
        }
        if (taille < 1 || taille > TAILLE_PAGE_MAX) {
            throw new RegleGestionException("La taille de page doit etre comprise entre 1 et " + TAILLE_PAGE_MAX);
        }
        StatutFicheSignee filtre = statut != null ? statut : StatutFicheSignee.EN_ATTENTE;
        return ficheSigneeRepository.findAViserByStatut(filtre, PageRequest.of(page, taille, TRI_PLUS_ANCIENNES))
                .map(ficheSigneeMapper::toModel);
    }

    @Override
    public void viser(UUID visiteId, UUID seniorId, String nomSenior) {
        EtatFicheSignee etat = ficheEnAttente(visiteId, "visee");
        int modifiees = ficheSigneeRepository.viserSiVersion(etat.getId(), etat.getVersion(), seniorId, nomSenior,
                Instant.now());
        verifierModifiee(modifiees, visiteId);
    }

    @Override
    public void renvoyer(UUID visiteId, String motif) {
        EtatFicheSignee etat = ficheEnAttente(visiteId, "renvoyee");
        int modifiees = ficheSigneeRepository.renvoyerSiVersion(etat.getId(), etat.getVersion(), motif);
        verifierModifiee(modifiees, visiteId);
    }

    private EtatFicheSignee ficheEnAttente(UUID visiteId, String action) {
        EtatFicheSignee etat = ficheSigneeRepository.findEtatByConsultationId(visiteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune fiche signee importee pour la visite : " + visiteId));
        if (etat.getStatut() != StatutFicheSignee.EN_ATTENTE) {
            throw new ConflitEtatException("La fiche de la visite " + visiteId + " est " + etat.getStatut()
                    + " : seule une fiche EN_ATTENTE peut etre " + action);
        }
        return etat;
    }

    private static void verifierModifiee(int modifiees, UUID visiteId) {
        if (modifiees != 1) {
            throw new ConflitEtatException(MESSAGE_MODIFIEE_ENTRE_TEMPS.formatted(visiteId));
        }
    }
}
