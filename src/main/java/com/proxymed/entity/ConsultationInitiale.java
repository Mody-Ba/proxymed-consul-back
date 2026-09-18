package com.proxymed.entity;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.ExamensBiologiquesPrescrits;
import com.proxymed.enums.FrequenceVisites;
import com.proxymed.enums.NiveauAutonomie;
import com.proxymed.enums.NiveauPrecarite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RisqueIsolement;
import com.proxymed.enums.StatutConsultation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entite racine de la Fiche 1 (evaluation medicale initiale d'eligibilite).
 * Les regles de gestion (obligatoires conditionnels, calcul IMC, alertes, etc.)
 * sont appliquees au niveau service, pas ici.
 */
@Entity
@Table(name = "consultation_initiale")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationInitiale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medecin_senior_id", nullable = false)
    private Medecin medecinSenior;

    @Column(name = "date_consultation")
    private LocalDate dateConsultation;

    @Column(name = "heure_consultation")
    private LocalTime heureConsultation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutConsultation statut = StatutConsultation.BROUILLON;

    // --- Section 2 : Motif de la demande ---

    @Enumerated(EnumType.STRING)
    @Column(name = "origine_demande")
    private OrigineDemande origineDemande;

    @Column(name = "origine_demande_autre_precision")
    private String origineDemandeAutrePrecision;

    @Column(name = "motif_principal_consultation", columnDefinition = "TEXT")
    private String motifPrincipalConsultation;

    // --- Section 3 : Antecedents medicaux et chirurgicaux ---

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AntecedentMaladie> maladiesChroniques = new ArrayList<>();

    @Column(name = "precisions_maladies_chroniques", columnDefinition = "TEXT")
    private String precisionsMaladiesChroniques;

    @Column(name = "allergies_connues")
    private Boolean allergiesConnues;

    @Column(name = "precisions_allergies_medicamenteuses", columnDefinition = "TEXT")
    private String precisionsAllergiesMedicamenteuses;

    @Column(name = "hospitalisations_anterieures")
    private Boolean hospitalisationsAnterieures;

    @Column(name = "chirurgies_anterieures")
    private Boolean chirurgiesAnterieures;

    @Column(name = "details_hospitalisations_chirurgies", columnDefinition = "TEXT")
    private String detailsHospitalisationsChirurgies;

    // --- Section 4 : Habitudes de vie et facteurs de risque ---

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "consultation_facteur_de_risque",
            joinColumns = @JoinColumn(name = "consultation_id"),
            inverseJoinColumns = @JoinColumn(name = "facteur_de_risque_id"))
    @Builder.Default
    private List<FacteurDeRisque> facteursDeRisque = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "consultation_situation_sociale",
            joinColumns = @JoinColumn(name = "consultation_id"),
            inverseJoinColumns = @JoinColumn(name = "situation_sociale_id"))
    @Builder.Default
    private List<SituationSociale> situationSociale = new ArrayList<>();

    // --- Section 6 : Evaluation sociale ---

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_autonomie")
    private NiveauAutonomie niveauAutonomie;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_isolement")
    private RisqueIsolement risqueIsolement;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_precarite")
    private NiveauPrecarite niveauPrecarite;

    @Column(name = "besoins_sociaux_identifies", columnDefinition = "TEXT")
    private String besoinsSociauxIdentifies;

    // --- Section 7 : Diagnostic et plan de suivi ---

    @Column(name = "resume_syndromique_cim10", columnDefinition = "TEXT")
    private String resumeSyndromiqueCim10;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_eligibilite")
    private DecisionEligibilite decisionEligibilite;

    @Column(name = "motif_non_eligibilite", columnDefinition = "TEXT")
    private String motifNonEligibilite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_junior_affecte_id")
    private Medecin medecinJuniorAffecte;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequence_visites_suivi")
    private FrequenceVisites frequenceVisitesSuivi;

    @Column(name = "plan_suivi_personnalise", columnDefinition = "TEXT")
    private String planSuiviPersonnalise;

    @Column(name = "traitement_prescrit", columnDefinition = "TEXT")
    private String traitementPrescrit;

    @Enumerated(EnumType.STRING)
    @Column(name = "examens_biologiques_prescrits")
    private ExamensBiologiquesPrescrits examensBiologiquesPrescrits;

    @Column(name = "date_prochaine_consultation_senior")
    private LocalDate dateProchaineConsultationSenior;

    @Column(name = "delai_recommande")
    private String delaiRecommande;

    // --- Section 8 : Validation et signature ---

    @Column(name = "signature_medecin_senior", nullable = false)
    @Builder.Default
    private boolean signatureMedecinSenior = false;

    @Column(name = "horodatage_signature_medecin_senior")
    private Instant horodatageSignatureMedecinSenior;

    @Column(name = "signature_point_focal_dass", nullable = false)
    @Builder.Default
    private boolean signaturePointFocalDass = false;

    @Column(name = "horodatage_signature_point_focal_dass")
    private Instant horodatageSignaturePointFocalDass;

    @Column(name = "nom_point_focal_dass")
    private String nomPointFocalDass;

    @Column(name = "visa_directeur_samu", nullable = false)
    @Builder.Default
    private boolean visaDirecteurSamu = false;

    @Column(name = "horodatage_visa_directeur_samu")
    private Instant horodatageVisaDirecteurSamu;

    @Column(name = "nom_directeur_samu")
    private String nomDirecteurSamu;

    @Column(name = "numero_fiche_dmi")
    private String numeroFicheDmi;

    @Column(name = "date_saisie_dmi")
    private LocalDate dateSaisieDmi;

    @Column(name = "saisie_par")
    private String saisiePar;

    // --- Section 5 : Examen clinique initial ---

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConstanteVitale> constantesVitales = new ArrayList<>();

    @OneToOne(mappedBy = "consultation", cascade = CascadeType.ALL, orphanRemoval = true)
    private ExamenParAppareil examenParAppareil;
}
