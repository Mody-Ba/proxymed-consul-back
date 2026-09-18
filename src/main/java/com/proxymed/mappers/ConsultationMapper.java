package com.proxymed.mappers;

import com.proxymed.service.model.ConsultationCreateRequest;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ConsultationResponse;
import com.proxymed.service.model.ConsultationSummaryResponse;
import com.proxymed.service.model.ConsultationUpdateRequest;
import com.proxymed.service.model.FacteurDeRisqueModel;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.PatientModel;
import com.proxymed.service.model.SituationSocialeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement ConsultationCreateRequest/UpdateRequest <-> ConsultationModel
 * et ConsultationModel -> ConsultationResponse/ConsultationSummaryResponse.
 * Ne doit jamais connaitre l'entite JPA ConsultationInitiale.
 *
 * Pour une intention de mise a jour, les relations (facteurs de risque, situation sociale,
 * medecin junior) ne portent que l'id dans le modele produit ici : c'est au service de
 * resoudre ces id en objets complets via ses propres repositories/services.
 */
@Component("apiConsultationMapper")
@RequiredArgsConstructor
public class ConsultationMapper {

    private final PatientMapper patientMapper;
    private final MedecinMapper medecinMapper;
    private final FacteurDeRisqueMapper facteurDeRisqueMapper;
    private final SituationSocialeMapper situationSocialeMapper;
    private final ConstanteVitaleMapper constanteVitaleMapper;
    private final ExamenParAppareilMapper examenParAppareilMapper;
    private final AntecedentMaladieMapper antecedentMaladieMapper;

    public ConsultationModel toModel(ConsultationCreateRequest req) {
        return ConsultationModel.builder()
                .patient(PatientModel.builder().id(req.patientId()).build())
                .medecinSenior(MedecinModel.builder().id(req.medecinSeniorId()).build())
                .dateConsultation(req.dateConsultation())
                .heureConsultation(req.heureConsultation())
                .saisiePar(req.saisiePar())
                .build();
    }

    /**
     * Intention de mise a jour partielle : les champs absents de la requete restent null
     * dans le modele produit (le service se charge de les fusionner avec l'etat courant).
     */
    public ConsultationModel toModel(ConsultationUpdateRequest req) {
        return ConsultationModel.builder()
                .dateConsultation(req.dateConsultation())
                .heureConsultation(req.heureConsultation())

                .origineDemande(req.origineDemande())
                .origineDemandeAutrePrecision(req.origineDemandeAutrePrecision())
                .motifPrincipalConsultation(req.motifPrincipalConsultation())

                .precisionsMaladiesChroniques(req.precisionsMaladiesChroniques())
                .allergiesConnues(req.allergiesConnues())
                .precisionsAllergiesMedicamenteuses(req.precisionsAllergiesMedicamenteuses())
                .hospitalisationsAnterieures(req.hospitalisationsAnterieures())
                .chirurgiesAnterieures(req.chirurgiesAnterieures())
                .detailsHospitalisationsChirurgies(req.detailsHospitalisationsChirurgies())

                .facteursDeRisque(facteurStubs(req.facteursDeRisqueIds()))
                .situationSociale(situationStubs(req.situationSocialeIds()))

                .niveauAutonomie(req.niveauAutonomie())
                .risqueIsolement(req.risqueIsolement())
                .niveauPrecarite(req.niveauPrecarite())
                .besoinsSociauxIdentifies(req.besoinsSociauxIdentifies())

                .resumeSyndromiqueCim10(req.resumeSyndromiqueCim10())
                .decisionEligibilite(req.decisionEligibilite())
                .motifNonEligibilite(req.motifNonEligibilite())
                .medecinJuniorAffecte(req.medecinJuniorAffecteId() != null
                        ? MedecinModel.builder().id(req.medecinJuniorAffecteId()).build() : null)
                .frequenceVisitesSuivi(req.frequenceVisitesSuivi())
                .planSuiviPersonnalise(req.planSuiviPersonnalise())
                .traitementPrescrit(req.traitementPrescrit())
                .examensBiologiquesPrescrits(req.examensBiologiquesPrescrits())
                .dateProchaineConsultationSenior(req.dateProchaineConsultationSenior())
                .delaiRecommande(req.delaiRecommande())

                .signaturePointFocalDass(req.signaturePointFocalDass())
                .nomPointFocalDass(req.nomPointFocalDass())
                .visaDirecteurSamu(req.visaDirecteurSamu())
                .nomDirecteurSamu(req.nomDirecteurSamu())
                .numeroFicheDmi(req.numeroFicheDmi())
                .dateSaisieDmi(req.dateSaisieDmi())
                .saisiePar(req.saisiePar())
                .build();
    }

