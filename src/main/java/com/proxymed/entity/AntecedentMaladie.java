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

/**
 * Table de jointure entre une consultation et une maladie chronique du referentiel,
 * avec une precision libre par antecedent (ex. date de diagnostic, severite).
 */
@Entity
@Table(name = "antecedent_maladie")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntecedentMaladie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private ConsultationInitiale consultation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maladie_chronique_id", nullable = false)
    private MaladieChronique maladieChronique;

    @Column(columnDefinition = "TEXT")
    private String precision;
}
