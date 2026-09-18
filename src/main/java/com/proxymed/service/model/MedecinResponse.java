package com.proxymed.service.model;

import com.proxymed.enums.RoleMedecin;
import lombok.Builder;

@Builder(toBuilder = true)
public record MedecinResponse(
        Long id,
        String nom,
        String prenom,
        String numeroOrdre,
        RoleMedecin role,
        Long structureRattachementId,
        String structureRattachementLibelle
) {
}
