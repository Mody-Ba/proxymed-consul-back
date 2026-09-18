package com.proxymed.service.model;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.ExamensBiologiquesPrescrits;
import com.proxymed.enums.FrequenceVisites;
import com.proxymed.enums.NiveauAutonomie;
import com.proxymed.enums.NiveauPrecarite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RisqueIsolement;
import com.proxymed.enums.StatutConsultation;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record ConsultationResponse(
        UUID id,
        PatientResponse patient,
        MedecinResponse medecinSenior,
        LocalDate dateConsultation,
        LocalTime heureConsultation,
        StatutConsultation statut,

        OrigineDemande origineDemande,
        String origineDemandeAutrePrecision,
        String motifPrincipalConsultation,

        List<AntecedentMaladieResponse> maladiesChroniques,
        String precisionsMaladiesChroniques,
        Boolean allergiesConnues,
        String precisionsAllergiesMedicamenteuses,
        Boolean hospitalisationsAnterieures,
        Boolean chirurgiesAnterieures,
        String detailsHospitalisationsChirurgies,

        List<FacteurDeRisqueResponse> facteursDeRisque,
        List<SituationSocialeResponse> situationSociale,

        List<ConstanteVitaleResponse> constantesVitales,
        ExamenParAppareilResponse examenParAppareil,

        NiveauAutonomie niveauAutonomie,
        RisqueIsolement risqueIsolement,
        NiveauPrecarite niveauPrecarite,
        String besoinsSociauxIdentifies,

        String resumeSyndromiqueCim10,
        DecisionEligibilite decisionEligibilite,
        String motifNonEligibilite,
        MedecinResponse medecinJuniorAffecte,
        FrequenceVisites frequenceVisitesSuivi,
        String planSuiviPersonnalise,
        String traitementPrescrit,
        ExamensBiologiquesPrescrits examensBiologiquesPrescrits,
        LocalDate dateProchaineConsultationSenior,
        String delaiRecommande,

        boolean signatureMedecinSenior,
        Instant horodatageSignatureMedecinSenior,
        boolean signaturePointFocalDass,
        Instant horodatageSignaturePointFocalDass,
        String nomPointFocalDass,
        boolean visaDirecteurSamu,
        Instant horodatageVisaDirecteurSamu,
        String nomDirecteurSamu,

        String numeroFicheDmi,
        LocalDate dateSaisieDmi,
        String saisiePar
) {
}
