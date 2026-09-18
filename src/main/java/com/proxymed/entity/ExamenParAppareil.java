package com.proxymed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "examen_par_appareil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenParAppareil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false, unique = true)
    private ConsultationInitiale consultation;

    @Column(name = "etat_general", columnDefinition = "TEXT")
    private String etatGeneral;

    @Column(name = "cardio_vasculaire", columnDefinition = "TEXT")
    private String cardioVasculaire;

    @Column(columnDefinition = "TEXT")
    private String respiratoire;

    @Column(columnDefinition = "TEXT")
    private String digestif;

    @Column(columnDefinition = "TEXT")
    private String neurologique;

    @Column(name = "locomoteur_cutane_autre", columnDefinition = "TEXT")
    private String locomoteurCutaneAutre;
}
