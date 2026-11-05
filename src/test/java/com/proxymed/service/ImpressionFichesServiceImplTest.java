package com.proxymed.service;

import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeVisite;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.PatientModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que l'admin voit sur une fiche imprimee : l'identite du patient, l'id et le type de la
 * visite, rien d'autre.
 */
class ImpressionFichesServiceImplTest {

    @Test
    void identiteSeulement_garde_lesChampsDIdentite_etRetire_laCouvertureSociale() {
        PatientModel complet = PatientModel.builder()
                .id(UUID.randomUUID()).numeroDossierProxymed("DOS-1").numeroDmi("DMI-1").nomComplet("Awa Ndiaye")
                .dateNaissance(LocalDate.of(1950, 2, 1)).age(76).sexe(Sexe.F).telephone("771112233")
                .adresseDomicile("Rue 1").commune("Dakar").quartier("Medina")
                .personneAContacterNom("Fatou Ndiaye").personneAContacterTelephone("773334455")
                .couvertureSociale(CouvertureSociale.IPM)
                .build();

        assertThat(ImpressionFichesServiceImpl.identiteSeulement(complet))
                .isEqualTo(complet.toBuilder().couvertureSociale(null).build());
    }

    @Test
    void ficheVierge_neGardeQueLIdLeStatutLeTypeEtLIdentite() {
        PatientModel identite = PatientModel.builder().id(UUID.randomUUID()).nomComplet("Awa Ndiaye").build();
        ConsultationModel visite = ConsultationModel.builder()
                .id(UUID.randomUUID()).statut(StatutConsultation.BROUILLON).typeVisite(TypeVisite.ROUTINE)
                .patient(PatientModel.builder().id(identite.id()).couvertureSociale(CouvertureSociale.CMU).build())
                .medecinSenior(MedecinModel.builder().id(1L).nom("Fall").build())
                .dateConsultation(LocalDate.now())
                .motifPrincipalConsultation("motif").resumeSyndromiqueCim10("cim10")
                .decisionEligibilite(DecisionEligibilite.ELIGIBLE).traitementPrescrit("traitement")
                .build();

        assertThat(ImpressionFichesServiceImpl.ficheVierge(visite, identite)).isEqualTo(ConsultationModel.builder()
                .id(visite.id()).statut(StatutConsultation.BROUILLON).typeVisite(TypeVisite.ROUTINE)
                .patient(identite)
                .maladiesChroniques(List.of()).facteursDeRisque(List.of())
                .situationSociale(List.of()).constantesVitales(List.of())
                .build());
    }
}
