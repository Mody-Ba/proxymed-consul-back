package com.proxymed.service.model;

import lombok.Builder;

/**
 * Couche interne neutre : ni le contrat API (AntecedentMaladieRequest/Response), ni l'entite JPA.
 * maladieChroniqueLibelle n'est renseigne que sur le chemin de lecture (Entity -> Model).
 */
@Builder(toBuilder = true)
public record AntecedentMaladieModel(
        Long id,
        Long maladieChroniqueId,
        String maladieChroniqueLibelle,
        String precision
) {
}