    public ConsultationSummaryResponse toSummaryResponse(ConsultationModel model) {
        MedecinModel junior = model.medecinJuniorAffecte();
        return ConsultationSummaryResponse.builder()
                .id(model.id())
                .patientNomComplet(model.patient().nomComplet())
                .patientNumeroDossierProxymed(model.patient().numeroDossierProxymed())
                .patientNumeroDmi(model.patient().numeroDmi())
                .medecinSeniorNomComplet(model.medecinSenior().nom() + " " + model.medecinSenior().prenom())
                .medecinJuniorAffecteNomComplet(junior != null ? junior.nom() + " " + junior.prenom() : null)
                .dateConsultation(model.dateConsultation())
                .statut(model.statut())
                .decisionEligibilite(model.decisionEligibilite())
                .build();
    }

    public ConsultationResponse toResponse(ConsultationModel model) {
        return ConsultationResponse.builder()
                .id(model.id())
                .patient(patientMapper.toResponse(model.patient()))
                .medecinSenior(medecinMapper.toResponse(model.medecinSenior()))
                .dateConsultation(model.dateConsultation())
                .heureConsultation(model.heureConsultation())
                .statut(model.statut())

                .origineDemande(model.origineDemande())
                .origineDemandeAutrePrecision(model.origineDemandeAutrePrecision())
                .motifPrincipalConsultation(model.motifPrincipalConsultation())

                .maladiesChroniques(antecedentMaladieMapper.toResponseList(model.maladiesChroniques()))
                .precisionsMaladiesChroniques(model.precisionsMaladiesChroniques())
                .allergiesConnues(model.allergiesConnues())
                .precisionsAllergiesMedicamenteuses(model.precisionsAllergiesMedicamenteuses())
                .hospitalisationsAnterieures(model.hospitalisationsAnterieures())
                .chirurgiesAnterieures(model.chirurgiesAnterieures())
                .detailsHospitalisationsChirurgies(model.detailsHospitalisationsChirurgies())

                .facteursDeRisque(facteurDeRisqueMapper.toResponseList(model.facteursDeRisque()))
                .situationSociale(situationSocialeMapper.toResponseList(model.situationSociale()))

                .constantesVitales(constanteVitaleMapper.toResponseList(model.constantesVitales()))
                .examenParAppareil(examenParAppareilMapper.toResponse(model.examenParAppareil()))

                .niveauAutonomie(model.niveauAutonomie())
                .risqueIsolement(model.risqueIsolement())
                .niveauPrecarite(model.niveauPrecarite())
                .besoinsSociauxIdentifies(model.besoinsSociauxIdentifies())

                .resumeSyndromiqueCim10(model.resumeSyndromiqueCim10())
                .decisionEligibilite(model.decisionEligibilite())
                .motifNonEligibilite(model.motifNonEligibilite())
                .medecinJuniorAffecte(model.medecinJuniorAffecte() != null ? medecinMapper.toResponse(model.medecinJuniorAffecte()) : null)
                .frequenceVisitesSuivi(model.frequenceVisitesSuivi())
                .planSuiviPersonnalise(model.planSuiviPersonnalise())
                .traitementPrescrit(model.traitementPrescrit())
                .examensBiologiquesPrescrits(model.examensBiologiquesPrescrits())
                .dateProchaineConsultationSenior(model.dateProchaineConsultationSenior())
                .delaiRecommande(model.delaiRecommande())

                .signatureMedecinSenior(Boolean.TRUE.equals(model.signatureMedecinSenior()))
                .horodatageSignatureMedecinSenior(model.horodatageSignatureMedecinSenior())
                .signaturePointFocalDass(Boolean.TRUE.equals(model.signaturePointFocalDass()))
                .horodatageSignaturePointFocalDass(model.horodatageSignaturePointFocalDass())
                .nomPointFocalDass(model.nomPointFocalDass())
                .visaDirecteurSamu(Boolean.TRUE.equals(model.visaDirecteurSamu()))
                .horodatageVisaDirecteurSamu(model.horodatageVisaDirecteurSamu())
                .nomDirecteurSamu(model.nomDirecteurSamu())

                .numeroFicheDmi(model.numeroFicheDmi())
                .dateSaisieDmi(model.dateSaisieDmi())
                .saisiePar(model.saisiePar())
                .build();
    }

    private List<FacteurDeRisqueModel> facteurStubs(List<Long> ids) {
        return ids == null ? null : ids.stream().map(id -> FacteurDeRisqueModel.builder().id(id).build()).toList();
    }

    private List<SituationSocialeModel> situationStubs(List<Long> ids) {
        return ids == null ? null : ids.stream().map(id -> SituationSocialeModel.builder().id(id).build()).toList();
    }
}
