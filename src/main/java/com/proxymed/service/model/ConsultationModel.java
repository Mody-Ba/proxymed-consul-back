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

/**
 * Couche interne neutre : ni le contrat API (ConsultationCreateRequest/UpdateRequest/Response),
 * ni l'entite JPA ConsultationInitiale. C'est ce type que le service manipule.
 *
 * Utilise aussi comme "intention de mise a jour" partielle : dans ce cas, un champ absent
 * de la requete reste null et n'ecrase pas l'etat courant (le service fait la fusion).
 * Pour les relations, seul l'id est renseigne dans une intention de mise a jour (ex.
 * medecinJuniorAffecte = MedecinModel avec juste l'id) ; le service resout le reste.
 */
@Builder(toBuilder = true)
public record ConsultationModel(
        UUID id,
        PatientModel patient,
        MedecinModel medecinSenior,
        LocalDate dateConsultation,
        LocalTime heureConsultation,
        StatutConsultation statut,

        OrigineDemande origineDemande,
        String origineDemandeAutrePrecision,
        String motifPrincipalConsultation,

        List<AntecedentMaladieModel> maladiesChroniques,
        String precisionsMaladiesChroniques,
        Boolean allergiesConnues,
        String precisionsAllergiesMedicamenteuses,
        Boolean hospitalisationsAnterieures,
        Boolean chirurgiesAnterieures,
        String detailsHospitalisationsChirurgies,

        List<FacteurDeRisqueModel> facteursDeRisque,
        List<SituationSocialeModel> situationSociale,

        List<ConstanteVitaleModel> constantesVitales,
        ExamenParAppareilModel examenParAppareil,

        NiveauAutonomie niveauAutonomie,
        RisqueIsolement risqueIsolement,
        NiveauPrecarite niveauPrecarite,
        String besoinsSociauxIdentifies,

        String resumeSyndromiqueCim10,
        DecisionEligibilite decisionEligibilite,
        String motifNonEligibilite,
        MedecinModel medecinJuniorAffecte,
        FrequenceVisites frequenceVisitesSuivi,
        String planSuiviPersonnalise,
        String traitementPrescrit,
        ExamensBiologiquesPrescrits examensBiologiquesPrescrits,
        LocalDate dateProchaineConsultationSenior,
        String delaiRecommande,

        Boolean signatureMedecinSenior,
        Instant horodatageSignatureMedecinSenior,
        Boolean signaturePointFocalDass,
        Instant horodatageSignaturePointFocalDass,
        String nomPointFocalDass,
        Boolean visaDirecteurSamu,
        Instant horodatageVisaDirecteurSamu,
        String nomDirecteurSamu,

        String numeroFicheDmi,
        LocalDate dateSaisieDmi,
        String saisiePar
) {
}
