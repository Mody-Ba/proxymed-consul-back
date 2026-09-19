package com.proxymed.entity;

import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "numero_dossier_proxymed", nullable = false, unique = true)
    private String numeroDossierProxymed;

    @Column(name = "numero_dmi", unique = true)
    private String numeroDmi;

    @Column(name = "nom_complet", nullable = false)
    private String nomComplet;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Sexe sexe;

    private String telephone;

    @Column(name = "adresse_domicile")
    private String adresseDomicile;

    private String commune;

    private String quartier;

    @Column(name = "personne_a_contacter_nom")
    private String personneAContacterNom;

    @Column(name = "personne_a_contacter_telephone")
    private String personneAContacterTelephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "couverture_sociale")
    private CouvertureSociale couvertureSociale;
}
