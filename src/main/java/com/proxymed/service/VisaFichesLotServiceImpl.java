package com.proxymed.service;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.ResultatDecisionModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Volontairement sans @Transactional, comme l'import en lot : chaque appel a VisaFicheSigneeService
 * (bean transactionnel distinct) ouvre et termine sa propre transaction, si bien qu'une fiche refusee
 * n'annule pas celles deja visees ou renvoyees.
 * Un conflit de verrou signale par la base (deux requetes sur la meme ligne) devient un 409 pour
 * cette fiche, comme une fiche deja traitee.
 * Journaux : seulement la position de la fiche et le type d'erreur imprevue, jamais d'identifiant,
 * de nom, de motif ni le message de l'erreur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisaFichesLotServiceImpl implements VisaFichesLotService {

    private final VisaFicheSigneeService visaFicheSigneeService;

    @Override
    public List<ResultatDecisionModel> viser(List<UUID> visiteIds, UUID seniorId, String nomSenior) {
        verifierListe(visiteIds);
        if (seniorId == null || nomSenior == null || nomSenior.isBlank() || nomSenior.length() > NOM_SENIOR_MAX) {
            throw new RegleGestionException("Le badge ne permet pas d'identifier le senior (identifiant ou "
                    + "adresse e-mail absente, ou e-mail de plus de " + NOM_SENIOR_MAX + " caracteres) : "
                    + "aucune fiche n'a ete visee");
        }
        return traiter(visiteIds, StatutFicheSignee.VISEE,
                visiteId -> visaFicheSigneeService.viser(visiteId, seniorId, nomSenior));
    }

    @Override
    public List<ResultatDecisionModel> renvoyer(List<UUID> visiteIds, String motif) {
        verifierListe(visiteIds);
        if (motif == null || motif.isBlank()) {
            throw new RegleGestionException("Le motif du renvoi est obligatoire : aucune fiche n'a ete renvoyee");
        }
        String motifNettoye = motif.strip();
        if (motifNettoye.length() > MOTIF_MAX) {
            throw new RegleGestionException("Le motif du renvoi ne doit pas depasser " + MOTIF_MAX
                    + " caracteres : aucune fiche n'a ete renvoyee");
        }
        return traiter(visiteIds, StatutFicheSignee.RENVOYEE,
                visiteId -> visaFicheSigneeService.renvoyer(visiteId, motifNettoye));
    }

    private static void verifierListe(List<UUID> visiteIds) {
        if (visiteIds == null || visiteIds.isEmpty()) {
            throw new RegleGestionException("Aucune fiche a traiter : indiquez au moins une visite");
        }
        if (visiteIds.size() > MAX_FICHES) {
            throw new RegleGestionException("Au plus " + MAX_FICHES + " fiches par requete ("
                    + visiteIds.size() + " recues) : aucune fiche n'a ete traitee");
        }
        if (visiteIds.stream().anyMatch(Objects::isNull)) {
            throw new RegleGestionException("Un identifiant de visite est vide : aucune fiche n'a ete traitee");
        }
        if (new HashSet<>(visiteIds).size() != visiteIds.size()) {
            throw new RegleGestionException("La liste contient plusieurs fois la meme visite : "
                    + "aucune fiche n'a ete traitee");
        }
    }

    private List<ResultatDecisionModel> traiter(List<UUID> visiteIds, StatutFicheSignee statutObtenu,
                                                Consumer<UUID> action) {
        List<ResultatDecisionModel> resultats = new ArrayList<>();
        for (int index = 0; index < visiteIds.size(); index++) {
            UUID visiteId = visiteIds.get(index);
            ResultatDecisionModel.ResultatDecisionModelBuilder resultat = ResultatDecisionModel.builder()
                    .index(index).visiteId(visiteId);
            try {
                action.accept(visiteId);
                resultats.add(resultat.statut(statutObtenu).build());
            } catch (ConcurrencyFailureException e) {
                resultats.add(resultat.erreur(new ConflitEtatException(
                        VisaFicheSigneeServiceImpl.MESSAGE_MODIFIEE_ENTRE_TEMPS.formatted(visiteId))).build());
            } catch (RuntimeException e) {
                if (!erreurPrevue(e)) {
                    log.error("Decision en lot ({}) : erreur imprevue sur la fiche n° {} ({})",
                            statutObtenu, index + 1, e.getClass().getName());
                }
                resultats.add(resultat.erreur(e).build());
            }
        }
        return resultats;
    }

    /** Erreurs metier attendues : elles sont rendues au client, sans journal. */
    static boolean erreurPrevue(RuntimeException e) {
        return e instanceof ResourceNotFoundException
                || e instanceof ConflitEtatException
                || e instanceof RegleGestionException;
    }
}
