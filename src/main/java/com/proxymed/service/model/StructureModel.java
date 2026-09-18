package com.proxymed.service.model;

import lombok.Builder;

/**
 * Couche interne neutre : ni le contrat API (StructureRequest/Response), ni l'entite JPA.
 * C'est ce type que le service manipule.
 */
@Builder(toBuilder = true)
public record StructureModel(
        Long id,
        String libelle,
        boolean actif
) {
}
