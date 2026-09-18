package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.NiveauAutonomie;
import com.proxymed.enums.NiveauPrecarite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RisqueIsolement;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeConstanteVitale;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.PatientModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Teste ConsultationService en isolation : les collaborateurs externes (repository,
 * autres services) sont mockes, mais les mappers DB (service.mappers.*) sont de vraies
 * instances (ce sont de simples convertisseurs sans dependance repository).
 */
@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private com.proxymed.repository.ConsultationRepository consultationRepository;
    @Mock
    private PatientService patientService;
    @Mock
    private MedecinService medecinService;
    @Mock
    private FacteurDeRisqueService facteurDeRisqueService;
    @Mock
    private SituationSocialeService situationSocialeService;

    private ConsultationService consultationService;

    private Medecin medecinSenior;
    private Medecin medecinJunior;
    private Patient patient;

    @BeforeEach
    void setUp() {
        com.proxymed.service.mappers.ConsultationMapper consultationDbMapper = new com.proxymed.service.mappers.ConsultationMapper(
                new com.proxymed.service.mappers.PatientMapper(),
                new com.proxymed.service.mappers.MedecinMapper(),
                new com.proxymed.service.mappers.FacteurDeRisqueMapper(),
                new com.proxymed.service.mappers.SituationSocialeMapper(),
                new com.proxymed.service.mappers.ConstanteVitaleMapper(),
                new com.proxymed.service.mappers.ExamenParAppareilMapper(),
                new com.proxymed.service.mappers.AntecedentMaladieMapper());

        consultationService = new ConsultationService(
                consultationRepository, patientService, medecinService,
                facteurDeRisqueService, situationSocialeService,
                consultationDbMapper,
                new com.proxymed.service.mappers.MedecinMapper(),
                new com.proxymed.service.mappers.FacteurDeRisqueMapper(),
                new com.proxymed.service.mappers.SituationSocialeMapper());

        medecinSenior = Medecin.builder().id(1L).nom("Diop").prenom("Awa").numeroOrdre("S-1").role(RoleMedecin.SENIOR).build();
        medecinJunior = Medecin.builder().id(2L).nom("Fall").prenom("Omar").numeroOrdre("J-1").role(RoleMedecin.JUNIOR).build();
        patient = Patient.builder().id(UUID.randomUUID()).nomComplet("Test Patient").dateNaissance(LocalDate.now().minusYears(80)).build();

        lenient().when(consultationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creerBrouillon_rejette_siMedecinNestPasSenior() {
        when(patientService.getEntityById(patient.getId())).thenReturn(patient);
        when(medecinService.getEntityById(medecinJunior.getId())).thenReturn(medecinJunior);

        ConsultationModel intention = ConsultationModel.builder()
                .patient(PatientModel.builder().id(patient.getId()).build())
                .medecinSenior(MedecinModel.builder().id(medecinJunior.getId()).build())
                .build();

        assertThatThrownBy(() -> consultationService.creerBrouillon(intention))
                .isInstanceOf(RegleGestionException.class);
    }

    @Test
    void creerBrouillon_creeUneFicheEnBrouillon() {
        when(patientService.getEntityById(patient.getId())).thenReturn(patient);
        when(medecinService.getEntityById(medecinSenior.getId())).thenReturn(medecinSenior);

        ConsultationModel intention = ConsultationModel.builder()
                .patient(PatientModel.builder().id(patient.getId()).build())
                .medecinSenior(MedecinModel.builder().id(medecinSenior.getId()).build())
                .saisiePar("Dr Diop")
                .build();

        ConsultationModel resultat = consultationService.creerBrouillon(intention);

        assertThat(resultat.statut()).isEqualTo(StatutConsultation.BROUILLON);
        assertThat(resultat.dateConsultation()).isEqualTo(LocalDate.now());
    }

    @Test
    void mettreAJour_rejette_siOrigineAutreSansPrecision() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel intention = ConsultationModel.builder().origineDemande(OrigineDemande.AUTRE).build();

        assertThatThrownBy(() -> consultationService.mettreAJour(consultation.getId(), intention))
                .isInstanceOf(RegleGestionException.class);
    }

    @Test
    void mettreAJour_accepte_siOrigineAutreAvecPrecision() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel intention = ConsultationModel.builder()
                .origineDemande(OrigineDemande.AUTRE)
                .origineDemandeAutrePrecision("precision fournie")
                .build();

        ConsultationModel resultat = consultationService.mettreAJour(consultation.getId(), intention);

        assertThat(resultat.origineDemande()).isEqualTo(OrigineDemande.AUTRE);
        assertThat(resultat.origineDemandeAutrePrecision()).isEqualTo("precision fournie");
    }

    @Test
    void mettreAJour_rejette_siNonEligibleSansMotif() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel intention = ConsultationModel.builder().decisionEligibilite(DecisionEligibilite.NON_ELIGIBLE).build();

        assertThatThrownBy(() -> consultationService.mettreAJour(consultation.getId(), intention))
                .isInstanceOf(RegleGestionException.class);
    }

    @Test
    void mettreAJour_nonEligible_desactiveLeMedecinJunior() {
        ConsultationInitiale consultation = consultationBrouillon();
        consultation.setMedecinJuniorAffecte(medecinJunior);
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel intention = ConsultationModel.builder()
                .decisionEligibilite(DecisionEligibilite.NON_ELIGIBLE)
                .motifNonEligibilite("motif valable")
                .build();

        ConsultationModel resultat = consultationService.mettreAJour(consultation.getId(), intention);

        assertThat(resultat.decisionEligibilite()).isEqualTo(DecisionEligibilite.NON_ELIGIBLE);
        assertThat(resultat.medecinJuniorAffecte()).isNull();
    }

    @Test
    void mettreAJour_rejette_siEligibleSansMedecinJunior() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel intention = ConsultationModel.builder().decisionEligibilite(DecisionEligibilite.ELIGIBLE).build();

        assertThatThrownBy(() -> consultationService.mettreAJour(consultation.getId(), intention))
                .isInstanceOf(RegleGestionException.class);
    }

    @Test
    void mettreAJour_eligibleAvecMedecinJunior_estAccepte() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));
        when(medecinService.getEntityById(medecinJunior.getId())).thenReturn(medecinJunior);

        ConsultationModel intention = ConsultationModel.builder()
                .decisionEligibilite(DecisionEligibilite.ELIGIBLE)
                .medecinJuniorAffecte(MedecinModel.builder().id(medecinJunior.getId()).build())
                .build();

        ConsultationModel resultat = consultationService.mettreAJour(consultation.getId(), intention);

        assertThat(resultat.decisionEligibilite()).isEqualTo(DecisionEligibilite.ELIGIBLE);
        assertThat(resultat.medecinJuniorAffecte().id()).isEqualTo(medecinJunior.getId());
    }

    @Test
    void mettreAJour_rejette_siFicheSignee() {
        ConsultationInitiale consultation = consultationBrouillon();
        consultation.setStatut(StatutConsultation.SIGNEE);
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel intention = ConsultationModel.builder().origineDemande(OrigineDemande.SAMU).build();

        assertThatThrownBy(() -> consultationService.mettreAJour(consultation.getId(), intention))
                .isInstanceOf(ConflitEtatException.class);
    }

    @Test
    void valider_rejette_siPasEnBrouillon() {
        ConsultationInitiale consultation = consultationBrouillon();
        consultation.setStatut(StatutConsultation.VALIDEE);
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        assertThatThrownBy(() -> consultationService.valider(consultation.getId()))
                .isInstanceOf(ConflitEtatException.class);
    }

    @Test
    void valider_passeDeBrouillonAValidee() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel resultat = consultationService.valider(consultation.getId());

        assertThat(resultat.statut()).isEqualTo(StatutConsultation.VALIDEE);
    }

    @Test
    void signer_rejette_siPasValidee() {
        ConsultationInitiale consultation = consultationBrouillon();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        assertThatThrownBy(() -> consultationService.signer(consultation.getId()))
                .isInstanceOf(ConflitEtatException.class);
    }

    @Test
    void signer_rejette_siChampsObligatoiresManquants() {
        ConsultationInitiale consultation = consultationBrouillon();
        consultation.setStatut(StatutConsultation.VALIDEE);
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        assertThatThrownBy(() -> consultationService.signer(consultation.getId()))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("decisionEligibilite");
    }

    @Test
    void signer_reussitQuandFicheComplete() {
        ConsultationInitiale consultation = ficheCompleteValidee();
        when(consultationRepository.findById(consultation.getId())).thenReturn(java.util.Optional.of(consultation));

        ConsultationModel resultat = consultationService.signer(consultation.getId());

        assertThat(resultat.statut()).isEqualTo(StatutConsultation.SIGNEE);
        assertThat(resultat.signatureMedecinSenior()).isTrue();
        assertThat(resultat.horodatageSignatureMedecinSenior()).isNotNull();
    }

    private ConsultationInitiale consultationBrouillon() {
        return ConsultationInitiale.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .medecinSenior(medecinSenior)
                .statut(StatutConsultation.BROUILLON)
                .build();
    }

    private ConsultationInitiale ficheCompleteValidee() {
        ConsultationInitiale c = consultationBrouillon();
        c.setStatut(StatutConsultation.VALIDEE);
        c.setDateConsultation(LocalDate.now());
        c.setHeureConsultation(java.time.LocalTime.NOON);
        c.setOrigineDemande(OrigineDemande.SAMU);
        c.setMotifPrincipalConsultation("motif");
        c.setNiveauAutonomie(NiveauAutonomie.TOTALEMENT_AUTONOME);
        c.setRisqueIsolement(RisqueIsolement.FAIBLE);
        c.setNiveauPrecarite(NiveauPrecarite.AUCUNE);
        c.setResumeSyndromiqueCim10("resume");
        c.setDecisionEligibilite(DecisionEligibilite.ELIGIBLE);
        c.setMedecinJuniorAffecte(medecinJunior);
        c.setExamenParAppareil(com.proxymed.entity.ExamenParAppareil.builder().consultation(c).etatGeneral("bon").build());
        c.getConstantesVitales().add(com.proxymed.entity.ConstanteVitale.builder()
                .consultation(c).type(TypeConstanteVitale.FC).valeur(BigDecimal.valueOf(80)).build());
        return c;
    }
}
