package com.proxymed.entity;

import com.proxymed.enums.StatutDemandeConsultation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Demande de consultation initiale. demandeurId, medecinSeniorId et juniorId sont des UUID de
 * comptes du service d'authentification (claim "sub" du badge) : la Fiche 1 n'a pas de table
 * d'utilisateurs, donc aucune relation JPA. consultationId reference une consultation initiale
 * (cle etrangere en base, sans relation JPA : ce service ne manipule que sa propre entite).
 * version protege contre deux decisions (ou deux commencements) simultanes.
 */
@Entity
@Table(name = "demande_consultation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "nom_patient", nullable = false, length = 200)
    private String nomPatient;

    @Column(name = "pour_proche", nullable = false)
    private boolean pourProche;

    @Column(nullable = false, length = 2000)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemandeConsultation statut;

    @Column(name = "medecin_senior_id")
    private UUID medecinSeniorId;

    @Column(name = "junior_id")
    private UUID juniorId;

    @Column(name = "motif_refus", length = 1000)
    private String motifRefus;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_decision")
    private Instant dateDecision;

    /** Consultation initiale (Fiche 1) commencee par le junior assigne ; null tant qu'elle ne l'est pas. */
    @Column(name = "consultation_id")
    private UUID consultationId;

    @Version
    private Long version;
}
