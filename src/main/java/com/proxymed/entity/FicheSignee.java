package com.proxymed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Fiche de consultation signee (PDF scanne) importee et rattachee a une consultation.
 * contenu est volontairement un byte[] sans @Lob : Hibernate le mappe ainsi sur bytea
 * (avec @Lob ce serait un oid PostgreSQL, qui ne correspondrait plus a la table).
 */
@Entity
@Table(name = "fiche_signee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FicheSignee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private ConsultationInitiale consultation;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "type_contenu", nullable = false, length = 100)
    private String typeContenu;

    @Column(name = "taille_octets", nullable = false)
    private long tailleOctets;

    @Column(name = "date_import", nullable = false)
    private Instant dateImport;

    @Column(nullable = false)
    private byte[] contenu;
}
