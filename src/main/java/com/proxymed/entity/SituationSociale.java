package com.proxymed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Liste a cocher pour la situation sociale du patient (cahier des charges 4.3 / 9) :
 * table de parametrage, a l'image de FacteurDeRisque, en attendant les libelles exacts.
 */
@Entity
@Table(name = "situation_sociale")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SituationSociale extends ReferentielItem {
}
