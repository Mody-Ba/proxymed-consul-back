package com.proxymed.entity;

import com.proxymed.enums.TypeConstanteVitale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Une constante vitale mesuree pendant la consultation (1..N par consultation).
 * estNormal / estAlerte sont calcules par le service metier a partir des bornes
 * par type de constante (cf. cahier des charges section 9, a confirmer avec l'utilisateur).
 * valeur est stockee en Decimal (et non String comme evoque dans le cahier) pour permettre
 * le calcul de l'IMC et la comparaison aux bornes d'alerte.
 */
@Entity
@Table(name = "constante_vitale")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstanteVitale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private ConsultationInitiale consultation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeConstanteVitale type;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal valeur;

    private LocalTime heure;

    @Column(name = "est_normal")
    private Boolean estNormal;

    @Column(name = "est_alerte")
    private Boolean estAlerte;

    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
