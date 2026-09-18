package com.proxymed.service.model;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.ExamensBiologiquesPrescrits;
import com.proxymed.enums.FrequenceVisites;
import com.proxymed.enums.NiveauAutonomie;
import com.proxymed.enums.NiveauPrecarite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RisqueIsolement;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Mise a jour partielle de la fiche (une ou plusieurs sections 2 a 8 a la fois).
 * Un champ absent/null n'est pas modifie ; pour vider une liste (ex. plus aucun facteur
 * de risque), il faut envoyer une liste vide explicite plutot que null.
 */
@Builder(toBuilder = true)
public record ConsultationUpdateRequest(

        LocalDate dateConsultation,
        LocalTime heureConsultation,

        // Section 2 : motif de la demande
        OrigineDemande origineDemande,
        String origineDemandeAutrePrecision,
        String motifPrincipalConsultation,

        // Section 3 : antecedents medicaux et chirurgicaux
        // (la liste des maladies chroniques se gere via /api/consultations/{id}/antecedents-maladies)
        String precisionsMaladiesChroniques,
        Boolean allergiesConnues,
        String precisionsAllergiesMedicamenteuses,
        Boolean hospitalisationsAnterieures,
        Boolean chirurgiesAnterieures,
        String detailsHospitalisationsChirurgies,

        // Section 4 : habitudes de vie et facteurs de risque
        List<Long> facteursDeRisqueIds,
        List<Long> situationSocialeIds,

        // Section 5 : examen clinique initial
        // (constantes vitales et examen par appareil geres via leurs endpoints dedies)

        // Section 6 : evaluation sociale
        NiveauAutonomie niveauAutonomie,
        RisqueIsolement risqueIsolement,
        NiveauPrecarite niveauPrecarite,
        String besoinsSociauxIdentifies,

        // Section 7 : diagnostic et plan de suivi
        String resumeSyndromiqueCim10,
        DecisionEligibilite decisionEligibilite,
        String motifNonEligibilite,
        Long medecinJuniorAffecteId,
        FrequenceVisites frequenceVisitesSuivi,
        String planSuiviPersonnalise,
        String traitementPrescrit,
        ExamensBiologiquesPrescrits examensBiologiquesPrescrits,
        LocalDate dateProchaineConsultationSenior,
        String delaiRecommande,

        // Section 8 : validation (hors signature medecin senior, geree par /signer)
        Boolean signaturePointFocalDass,
        String nomPointFocalDass,
        Boolean visaDirecteurSamu,
        String nomDirecteurSamu,
        String numeroFicheDmi,
        LocalDate dateSaisieDmi,
        String saisiePar
) {
}
