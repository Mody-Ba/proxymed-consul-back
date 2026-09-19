package com.proxymed.service.mappers;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.SituationSociale;
import com.proxymed.repository.FacteurDeRisqueRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.repository.SituationSocialeRepository;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.FacteurDeRisqueModel;
import com.proxymed.service.model.SituationSocialeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper DB : convertit uniquement entre ConsultationModel et l'entite JPA ConsultationInitiale.
 * Ne doit jamais connaitre ConsultationCreateRequest/UpdateRequest/Response.
 *
 * Resout lui-meme les references JPA vers les autres aggregats (patient, medecins, facteurs
 * de risque, situation sociale) via getReferenceById sur leurs repositories respectifs :
 * simple traduction d'un id en relation, jamais un appel metier. Le service appelant ne
 * manipule ainsi jamais ces entites, seulement leurs id (ou les Model renvoyes par les
 * services correspondants, deja valides en amont).
 */
@Component("dbConsultationMapper")
@RequiredArgsConstructor
public class ConsultationMapper {

    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final FacteurDeRisqueRepository facteurDeRisqueRepository;
    private final SituationSocialeRepository situationSocialeRepository;
    private final PatientMapper patientMapper;
    private final MedecinMapper medecinMapper;
    private final FacteurDeRisqueMapper facteurDeRisqueMapper;
    private final SituationSocialeMapper situationSocialeMapper;
    private final ConstanteVitaleMapper constanteVitaleMapper;
    private final ExamenParAppareilMapper examenParAppareilMapper;
    private final AntecedentMaladieMapper antecedentMaladieMapper;

    public ConsultationInitiale toNewEntity(ConsultationModel model) {
        return ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(model.patient().id()))
                .medecinSenior(medecinRepository.getReferenceById(model.medecinSenior().id()))
                .statut(model.statut())
                .dateConsultation(model.dateConsultation())
                .heureConsultation(model.heureConsultation())
                .saisiePar(model.saisiePar())
                .build();
    }

    /**
     * Applique les champs scalaires (hors relations) du modele sur une entite managee
     * existante (mutation en place, necessaire pour qu'Hibernate suive les changements).
     * Les relations (facteurs de risque, situation sociale, medecin junior) se font via
     * appliquerFacteursDeRisque/appliquerSituationSociale/appliquerMedecinJuniorAffecte,
     * pour ne les toucher que quand le service decide explicitement de les modifier.
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

    public void appliquerFacteursDeRisque(ConsultationInitiale entity, List<FacteurDeRisqueModel> modeles) {
        List<FacteurDeRisque> references = new ArrayList<>();
        for (FacteurDeRisqueModel m : modeles) {
            references.add(facteurDeRisqueRepository.getReferenceById(m.id()));
        }
        entity.setFacteursDeRisque(references);
    }

    public void appliquerSituationSociale(ConsultationInitiale entity, List<SituationSocialeModel> modeles) {
        List<SituationSociale> references = new ArrayList<>();
        for (SituationSocialeModel m : modeles) {
            references.add(situationSocialeRepository.getReferenceById(m.id()));
        }
        entity.setSituationSociale(references);
    }

    public void appliquerMedecinJuniorAffecte(ConsultationInitiale entity, Long medecinJuniorId) {
        entity.setMedecinJuniorAffecte(medecinJuniorId != null ? medecinRepository.getReferenceById(medecinJuniorId) : null);
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
