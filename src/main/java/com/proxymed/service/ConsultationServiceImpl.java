package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.FacteurDeRisqueModel;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.SituationSocialeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Travaille avec ConsultationModel comme type de travail. La seule entite JPA visible dans
 * cette classe est ConsultationInitiale (l'aggregat propre a ce service) : chargee par un
 * appel direct a ConsultationRepository, convertie en Model immediatement, et reconvertie
 * en Entity (via consultationMapper) juste avant repository.save(...).
 * Les entites d'autres aggregats (Patient, Medecin, FacteurDeRisque, SituationSociale) ne
 * sont JAMAIS manipulees ici : leur existence et les regles metier associees (role du
 * medecin, etc.) sont verifiees via les Model renvoyes par PatientService/MedecinService/...,
 * et le rattachement JPA (traduction d'un id en reference) est delegue au mapper DB
 * (consultationMapper), qui est le seul a savoir resoudre un id vers son entite.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientService patientService;
    private final MedecinService medecinService;
    private final FacteurDeRisqueService facteurDeRisqueService;
    private final SituationSocialeService situationSocialeService;
    private final com.proxymed.service.mappers.ConsultationMapper consultationMapper;

    @Override
    public ConsultationModel findById(UUID id) {
        return consultationMapper.toModel(getEntityById(id));
    }

    @Override
    public void verifierModifiable(UUID id) {
        verifierModifiable(findById(id));
    }

    @Override
    public List<ConsultationModel> rechercher(
            String nomPatient,
            String numeroDossierProxymed,
            String numeroDmi,
            Long medecinSeniorId,
            Long medecinJuniorAffecteId,
            StatutConsultation statut,
            DecisionEligibilite decisionEligibilite,
            Long structureId
    ) {
        return consultationRepository.findAll(ConsultationSpecifications.filtrer(
                        nomPatient, numeroDossierProxymed, numeroDmi, medecinSeniorId,
                        medecinJuniorAffecteId, statut, decisionEligibilite, structureId))
                .stream().map(consultationMapper::toModel).toList();
    }

    @Override
    public List<ConsultationModel> fichesEnAttenteValidationDass() {
        return consultationRepository.findAll(ConsultationSpecifications.enAttenteValidationDass())
                .stream().map(consultationMapper::toModel).toList();
    }

    @Override
    public ConsultationModel creerBrouillon(ConsultationModel intention) {
        patientService.findById(intention.patient().id());
        MedecinModel medecinSeniorModel = medecinService.findById(intention.medecinSenior().id());
        if (medecinSeniorModel.role() != RoleMedecin.SENIOR) {
            throw new RegleGestionException("Le medecin senior doit avoir le role SENIOR (medecin " + medecinSeniorModel.id() + ")");
        }
        ConsultationModel avecDefauts = intention.toBuilder()
                .statut(StatutConsultation.BROUILLON)
                .dateConsultation(intention.dateConsultation() != null ? intention.dateConsultation() : LocalDate.now())
                .heureConsultation(intention.heureConsultation() != null ? intention.heureConsultation() : LocalTime.now())
                .build();
        ConsultationInitiale entity = consultationMapper.toNewEntity(avecDefauts);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    @Override
    public ConsultationModel mettreAJour(UUID id, ConsultationModel intention) {
        ConsultationInitiale entity = getEntityById(id);
        ConsultationModel courant = consultationMapper.toModel(entity);
        verifierModifiable(courant);

        ConsultationModel fusionne = fusionnerChampsScalaires(courant, intention);

        if (intention.facteursDeRisque() != null) {
            List<FacteurDeRisqueModel> modeles = intention.facteursDeRisque().stream()
                    .map(f -> facteurDeRisqueService.findById(f.id()))
                    .toList();
            consultationMapper.appliquerFacteursDeRisque(entity, modeles);
            fusionne = fusionne.toBuilder().facteursDeRisque(modeles).build();
        }

        if (intention.situationSociale() != null) {
            List<SituationSocialeModel> modeles = intention.situationSociale().stream()
                    .map(s -> situationSocialeService.findById(s.id()))
                    .toList();
            consultationMapper.appliquerSituationSociale(entity, modeles);
            fusionne = fusionne.toBuilder().situationSociale(modeles).build();
        }

        if (intention.medecinJuniorAffecte() != null) {
            MedecinModel juniorModel = medecinService.findById(intention.medecinJuniorAffecte().id());
            if (juniorModel.role() != RoleMedecin.JUNIOR) {
                throw new RegleGestionException("Le medecin affecte en suivi doit avoir le role JUNIOR (medecin " + juniorModel.id() + ")");
            }
            consultationMapper.appliquerMedecinJuniorAffecte(entity, juniorModel.id());
            fusionne = fusionne.toBuilder().medecinJuniorAffecte(juniorModel).build();
        }

        if (intention.signaturePointFocalDass() != null) {
            boolean nouveauEtat = intention.signaturePointFocalDass();
            boolean etaitSignee = Boolean.TRUE.equals(courant.signaturePointFocalDass());
            fusionne = fusionne.toBuilder()
                    .signaturePointFocalDass(nouveauEtat)
                    .horodatageSignaturePointFocalDass(
                            nouveauEtat ? (etaitSignee ? courant.horodatageSignaturePointFocalDass() : Instant.now()) : null)
                    .build();
        }

        if (intention.visaDirecteurSamu() != null) {
            boolean nouveauEtat = intention.visaDirecteurSamu();
            boolean etaitVise = Boolean.TRUE.equals(courant.visaDirecteurSamu());
            fusionne = fusionne.toBuilder()
                    .visaDirecteurSamu(nouveauEtat)
                    .horodatageVisaDirecteurSamu(
                            nouveauEtat ? (etaitVise ? courant.horodatageVisaDirecteurSamu() : Instant.now()) : null)
                    .build();
        }

        fusionne = validerReglesConditionnelles(fusionne);
        if (fusionne.medecinJuniorAffecte() == null) {
            consultationMapper.appliquerMedecinJuniorAffecte(entity, null);
        }

        consultationMapper.applyScalarFieldsToEntity(entity, fusionne);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    @Override
    public ConsultationModel valider(UUID id) {
        ConsultationInitiale entity = getEntityById(id);
        ConsultationModel model = consultationMapper.toModel(entity);
        if (model.statut() != StatutConsultation.BROUILLON) {
            throw new ConflitEtatException("Seule une fiche en BROUILLON peut etre validee (statut actuel : " + model.statut() + ")");
        }
        ConsultationModel misAJour = model.toBuilder().statut(StatutConsultation.VALIDEE).build();
        consultationMapper.applyScalarFieldsToEntity(entity, misAJour);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    @Override
    public ConsultationModel signer(UUID id) {
        ConsultationInitiale entity = getEntityById(id);
        ConsultationModel model = consultationMapper.toModel(entity);
        if (model.statut() != StatutConsultation.VALIDEE) {
            throw new ConflitEtatException("Seule une fiche VALIDEE peut etre signee (statut actuel : " + model.statut() + ")");
        }
        List<String> manquants = champsManquantsPourSignature(model);
        if (!manquants.isEmpty()) {
            throw new RegleGestionException("Champs obligatoires manquants avant signature : " + String.join(", ", manquants));
        }
        ConsultationModel misAJour = model.toBuilder()
                .signatureMedecinSenior(true)
                .horodatageSignatureMedecinSenior(Instant.now())
                .statut(StatutConsultation.SIGNEE)
                .build();
        consultationMapper.applyScalarFieldsToEntity(entity, misAJour);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private ConsultationInitiale getEntityById(UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation introuvable : " + id));
    }

    private void verifierModifiable(ConsultationModel model) {
        if (model.statut() == StatutConsultation.SIGNEE) {
            throw new ConflitEtatException("La fiche est signee et n'est plus modifiable");
        }
    }

    private ConsultationModel fusionnerChampsScalaires(ConsultationModel courant, ConsultationModel maj) {
        return courant.toBuilder()
                .dateConsultation(coalesce(maj.dateConsultation(), courant.dateConsultation()))
                .heureConsultation(coalesce(maj.heureConsultation(), courant.heureConsultation()))
                .origineDemande(coalesce(maj.origineDemande(), courant.origineDemande()))
                .origineDemandeAutrePrecision(coalesce(maj.origineDemandeAutrePrecision(), courant.origineDemandeAutrePrecision()))
                .motifPrincipalConsultation(coalesce(maj.motifPrincipalConsultation(), courant.motifPrincipalConsultation()))
                .precisionsMaladiesChroniques(coalesce(maj.precisionsMaladiesChroniques(), courant.precisionsMaladiesChroniques()))
                .allergiesConnues(coalesce(maj.allergiesConnues(), courant.allergiesConnues()))
                .precisionsAllergiesMedicamenteuses(coalesce(maj.precisionsAllergiesMedicamenteuses(), courant.precisionsAllergiesMedicamenteuses()))
                .hospitalisationsAnterieures(coalesce(maj.hospitalisationsAnterieures(), courant.hospitalisationsAnterieures()))
                .chirurgiesAnterieures(coalesce(maj.chirurgiesAnterieures(), courant.chirurgiesAnterieures()))
                .detailsHospitalisationsChirurgies(coalesce(maj.detailsHospitalisationsChirurgies(), courant.detailsHospitalisationsChirurgies()))
                .niveauAutonomie(coalesce(maj.niveauAutonomie(), courant.niveauAutonomie()))
                .risqueIsolement(coalesce(maj.risqueIsolement(), courant.risqueIsolement()))
                .niveauPrecarite(coalesce(maj.niveauPrecarite(), courant.niveauPrecarite()))
                .besoinsSociauxIdentifies(coalesce(maj.besoinsSociauxIdentifies(), courant.besoinsSociauxIdentifies()))
                .resumeSyndromiqueCim10(coalesce(maj.resumeSyndromiqueCim10(), courant.resumeSyndromiqueCim10()))
                .decisionEligibilite(coalesce(maj.decisionEligibilite(), courant.decisionEligibilite()))
                .motifNonEligibilite(coalesce(maj.motifNonEligibilite(), courant.motifNonEligibilite()))
                .frequenceVisitesSuivi(coalesce(maj.frequenceVisitesSuivi(), courant.frequenceVisitesSuivi()))
                .planSuiviPersonnalise(coalesce(maj.planSuiviPersonnalise(), courant.planSuiviPersonnalise()))
                .traitementPrescrit(coalesce(maj.traitementPrescrit(), courant.traitementPrescrit()))
                .examensBiologiquesPrescrits(coalesce(maj.examensBiologiquesPrescrits(), courant.examensBiologiquesPrescrits()))
                .dateProchaineConsultationSenior(coalesce(maj.dateProchaineConsultationSenior(), courant.dateProchaineConsultationSenior()))
                .delaiRecommande(coalesce(maj.delaiRecommande(), courant.delaiRecommande()))
                .nomPointFocalDass(coalesce(maj.nomPointFocalDass(), courant.nomPointFocalDass()))
                .nomDirecteurSamu(coalesce(maj.nomDirecteurSamu(), courant.nomDirecteurSamu()))
                .numeroFicheDmi(coalesce(maj.numeroFicheDmi(), courant.numeroFicheDmi()))
                .dateSaisieDmi(coalesce(maj.dateSaisieDmi(), courant.dateSaisieDmi()))
                .saisiePar(coalesce(maj.saisiePar(), courant.saisiePar()))
                .build();
    }

    private <T> T coalesce(T nouveau, T ancien) {
        return nouveau != null ? nouveau : ancien;
    }

    private ConsultationModel validerReglesConditionnelles(ConsultationModel model) {
        if (model.origineDemande() == OrigineDemande.AUTRE && isBlank(model.origineDemandeAutrePrecision())) {
            throw new RegleGestionException("La precision est obligatoire quand l'origine de la demande est 'Autre'");
        }
        boolean hospiOuChirurgie = Boolean.TRUE.equals(model.hospitalisationsAnterieures())
                || Boolean.TRUE.equals(model.chirurgiesAnterieures());
        if (hospiOuChirurgie && isBlank(model.detailsHospitalisationsChirurgies())) {
            throw new RegleGestionException("Le detail est obligatoire en cas d'hospitalisation ou de chirurgie anterieure");
        }
        ConsultationModel resultat = model;
        if (model.decisionEligibilite() == DecisionEligibilite.NON_ELIGIBLE) {
            if (isBlank(model.motifNonEligibilite())) {
                throw new RegleGestionException("Le motif de non-eligibilite est obligatoire quand la decision est NON_ELIGIBLE");
            }
            resultat = resultat.toBuilder().medecinJuniorAffecte(null).build();
        }
        if (resultat.decisionEligibilite() == DecisionEligibilite.ELIGIBLE && resultat.medecinJuniorAffecte() == null) {
            throw new RegleGestionException("L'affectation a un medecin junior est obligatoire quand la decision est ELIGIBLE");
        }
        return resultat;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> champsManquantsPourSignature(ConsultationModel c) {
        List<String> manquants = new ArrayList<>();
        if (c.dateConsultation() == null) {
            manquants.add("dateConsultation");
        }
        if (c.heureConsultation() == null) {
            manquants.add("heureConsultation");
        }
        if (c.origineDemande() == null) {
            manquants.add("origineDemande");
        }
        if (isBlank(c.motifPrincipalConsultation())) {
            manquants.add("motifPrincipalConsultation");
        }
        if (c.niveauAutonomie() == null) {
            manquants.add("niveauAutonomie");
        }
        if (c.risqueIsolement() == null) {
            manquants.add("risqueIsolement");
        }
        if (c.niveauPrecarite() == null) {
            manquants.add("niveauPrecarite");
        }
        if (isBlank(c.resumeSyndromiqueCim10())) {
            manquants.add("resumeSyndromiqueCim10");
        }
        if (c.decisionEligibilite() == null) {
            manquants.add("decisionEligibilite");
        }
        if (c.examenParAppareil() == null) {
            manquants.add("examenParAppareil");
        }
        if (c.constantesVitales() == null || c.constantesVitales().isEmpty()) {
            manquants.add("constantesVitales (au moins une)");
        }
        return manquants;
    }
}
