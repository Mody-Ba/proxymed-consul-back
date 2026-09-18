package com.proxymed.service.mappers;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.service.model.ConsultationModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre ConsultationModel et l'entite JPA ConsultationInitiale.
 * Ne doit jamais connaitre ConsultationCreateRequest/UpdateRequest/Response.
 *
 * Les relations (patient, medecins, facteurs de risque, situation sociale) sont fournies
 * par le service sous forme d'entites deja chargees via leurs repositories respectifs :
 * ce mapper ne fait lui-meme aucun appel repository.
 */
@Component("dbConsultationMapper")
@RequiredArgsConstructor
public class ConsultationMapper {

    private final PatientMapper patientMapper;
    private final MedecinMapper medecinMapper;
    private final FacteurDeRisqueMapper facteurDeRisqueMapper;
    private final SituationSocialeMapper situationSocialeMapper;
    private final ConstanteVitaleMapper constanteVitaleMapper;
    private final ExamenParAppareilMapper examenParAppareilMapper;
    private final AntecedentMaladieMapper antecedentMaladieMapper;

    public ConsultationInitiale toNewEntity(ConsultationModel model, Patient patient, Medecin medecinSenior) {
        return ConsultationInitiale.builder()
                .patient(patient)
                .medecinSenior(medecinSenior)
                .statut(model.statut())
                .dateConsultation(model.dateConsultation())
                .heureConsultation(model.heureConsultation())
                .saisiePar(model.saisiePar())
                .build();
    }

    /**
     * Applique les champs scalaires (hors relations) du modele sur une entite managee
     * existante (mutation en place, necessaire pour qu'Hibernate suive les changements).
     * Les relations (facteurs de risque, situation sociale, medecin junior) sont a la
     * charge du service, qui doit les affecter lui-meme sur l'entite avec des references
     * chargees via ses repositories.
     */
    public void applyScalarFieldsToEntity(ConsultationInitiale entity, ConsultationModel model) {
        entity.setDateConsultation(model.dateConsultation());
        entity.setHeureConsultation(model.heureConsultation());
        entity.setStatut(model.statut());

        entity.setOrigineDemande(model.origineDemande());
        entity.setOrigineDemandeAutrePrecision(model.origineDemandeAutrePrecision());
        entity.setMotifPrincipalConsultation(model.motifPrincipalConsultation());

        entity.setPrecisionsMaladiesChroniques(model.precisionsMaladiesChroniques());
        entity.setAllergiesConnues(model.allergiesConnues());
        entity.setPrecisionsAllergiesMedicamenteuses(model.precisionsAllergiesMedicamenteuses());
        entity.setHospitalisationsAnterieures(model.hospitalisationsAnterieures());
        entity.setChirurgiesAnterieures(model.chirurgiesAnterieures());
        entity.setDetailsHospitalisationsChirurgies(model.detailsHospitalisationsChirurgies());

        entity.setNiveauAutonomie(model.niveauAutonomie());
        entity.setRisqueIsolement(model.risqueIsolement());
        entity.setNiveauPrecarite(model.niveauPrecarite());
        entity.setBesoinsSociauxIdentifies(model.besoinsSociauxIdentifies());

        entity.setResumeSyndromiqueCim10(model.resumeSyndromiqueCim10());
        entity.setDecisionEligibilite(model.decisionEligibilite());
        entity.setMotifNonEligibilite(model.motifNonEligibilite());
        entity.setFrequenceVisitesSuivi(model.frequenceVisitesSuivi());
        entity.setPlanSuiviPersonnalise(model.planSuiviPersonnalise());
        entity.setTraitementPrescrit(model.traitementPrescrit());
        entity.setExamensBiologiquesPrescrits(model.examensBiologiquesPrescrits());
        entity.setDateProchaineConsultationSenior(model.dateProchaineConsultationSenior());
        entity.setDelaiRecommande(model.delaiRecommande());

        entity.setSignatureMedecinSenior(Boolean.TRUE.equals(model.signatureMedecinSenior()));
        entity.setHorodatageSignatureMedecinSenior(model.horodatageSignatureMedecinSenior());
        entity.setSignaturePointFocalDass(Boolean.TRUE.equals(model.signaturePointFocalDass()));
        entity.setHorodatageSignaturePointFocalDass(model.horodatageSignaturePointFocalDass());
        entity.setNomPointFocalDass(model.nomPointFocalDass());
        entity.setVisaDirecteurSamu(Boolean.TRUE.equals(model.visaDirecteurSamu()));
        entity.setHorodatageVisaDirecteurSamu(model.horodatageVisaDirecteurSamu());
        entity.setNomDirecteurSamu(model.nomDirecteurSamu());

        entity.setNumeroFicheDmi(model.numeroFicheDmi());
        entity.setDateSaisieDmi(model.dateSaisieDmi());
        entity.setSaisiePar(model.saisiePar());
    }

