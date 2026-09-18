package com.proxymed.entity;

import com.proxymed.enums.RoleMedecin;
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

/**
 * Le lien vers un compte utilisateur (authentification) sera ajoute en phase 2,
 * cf. cahier des charges section 5.5.
 */
@Entity
@Table(name = "medecin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medecin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "numero_ordre", nullable = false, unique = true)
    private String numeroOrdre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleMedecin role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_rattachement_id")
    private Structure structureRattachement;
}
