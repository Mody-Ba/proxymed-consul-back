package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.entity.SituationSociale;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.service.mappers.FacteurDeRisqueMapper;
import com.proxymed.service.mappers.MedecinMapper;
import com.proxymed.service.mappers.SituationSocialeMapper;
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
 * Travaille avec ConsultationModel (couche interne neutre) comme type de travail :
 * l'entite JPA n'est touchee qu'aux frontieres des appels repository (chargement,
 * rattachement de relations, sauvegarde), jamais pour la logique metier elle-meme.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final PatientService patientService;
    private final MedecinService medecinService;
    private final FacteurDeRisqueService facteurDeRisqueService;
    private final SituationSocialeService situationSocialeService;
    private final com.proxymed.service.mappers.ConsultationMapper consultationMapper;
    private final MedecinMapper medecinMapper;
    private final FacteurDeRisqueMapper facteurDeRisqueMapper;
    private final SituationSocialeMapper situationSocialeMapper;

    public ConsultationInitiale getEntityById(UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation introuvable : " + id));
    }

    /**
     * A utiliser par les services des sous-ressources (constantes vitales, antecedents, etc.)
     * qui doivent charger la consultation parente et refuser toute modification
     * si la fiche est deja signee.
     */
    public ConsultationInitiale getModifiableEntityById(UUID id) {
        ConsultationInitiale consultation = getEntityById(id);
        verifierModifiable(consultation);
        return consultation;
    }

    public ConsultationModel findById(UUID id) {
        return consultationMapper.toModel(getEntityById(id));
    }

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

    public List<ConsultationModel> fichesEnAttenteValidationDass() {
        return consultationRepository.findAll(ConsultationSpecifications.enAttenteValidationDass())
                .stream().map(consultationMapper::toModel).toList();
    }

    public ConsultationModel creerBrouillon(ConsultationModel intention) {
        Patient patient = patientService.getEntityById(intention.patient().id());
        Medecin medecinSenior = medecinService.getEntityById(intention.medecinSenior().id());
        if (medecinSenior.getRole() != RoleMedecin.SENIOR) {
            throw new RegleGestionException("Le medecin senior doit avoir le role SENIOR (medecin " + medecinSenior.getId() + ")");
        }
        ConsultationModel avecDefauts = intention.toBuilder()
                .statut(StatutConsultation.BROUILLON)
                .dateConsultation(intention.dateConsultation() != null ? intention.dateConsultation() : LocalDate.now())
                .heureConsultation(intention.heureConsultation() != null ? intention.heureConsultation() : LocalTime.now())
                .build();
        ConsultationInitiale entity = consultationMapper.toNewEntity(avecDefauts, patient, medecinSenior);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    public ConsultationModel mettreAJour(UUID id, ConsultationModel intention) {
        ConsultationInitiale entity = getModifiableEntityById(id);
        ConsultationModel courant = consultationMapper.toModel(entity);

        ConsultationModel fusionne = fusionnerChampsScalaires(courant, intention);

        if (intention.facteursDeRisque() != null) {
            List<FacteurDeRisque> entites = intention.facteursDeRisque().stream()
                    .map(f -> facteurDeRisqueService.getEntityById(f.id()))
                    .toList();
            entity.setFacteursDeRisque(new ArrayList<>(entites));
            fusionne = fusionne.toBuilder().facteursDeRisque(facteurDeRisqueMapper.toModelList(entites)).build();
        }

        if (intention.situationSociale() != null) {
            List<SituationSociale> entites = intention.situationSociale().stream()
                    .map(s -> situationSocialeService.getEntityById(s.id()))
                    .toList();
            entity.setSituationSociale(new ArrayList<>(entites));
            fusionne = fusionne.toBuilder().situationSociale(situationSocialeMapper.toModelList(entites)).build();
        }

        if (intention.medecinJuniorAffecte() != null) {
            Medecin junior = medecinService.getEntityById(intention.medecinJuniorAffecte().id());
            if (junior.getRole() != RoleMedecin.JUNIOR) {
                throw new RegleGestionException("Le medecin affecte en suivi doit avoir le role JUNIOR (medecin " + junior.getId() + ")");
            }
            entity.setMedecinJuniorAffecte(junior);
            fusionne = fusionne.toBuilder().medecinJuniorAffecte(medecinMapper.toModel(junior)).build();
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
            entity.setMedecinJuniorAffecte(null);
        }

        consultationMapper.applyScalarFieldsToEntity(entity, fusionne);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    public ConsultationModel valider(UUID id) {
        ConsultationInitiale entity = getEntityById(id);
        if (entity.getStatut() != StatutConsultation.BROUILLON) {
            throw new ConflitEtatException("Seule une fiche en BROUILLON peut etre validee (statut actuel : " + entity.getStatut() + ")");
        }
        entity.setStatut(StatutConsultation.VALIDEE);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    public ConsultationModel signer(UUID id) {
        ConsultationInitiale entity = getEntityById(id);
        if (entity.getStatut() != StatutConsultation.VALIDEE) {
            throw new ConflitEtatException("Seule une fiche VALIDEE peut etre signee (statut actuel : " + entity.getStatut() + ")");
        }
        List<String> manquants = champsManquantsPourSignature(consultationMapper.toModel(entity));
        if (!manquants.isEmpty()) {
            throw new RegleGestionException("Champs obligatoires manquants avant signature : " + String.join(", ", manquants));
        }
        entity.setSignatureMedecinSenior(true);
        entity.setHorodatageSignatureMedecinSenior(Instant.now());
        entity.setStatut(StatutConsultation.SIGNEE);
        return consultationMapper.toModel(consultationRepository.save(entity));
    }

    private void verifierModifiable(ConsultationInitiale consultation) {
        if (consultation.getStatut() == StatutConsultation.SIGNEE) {
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