    public ConsultationModel toModel(ConsultationInitiale entity) {
        return ConsultationModel.builder()
                .id(entity.getId())
                .patient(patientMapper.toModel(entity.getPatient()))
                .medecinSenior(medecinMapper.toModel(entity.getMedecinSenior()))
                .dateConsultation(entity.getDateConsultation())
                .heureConsultation(entity.getHeureConsultation())
                .statut(entity.getStatut())

                .origineDemande(entity.getOrigineDemande())
                .origineDemandeAutrePrecision(entity.getOrigineDemandeAutrePrecision())
                .motifPrincipalConsultation(entity.getMotifPrincipalConsultation())

                .maladiesChroniques(antecedentMaladieMapper.toModelList(entity.getMaladiesChroniques()))
                .precisionsMaladiesChroniques(entity.getPrecisionsMaladiesChroniques())
                .allergiesConnues(entity.getAllergiesConnues())
                .precisionsAllergiesMedicamenteuses(entity.getPrecisionsAllergiesMedicamenteuses())
                .hospitalisationsAnterieures(entity.getHospitalisationsAnterieures())
                .chirurgiesAnterieures(entity.getChirurgiesAnterieures())
                .detailsHospitalisationsChirurgies(entity.getDetailsHospitalisationsChirurgies())

                .facteursDeRisque(facteurDeRisqueMapper.toModelList(entity.getFacteursDeRisque()))
                .situationSociale(situationSocialeMapper.toModelList(entity.getSituationSociale()))

                .constantesVitales(constanteVitaleMapper.toModelList(entity.getConstantesVitales()))
                .examenParAppareil(examenParAppareilMapper.toModel(entity.getExamenParAppareil()))

                .niveauAutonomie(entity.getNiveauAutonomie())
                .risqueIsolement(entity.getRisqueIsolement())
                .niveauPrecarite(entity.getNiveauPrecarite())
                .besoinsSociauxIdentifies(entity.getBesoinsSociauxIdentifies())

                .resumeSyndromiqueCim10(entity.getResumeSyndromiqueCim10())
                .decisionEligibilite(entity.getDecisionEligibilite())
                .motifNonEligibilite(entity.getMotifNonEligibilite())
                .medecinJuniorAffecte(entity.getMedecinJuniorAffecte() != null
                        ? medecinMapper.toModel(entity.getMedecinJuniorAffecte()) : null)
                .frequenceVisitesSuivi(entity.getFrequenceVisitesSuivi())
                .planSuiviPersonnalise(entity.getPlanSuiviPersonnalise())
                .traitementPrescrit(entity.getTraitementPrescrit())
                .examensBiologiquesPrescrits(entity.getExamensBiologiquesPrescrits())
                .dateProchaineConsultationSenior(entity.getDateProchaineConsultationSenior())
                .delaiRecommande(entity.getDelaiRecommande())

                .signatureMedecinSenior(entity.isSignatureMedecinSenior())
                .horodatageSignatureMedecinSenior(entity.getHorodatageSignatureMedecinSenior())
                .signaturePointFocalDass(entity.isSignaturePointFocalDass())
                .horodatageSignaturePointFocalDass(entity.getHorodatageSignaturePointFocalDass())
                .nomPointFocalDass(entity.getNomPointFocalDass())
                .visaDirecteurSamu(entity.isVisaDirecteurSamu())
                .horodatageVisaDirecteurSamu(entity.getHorodatageVisaDirecteurSamu())
                .nomDirecteurSamu(entity.getNomDirecteurSamu())

                .numeroFicheDmi(entity.getNumeroFicheDmi())
                .dateSaisieDmi(entity.getDateSaisieDmi())
                .saisiePar(entity.getSaisiePar())
                .build();
    }
}
